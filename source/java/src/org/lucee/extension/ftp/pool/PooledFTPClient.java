package org.lucee.extension.ftp.pool;

import java.io.Closeable;
import java.io.IOException;

import org.apache.commons.net.ftp.FTP;
import org.lucee.extension.ftp.AFTPClient;
import org.lucee.extension.ftp.FTPConnection;
import org.lucee.extension.ftp.FTPConnectionImpl;
import org.lucee.extension.ftp.FTPConstant;
import org.lucee.extension.ftp.FTPWrap;

/**
 * Wrapper for an FTP client that tracks pooling metadata
 */
public class PooledFTPClient implements Closeable {

	private final AFTPClient client;
	private final FTPConnection connection;
	private final long creationTime;
	private long lastAccessTime;

	public PooledFTPClient(AFTPClient client, FTPConnection connection) {
		this.client = client;
		this.connection = connection;
		this.creationTime = System.currentTimeMillis();
		this.lastAccessTime = this.creationTime;
	}

	public AFTPClient getClient() {
		return client;
	}

	public FTPConnection getConnection() {
		return connection;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public long getLastAccessTime() {
		return lastAccessTime;
	}

	public void setLastAccessTime(long lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}

	public long getAgeMs() {
		return System.currentTimeMillis() - creationTime;
	}

	public long getIdleTimeMs() {
		return System.currentTimeMillis() - lastAccessTime;
	}

	/**
	 * Reconnect with a different transfer mode
	 */
	public void reconnectWithTransferMode(short transferMode) throws IOException {
		// Update the connection's transfer mode
		if (connection instanceof FTPConnectionImpl) {
			((FTPConnectionImpl) connection).setTransferMode(transferMode);
		}

		// Disconnect if connected
		if (client != null && client.isConnected()) {
			try {
				client.quit();
				client.disconnect();
			} catch (IOException e) {
				// Ignore disconnect errors
			}
		}

		// Reconnect
		client.connect();

		// Apply settings
		FTPWrap.setConnectionSettings(client, connection);

		// Set new transfer mode
		if (transferMode == FTPConstant.TRANSFER_MODE_ASCCI) {
			client.setFileType(FTP.ASCII_FILE_TYPE);
		} else if (transferMode == FTPConstant.TRANSFER_MODE_BINARY) {
			client.setFileType(FTP.BINARY_FILE_TYPE);
		}

		this.lastAccessTime = System.currentTimeMillis();
	}

	@Override
	public void close() throws IOException {
		if (client != null && client.isConnected()) {
			try {
				client.quit();
			} catch (IOException e) {
				// Ignore
			}
			try {
				client.disconnect();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	@Override
	public String toString() {
		return String.format("PooledFTPClient[server=%s, age=%dms, idle=%dms, connected=%s]", connection.getServer(),
				getAgeMs(), getIdleTimeMs(), client != null && client.isConnected());
	}
}