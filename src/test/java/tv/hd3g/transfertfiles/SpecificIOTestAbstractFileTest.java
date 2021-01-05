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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tv.hd3g.transfertfiles.AbstractFile.checkIsSameFileSystem;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.MockUtil;

import tv.hd3g.commons.IORuntimeException;

class SpecificIOTestAbstractFileTest {

	static Random random;

	@BeforeAll
	static void initAll() {
		random = new Random();
	}

	@Mock
	MockAbstractFile destination;
	@Mock
	DataExchangeObserver observer;
	@Mock
	AbstractFileSystem<MockAbstractFile> fsSource;
	@Mock
	AbstractFileSystem<MockAbstractFile> fsDest;

	AbstractFileImpl source;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		MockUtil.resetMock(destination);
		MockUtil.resetMock(observer);
		MockUtil.resetMock(fsSource);
		MockUtil.resetMock(fsDest);

		source = new AbstractFileImpl(fsSource);
		when(destination.getFileSystem()).thenReturn(fsDest);
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(destination, observer, fsSource, fsDest);
	}

	@Test
	void testCopyDefault() {
		source.copyAbstractToAbstract(destination, observer);
		verify(destination, times(1)).uploadAbstract(any(InputStream.class), eq(8192), any(
		        SizedStoppableCopyCallback.class));
		assertEquals(1, source.outputStreams.size());
		assertEquals(1, source.copyCallbacks.size());

		verify(destination, times(2)).getFileSystem();
		verify(fsSource, times(1)).getIOBufferSize();
		verify(fsDest, times(1)).getIOBufferSize();
		verify(observer, times(1)).beforeTransfert(eq(source), eq(destination));
		verify(observer, times(1)).afterTransfert(eq(source), eq(destination),
		        eq(source.copiedFrom), eq(0L), any(Duration.class));
	}

	@Test
	void testSpecific() {
		final var exchange = new DataExchangeInOutStream();
		final var outputStream = exchange.getDestTargetStream();
		final var inputStream = exchange.getSourceOriginStream();

		source.copyAbstractToAbstract(destination, observer, exchange);
		verify(destination, times(1)).uploadAbstract(eq(inputStream), eq(exchange.getBufferSize()),
		        any(SizedStoppableCopyCallback.class));
		assertEquals(1, source.outputStreams.size());
		assertEquals(outputStream, source.outputStreams.get(0));
		assertEquals(1, source.copyCallbacks.size());

		verify(destination, times(1)).getFileSystem();
		verify(observer, times(1)).beforeTransfert(eq(source), eq(destination));
		verify(observer, times(1)).afterTransfert(eq(source), eq(destination),
		        eq(source.copiedFrom), eq(0L), any(Duration.class));
	}

	@Test
	void testCheckIsSameFileSystem_notSame() {
		checkIsSameFileSystem(source, destination);
		verify(destination, only()).getFileSystem();
	}

	@Test
	void testCheckIsSameFileSystem_SameTwoInstances() {
		final var hash = random.nextInt();
		when(destination.getFileSystem()).thenReturn(fsSource);
		when(fsSource.reusableHashCode()).thenReturn(hash);

		assertThrows(IORuntimeException.class, () -> checkIsSameFileSystem(source, destination));

		verify(destination, only()).getFileSystem();
		verify(fsSource, times(2)).reusableHashCode();
	}

	@Test
	void testCheckIsSameFileSystem_NotTwoInstances() {
		final var hash0 = random.nextInt();
		final var hash1 = random.nextInt();
		when(destination.getFileSystem()).thenReturn(fsSource);
		when(fsSource.reusableHashCode()).thenReturn(hash0, hash1);

		checkIsSameFileSystem(source, destination);

		verify(destination, only()).getFileSystem();
		verify(fsSource, times(2)).reusableHashCode();
	}

	class AbstractFileImpl implements AbstractFile {

		final AbstractFileSystem<?> fs;
		final List<OutputStream> outputStreams;
		final List<InputStream> inputStreams;
		final List<SizedStoppableCopyCallback> copyCallbacks;
		long copiedFrom;

		public AbstractFileImpl(final AbstractFileSystem<?> fs) {
			this.fs = fs;
			outputStreams = new ArrayList<>();
			inputStreams = new ArrayList<>();
			copyCallbacks = new ArrayList<>();
			copiedFrom = random.nextLong();
		}

		@Override
		public AbstractFileSystem<?> getFileSystem() {
			return fsSource;
		}

		@Override
		public long uploadAbstract(final InputStream inputStream,
		                           final int bufferSize,
		                           final SizedStoppableCopyCallback copyCallback) {
			inputStreams.add(Objects.requireNonNull(inputStream));
			copyCallbacks.add(Objects.requireNonNull(copyCallback));
			return -1;
		}

		@Override
		public long downloadAbstract(final OutputStream outputStream,
		                             final int bufferSize,
		                             final SizedStoppableCopyCallback copyCallback) {
			outputStreams.add(Objects.requireNonNull(outputStream));
			copyCallbacks.add(Objects.requireNonNull(copyCallback));
			return copiedFrom;
		}

		@Override
		public void copyAbstractToLocal(final File localFile, final TransfertObserver observer) {
			throw new IllegalAccessError();
		}

		@Override
		public void sendLocalToAbstract(final File localFile, final TransfertObserver observer) {
			throw new IllegalAccessError();
		}

		@Override
		public String getPath() {
			throw new IllegalAccessError();
		}

		@Override
		public String getName() {
			throw new IllegalAccessError();
		}

		@Override
		public AbstractFile getParent() {
			throw new IllegalAccessError();
		}

		@Override
		public long length() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean exists() {
			throw new IllegalAccessError();
		}

		@Override
		public void delete() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isDirectory() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isFile() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isLink() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isSpecial() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isHidden() {
			throw new IllegalAccessError();
		}

		@Override
		public long lastModified() {
			throw new IllegalAccessError();
		}

		@Override
		public Stream<AbstractFile> list() {
			throw new IllegalAccessError();
		}

		@Override
		public void mkdir() {
			throw new IllegalAccessError();
		}

		@Override
		public AbstractFile renameTo(final String path) {
			throw new IllegalAccessError();
		}
	}

	class MockAbstractFile implements AbstractFile {
		@Override
		public void copyAbstractToLocal(final File localFile, final TransfertObserver observer) {
			throw new IllegalAccessError();
		}

		@Override
		public void sendLocalToAbstract(final File localFile, final TransfertObserver observer) {
			throw new IllegalAccessError();
		}

		@Override
		public String getPath() {
			throw new IllegalAccessError();
		}

		@Override
		public String getName() {
			throw new IllegalAccessError();
		}

		@Override
		public AbstractFile getParent() {
			throw new IllegalAccessError();
		}

		@Override
		public long length() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean exists() {
			throw new IllegalAccessError();
		}

		@Override
		public void delete() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isDirectory() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isFile() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isLink() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isSpecial() {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isHidden() {
			throw new IllegalAccessError();
		}

		@Override
		public long lastModified() {
			throw new IllegalAccessError();
		}

		@Override
		public Stream<AbstractFile> list() {
			throw new IllegalAccessError();
		}

		@Override
		public void mkdir() {
			throw new IllegalAccessError();
		}

		@Override
		public AbstractFile renameTo(final String path) {
			throw new IllegalAccessError();
		}

		@Override
		public AbstractFileSystem<MockAbstractFile> getFileSystem() {
			throw new IllegalAccessError();
		}

		@Override
		public long downloadAbstract(final OutputStream outputStream,
		                             final int bufferSize,
		                             final SizedStoppableCopyCallback copyCallback) {
			throw new IllegalAccessError();
		}

		@Override
		public long uploadAbstract(final InputStream inputStream,
		                           final int bufferSize,
		                           final SizedStoppableCopyCallback copyCallback) {
			throw new IllegalAccessError();
		}
	}

}
