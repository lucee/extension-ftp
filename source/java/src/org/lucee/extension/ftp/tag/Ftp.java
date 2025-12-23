/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
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
 **/
package org.lucee.extension.ftp.tag;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.lucee.extension.ftp.AFTPClient;
import org.lucee.extension.ftp.FTPConnection;
import org.lucee.extension.ftp.FTPConnectionImpl;
import org.lucee.extension.ftp.FTPConstant;
import org.lucee.extension.ftp.FTPPath;
import org.lucee.extension.ftp.pool.FTPConnectionPool;
import org.lucee.extension.ftp.pool.PooledFTPClient;
import org.lucee.extension.ftp.util.ExceptionUtil;

import jakarta.ejb.ApplicationException;
import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Creation;

/**
 * 
 * Lets users implement File Transfer Protocol (FTP) operations.
 *
 *
 *
 **/
public final class Ftp extends TagImpl {

	private static final String ASCCI_EXT_LIST = "txt;htm;html;cfm;cfml;shtm;shtml;css;asp;asa";
	private static final int PORT_FTP = 21;
	private static final int PORT_SFTP = 22;
	private static final int PORT_FTPS = 990;

	private static final Key SUCCEEDED;
	private static final Key ERROR_CODE;
	private static final Key ERROR_TEXT;
	private static final Key RETURN_VALUE;
	private static final Key CFFTP;

	static {
		Creation creator = CFMLEngineFactory.getInstance().getCreationUtil();
		SUCCEEDED = creator.createKey("succeeded");
		ERROR_CODE = creator.createKey("errorCode");
		ERROR_TEXT = creator.createKey("errorText");
		RETURN_VALUE = creator.createKey("returnValue");
		CFFTP = creator.createKey("cfftp");
	}

	/*
	 * private static final Key = KeyImpl.getInstance(); private static final Key =
	 * KeyImpl.getInstance(); private static final Key = KeyImpl.getInstance();
	 * private static final Key = KeyImpl.getInstance(); private static final Key =
	 * KeyImpl.getInstance(); private static final Key = KeyImpl.getInstance();
	 */

	private String action;
	private String actionParams;
	private String username;
	private String password;
	private String server;
	private int timeout = 30;
	private int port = -1;
	private String connectionName;
	private int retrycount = 1;
	private int count = 0;
	private boolean stoponerror = true;
	private boolean passive;
	private String name;
	private String directory;
	private String ASCIIExtensionList = ASCCI_EXT_LIST;
	private short transferMode = FTPConstant.TRANSFER_MODE_AUTO;
	private String remotefile;
	private String localfile;
	private boolean failifexists = true;
	private String existing;
	private String _new;
	private String item;
	private String result;

	private String proxyserver;
	private int proxyport = 80;
	private String proxyuser;
	private String proxypassword = "";
	private String fingerprint;
	private String secure = "FALSE";

	private boolean recursive;
	private String key;
	private String passphrase = "";
	private FTPConnectionPool pool;

	// private Struct cfftp=new StructImpl();

	@Override
	public void release() {
		super.release();
		this.pool = null;

		this.action = null;
		this.actionParams = null;
		this.username = null;
		this.password = null;
		this.server = null;
		this.timeout = 30;
		this.port = -1;
		this.connectionName = null;
		this.proxyserver = null;
		this.proxyport = 80;
		this.proxyuser = null;
		this.proxypassword = "";
		this.retrycount = 1;
		this.count = 0;
		this.stoponerror = true;
		this.passive = false;
		this.name = null;
		this.directory = null;
		this.ASCIIExtensionList = ASCCI_EXT_LIST;
		this.transferMode = FTPConstant.TRANSFER_MODE_AUTO;
		this.remotefile = null;
		this.localfile = null;
		this.failifexists = true;
		this.existing = null;
		this._new = null;
		this.item = null;
		this.result = null;

		this.fingerprint = null;
		this.secure = "FALSE";
		this.recursive = false;
		this.key = null;
		this.passphrase = "";
	}

