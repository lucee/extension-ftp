<!--- 
 *
 * Copyright (c) 2016, Lucee Association Switzerland. All rights reserved.*
 * Copyright (c) 2014, the Railo Company LLC. All rights reserved.
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
 ---><cfscript>
component extends="org.lucee.cfml.test.LuceeTestCase" labels="ftp" {

	function beforeAll() {
		variables.settings = getApplicationSettings();
	}

	function afterAll() {
		application action="update" mappings=variables.settings.mappings;
	}

	// ========================================================================
	// Main Test Entry Points
	// ========================================================================

	public void function testFTP() {
		var ftp = getFTPCredentials();
		if (!structCount(ftp)) return;
		
		test(
			label: "ftp",
			root: buildFtpRoot(false, ftp.server, ftp.port, ftp.username, ftp.password, ftp.base_path)
		);
	}

	public void function testFTPAsMapping() {
		var ftp = getFTPCredentials();
		if (!structCount(ftp)) return;
		
		var mappingName = "/testResFtp";
		addMapping(
			mappingName,
			buildFtpRoot(false, ftp.server, ftp.port, ftp.username, ftp.password, ftp.base_path)
		);
		
		test(
			label: "ftp-mapping",
			root: mappingName & "/"
		);
	}

	public void function testSFTP() {
		var sftp = getSFTPCredentials();
		if (!structCount(sftp)) return;
		
		test(
			label: "sftp",
			root: buildFtpRoot(true, sftp.server, sftp.port, sftp.username, sftp.password, sftp.base_path)
		);
	}

	public void function testSFTPAsMapping() {
		var sftp = getSFTPCredentials();
		if (!structCount(sftp)) return;
		
		var mappingName = "/testResSftp";
		addMapping(
			mappingName,
			buildFtpRoot(true, sftp.server, sftp.port, sftp.username, sftp.password, sftp.base_path)
		);
		
		test(
			label: "sftp-mapping",
			root: mappingName & "/"
		);
	}

	public void function testFTPS() {
		var ftps = getFTPSCredentials();
		if (!structCount(ftps)) return;
		
		test(
			label: "ftps",
			root: buildFtpRoot("FTPS", ftps.server, ftps.port, ftps.username, ftps.password, ftps.base_path)
		);
	}

	public void function testFTPSAsMapping() {
		var ftps = getFTPSCredentials();
		if (!structCount(ftps)) return;
		
		var mappingName = "/testResFtps";
		addMapping(
			mappingName,
			buildFtpRoot("FTPS", ftps.server, ftps.port, ftps.username, ftps.password, ftps.base_path)
		);
		
		test(
			label: "ftps-mapping",
			root: mappingName & "/"
		);
	}

	public void function testFTPSupported() {
		if (!hasResourceProviderSchemeName("ftp")) {
			throw "there is no [ftp] resource provider, only the following providers are available [#arrayToList(getResourceProviderSchemeNames())#]";
		}
	}

	public void function testSFTPSupported() {
		if (!hasResourceProviderSchemeName("sftp")) {
			throw "there is no [sftp] resource provider, only the following providers are available [#arrayToList(getResourceProviderSchemeNames())#]";
		}
	}

	// ========================================================================
	// Core VFS Test Suite (matches S3/ZIP pattern)
	// ========================================================================

	private void function test(required string label, required string root) {
		var start = getTickCount();
		var dir = arguments.root & "lucee-ftp-test-#lcase(hash(createGUID()))#/";

		// Make sure there are no data from a previous run
		if (directoryExists(dir)) {
			directory directory="#dir#" action="delete" recurse="yes";
		}
		directory directory="#dir#" action="create";
		
		var error = {};
		try {
			assertTrue(directoryExists(dir));
			directoryCreateDelete(arguments.label, dir);
			dirList(arguments.label, dir);
			dirRename(arguments.label, dir);
			fileACopy(arguments.label, dir);
			fileAMove(arguments.label, dir);
			fileAReadAppend(arguments.label, dir);
			fileAReadBinary(arguments.label, dir);
			testResourceProvider(dir & "testcaseres1");
		} catch (e) {
			error = e;
		} finally {
			if (directoryExists(dir)) {
				directory directory="#dir#" action="delete" recurse="yes";
			}
		}
		
		if (structCount(error)) {
			throw (message="test failed - #arguments.label#", cause=error);
		}
		
		assertFalse(directoryExists(dir));
	}

	// ========================================================================
	// Directory Tests
	// ========================================================================

	private void function directoryCreateDelete(string label, string dir) {
		var sub = arguments.dir & "test1/";
		var subsub = sub & "test2/";

		// Before doing anything it should not exist
		assertFalse(directoryExists(sub));
		assertFalse(directoryExists(subsub));

		// Create the dirs
		directory directory="#sub#" action="create";
		directory directory="#subsub#" action="create";

		// Now it should exist
		assertTrue(directoryExists(sub));
		assertTrue(directoryExists(subsub));

		// Delete them again
		directory directory="#subsub#" action="delete" recurse="no";
		directory directory="#sub#" action="delete" recurse="no";

		// Should be gone again
		assertFalse(directoryExists(sub));
		assertFalse(directoryExists(subsub));

		// Create the dirs again
		directory directory="#sub#" action="create";
		directory directory="#subsub#" action="create";

		// Now it should exist
		assertTrue(directoryExists(sub));
		assertTrue(directoryExists(subsub));

		// Delete them again recursively
		directory directory="#sub#" action="delete" recurse="yes";

		// Should be gone again
		assertFalse(directoryExists(sub));
		assertFalse(directoryExists(subsub));

		// Create the dirs again
		directory directory="#sub#" action="create";
		directory directory="#subsub#" action="create";

		// Now it should exist
		assertTrue(directoryExists(sub));
		assertTrue(directoryExists(subsub));

		// This must throw an exception
		var hasException = false;
		try {
			// Can not remove directory - directory is not empty
			directory directory="#sub#" action="delete" recurse="no";
		} catch (local.e) {
			hasException = true;
		}
		assertTrue(hasException);

		directory directory="#sub#" action="delete" recurse="yes";

		// Should be gone again
		assertFalse(directoryExists(sub));
		assertFalse(directoryExists(subsub));
	}

	private void function dirList(string label, string dir) {
		var children = "";
		var sd = arguments.dir & "test1/";
		var sf = arguments.dir & "test2.txt";
		var sdsd = sd & "test3/";
		var sdsf = sd & "test4.txt";
		var sdsdsf = sdsd & "test5.txt";
		var error = {};
		
		try {
			directory directory="#dir#" action="list" name="children" recurse="no";
			assertEquals(0, children.recordcount);

			// Create the data
			directory directory="#sd#" action="create";
			directory directory="#sdsd#" action="create";
			file action="write" file="#sf#" output="" addnewline="no" fixnewline="no";
			file action="write" file="#sdsf#" output="" addnewline="no" fixnewline="no";
			file action="write" file="#sdsdsf#" output="" addnewline="no" fixnewline="no";

			directory directory="#dir#" action="list" name="children" recurse="no";
			assertEquals(2, children.recordcount);
			assertEquals("test1,test2.txt", listSort(valueList(children.name), 'textnocase'));
			assertEquals("0,0", listSort(valueList(children.size), 'textnocase'));
			assertEquals("Dir,File", listSort(valueList(children.type), 'textnocase'));

			directory directory="#dir#" action="list" name="children" recurse="yes";
			assertEquals(5, children.recordcount);

			directory directory="#dir#" action="list" name="children" recurse="yes" filter="*5.txt";
			assertEquals(1, children.recordcount);
		} catch (e) {
			error = e;
		} finally {
			if (directoryExists(sd)) directory directory="#sd#" action="delete" recurse="yes";
			if (fileExists(sf)) file action="delete" file="#sf#";
		}
		
		if (structCount(error)) {
			throw (message="dirList failed - #label#", cause=error);
		}
	}

	private void function dirRename(string label, string dir) {
		var children = "";
		var sd = arguments.dir & "test1/";
		var sdNew = arguments.dir & "test1New/";
		var sf = arguments.dir & "test2.txt";
		var sdsd = sd & "test3/";
		var sdsdNew = sd & "test3New/";
		var sdsf = sd & "test4.txt";
		var sdsdsf = sdsd & "test5.txt";
		var error = {};
		
		try {
			directory directory="#sd#" action="create";
			directory directory="#sdsd#" action="create";
			file action="write" file="#sf#" output="" addnewline="no" fixnewline="no";
			file action="write" file="#sdsf#" output="" addnewline="no" fixnewline="no";
			file action="write" file="#sdsdsf#" output="" addnewline="no" fixnewline="no";

			directory directory="#dir#" action="list" name="children" recurse="yes";
			assertEquals(
				"test1,test2.txt,test3,test4.txt,test5.txt",
				listSort(valueList(children.name), 'textnocase')
			);

			directory directory="#sdsd#" action="rename" newdirectory="#sdsdNew#";
			directory directory="#sd#" action="rename" newdirectory="#sdNew#";

			directory directory="#dir#" action="list" name="children" recurse="yes";
			assertEquals(
				"test1New,test2.txt,test3New,test4.txt,test5.txt",
				listSort(valueList(children.name), 'text')
			);
		} catch (e) {
			error = e;
		} finally {
			if (directoryExists(sdNew)) directory directory="#sdNew#" action="delete" recurse="yes";
			if (fileExists(sf)) file action="delete" file="#sf#";
		}
		
		if (structCount(error)) {
			throw (message="dirRename failed - #label#", cause=error);
		}
	}

	// ========================================================================
	// File Tests
	// ========================================================================

	private void function fileACopy(string label, string dir) {
		var children = "";
		var s = arguments.dir & "copy1.txt";
		var d = arguments.dir & "copy2.txt";
		var sd = arguments.dir & "test1/";
		var sdsf = sd & "test4.txt";
		var error = {};
		
		try {
			directory directory="#sd#" action="create";
			file action="write" file="#s#" output="aaa" addnewline="no" fixnewline="no";
			file action="copy" source="#s#" destination="#d#";
			file action="copy" source="#s#" destination="#sdsf#";

			directory directory="#dir#" action="list" name="children" recurse="yes";
			assertEquals(
				"copy1.txt,copy2.txt,test1,test4.txt",
				listSort(valueList(children.name), 'text')
			);
		} catch (e) {
			error = e;
		} finally {
			if (directoryExists(sd)) directory directory="#sd#" action="delete" recurse="yes";
			if (fileExists(s)) file action="delete" file="#s#";
			if (fileExists(d)) file action="delete" file="#d#";
		}
		
		if (structCount(error)) {
			throw (message="fileACopy failed - #label#", cause=error);
		}
	}

	private void function fileAMove(string label, string dir) {
		var children = "";
		var s = arguments.dir & "move1.txt";
		var d = arguments.dir & "move2.txt";
		var sd = arguments.dir & "test1/";
		var sdsf = sd & "test4.txt";
		var error = {};
		
		try {
			directory directory="#sd#" action="create";
			file action="write" file="#s#" output="" addnewline="no" fixnewline="no";
			file action="move" source="#s#" destination="#d#";
			file action="move" source="#d#" destination="#sdsf#";

			directory directory="#dir#" action="list" name="children" recurse="yes";
			assertEquals("test1,test4.txt", valueList(children.name));
		} catch (e) {
			error = e;
		} finally {
			if (directoryExists(sd)) directory directory="#sd#" action="delete" recurse="yes";
		}
		
		if (structCount(error)) {
			throw (message="fileAMove failed - #label#", cause=error);
		}
	}

	private void function fileAReadAppend(string label, string dir) {
		var content = "";
		var s = arguments.dir & "read.txt";
		var error = {};
		
		try {
			file action="write" file="#s#" output="Write" addnewline="no" fixnewline="no";
			file action="append" addnewline="no" file="#s#" output="Append" fixnewline="no";
			file action="read" file="#s#" variable="content";
			assertEquals("WriteAppend", content);
		} catch (e) {
			error = e;
		} finally {
			if (fileExists(s)) file action="delete" file="#s#";
		}
		
		if (structCount(error)) {
			throw (message="fileAReadAppend failed - #label#", cause=error);
		}
	}

	private void function fileAReadBinary(string label, string dir) {
		var content = "";
		var s = arguments.dir & "read.gif";
		var error = {};
		
		try {
			file action="write" file="#s#" output="Susi" addnewline="no" fixnewline="no";
			file action="readbinary" file="#s#" variable="content";
			assertEquals("U3VzaQ==", toBase64(content));
		} catch (e) {
			error = e;
		} finally {
			if (fileExists(s)) file action="delete" file="#s#";
		}
		
		if (structCount(error)) {
			throw (message="fileAReadBinary failed - #label#", cause=error);
		}
	}

	// ========================================================================
	// Resource Provider Tests
	// ========================================================================

	private function testResourceProvider(string path) localmode=true {
		var res = createObject('java','lucee.commons.io.res.util.ResourceUtil')
			.toResourceNotExisting(getPageContext(), path);
		var error = {};

		// Delete when exists
		if (res.exists()) res.remove(true);

		// Test create/delete directory
		try {
			res.createDirectory(true);
			testResourceDirectoryCreateDelete(res);
		} finally {
			if (res.exists()) res.remove(true);
		}

		// Test create/delete file
		try {
			res.createDirectory(true);
			testResourceFileCreateDelete(res);
		} catch (e) {
			error = e;
		} finally {
			if (res.exists()) res.remove(true);
		}

		// Test listening
		try {
			res.createDirectory(true);
			testResourceListening(res);
		} catch (e) {
			error = e;
		} finally {
			if (res.exists()) res.remove(true);
		}

		// Test "is"
		try {
			res.createDirectory(true);
			testResourceIS(res);
		} catch (e) {
			error = e;
		} finally {
			if (res.exists()) res.remove(true);
		}

		// Test move and copy
		try {
			res.createDirectory(true);
			testResourceMoveCopy(res);
		} catch (e) {
			error = e;
		} finally {
			if (res.exists()) res.remove(true);
		}

		// Test Getter
		try {
			res.createDirectory(true);
			testResourceGetter(res);
		} catch (e) {
			error = e;
		} finally {
			if (res.exists()) res.remove(true);
		}

		// Test read/write
		try {
			res.createDirectory(true);
			testResourceReadWrite(res);
		} catch (e) {
			error = e;
		} finally {
			if (res.exists()) res.remove(true);
		}

		if (structCount(error)) {
			throw (message="testResourceProvider failed", cause=error);
		}
	}

	private function testResourceDirectoryCreateDelete(res) localMode=true {
		var sss = res.getRealResource("s/ss");
		var fail = false;

		// Must fail
		try {
			sss.createDirectory(false);
			fail = false;
		} catch (e) {
			fail = true;
		}
		assertTrue(fail);

		// Must work
		sss.createDirectory(true);
		assertTrue(sss.exists());
		assertTrue(sss.getParentResource().exists());

		var s = sss.getParentResource();

		// Must fail - dir with kids
		try {
			s.remove(false);
			fail = false;
		} catch (e) {
			fail = true;
		}
		assertTrue(s.exists());
		assertTrue(fail);

		s.remove(true);
		assertFalse(s.exists());

		// Must fail
		try {
			s.remove(true); // delete
			fail = false;
		} catch (e) {
			fail = true;
		}
		assertTrue(fail);

		assertFalse(sss.exists());

		var d = res.getRealResource("notExist");
		try {
			d.remove(false);
			fail = false;
		} catch (e) {
			fail = true;
		}
		assertTrue(fail);
	}

	private function testResourceFileCreateDelete(res) localMode=true {
		var sss = res.getRealResource("s/ss.txt");
		var fail = false;

		// Must fail
		try {
			sss.createFile(false);
			fail = false;
		} catch (e) {
			fail = true;
		}
		assertTrue(fail);

		// Must work
		sss.createFile(true);
		assertTrue(sss.exists());

		var s = sss.getParentResource();

		// Must fail
		try {
			s.remove(false);
			fail = false;
		} catch (e) {
			fail = true;
		}
		assertTrue(fail);

		s.remove(true);
		assertFalse(sss.exists());
	}

	private function testResourceListening(res) localMode=true {
		var s = res.getRealResource("s/ss.txt");
		s.createFile(true);
		var ss = res.getRealResource("ss/");
		ss.createDirectory(true);
		var sss = res.getRealResource("sss.txt");
		sss.createFile(true);

		// All
		var children = res.list();
		assertEquals("s,ss,sss.txt", listSort(arrayToList(children), "textnoCase"));

		// Filter
		var filter = createObject("java", "lucee.commons.io.res.filter.ExtensionResourceFilter")
			.init(false, ["txt"]);
		children = res.list(filter);
		assertEquals("sss.txt", listSort(arrayToList(children), "textnoCase"));
	}

	private function testResourceIS(res) localMode=true {
		// Must be an existing dir
		assertTrue(res.exists());
		assertTrue(res.isDirectory());
		assertFalse(res.isFile());

		var s = res.getRealResource("s/ss.txt");
		assertFalse(s.exists());
		assertFalse(s.isDirectory());
		assertFalse(s.isFile());

		s = res.getRealResource("ss/");
		assertFalse(s.exists());
		assertFalse(s.isDirectory());
		assertFalse(s.isFile());
	}

	private function testResourceMoveCopy(res) localMode=true {
		var o = res.getRealResource("original.txt");
		o.createFile(true);
		assertTrue(o.exists());

		// Copy
		var c = res.getRealResource("copy.txt");
		assertFalse(c.exists());
		o.copyTo(c, false);
		assertTrue(o.exists());
		assertTrue(c.exists());

		c = res.getRealResource("copy2.txt");
		assertFalse(c.exists());
		c.copyFrom(o, false);
		assertTrue(o.exists());
		assertTrue(c.exists());

		// Move
		var m = res.getRealResource("move.txt");
		assertFalse(m.exists());
		o.moveTo(m);
		assertFalse(o.exists());
		assertTrue(m.exists());
	}

	private function testResourceGetter(res) localMode=true {
		var f = res.getRealResource("original.txt");
		var d = res.getRealResource("dir/");
		var d2 = res.getRealResource("dir2");
		var dd = res.getRealResource("dir/test.txt");

		// Name
		assertEqualPaths("original.txt", f.getName());
		assertEqualPaths("dir", d.getName());
		assertEqualPaths("dir2", d2.getName());

		// Parent
		assertEqualPaths("dir", dd.getParentResource().getName());

		// getRealPath
		assertEqualPaths(res.toString() & "/dir/test.txt", dd.toString());
	}

	private function testResourceReadWrite(res) localMode=true {
		var f = res.getRealResource("original.txt");
		var IOUtil = createObject("java", "lucee.commons.io.IOUtil");

		IOUtil.write(f, "Susi Sorglos", nullValue(), false);
		var result = IOUtil.toString(f, nullValue());
		assertEquals("Susi Sorglos", result);

		IOUtil.write(f, "Susi Sorglos", nullValue(), false);
		result = IOUtil.toString(f, nullValue());
		assertEquals("Susi Sorglos", result);

		IOUtil.write(f, " foehnte Ihr Haar", nullValue(), true);
		result = IOUtil.toString(f, nullValue());
		assertEquals("Susi Sorglos foehnte Ihr Haar", result);
	}

	// ========================================================================
	// Helper Functions
	// ========================================================================

	private string function buildFtpRoot(
		required any secure,
		required string host,
		required numeric port,
		required string username,
		required string password,
		required string basePath
	) {
		var protocol = "ftp://";
		
		if (secure == true) {
			protocol = "sftp://";
		} else if (secure == "FTPS") {
			protocol = "ftps://";
		}

		var credentials = username & ":" & password & "@";
		var portStr = ":" & port;
		
		// Ensure basePath ends with /
		if (right(basePath, 1) != "/") {
			basePath = basePath & "/";
		}

		return protocol & credentials & host & portStr & "/" & basePath;
	}

	private void function addMapping(required string virtual, required string path) {
		var mappings = getApplicationSettings().mappings;
		mappings[virtual] = path;
		application action="update" mappings=mappings;
	}

	private function assertEqualPaths(string path1, string path2) {
		assertEquals(
			replace(path1, "\", "/", "all"),
			replace(path2, "\", "/", "all")
		);
	}

	private array function getResourceProviderSchemeNames() {
		var names = [];
		loop array=getPageContext().getConfig().getResourceProviders() item="local.provider" {
			arrayAppend(names, provider.getScheme());
		}
		return names;
	}

	private boolean function hasResourceProviderSchemeName(required string name) {
		loop array=getPageContext().getConfig().getResourceProviders() item="local.provider" {
			if (provider.getScheme() == arguments.name) return true;
		}
		return false;
	}

	private struct function getFTPCredentials() {
		return server.getTestService("ftp");
	}

	private struct function getFTPSCredentials() {
		return server.getTestService("ftps");
	}

	private struct function getSFTPCredentials() {
		return server.getTestService("sftp");
	}
} 
</cfscript>