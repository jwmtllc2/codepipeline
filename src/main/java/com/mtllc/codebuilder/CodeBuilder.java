package com.mtllc.codebuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CodeBuilder extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String BUILD_SCRIPT = "/home/ec2-user/bin/update-beanstalk.sh";
	private static final String PROMOTE_SCRIPT = "/home/ec2-user/bin/promote-beanstalk.sh";

	private final AmazonS3Client s3 = new AmazonS3Client(new AWSCredentialsProviderChain(
			new InstanceProfileCredentialsProvider(), new ProfileCredentialsProvider("{CREDENTIAL_PROFILE}")));

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {

		try {

			// Parse the work to be done from the POST request body.

			InputStreamReader isr = new InputStreamReader(request.getInputStream());
			StringBuilder stringBuilder = new StringBuilder();
			char[] cbuf = new char[1024];
			while (true) {
				int cc = isr.read(cbuf, 0, cbuf.length);
				if (cc <= 0) {
					break;
				}

				stringBuilder.append(new String(cbuf, 0, cc));
			}
			isr.close();

			try {

				String json = stringBuilder.toString();
				System.out.println("Request from SQS:" + json);

				ObjectMapper objectMapper = new ObjectMapper();
				BuildRequest buildRequest = objectMapper.readValue(json, BuildRequest.class);
				System.out.println(buildRequest);

				System.out.println("========================>MY COMMENT");

				String repo = buildRequest.getRepo();
				String beanstalk = buildRequest.getBeanstalk();
				String prefix = buildRequest.getPrefix();
				String bucket = buildRequest.getBucket();
				BuildRequest.Action action = buildRequest.getAction();

				ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "echo", "no", "command");
				if (action == BuildRequest.Action.BUILD) {
					processBuilder = new ProcessBuilder("/bin/sh", BUILD_SCRIPT, beanstalk, repo);
				} else if (action == BuildRequest.Action.PROMOTE) {
					processBuilder = new ProcessBuilder("/bin/sh", PROMOTE_SCRIPT, beanstalk, repo);
				}

				String outputFileName = "/tmp/" + prefix + ".out";

				System.out.println("COMMAND:" + processBuilder.command());

				long t = System.currentTimeMillis();

				File stdouterrFile = new File(outputFileName);

				processBuilder.redirectOutput(stdouterrFile);
				processBuilder.redirectError(stdouterrFile);
				Process p = processBuilder.start();

				p.waitFor();
				System.out.println("COMPLETE:" + (System.currentTimeMillis() - t) + "ms");
				System.out.println("exit code:" + p.exitValue());

				String keyName = prefix + ".success";
				if (p.exitValue() != 0) {
					keyName = prefix + ".failure";
				}

				File file = new File(outputFileName);
				s3.putObject(new PutObjectRequest(bucket, keyName, file));
				System.out.println("putObject() complete: s3://" + bucket + "/" + keyName);

			} catch (Exception e) {
				e.printStackTrace();
			}
			// Write the "result" of the work into Amazon S3.
			// Signal to beanstalk that processing was successful so this work
			// item should not be retried.

			response.setStatus(200);

		} catch (RuntimeException exception) {

			// Signal to beanstalk that something went wrong while processing
			// the request. The work request will be retried several times in
			// case the failure was transient (eg a temporary network issue
			// when writing to Amazon S3).

			response.setStatus(500);
			try (PrintWriter writer = new PrintWriter(response.getOutputStream())) {
				exception.printStackTrace(writer);
			}
		}
	}

}