	public void setAction(String action) {
		this.action = action.trim().toLowerCase();
	}

	/**
	 * sets the secure flag, true / false / sftp
	 * 
	 * @param secure
	 * @throws PageException
	 */
	public void setSecure(String secure) throws PageException {
		if (Util.isEmpty(secure, true))
			return;
		this.secure = secure.trim().toUpperCase();
		// convert yes|no to true|false
		if (eng().getDecisionUtil().isBoolean(this.secure))
			this.secure = eng().getCastUtil().toString(eng().getCastUtil().toBooleanValue(this.secure)).toUpperCase();

	}

	@Override
	public int doStartTag() {
		return SKIP_BODY;
	}

	@Override
	public int doEndTag() throws PageException {
		pool = FTPConnectionPool.getInstance();
		PooledFTPClient pooledClient = null;

		boolean invalidateClient = false;
		try {
			pooledClient = pool.borrowClient(_createConnection());
			final AFTPClient client = pooledClient.getClient();
			// retries
			do {

				try {
					if (action.equals("open"))
						actionOpen(client);
					else if (action.equals("close")) {
						actionClose(client);
						invalidateClient = true;
						break;
					} else if (action.equals("changedir"))
						actionChangeDir(client);
					else if (action.equals("createdir"))
						actionCreateDir(client);
					else if (action.equals("listdir"))
						actionListDir(client);
					else if (action.equals("removedir"))
						actionRemoveDir(client);
					else if (action.equals("getfile"))
						actionGetFile(client);
					else if (action.equals("putfile"))
						actionPutFile(client);
					else if (action.equals("rename"))
						actionRename(client);
					else if (action.equals("remove"))
						actionRemove(client);
					else if (action.equals("getcurrentdir"))
						actionGetCurrentDir(client);
					else if (action.equals("getcurrenturl"))
						actionGetCurrentURL(client);
					else if (action.equals("existsdir"))
						actionExistsDir(client);
					else if (action.equals("existsfile"))
						actionExistsFile(client);
					else if (action.equals("exists"))
						actionExists(client);
					else if (action.equals("quote"))
						actionQuote(client);
					// else if(action.equals("copy")) client=actionCopy();

					else
						throw eng().getExceptionUtil().createApplicationException(
								"Tag [ftp] attribute [action] has an invalid value [" + action + "]",
								"valid values are [open, close, listDir, createDir, removeDir, changeDir, getCurrentDir, "
										+ "getCurrentURL, existsFile, existsDir, exists, getFile, putFile, quote, rename, remove]");

				} catch (IOException ioe) {
					if (count++ < retrycount)
						continue;
					throw eng().getCastUtil().toPageException(ioe);
				}

				if (client == null || !checkCompletion(client))
					break;
			} while (true);

		} catch (IOException ioe) {
			throw eng().getCastUtil().toPageException(ioe);
		} finally {
			if (pooledClient != null) {
				if (invalidateClient) {
					pool.invalidateClient(pooledClient); // Destroy instead of return
				} else {
					pool.returnClient(pooledClient); // Normal return
				}
			}
		}

		return EVAL_PAGE;
	}

	/**
	 * check if a directory exists or not
	 * 
	 * @return FTPCLient
	 * @throws PageException
	 * @throws IOException
	 */
	private void actionExistsDir(AFTPClient client) throws PageException, IOException {
		required("directory", directory);

		boolean res = existsDir(client, directory);
		Struct cfftp = writeCfftp(client);

		cfftp.setEL(RETURN_VALUE, eng().getCastUtil().toBoolean(res));
		cfftp.setEL(SUCCEEDED, Boolean.TRUE);

		stoponerror = false;
	}

	/**
	 * check if a file exists or not
	 * 
	 * @return FTPCLient
	 * @throws IOException
	 * @throws PageException
	 */
	private void actionExistsFile(AFTPClient client) throws PageException, IOException {
		required("remotefile", remotefile);

		FTPFile file = existsFile(client, remotefile, true);

		Struct cfftp = writeCfftp(client);

		cfftp.setEL(RETURN_VALUE, eng().getCastUtil().toBoolean(file != null && file.isFile()));
		cfftp.setEL(SUCCEEDED, Boolean.TRUE);

		stoponerror = false;
	}

