package org.lucee.extension.ftp.util;

import java.lang.reflect.Method;
import java.util.Calendar;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.net.proxy.ProxyData;

public class ReflectionUtil {

	private static Method isValid;
	private static Method start1;
	private static Method start4;
	private static Method end;
	private static Method getThreadCalendar;

	public static boolean isValid(ProxyData proxyData, String host) {

		CFMLEngine eng = CFMLEngineFactory.getInstance();

		if (proxyData == null)
			return false;
		try {
			if (isValid == null || isValid.getDeclaringClass() != proxyData.getClass()) {
				isValid = proxyData.getClass().getMethod("isValid", new Class[] { ProxyData.class, String.class });
			}
			return eng.getCastUtil().toBooleanValue(isValid.invoke(null, new Object[] { proxyData, host }));

		} catch (Exception e) {
			throw eng.getExceptionUtil().createPageRuntimeException(eng.getCastUtil().toPageException(e));
		}
	}

	public static void Proxy_start(ProxyData proxyData) {
		CFMLEngine eng = CFMLEngineFactory.getInstance();

		try {
			Class<?> proxyClass = eng.getClassUtil().loadClass("lucee.runtime.net.proxy.Proxy");
			if (start1 == null || start1.getDeclaringClass() != proxyClass) {
				start1 = proxyClass.getMethod("start", new Class[] { ProxyData.class });
			}
			start1.invoke(null, new Object[] { proxyData });
		} catch (Exception e) {
			throw eng.getExceptionUtil().createPageRuntimeException(eng.getCastUtil().toPageException(e));
		}
	}

	public static void Proxy_start(String proxyServer, int proxyPort, String proxyUser, String proxyPassword) {
		CFMLEngine eng = CFMLEngineFactory.getInstance();

		try {
			Class<?> proxyClass = eng.getClassUtil().loadClass("lucee.runtime.net.proxy.Proxy");
			if (start4 == null || start4.getDeclaringClass() != proxyClass) {
				start4 = proxyClass.getMethod("start",
						new Class[] { String.class, int.class, String.class, String.class });
			}
			start4.invoke(null, new Object[] { proxyServer, proxyPort, proxyUser, proxyPassword });
		} catch (Exception e) {
			throw eng.getExceptionUtil().createPageRuntimeException(eng.getCastUtil().toPageException(e));
		}
	}

	public static void Proxy_end() {
		CFMLEngine eng = CFMLEngineFactory.getInstance();

		try {
			Class<?> proxyClass = eng.getClassUtil().loadClass("lucee.runtime.net.proxy.Proxy");
			if (end == null || end.getDeclaringClass() != proxyClass) {
				end = proxyClass.getMethod("end", new Class[] {});
			}
			end.invoke(null, new Object[] {});
		} catch (Exception e) {
			throw eng.getExceptionUtil().createPageRuntimeException(eng.getCastUtil().toPageException(e));
		}
	}

	public static Calendar JREDateTimeUtil_getThreadCalendar() {
		CFMLEngine eng = CFMLEngineFactory.getInstance();

		try {
			Class<?> utilClass = eng.getClassUtil().loadClass("lucee.commons.date.JREDateTimeUtil");
			if (getThreadCalendar == null || getThreadCalendar.getDeclaringClass() != utilClass) {
				getThreadCalendar = utilClass.getMethod("getThreadCalendar", new Class[] {});
			}
			return (Calendar) end.invoke(null, new Object[] {});
		} catch (Exception e) {
			throw eng.getExceptionUtil().createPageRuntimeException(eng.getCastUtil().toPageException(e));
		}
	}

}
