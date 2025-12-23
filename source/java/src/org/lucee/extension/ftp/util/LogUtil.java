package org.lucee.extension.ftp.util;

import lucee.commons.io.log.Log;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.config.Config;

public class LogUtil {

	public static void log(int level, Throwable t) {
		Log log = log();
		if (log != null) {
			log.log(Log.LEVEL_WARN, "ftp", t);
		}
	}

	public static void log(int level, String msg) {
		Log log = log();
		if (log != null) {
			log.log(level, "ftp", msg);
		}
	}

	private static Log log() {
		Config config = CFMLEngineFactory.getInstance().getThreadConfig();
		Log log = null;
		if (config != null) {
			try {
				log = config.getLog("ftp");
			} catch (Exception ex) {

			}
			if (log == null) {
				log = config.getLog("application");
			}
		}
		return log;
	}
}
