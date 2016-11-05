package com.mtllc.codebuilder;

public class BuildRequest {
	enum Action {
		BUILD, PROMOTE
	};

	private Action action;
	private String bucket;
	private String prefix;
	private String sqs;
	private String repo; // Github URL of code to deploy
	private String beanstalk;

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSqs() {
		return sqs;
	}

	public void setSqs(String sqs) {
		this.sqs = sqs;
	}

	public String getBeanstalk() {
		return beanstalk;
	}

	public void setBeanstalk(String beanstalk) {
		this.beanstalk = beanstalk;
	}

	public String getRepo() {
		return repo;
	}

	public void setRepo(String repo) {
		this.repo = repo;
	}

	public String toString() {
		return "REQUEST:{\n" + "\t   action:" + getAction() + "\n" + "\t   bucket:" + getBucket() + "\n"
				+ "\t      sqs:" + getSqs() + "\n" + "\t     repo:" + getRepo() + "\n" + "\t   prefix:" + getPrefix()
				+ "\n" + "\tbeanstalk:" + getBeanstalk() + "\n" + "}";
	}

}