	/**
	 * check if a file or directory exists
	 * 
	 * @return FTPCLient
	 * @throws PageException
	 * @throws IOException
	 */
	private void actionExists(AFTPClient client) throws PageException, IOException {
		required("item", item);

		FTPFile file = existsFile(client, item, false);
		Struct cfftp = writeCfftp(client);

		cfftp.setEL(RETURN_VALUE, eng().getCastUtil().toBoolean(file != null));
		cfftp.setEL(SUCCEEDED, Boolean.TRUE);
	}

	/*
	 * * check if file or directory exists if it exists return FTPFile otherwise
	 * null
	 * 
	 * @param client
	 * 
	 * @param strPath
	 * 
	 * @return FTPFile or null
	 * 
	 * @throws IOException
	 * 
	 * @throws PageException / private FTPFile exists(FTPClient client, String
	 * strPath) throws PageException, IOException { strPath=strPath.trim();
	 * 
	 * // get parent path FTPPath path=new
	 * FTPPath(client.printWorkingDirectory(),strPath); String name=path.getName();
	 * print.out("path:"+name);
	 * 
	 * // when directory FTPFile[] files=null; try { files =
	 * client.listFiles(path.getPath()); } catch (IOException e) {}
	 * 
	 * if(files!=null) { for(int i=0;i<files.length;i++) {
	 * if(files[i].getName().equalsIgnoreCase(name)) { return files[i]; } }
	 * 
	 * } return null; }
	 */

