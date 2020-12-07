/*
 * This file is part of transfertfiles.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.transfertfiles;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tv.hd3g.transfertfiles.AbstractFileSystemURL.parseUserInfo;
import static tv.hd3g.transfertfiles.AbstractFileSystemURL.protectedSplit;
import static tv.hd3g.transfertfiles.AbstractFileSystemURL.splitURLQuery;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import tv.hd3g.transfertfiles.ftp.FTPESFileSystem;
import tv.hd3g.transfertfiles.ftp.FTPFileSystem;
import tv.hd3g.transfertfiles.ftp.FTPSFileSystem;
import tv.hd3g.transfertfiles.local.LocalFileSystem;
import tv.hd3g.transfertfiles.sftp.SFTPFileSystem;

class AbstractFileSystemURLTest {

	@Nested
	class Specific {

		@Test
		void testURL() throws MalformedURLException {
			var u = new URL("http://user:password@host/path?aa=bb");
			assertEquals("http", u.getProtocol());
			assertEquals("user:password", u.getUserInfo());
			assertEquals("host", u.getHost());
			assertEquals("/path", u.getPath());
			assertEquals("aa=bb", u.getQuery());

			u = new URL("http://user:password@host/");
			assertEquals("/", u.getPath());
			assertNull(u.getQuery());

			// URLDecoder.decode(proxyRequestParam.replace("+", "%2B"), "UTF-8") .replace("%2B", "+")
			u = new URL("http://host/?aa=bb");
			assertEquals("/", u.getPath());
			assertEquals("aa=bb", u.getQuery());

			assertEquals(Map.of("aa", List.of("bb")), splitURLQuery(new URL("http://host/?aa=bb")));
			assertEquals(Map.of("aa", List.of("b b")), splitURLQuery(new URL("http://host/?aa=b b")));
			assertEquals(Map.of("aa", List.of("b b"), "c", List.of("d")),
			        splitURLQuery(new URL("http://host/?aa=b b&c=d")));
			assertEquals(Map.of("aa", List.of("b/b"), "c", List.of("d")),
			        splitURLQuery(new URL("http://host/?aa=b/b&c=d")));
			assertEquals(Map.of("aa", List.of("b\\b"), "c", List.of("d")),
			        splitURLQuery(new URL("http://host/?aa=b\\b&c=d")));

			assertEquals(Map.of("aa", List.of("bb"), "c", List.of("d")),
			        splitURLQuery(new URL("http://host/?aa=bb&c=\"d\"")));
			assertEquals(Map.of("aa", List.of("bb"), "c", List.of("d&d")),
			        splitURLQuery(new URL("http://host/?aa=bb&c=\"d&d\"")));
			assertEquals(Map.of("aa", List.of("bb"),
			        "c", List.of("o&o^o0:o?o|o+o0\\o=O,O#o0@;;o&!0-o_o~oo"),
			        "ee", List.of("ff")),
			        splitURLQuery(new URL("http://host/?aa=bb&c=\"o&o^o0:o?o|o+o0\\o=O,O#o0@;;o&!0-o_o~oo\"&ee=ff")));
		}

		@Test
		void testProtectedSplit() {
			assertEquals(List.of("aaa"), protectedSplit("aaa").collect(toUnmodifiableList()));
			assertEquals(List.of("aaa", "bbb"), protectedSplit("aaa&bbb").collect(toUnmodifiableList()));
			assertEquals(List.of("aaa", "bbb", "ccc"), protectedSplit("aaa&bbb&ccc").collect(toUnmodifiableList()));
			assertEquals(List.of("aaabbbccc"), protectedSplit("aaa\"bbb\"ccc").collect(toUnmodifiableList()));
			assertEquals(List.of("aaab&bccc"), protectedSplit("aaa\"b&b\"ccc").collect(toUnmodifiableList()));
			assertEquals(List.of("aa", "ab&bc", "cc"), protectedSplit("aa&a\"b&b\"c&cc").collect(toUnmodifiableList()));
			assertEquals(List.of("bbb"), protectedSplit("&bbb").collect(toUnmodifiableList()));
			assertEquals(List.of("aaa"), protectedSplit("aaa&").collect(toUnmodifiableList()));
		}

		@Test
		void testSpecificURL() throws MalformedURLException {
			AbstractFileSystemURL.class.getName();
			assertEquals("sftp", new URL("sftp://host").getProtocol());
			assertEquals("ftp", new URL("ftp://host").getProtocol());
			assertEquals("ftps", new URL("ftps://host").getProtocol());
			assertEquals("ftpes", new URL("ftpes://host").getProtocol());
			assertThrows(MalformedURLException.class, () -> new URL("noset://host"));

			final var u = new URL("sftp://host");
			assertThrows(IllegalAccessError.class, () -> u.openConnection());
		}

		@Test
		void testParseUserInfo() {
			final var defaultPassword = String.valueOf(System.nanoTime());
			final var user = String.valueOf(System.nanoTime());
			final var password = String.valueOf(System.nanoTime());

			checkSimpleImmutableEntry(null, defaultPassword, parseUserInfo(null, defaultPassword));
			checkSimpleImmutableEntry(null, defaultPassword, parseUserInfo("", defaultPassword));
			checkSimpleImmutableEntry(user, defaultPassword, parseUserInfo(user, defaultPassword));
			checkSimpleImmutableEntry(user + ":", defaultPassword, parseUserInfo(user + ":", defaultPassword));
			checkSimpleImmutableEntry(null, ":" + password, parseUserInfo(":" + password, defaultPassword));
			checkSimpleImmutableEntry(user, password, parseUserInfo(user + ":" + password, defaultPassword));
			checkSimpleImmutableEntry(user, password + ":",
			        parseUserInfo(user + ":" + password + ":", defaultPassword));
			checkSimpleImmutableEntry(user, password + ":" + password,
			        parseUserInfo(user + ":" + password + ":" + password, defaultPassword));
		}

		private void checkSimpleImmutableEntry(final String k,
		                                       final String v,
		                                       final SimpleImmutableEntry<String, String> entry) {
			assertNotNull(entry);
			assertEquals(k, entry.getKey(), "Invalid key");
			assertEquals(v, entry.getValue(), "Invalid value");
		}
	}

	AbstractFileSystemURL afs;

	@BeforeEach
	void init() {
		afs = new AbstractFileSystemURL("ftpes://user:secret0@localhost/basePath?password=secret1&active");
	}

	@Test
	void testGetFileSystem_FTPES() throws UnknownHostException {
		final var fs = afs.getFileSystem();
		assertNotNull(fs);
		assertTrue(fs instanceof FTPESFileSystem);
		final var fs0 = (FTPESFileSystem) fs;
		assertFalse(fs0.isPassiveMode());
		assertTrue(fs0.getHost().isLoopbackAddress());
		assertEquals("user", fs0.getUsername());

	}

	@Test
	void testGetFileSystem_FTPS() throws UnknownHostException {
		afs = new AbstractFileSystemURL("ftps://user:secret0@localhost/basePath?password=secret1&active");
		final var fs = afs.getFileSystem();
		assertNotNull(fs);
		assertTrue(fs instanceof FTPSFileSystem);
		final var fs0 = (FTPSFileSystem) fs;
		assertFalse(fs0.isPassiveMode());
		assertTrue(fs0.getHost().isLoopbackAddress());
		assertEquals("user", fs0.getUsername());

	}

	@Test
	void testGetFileSystem_FTP() throws UnknownHostException {
		afs = new AbstractFileSystemURL("ftp://user:secret0@localhost/basePath?password=secret1&active");
		final var fs = afs.getFileSystem();
		assertNotNull(fs);
		assertTrue(fs instanceof FTPFileSystem);
		final var ftpes = (FTPFileSystem) fs;
		assertFalse(ftpes.isPassiveMode());
		assertTrue(ftpes.getHost().isLoopbackAddress());
		assertEquals("user", ftpes.getUsername());
	}

	@Test
	void testGetFileSystem_SFTP() throws UnknownHostException {
		afs = new AbstractFileSystemURL("sftp://user:secret0@localhost/basePath?password=secret1");
		final var fs = afs.getFileSystem();
		assertNotNull(fs);
		assertTrue(fs instanceof SFTPFileSystem);
		final var fs0 = (SFTPFileSystem) fs;
		assertTrue(fs0.getHost().isLoopbackAddress());
		assertEquals("user", fs0.getUsername());
	}

	@Test
	void testGetFileSystem_File() throws UnknownHostException {
		afs = new AbstractFileSystemURL("file://localhost/" + new File("").getAbsolutePath());
		final var fs = afs.getFileSystem();
		assertNotNull(fs);
		assertTrue(fs instanceof LocalFileSystem);
	}

	@Test
	void testGetFileSystem_passive() {
		afs = new AbstractFileSystemURL("ftpes://user:secret0@localhost/");
		final var fs = afs.getFileSystem();
		assertNotNull(fs);
		assertTrue(fs instanceof FTPESFileSystem);
		final var ftpes = (FTPESFileSystem) fs;
		assertTrue(ftpes.isPassiveMode());
	}

	@Test
	void testToString() {
		assertEquals("ftpes://user:*******@localhost/basePath?password=secret1&active", afs.toString());
		afs = new AbstractFileSystemURL("ftpes://user:sec:ret@localhost/?password=sec:ret");
		assertEquals("ftpes://user:*******@localhost/?password=*******", afs.toString());
		afs = new AbstractFileSystemURL("ftpes://user@localhost/?password=sec:ret");
		assertEquals("ftpes://user@localhost/?password=*******", afs.toString());
	}

	static class TestAbstractFileSystemURL extends AbstractFileSystemURL {

		public TestAbstractFileSystemURL(final String protectedRessourceURL,
		                                 final AbstractFileSystem<?> fileSystem,
		                                 final String basePath) {
			super(protectedRessourceURL, fileSystem, basePath);
		}
	}

	@Nested
	class FakedFileSystem {
		@Mock
		AbstractFileSystem<AbstractFile> fileSystem;
		@Mock
		AbstractFile f;
		String protectedRessourceURL;
		String basePath;

		@BeforeEach
		void init() throws Exception {
			MockitoAnnotations.openMocks(this).close();

			protectedRessourceURL = String.valueOf(System.nanoTime());
			basePath = String.valueOf(System.nanoTime());
			afs = new TestAbstractFileSystemURL(protectedRessourceURL, fileSystem, basePath);
		}

		@AfterEach
		void end() {
			Mockito.verifyNoMoreInteractions(fileSystem, f);
		}

		@Test
		void testGetFromPath() {
			final var path = String.valueOf(System.nanoTime());
			final var fullPath = "/" + path;
			when(fileSystem.getFromPath(eq(fullPath))).thenReturn(f);
			final var p = afs.getFromPath(path);
			assertNotNull(p);
			assertEquals(f, p);
			verify(fileSystem, times(1)).connect();
			verify(fileSystem, times(1)).getFromPath(eq(fullPath));
		}

		@Test
		void testGetBasePath() {
			assertEquals(basePath, afs.getBasePath());
		}

		@Test
		void testGetRootPath() {
			when(fileSystem.getFromPath(eq("/"))).thenReturn(f);
			final var r = afs.getRootPath();
			assertNotNull(r);
			assertEquals(f, r);
			verify(fileSystem, times(1)).connect();
			verify(fileSystem, times(1)).getFromPath(eq("/"));
		}

		@Test
		void testClose() throws IOException {
			afs.close();
			verify(fileSystem, times(1)).close();
		}

		@Test
		void testIsReusable() {
			when(fileSystem.isReusable()).thenReturn(false);
			assertFalse(afs.isReusable());
			when(fileSystem.isReusable()).thenReturn(true);
			assertTrue(afs.isReusable());
			verify(fileSystem, times(2)).isReusable();
		}

	}
}
