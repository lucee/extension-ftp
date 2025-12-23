/**
 * Copyright (c) 2014, the Railo Company Ltd.
 * Copyright (c) 2015, Lucee Association Switzerland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package org.lucee.extension.ftp.vfs;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import org.lucee.extension.ftp.util.LogUtil;
import org.lucee.extension.ftp.util.ReflectionUtil;
import org.lucee.extension.ftp.vfs.FTPConnectionData.DataAndPath;
import org.lucee.extension.ftp.vfs.FTPResourceClient.SerializableObject;

import lucee.commons.io.log.Log;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.ResourceLock;
import lucee.commons.io.res.ResourceProvider;
import lucee.commons.io.res.Resources;
import lucee.commons.io.res.type.ftp.IFTPConnectionData;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;

// TODO check connection timeout
public final class FTPResourceProvider implements ResourceProvider {

	private static CFMLEngine eng;

	private String scheme = "ftp";
	private final Map clients = new HashMap();
	private int clientTimeout = 60000;
	private int socketTimeout = -1;
	private int lockTimeout = 20000;
	private int cache = 20000;

	private FTPResourceClientCloser closer = null;
	private ResourceLock _lock;
	private Map arguments;
	private final Object sync = new SerializableObject();

	private Resources resources;

	public static CFMLEngine eng() {
		if (eng == null)
			eng = CFMLEngineFactory.getInstance();
		return eng;
	}

	@Override
	public ResourceProvider init(String scheme, Map arguments) {
		setScheme(scheme);

		if (arguments != null) {
			this.arguments = arguments;
			// client-timeout
			String strTimeout = (String) arguments.get("client-timeout");
			if (strTimeout != null) {
				clientTimeout = eng().getCastUtil().toIntValue(strTimeout, clientTimeout);
			}
			// socket-timeout
			strTimeout = (String) arguments.get("socket-timeout");
			if (strTimeout != null) {
				socketTimeout = eng().getCastUtil().toIntValue(strTimeout, socketTimeout);
			}
			// lock-timeout
			strTimeout = (String) arguments.get("lock-timeout");
			if (strTimeout != null) {
				lockTimeout = eng().getCastUtil().toIntValue(strTimeout, lockTimeout);
			}
			// cache
			String strCache = (String) arguments.get("cache");
			if (strCache != null) {
				cache = eng().getCastUtil().toIntValue(strCache, cache);
			}
		}

		return this;
	}

	@Override
	public Resource getResource(String path) {
		path = eng().getResourceUtil().removeScheme(scheme, path);

		PageContext pc = eng().getThreadPageContext();
		IFTPConnectionData base = null;
		if (pc != null) {
			base = pc.getApplicationContext().getFTP();
		}
		DataAndPath dap = FTPConnectionData.load(base, path);
		return new FTPResource(this, dap.data, dap.path);
	}

	FTPResourceClient getClient(FTPConnectionData data) throws IOException {

		FTPResourceClient client = (FTPResourceClient) clients.remove(data.toString());
		if (client == null) {
			client = new FTPResourceClient(data, cache);
			if (socketTimeout > 0)
				client.setSoTimeout(socketTimeout);
		}

		if (!client.isConnected()) {
			if (ReflectionUtil.isValid(data.getProxyData(), data.host)) {
				try {
					ReflectionUtil.Proxy_start(data.getProxyData());
					connect(client, data);
				} finally {
					ReflectionUtil.Proxy_end();
				}
			} else {
				connect(client, data);
			}

			int replyCode = client.getReplyCode();
			if (replyCode >= 400)
				throw new FTPException(replyCode);
		}
		startCloser();
		return client;
	}

	private void startCloser() {
		if (closer == null || !closer.isAlive()) {
			synchronized (sync) {
				if (closer == null || !closer.isAlive()) {
					closer = new FTPResourceClientCloser(this);
					closer.start();
				}
			}
		}
	}

	private void connect(FTPResourceClient client, FTPConnectionData data) throws SocketException, IOException {
		if (data.port > 0)
			client.connect(data.host, data.port);
		else
			client.connect(data.host);
		if (!Util.isEmpty(data.username))
			client.login(data.username, data.password);
	}

	public void returnClient(FTPResourceClient client) {
		if (client == null)
			return;
		client.touch();
		clients.put(client.getFtpConnectionData().toString(), client);
	}

	@Override
	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		if (!Util.isEmpty(scheme))
			this.scheme = scheme;
	}

	@Override
	public void setResources(Resources resources) {
		this.resources = resources;
	}

	@Override
	public void lock(Resource res) throws IOException {
		lock().lock(res);
	}

	@Override
	public void unlock(Resource res) {
		lock().unlock(res);
	}

	@Override
	public void read(Resource res) throws IOException {
		lock().read(res);
	}

	public void clean() {
		Object[] keys = clients.keySet().toArray();
		FTPResourceClient client;
		for (int i = 0; i < keys.length; i++) {
			client = (FTPResourceClient) clients.get(keys[i]);
			if (client.getLastAccess() + clientTimeout < System.currentTimeMillis()) {
				// lucee.print.ln("disconnect:"+client.getFtpConnectionData().key());
				if (client.isConnected()) {
					try {
						client.disconnect();
					} catch (IOException e) {
						LogUtil.log(Log.LEVEL_WARN, e);
					}
				}
				clients.remove(client.getFtpConnectionData().toString());
			}
		}
	}

	class FTPResourceClientCloser extends Thread {

		private FTPResourceProvider provider;

		public FTPResourceClientCloser(FTPResourceProvider provider) {
			this.provider = provider;
		}

		@Override
		public void run() {
			// lucee.print.ln("closer start");
			do {
				sleepEL();
				provider.clean();
			} while (!clients.isEmpty());
			// lucee.print.ln("closer stop");
		}

		private void sleepEL() {
			try {
				sleep(provider.clientTimeout);
			} catch (InterruptedException e) {
				LogUtil.log(Log.LEVEL_WARN, e);
			}
		}
	}

	/**
	 * @return the cache
	 */
	public int getCache() {
		return cache;
	}

	@Override
	public boolean isAttributesSupported() {
		return false;
	}

	@Override
	public boolean isCaseSensitive() {
		return true;
	}

	@Override
	public boolean isModeSupported() {
		return true;
	}

	@Override
	public Map getArguments() {
		return arguments;
	}

	public char getSeparator() {
		return '/';
	}

	public boolean allowMatching() {
		return false;
	}

	private ResourceLock lock() {
		if (_lock == null) {
			_lock = resources.createResourceLock(lockTimeout, true);
		}
		return _lock;
	}

}