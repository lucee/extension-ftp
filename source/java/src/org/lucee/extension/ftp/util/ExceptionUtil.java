package org.lucee.extension.ftp.util;

import org.apache.commons.net.ftp.FTPClient;
import org.lucee.extension.ftp.AFTPClient;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;

public class ExceptionUtil {
	public static PageException createException(String action, FTPClient client) {
		PageException pe = CFMLEngineFactory.getInstance().getExceptionUtil()
				.createApplicationException("Action [" + action + "] from tag [ftp] failed", client.getReplyString());
		// setAdditional("ReplyCode",Caster.toDouble(client.getReplyCode()));
		// setAdditional("ReplyMessage",client.getReplyString());
		pe.setErrorCode(CFMLEngineFactory.getInstance().getCastUtil().toString(client.getReplyCode()));
		return pe;
	}

	public static PageException createException(String action, AFTPClient client) {
		PageException pe = CFMLEngineFactory.getInstance().getExceptionUtil()
				.createApplicationException("Action [" + action + "] from tag [ftp] failed", client.getReplyString());
		// setAdditional("ReplyCode",Caster.toDouble(client.getReplyCode()));
		// setAdditional("ReplyMessage",client.getReplyString());
		pe.setErrorCode(CFMLEngineFactory.getInstance().getCastUtil().toString(client.getReplyCode()));
		return pe;
	}
}
