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
package tv.hd3g.transfertfiles.local;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.InvalidPathException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tv.hd3g.commons.IORuntimeException;

class LocalFileSystemTest {

	static File root;

	@BeforeAll
	static void load() throws Exception {
		root = new File("target/testfs");
		FileUtils.forceMkdir(root);
		FileUtils.cleanDirectory(root);
	}

	@AfterAll
	static void ends() throws Exception {
		FileUtils.cleanDirectory(root);
	}

	LocalFileSystem fs;

	@BeforeEach
	void init() {
		fs = new LocalFileSystem(root);
	}

	@Test
	void testGetFromPath_ok() {
		assertEquals(new File(root, "this/path").getAbsoluteFile(),
		        fs.getFromPath("this/path").getInternalFile());
		assertEquals(new File(root, "/path").getAbsoluteFile(),
		        fs.getFromPath("this/../path").getInternalFile());
	}

	@Test
	void testIsReusable() {
		assertTrue(fs.isReusable());
	}

	@Test
	void testIsAvaliable() {
		assertTrue(fs.isAvaliable());
	}

	@Test
	void testGetFromPath_outsideRoot() {
		assertThrows(IORuntimeException.class, () -> fs.getFromPath("../path"));
		assertThrows(IORuntimeException.class, () -> fs.getFromPath("sub/../../path"));
		assertThrows(IORuntimeException.class, () -> fs.getFromPath("path/../.."));
	}

	@Test
	void testGetFromPath_error() {
		assertThrows(InvalidPathException.class, () -> fs.getFromPath("\0"));
	}

	@Test
	void testGetRelativePath() {
		assertEquals(root.getAbsoluteFile(), fs.getRelativePath());
	}

	@Test
	void testConnect() {
		assertDoesNotThrow(() -> fs.connect());
	}

	@Test
	void testClose() {
		assertDoesNotThrow(() -> fs.close());
	}

	@Test
	void testLocalFileSystem_error() {
		final var f0 = new File("zz:/dfdf\\dd");
		assertThrows(IORuntimeException.class, () -> new LocalFileSystem(f0));
		final var f1 = new File("a:/thisFileDontExists");
		assertThrows(IORuntimeException.class, () -> new LocalFileSystem(f1));
	}

	@Test
	void testEqualshashCode() {
		final var lfs = new LocalFileSystem(root);
		assertEquals(lfs, fs);
		assertEquals(lfs.hashCode(), fs.hashCode());
		lfs.close();
	}

	@Test
	void testToString() {
		final var str = fs.toString();
		final var compare = root.getAbsolutePath().replace('\\', '/');
		assertTrue(str.startsWith("file://"));
		assertTrue(str.endsWith(compare));
	}
}
