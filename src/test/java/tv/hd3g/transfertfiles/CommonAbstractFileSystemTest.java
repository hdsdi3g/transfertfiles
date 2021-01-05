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

import static java.util.concurrent.TimeUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonAbstractFileSystemTest {

	String basePath;
	TestCAFS cafs;

	@BeforeEach
	void init() throws Exception {
		basePath = String.valueOf(System.nanoTime());
		cafs = new TestCAFS(basePath);
	}

	@Test
	void testHashCode() throws IOException {
		final var t = new TestCAFS(basePath);
		assertEquals(t.hashCode(), cafs.hashCode());
		t.close();
	}

	@Test
	void testEqualsObject() throws IOException {
		final var t = new TestCAFS(basePath);
		assertEquals(t, cafs);
		t.close();
	}

	@Test
	void testGetPathFromRelative() {
		assertEquals("/" + basePath, cafs.getPathFromRelative(""));
		assertEquals("/" + basePath + "/AAAAA", cafs.getPathFromRelative("AAAAA"));
		assertEquals("/" + basePath + "/AA/AA/A", cafs.getPathFromRelative("AA/AA/A"));
		assertEquals("/" + basePath + "/AAA", cafs.getPathFromRelative("/AAA"));
		assertThrows(IllegalArgumentException.class, () -> cafs.getPathFromRelative("/A/../AA"));
	}

	@Test
	void testGetBasePath() {
		assertEquals("/" + basePath, cafs.getBasePath());
	}

	@Test
	void testSetTimeout() {
		cafs.setTimeout(10, DAYS);
		assertEquals(DAYS.toMillis(10), cafs.timeoutDuration);
		assertThrows(IllegalArgumentException.class, () -> cafs.setTimeout(-10, DAYS));
		assertThrows(IllegalArgumentException.class, () -> cafs.setTimeout(0, DAYS));
		assertThrows(IllegalArgumentException.class, () -> cafs.setTimeout(2 ^ 30, DAYS));
	}

	@Test
	void testGetTimeout() {
		assertEquals(0, cafs.getTimeout());
		cafs.setTimeout(10, DAYS);
		assertEquals(TimeUnit.DAYS.toMillis(10), cafs.getTimeout());
	}

	static class TestCAFS extends CommonAbstractFileSystem<AbstractFile> {

		protected TestCAFS(final String basePath) {
			super(basePath);
		}

		@Override
		public void connect() {
		}

		@Override
		public AbstractFile getFromPath(final String path) {
			return null;
		}

		@Override
		public boolean isReusable() {
			return false;
		}

		@Override
		public boolean isAvaliable() {
			return false;
		}

		@Override
		public void close() throws IOException {
		}

		@Override
		public int reusableHashCode() {
			return 0;
		}
	}
}
