package org.lucee.extension.ftp.pool;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.lucee.extension.ftp.FTPConnection;
import org.lucee.extension.ftp.FTPWrap;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;

/**
 * Global FTP Connection Pool using Apache Commons Pool2
 */
public final class FTPConnectionPool {

	private static final FTPConnectionPool INSTANCE = new FTPConnectionPool();

	// Map of connection name/key to object pool
	private final Map<String, GenericObjectPool<PooledFTPClient>> pools = new ConcurrentHashMap<>();

	// Default pool configuration
	private final GenericObjectPoolConfig<PooledFTPClient> defaultPoolConfig;

	// Default timeouts (can be overridden per connection)
	private static final long DEFAULT_MAX_IDLE_TIME_MS = 1 * 60 * 1000; // 1 minute
	private static final long DEFAULT_MAX_LIVE_TIME_MS = 5 * 60 * 1000; // 5 minutes

	private FTPConnectionPool() {
		defaultPoolConfig = new GenericObjectPoolConfig<>();

		// Pool sizing
		defaultPoolConfig.setMaxTotal(50); // Max connections across all pools
		defaultPoolConfig.setMaxIdle(10); // Max idle connections per pool
		defaultPoolConfig.setMinIdle(0); // Min idle connections per pool

		// Eviction policy for idle connections
		defaultPoolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
		defaultPoolConfig.setMinEvictableIdleTime(Duration.ofMillis(DEFAULT_MAX_IDLE_TIME_MS));
		defaultPoolConfig.setSoftMinEvictableIdleTime(Duration.ofMillis(DEFAULT_MAX_IDLE_TIME_MS));
		defaultPoolConfig.setNumTestsPerEvictionRun(3);

		// Connection validation
		defaultPoolConfig.setTestWhileIdle(true);
		defaultPoolConfig.setTestOnBorrow(true);
		defaultPoolConfig.setTestOnReturn(false);

		// Blocking behavior
		defaultPoolConfig.setBlockWhenExhausted(true);
		defaultPoolConfig.setMaxWait(Duration.ofSeconds(10));

		// Enable JMX monitoring
		defaultPoolConfig.setJmxEnabled(true);
	}

	public static FTPConnectionPool getInstance() {
		return INSTANCE;
	}

