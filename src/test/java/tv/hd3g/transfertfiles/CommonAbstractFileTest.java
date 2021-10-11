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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static tv.hd3g.transfertfiles.CommonAbstractFile.observableCopyStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class CommonAbstractFileTest {

	static Random random;

	@BeforeAll
	static void initAll() {
		random = new Random();
	}

	@Mock
	AbstractFileSystem<CAF> fs;
	@Mock
	CAF parent;

	CAF caf;
	String path;
	String parentPath;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		parentPath = String.valueOf(System.nanoTime());
		path = parentPath + "/sub-path";
		caf = new CAF(fs, path);

		path = "/" + path;
		parentPath = "/" + parentPath;
	}

	@AfterEach
	void end() throws Exception {
		Mockito.verifyNoMoreInteractions(fs, parent);
	}

	@Test
	void testGetParent() {
		Mockito.when(fs.getFromPath(eq(parentPath))).thenReturn(parent);
		assertEquals(parent, caf.getParent());
		Mockito.verify(fs, only()).getFromPath(eq(parentPath));
	}

	@Test
	void testHidden() {
		assertFalse(caf.isHidden());
		caf = new CAF(fs, parentPath, ".sub-path");
		assertTrue(caf.isHidden());
	}

	@Test
	void testHashCode() {
		assertEquals(new CAF(fs, path).hashCode(), caf.hashCode());
	}

	@Test
	void testEqualsObject() {
		assertEquals(new CAF(fs, path), caf);
	}

	@Test
	void testGetFileSystem() {
		assertEquals(fs, caf.getFileSystem());
	}

	@Test
	void testToString() {
		assertEquals(path, caf.toString());
	}

	@Test
	void testGetPath() {
		assertEquals(path, caf.getPath());
	}

	@Test
	void testGetName() {
		assertEquals("sub-path", caf.getName());
	}

	@Test
	void testObservableCopyStream() throws IOException {
		final var sourceDatas = new byte[200];
		random.nextBytes(sourceDatas);
		final var inputStream = new ByteArrayInputStream(sourceDatas);
		final var outputStream = new ByteArrayOutputStream();

		final var copyCallback = Mockito.mock(SizedStoppableCopyCallback.class);
		when(copyCallback.apply(anyLong())).thenReturn(true);

		final var totalSize = observableCopyStream(inputStream, outputStream, 64, copyCallback);

		assertEquals(sourceDatas.length, totalSize);
		verify(copyCallback, atLeastOnce()).apply(anyLong());

		assertTrue(Arrays.equals(sourceDatas, outputStream.toByteArray()));
	}

	@Test
	void testObservableCopyStream_stop() throws IOException {
		final var sourceDatas = new byte[200];
		random.nextBytes(sourceDatas);
		final var inputStream = new ByteArrayInputStream(sourceDatas);
		final var outputStream = new ByteArrayOutputStream();

		final var copyCallback = Mockito.mock(SizedStoppableCopyCallback.class);
		when(copyCallback.apply(anyLong())).thenReturn(true, true, false);

		final var totalSize = (int) observableCopyStream(inputStream, outputStream, 10, copyCallback);

		assertEquals(30, totalSize);
		verify(copyCallback, Mockito.times(1)).apply(eq(10L));
		verify(copyCallback, Mockito.times(1)).apply(eq(20L));
		verify(copyCallback, Mockito.times(1)).apply(eq(30L));
		verifyNoMoreInteractions(copyCallback);

		assertTrue(Arrays.equals(sourceDatas, 0, totalSize, outputStream.toByteArray(), 0, totalSize));
	}

	@Test
	void testNormalizePath() {
		assertEquals("/a", AbstractFile.normalizePath("a"));
		assertEquals("/a", AbstractFile.normalizePath("a/"));
		assertEquals("/a", AbstractFile.normalizePath("/a/"));
		assertEquals("/a", AbstractFile.normalizePath("/a"));

		assertEquals("/b/c", AbstractFile.normalizePath("b/c"));
		assertEquals("/b/c", AbstractFile.normalizePath("b/c/"));
		assertEquals("/b/c", AbstractFile.normalizePath("/b/c/"));
		assertEquals("/b/c", AbstractFile.normalizePath("/b/c"));

		assertEquals("/", AbstractFile.normalizePath("/"));
		assertEquals("/", AbstractFile.normalizePath(""));

		assertThrows(NullPointerException.class, () -> AbstractFile.normalizePath(null));
		assertThrows(IllegalArgumentException.class, () -> AbstractFile.normalizePath("~"));
		assertThrows(IllegalArgumentException.class, () -> AbstractFile.normalizePath(".."));
	}

	static class CAF extends CommonAbstractFile<AbstractFileSystem<?>> {

		protected CAF(final AbstractFileSystem<?> fileSystem, final String... path) {
			super(fileSystem, path);
		}

		@Override
		public void copyAbstractToLocal(final File localFile, final TransfertObserver observer) {
		}

		@Override
		public void sendLocalToAbstract(final File localFile, final TransfertObserver observer) {
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
			return null;
		}

		@Override
		public CachedFileAttributes toCache() {
			return null;
		}

		@Override
		public Stream<CachedFileAttributes> toCachedList() {
			return null;
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
}
