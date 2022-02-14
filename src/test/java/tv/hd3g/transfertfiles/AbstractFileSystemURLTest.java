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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.UnknownHostException;

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

	@Test
	void testSetTimeout() {
		afs = new AbstractFileSystemURL("file://localhost/" + new File("").getAbsolutePath() + "?timeout=6");
		final var fs = afs.getFileSystem();
		assertNotNull(fs);
		assertTrue(fs instanceof CommonAbstractFileSystem);
		final var cfs = (CommonAbstractFileSystem<?>) fs;
		assertEquals(6000, cfs.timeoutDuration);
	}

	@Test
	void testNotSetTimeout() {
		afs = new AbstractFileSystemURL("file://localhost/" + new File("").getAbsolutePath());
		final var fs = afs.getFileSystem();
		assertNotNull(fs);
		assertTrue(fs instanceof CommonAbstractFileSystem);
		final var cfs = (CommonAbstractFileSystem<?>) fs;
		assertEquals(0, cfs.timeoutDuration);
	}

	@Test
	void testSetInvalidTimeout() {
		final var url = "file://localhost/" + new File("").getAbsolutePath() + "?timeout=NOPE";
		assertThrows(NumberFormatException.class, () -> new AbstractFileSystemURL(url));
	}

	@Test
	void testBadResolve() throws IOException {
		final var url = "ftpes://this-host-dont-exists/";
		try (var afs = new AbstractFileSystemURL(url)) {
			fail("Expect exception");
		} catch (final UncheckedIOException e) {
			assertTrue(e.getCause().getClass().isAssignableFrom(UnknownHostException.class));
			assertEquals("Can't resolve hostname: \"this-host-dont-exists\"", e.getMessage());
		}
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