	/**
	 * Get a pooled FTP client for the given connection
	 */
	public PooledFTPClient borrowClient(FTPConnection conn) throws IOException, PageException {
		String poolKey = getPoolKey(conn);
		GenericObjectPool<PooledFTPClient> pool = getOrCreatePool(poolKey, conn);

		try {
			PooledFTPClient pooledClient = pool.borrowObject();

			// Update connection settings in case they changed
			FTPWrap.setConnectionSettings(pooledClient.getClient(), conn);

			// Check if transfer mode changed and reconnect if needed
			if (pooledClient.getConnection().getTransferMode() != conn.getTransferMode()) {
				pooledClient.reconnectWithTransferMode(conn.getTransferMode());
			}

			return pooledClient;

		} catch (Exception e) {
			if (e instanceof IOException) {
				throw (IOException) e;
			}
			if (e instanceof PageException) {
				throw (PageException) e;
			}
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}

	/**
	 * Return a client to the pool
	 */
	public void returnClient(PooledFTPClient client) {
		if (client == null)
			return;

		String poolKey = getPoolKey(client.getConnection());
		GenericObjectPool<PooledFTPClient> pool = pools.get(poolKey);

		if (pool != null) {
			try {
				pool.returnObject(client);
			} catch (Exception e) {
				// Log error but don't throw - just invalidate the client
				try {
					pool.invalidateObject(client);
				} catch (Exception ex) {
					// Ignore
				}
			}
		} else {
			// Pool was removed, just close the client
			try {
				client.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	/**
	 * Invalidate a client (e.g., when an error occurs)
	 */
	public void invalidateClient(PooledFTPClient client) {
		if (client == null)
			return;

		String poolKey = getPoolKey(client.getConnection());
		GenericObjectPool<PooledFTPClient> pool = pools.get(poolKey);

		if (pool != null) {
			try {
				pool.invalidateObject(client);
			} catch (Exception e) {
				// Ignore
			}
		} else {
			// Pool was removed, just close the client
			try {
				client.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	/**
	 * Remove a named connection pool
	 */
	public void removePool(String name) {
		if (name == null)
			return;

		GenericObjectPool<PooledFTPClient> pool = pools.remove(name);
		if (pool != null) {
			pool.close();
		}
	}

	/**
	 * Clear all pools
	 */
	public void clearAll() {
		for (GenericObjectPool<PooledFTPClient> pool : pools.values()) {
			pool.close();
		}
		pools.clear();
	}

	/**
	 * Get pool statistics for monitoring
	 */
	public PoolStats getPoolStats(String name) {
		GenericObjectPool<PooledFTPClient> pool = pools.get(name);
		if (pool == null) {
			return null;
		}

		return new PoolStats(pool.getNumActive(), pool.getNumIdle(), pool.getNumWaiters(), pool.getMaxTotal(),
				pool.getMaxIdle(), pool.getMinIdle());
	}

	/**
	 * Get or create a pool for the given connection
	 */
	private GenericObjectPool<PooledFTPClient> getOrCreatePool(String poolKey, FTPConnection conn) {
		return pools.computeIfAbsent(poolKey, key -> {
			FTPClientPoolFactory factory = new FTPClientPoolFactory(conn, DEFAULT_MAX_IDLE_TIME_MS,
					DEFAULT_MAX_LIVE_TIME_MS);

			GenericObjectPoolConfig<PooledFTPClient> config = new GenericObjectPoolConfig<>();
			config.setMaxTotal(defaultPoolConfig.getMaxTotal());
			config.setMaxIdle(defaultPoolConfig.getMaxIdle());
			config.setMinIdle(defaultPoolConfig.getMinIdle());
			config.setTimeBetweenEvictionRuns(defaultPoolConfig.getTimeBetweenEvictionRuns());
			config.setMinEvictableIdleTime(defaultPoolConfig.getMinEvictableIdleTime());
			config.setSoftMinEvictableIdleTime(defaultPoolConfig.getSoftMinEvictableIdleTime());
			config.setNumTestsPerEvictionRun(defaultPoolConfig.getNumTestsPerEvictionRun());
			config.setTestWhileIdle(defaultPoolConfig.getTestWhileIdle());
			config.setTestOnBorrow(defaultPoolConfig.getTestOnBorrow());
			config.setTestOnReturn(defaultPoolConfig.getTestOnReturn());
			config.setBlockWhenExhausted(defaultPoolConfig.getBlockWhenExhausted());
			config.setMaxWait(defaultPoolConfig.getMaxWaitDuration());
			config.setJmxEnabled(false); // Disable per-pool JMX

			GenericObjectPool<PooledFTPClient> pool = new GenericObjectPool<>(factory, config);
			pool.setSwallowedExceptionListener(exception -> {
				// Log swallowed exceptions
				System.err.println("FTP Pool swallowed exception: " + exception.getMessage());
			});

			return pool;
		});
	}

	/**
	 * Generate a unique key for a connection pool
	 */
	private String getPoolKey(FTPConnection conn) {
		if (conn.hasName()) {
			return conn.getName();
		}

		// Generate key from connection parameters
		StringBuilder sb = new StringBuilder();
		sb.append(conn.getServer()).append(":");
		sb.append(conn.getPort()).append(":");
		sb.append(conn.getUsername() != null ? conn.getUsername() : "anonymous").append(":");
		sb.append(conn.secure());

		return sb.toString();
	}

	/**
	 * Simple stats holder
	 */
	public static class PoolStats {
		private final int numActive;
		private final int numIdle;
		private final int numWaiters;
		private final int maxTotal;
		private final int maxIdle;
		private final int minIdle;

		public PoolStats(int numActive, int numIdle, int numWaiters, int maxTotal, int maxIdle, int minIdle) {
			this.numActive = numActive;
			this.numIdle = numIdle;
			this.numWaiters = numWaiters;
			this.maxTotal = maxTotal;
			this.maxIdle = maxIdle;
			this.minIdle = minIdle;
		}

		public int getNumActive() {
			return numActive;
		}

		public int getNumIdle() {
			return numIdle;
		}

		public int getNumWaiters() {
			return numWaiters;
		}

		public int getMaxTotal() {
			return maxTotal;
		}

		public int getMaxIdle() {
			return maxIdle;
		}

		public int getMinIdle() {
			return minIdle;
		}

		@Override
		public String toString() {
			return String.format("PoolStats[active=%d, idle=%d, waiters=%d, maxTotal=%d, maxIdle=%d, minIdle=%d]",
					numActive, numIdle, numWaiters, maxTotal, maxIdle, minIdle);
		}
	}
}