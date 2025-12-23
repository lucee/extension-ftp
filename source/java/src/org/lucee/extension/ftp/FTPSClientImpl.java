package org.lucee.extension.ftp;

import org.apache.commons.net.ftp.FTPSClient;

public final class FTPSClientImpl extends FTPClientImpl {

	private FTPSClient client;

	public FTPSClientImpl(FTPSClient client) {
		super(client);
		this.client = client;
	}

	FTPSClientImpl() {
		this.client = new FTPSClient();
	}
}