	private FTPFile existsFile(AFTPClient client, String strPath, boolean isFile) throws PageException, IOException {
		strPath = strPath.trim();
		if (strPath.equals("/")) {
			FTPFile file = new FTPFile();
			file.setName("/");
			file.setType(FTPFile.DIRECTORY_TYPE);
			return file;
		}

		// get parent path
		FTPPath path = new FTPPath(client, strPath);
		String p = path.getPath();
		String n = path.getName();

		strPath = p;
		if ("//".equals(p))
			strPath = "/";
		if (isFile)
			strPath += n;

		// when directory
		FTPFile[] files = null;
		try {
			files = client.listFiles(p);
		} catch (IOException e) {
		}

		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].getName().equalsIgnoreCase(n)) {
					return files[i];
				}
			}

		}
		return null;
	}

	private boolean existsDir(AFTPClient client, String strPath) throws PageException, IOException {
		strPath = strPath.trim();

		// get parent path
		FTPPath path = new FTPPath(client, strPath);
		String p = path.getPath();
		String n = path.getName();

		strPath = p + "" + n;
		if ("//".equals(p))
			strPath = "/" + n;
		if (!strPath.endsWith("/"))
			strPath += "/";

		return client.directoryExists(directory);
	}

	/**
	 * removes a file on the server
	 * 
	 * @return FTPCLient
	 * @throws IOException
	 * @throws PageException
	 */
	private void actionRemove(AFTPClient client) throws IOException, PageException {
		required("item", item);
		client.deleteFile(item);
		writeCfftp(client);
	}

	/**
	 * rename a file on the server
	 * 
	 * @return FTPCLient
	 * @throws PageException
	 * @throws IOException
	 */
	private void actionRename(AFTPClient client) throws PageException, IOException {
		required("existing", existing);
		required("new", _new);

		client.rename(existing, _new);
		writeCfftp(client);
	}

	/**
	 * copy a local file to server
	 * 
	 * @return FTPClient
	 * @throws IOException
	 * @throws PageException
	 */
	private void actionPutFile(AFTPClient client) throws IOException, PageException {
		required("remotefile", remotefile);
		required("localfile", localfile);

		Resource local = eng().getResourceUtil().toResourceExisting(pageContext, localfile);
		// if(failifexists && local.exists()) throw new ApplicationException("File
		// ["+local+"] already
		// exist, if you want to overwrite, set attribute
		// failIfExists to false");
		InputStream is = null;

		try {
			is = eng().getIOUtil().toBufferedInputStream(local.getInputStream());
			client.setFileType(getType(local));
			client.storeFile(remotefile, is);
		} finally {
			eng().getIOUtil().closeSilent(is);
		}
		writeCfftp(client);
	}

	/**
	 * gets a file from server and copy it local
	 * 
	 * @return FTPCLient
	 * @throws PageException
	 * @throws IOException
	 */
	private void actionGetFile(AFTPClient client) throws PageException, IOException {
		required("remotefile", remotefile);
		required("localfile", localfile);

		Resource local = eng().getResourceUtil().toResourceExistingParent(pageContext, localfile);
		pageContext.getConfig().getSecurityManager().checkFileLocation(local);
		if (failifexists && local.exists())
			throw eng().getExceptionUtil().createApplicationException("FTP File [" + local
					+ "] already exists, if you want to overwrite, set attribute [failIfExists] to false");
		OutputStream fos = null;
		client.setFileType(getType(local));
		boolean success = false;
		try {
			fos = eng().getIOUtil().toBufferedOutputStream(local.getOutputStream());
			success = client.retrieveFile(remotefile, fos);
		} finally {
			eng().getIOUtil().closeSilent(fos);
			if (!success)
				local.delete();
		}
		writeCfftp(client);
	}

	/**
	 * get url of the working directory
	 * 
	 * @return FTPCLient
	 * @throws IOException
	 * @throws PageException
	 */
	private void actionGetCurrentURL(AFTPClient client) throws PageException, IOException {
		String pwd = client.printWorkingDirectory();
		Struct cfftp = writeCfftp(client);
		cfftp.setEL("returnValue", client.getPrefix() + "://" + client.getRemoteAddress().getHostName() + pwd);
	}

	/**
	 * get path from the working directory
	 * 
	 * @return FTPCLient
	 * @throws IOException
	 * @throws PageException
	 */
	private void actionGetCurrentDir(AFTPClient client) throws PageException, IOException {
		String pwd = client.printWorkingDirectory();
		Struct cfftp = writeCfftp(client);
		cfftp.setEL("returnValue", pwd);
	}

	/**
	 * change working directory
	 * 
	 * @return FTPCLient
	 * @throws IOException
	 * @throws PageException
	 */
	private void actionChangeDir(AFTPClient client) throws IOException, PageException {
		required("directory", directory);

		client.changeWorkingDirectory(directory);
		writeCfftp(client);
	}

	/**
	 * removes a remote directory on server
	 * 
	 * @return FTPCLient
	 * @throws IOException
	 * @throws PageException
	 */
	private void actionRemoveDir(AFTPClient client) throws IOException, PageException {
		required("directory", directory);

		if (recursive) {
			removeRecursive(client, directory, FTPFile.DIRECTORY_TYPE);
		} else
			client.removeDirectory(directory);

		writeCfftp(client);
	}

	private static void removeRecursive(AFTPClient client, String path, int type) throws IOException {
		// directory
		if (FTPFile.DIRECTORY_TYPE == type) {
			if (!path.endsWith("/"))
				path += "/";
			// first we remove the children
			FTPFile[] children = client.listFiles(path);
			for (FTPFile child : children) {
				if (child.getName().equals(".") || child.getName().equals(".."))
					continue;
				removeRecursive(client, path + child.getName(), child.getType());
			}
			// then the directory itself
			client.removeDirectory(path);

		}
		// file
		else if (FTPFile.FILE_TYPE == type) {
			client.deleteFile(path);
		}
	}

	/**
	 * create a remote directory
	 * 
	 * @return FTPCLient
	 * @throws IOException
	 * @throws PageException
	 */
	private void actionCreateDir(AFTPClient client) throws IOException, PageException {
		required("directory", directory);

		client.makeDirectory(directory);
		writeCfftp(client);
	}

	/**
	 * List data of a ftp connection
	 * 
	 * @return FTPCLient
	 * @throws PageException
	 * @throws IOException
	 */
	private void actionListDir(AFTPClient client) throws PageException, IOException {
		required("name", name);
		required("directory", directory);

		FTPFile[] files = client.listFiles(directory);
		if (files == null)
			files = new FTPFile[0];

		pageContext.setVariable(name, toQuery(files, "ftp", directory, client.getRemoteAddress().getHostName()));
		writeCfftp(client);
	}

	public static lucee.runtime.type.Query toQuery(FTPFile[] files, String prefix, String directory, String hostName)
			throws PageException {

		String[] cols = new String[] { "name", "isdirectory", "lastmodified", "length", "mode", "path", "url", "type",
				"raw", "attributes" };
		String[] types = new String[] { "VARCHAR", "BOOLEAN", "DATE", "DOUBLE", "VARCHAR", "VARCHAR", "VARCHAR",
				"VARCHAR", "VARCHAR", "VARCHAR" };
		CFMLEngine eng = CFMLEngineFactory.getInstance();

		lucee.runtime.type.Query query = eng.getCreationUtil().createQuery(cols, types, 0, "query");

		// translate directory path for display
		if (directory.length() == 0)
			directory = "/";
		else if (directory.startsWith("./"))
			directory = directory.substring(1);
		else if (directory.charAt(0) != '/')
			directory = '/' + directory;
		if (directory.charAt(directory.length() - 1) != '/')
			directory = directory + '/';
		int row;
		for (int i = 0; i < files.length; i++) {
			FTPFile file = files[i];
			if (file.getName().equals(".") || file.getName().equals(".."))
				continue;
			row = query.addRow();
			query.setAt("attributes", row, "");
			query.setAt("isdirectory", row, eng.getCastUtil().toBoolean(file.isDirectory()));
			query.setAt("lastmodified", row,
					eng.getCreationUtil().createDateTime(file.getTimestamp().getTimeInMillis()));
			query.setAt("length", row, eng.getCastUtil().toDouble(file.getSize()));
			query.setAt("mode", row, FTPConstant.getPermissionASInteger(file));
			query.setAt("type", row, FTPConstant.getTypeAsString(file.getType()));
			// query.setAt("permission",row,FTPConstant.getPermissionASInteger(file));
			query.setAt("raw", row, file.getRawListing());
			query.setAt("name", row, file.getName());
			query.setAt("path", row, directory + file.getName());
			query.setAt("url", row, prefix + "://" + hostName + "" + directory + file.getName());
		}
		return query;
	}

	/**
	 * Opens a FTP Connection
	 * 
	 * @return FTPCLinet
	 * @throws IOException
	 * @throws PageException
	 */
	private void actionOpen(AFTPClient client) throws IOException, PageException {
		required("server", server);
		required("username", username);
		// required("password", password);

		writeCfftp(client);
	}

	/**
	 * close an existing ftp connection
	 * 
	 * @return FTPCLient
	 * @throws PageException
	 */
	private void actionClose(AFTPClient client) throws PageException {
		// Just write status - invalidation handled in doEndTag's finally block
		Struct cfftp = writeCfftp(client);
		cfftp.setEL(SUCCEEDED, eng().getCastUtil().toBoolean(client != null));
	}

	/**
	 * send a custom command to the FTP server
	 * 
	 * @return FTPCLient
	 * @throws IOException,
	 *             PageException
	 */
	private void actionQuote(AFTPClient client) throws IOException, PageException {
		required("actionParams", actionParams); // SIZE filename, etc
		String params = "";
		String command = eng().getListUtil().first(actionParams, " ", false);

		if (eng().getListUtil().len(actionParams, " ", false) > 1) { // avoid duplicating single commands like "SYSTEM"
			params = eng().getListUtil().rest(actionParams, " ", false);
		}

		client.sendCommand(command, params);

		Struct cfftp = writeCfftp(client);
		if (cfftp.get(SUCCEEDED) == Boolean.FALSE)
			cfftp.setEL(RETURN_VALUE, (command + " " + params)); // otherwise errortext and returnValue are the same
		stoponerror = false;
	}

	/**
	 * throw an error if the value is empty (null)
	 * 
	 * @param attributeName
	 * @param atttributValue
	 * @throws ApplicationException
	 */
	private void required(String attributeName, String atttributValue) throws PageException {
		if (atttributValue == null)
			throw eng().getExceptionUtil().createApplicationException(
					"Invalid combination of attributes for the tag [ftp]",
					"attribute [" + attributeName + "] is required, if action is [" + action + "]");
	}

	/**
	 * writes cfftp variable
	 * 
	 * @param client
	 * @return FTPCLient
	 * @throws PageException
	 */
	private Struct writeCfftp(AFTPClient client) throws PageException {
		Struct cfftp = eng().getCreationUtil().createStruct();
		if (result == null)
			pageContext.variablesScope().setEL(CFFTP, cfftp);
		else
			pageContext.setVariable(result, cfftp);
		if (client == null) {
			cfftp.setEL(SUCCEEDED, Boolean.FALSE);
			cfftp.setEL(ERROR_CODE, Double.valueOf(-1));
			cfftp.setEL(ERROR_TEXT, "");
			cfftp.setEL(RETURN_VALUE, "");
			return cfftp;
		}

		int repCode = client.getReplyCode();
		String repStr = client.getReplyString(); // there's a trailing NL in the reply string
		if (repStr == null)
			repStr = ""; // no nulls for cfml
		else
			repStr = repStr.trim(); // trim coz I was always seeing a trailing new line
		cfftp.setEL(ERROR_CODE, Double.valueOf(repCode));
		cfftp.setEL(ERROR_TEXT, repStr);
		cfftp.setEL(SUCCEEDED, eng().getCastUtil().toBoolean(client.isPositiveCompletion()));
		cfftp.setEL(RETURN_VALUE, repStr);
		return cfftp;
	}

	/**
	 * check completion status of the client
	 * 
	 * @param client
	 * @return FTPCLient
	 * @throws ApplicationException
	 */
	private boolean checkCompletion(AFTPClient client) throws PageException {
		boolean isPositiveCompletion = client.isPositiveCompletion();
		if (isPositiveCompletion)
			return false;
		if (count++ < retrycount)
			return true;
		if (stoponerror) {
			throw ExceptionUtil.createException(action, client);
		}

		return false;
	}

	/**
	 * get FTP. ... _FILE_TYPE
	 * 
	 * @param file
	 * @return type
	 */
	private int getType(Resource file) {
		if (transferMode == FTPConstant.TRANSFER_MODE_BINARY)
			return AFTPClient.FILE_TYPE_BINARY;
		else if (transferMode == FTPConstant.TRANSFER_MODE_ASCCI)
			return AFTPClient.FILE_TYPE_TEXT;
		else {
			String ext = eng().getResourceUtil().getExtension(file, null);
			if (ext == null || eng().getListUtil().containsNoCase(ASCIIExtensionList, ext, ";", true, false) == -1)
				return AFTPClient.FILE_TYPE_BINARY;
			return AFTPClient.FILE_TYPE_TEXT;
		}
	}

	/**
	 * @return return a new FTP Connection Object
	 */
	private FTPConnection _createConnection() {
		return new FTPConnectionImpl(connectionName, server, username, password, getPort(), timeout, transferMode,
				passive, proxyserver, proxyport, proxyuser, proxypassword, fingerprint, stoponerror, secure, key,
				passphrase);
	}

	/**
	 * @param password
	 *            The password to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @param username
	 *            The username to set.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * The path to the file that contains a private key
	 * 
	 * @param key
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * The passphrase that protects the private key
	 * 
	 * @param passphrase
	 */
	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}

	/**
	 * @param server
	 *            The server to set.
	 */
	public void setServer(String server) {
		this.server = server;
	}

	/**
	 * @param timeout
	 *            The timeout to set.
	 */
	public void setTimeout(double timeout) {
		this.timeout = (int) timeout;
	}

	/**
	 * @param port
	 *            The port to set.
	 */
	public void setPort(double port) {
		this.port = (int) port;
	}

	public int getPort() {
		if (port != -1)
			return port;
		if (secure.equals("FTPS"))
			return PORT_FTPS;
		else if (secure.equals("TRUE"))
			return PORT_SFTP;
		else
			return PORT_FTP;
	}

	/**
	 * @param connection
	 *            The connection to set.
	 */
	public void setConnection(String connection) {
		this.connectionName = connection;
	}

	/**
	 * @param proxyserver
	 *            The proxyserver to set.
	 */
	public void setProxyserver(String proxyserver) {
		this.proxyserver = proxyserver;
	}

	/**
	 * set the value proxyport The port number on the proxy server from which the
	 * object is requested. Default is 80. When used with resolveURL, the URLs of
	 * retrieved documents that specify a port number are automatically resolved to
	 * preserve links in the retrieved document.
	 * 
	 * @param proxyport
	 *            value to set
	 **/
	public void setProxyport(double proxyport) {
		this.proxyport = (int) proxyport;
	}

	/**
	 * set the value username When required by a proxy server, a valid username.
	 * 
	 * @param proxyuser
	 *            value to set
	 **/
	public void setProxyuser(String proxyuser) {
		this.proxyuser = proxyuser;
	}

	/**
	 * set the value password When required by a proxy server, a valid password.
	 * 
	 * @param proxypassword
	 *            value to set
	 **/
	public void setProxypassword(String proxypassword) {
		this.proxypassword = proxypassword;
	}

	/**
	 * @param retrycount
	 *            The retrycount to set.
	 */
	public void setRetrycount(double retrycount) {
		this.retrycount = (int) retrycount;
	}

	/**
	 * @param stoponerror
	 *            The stoponerror to set.
	 */
	public void setStoponerror(boolean stoponerror) {
		this.stoponerror = stoponerror;
	}

	/**
	 * @param passive
	 *            The passive to set.
	 */
	public void setPassive(boolean passive) {
		this.passive = passive;
	}

	/**
	 * @param directory
	 *            The directory to set.
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	public void setRecurse(boolean recursive) {
		this.recursive = recursive;
	}

	/**
	 * @param extensionList
	 *            The aSCIIExtensionList to set.
	 */
	public void setAsciiextensionlist(String extensionList) {
		ASCIIExtensionList = extensionList.toLowerCase().trim();
	}

	/**
	 * @param transferMode
	 *            The transferMode to set.
	 */
	public void setTransfermode(String transferMode) {
		transferMode = transferMode.toLowerCase().trim();
		if (transferMode.equals("binary"))
			this.transferMode = FTPConstant.TRANSFER_MODE_BINARY;
		else if (transferMode.equals("ascci"))
			this.transferMode = FTPConstant.TRANSFER_MODE_ASCCI;
		else
			this.transferMode = FTPConstant.TRANSFER_MODE_AUTO;
	}

	/**
	 * @param localfile
	 *            The localfile to set.
	 */
	public void setLocalfile(String localfile) {
		this.localfile = localfile;
	}

	/**
	 * @param remotefile
	 *            The remotefile to set.
	 */
	public void setRemotefile(String remotefile) {
		this.remotefile = remotefile;
	}

	/**
	 * @param failifexists
	 *            The failifexists to set.
	 */
	public void setFailifexists(boolean failifexists) {
		this.failifexists = failifexists;
	}

	/**
	 * @param _new
	 *            The _new to set.
	 */
	public void setNew(String _new) {
		this._new = _new;
	}

	/**
	 * @param existing
	 *            The existing to set.
	 */
	public void setExisting(String existing) {
		this.existing = existing;
	}

	/**
	 * @param item
	 *            The item to set.
	 */
	public void setItem(String item) {
		this.item = item;
	}

	/**
	 * @param result
	 *            The result to set.
	 */
	public void setResult(String result) {
		this.result = result;
	}

	public void setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
	}

	/**
	 * @param actionParam
	 *            a custom ftp command, used with action="quote"
	 */
	public void setActionparam(String actionParam) {
		this.actionParams = actionParam;
	}
}