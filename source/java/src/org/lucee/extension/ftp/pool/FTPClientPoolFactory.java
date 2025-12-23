package org.lucee.extension.ftp.pool;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.lucee.extension.ftp.AFTPClient;
import org.lucee.extension.ftp.FTPConnection;
import org.lucee.extension.ftp.FTPConstant;
import org.lucee.extension.ftp.FTPWrap;
import org.lucee.extension.ftp.SFTPClientImpl;
import org.lucee.extension.ftp.util.ReflectionUtil;

/**
 * Factory for creating and managing pooled FTP clients
 */
public class FTPClientPoolFactory extends BasePooledObjectFactory<PooledFTPClient> {

	private final FTPConnection connection;
	private final InetAddress address;
	private final long maxIdleTimeMs;
	private final long maxLiveTimeMs;

	public FTPClientPoolFactory(FTPConnection connection, long maxIdleTimeMs, long maxLiveTimeMs)
			throws RuntimeException {
		this.connection = connection;
		this.maxIdleTimeMs = maxIdleTimeMs;
		this.maxLiveTimeMs = maxLiveTimeMs;

		try {
			this.address = InetAddress.getByName(connection.getServer());
		} catch (IOException e) {
			throw new RuntimeException("Failed to resolve server address: " + connection.getServer(), e);
		}
	}

	@Override
	public PooledFTPClient create() throws Exception {
		AFTPClient client = AFTPClient.getInstance(connection.secure(), address, connection.getPort(),
				connection.getUsername(), connection.getPassword(), connection.getFingerprint(),
				connection.getStopOnError());

		// Set SSH key if using SFTP
		if (client instanceof SFTPClientImpl && connection.getKey() != null) {
			((SFTPClientImpl) client).setSshKey(connection.getKey(), connection.getPassphrase());
		}

		// Apply connection settings
		FTPWrap.setConnectionSettings(client, connection);

		// Set transfer mode
		if (connection.getTransferMode() == FTPConstant.TRANSFER_MODE_ASCCI) {
			client.setFileType(FTP.ASCII_FILE_TYPE);
		} else if (connection.getTransferMode() == FTPConstant.TRANSFER_MODE_BINARY) {
			client.setFileType(FTP.BINARY_FILE_TYPE);
		}

		// Connect with proxy support
		try {
			ReflectionUtil.Proxy_start(connection.getProxyServer(), connection.getProxyPort(),
					connection.getProxyUser(), connection.getProxyPassword());
			client.connect();
		} finally {
			ReflectionUtil.Proxy_end();
		}

		return new PooledFTPClient(client, connection);
	}

	@Override
	public PooledObject<PooledFTPClient> wrap(PooledFTPClient client) {
		return new DefaultPooledObject<>(client);
	}

	@Override
	public void destroyObject(PooledObject<PooledFTPClient> p) throws Exception {
		PooledFTPClient pooledClient = p.getObject();
		if (pooledClient != null) {
			pooledClient.close();
		}
	}

	@Override
	public boolean validateObject(PooledObject<PooledFTPClient> p) {
		PooledFTPClient pooledClient = p.getObject();
		if (pooledClient == null) {
			return false;
		}

		AFTPClient client = pooledClient.getClient();
		if (client == null || !client.isConnected()) {
			return false;
		}

		// Check if connection has exceeded max live time
		long now = System.currentTimeMillis();
		if (maxLiveTimeMs > 0 && (now - pooledClient.getCreationTime()) > maxLiveTimeMs) {
			return false;
		}

		// Try a NOOP command to verify connection is alive
		try {
			return client.sendNoOp();
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void activateObject(PooledObject<PooledFTPClient> p) throws Exception {
		PooledFTPClient pooledClient = p.getObject();
		pooledClient.setLastAccessTime(System.currentTimeMillis());

		// Ensure connection settings are correct
		FTPWrap.setConnectionSettings(pooledClient.getClient(), connection);
	}

	@Override
	public void passivateObject(PooledObject<PooledFTPClient> p) throws Exception {
		// Could reset to a known state if needed
		PooledFTPClient pooledClient = p.getObject();
		pooledClient.setLastAccessTime(System.currentTimeMillis());
	}
}