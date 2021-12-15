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
 * Copyright (C) hdsdi3g for hd3g.tv 2021
 *
 */
package tv.hd3g.transfertfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractFileTest {

	static class DemoAF implements AbstractFile {

		final String path;

		DemoAF(final String path) {
			this.path = path;
		}

		@Override
		public AbstractFileSystem<?> getFileSystem() {
			return null;
		}

		@Override
		public void copyAbstractToLocal(final File localFile, final TransfertObserver observer) {
		}

		@Override
		public void sendLocalToAbstract(final File localFile, final TransfertObserver observer) {
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public AbstractFile getParent() {
			return null;
		}

		@Override
		public long length() {
			return 0;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public void delete() {
		}

		@Override
		public boolean isDirectory() {
			return false;
		}

		@Override
		public boolean isFile() {
			return false;
		}

		@Override
		public boolean isLink() {
			return false;
		}

		@Override
		public boolean isSpecial() {
			return false;
		}

		@Override
		public boolean isHidden() {
			return false;
		}

		@Override
		public long lastModified() {
			return 0;
		}

		@Override
		public Stream<AbstractFile> list() {
			return null;
		}

		@Override
		public void mkdir() {
		}

		@Override
		public AbstractFile renameTo(final String path) {
			return new DemoAF(path);
		}

		@Override
		public long downloadAbstract(final OutputStream outputStream,
		                             final int bufferSize,
		                             final SizedStoppableCopyCallback copyCallback) {
			return 0;
		}

		@Override
		public long uploadAbstract(final InputStream inputStream,
		                           final int bufferSize,
		                           final SizedStoppableCopyCallback copyCallback) {
			return 0;
		}

	}

	DemoAF demoAF;

	@BeforeEach
	void init() throws Exception {
		demoAF = new DemoAF(null);
	}

	@Test
	void testRenameToStringStringArray() {
		final var a = String.valueOf(System.nanoTime());
		final var b = String.valueOf(System.nanoTime());
		final var c = String.valueOf(System.nanoTime());
		final var result = demoAF.renameTo(a, b, c);
		assertNotNull(result);
		assertEquals(a + "/" + b + "/" + c, result.getPath());
	}

	@Test
	void testGetFromPathStringNull() {
		final var a = String.valueOf(System.nanoTime());
		final var result = demoAF.renameTo(a);
		assertNotNull(result);
		assertEquals(a, result.getPath());
	}

}
