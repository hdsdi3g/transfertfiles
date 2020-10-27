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
package tv.hd3g.transfertfiles.ftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class StoppableInputStreamTest {

	@Mock
	InputStream in;

	StoppableInputStream is;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		Mockito.when(in.read()).thenReturn(42);
		Mockito.when(in.read(any())).thenReturn(42);
		Mockito.when(in.read(any(), anyInt(), anyInt())).thenReturn(42);
		Mockito.when(in.skip(anyLong())).thenReturn(42L);
		Mockito.when(in.available()).thenReturn(42);
		is = new StoppableInputStream(in);
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(in);
	}

	@Test
	void testRead() throws IOException {
		assertEquals(42, is.read());
		Mockito.verify(in, Mockito.only()).read();
		is.setStop();
		assertEquals(-1, is.read());
	}

	@Test
	void testReadByteArray() throws IOException {
		assertEquals(42, is.read(new byte[] { 11 }));
		Mockito.verify(in, Mockito.only()).read(any(), anyInt(), anyInt());
		is.setStop();
		assertEquals(-1, is.read(new byte[] { 11 }));
	}

	@Test
	void testReadByteArrayIntInt() throws IOException {
		assertEquals(42, is.read(new byte[] { 11 }, 0, 0));
		Mockito.verify(in, Mockito.only()).read(any(), anyInt(), anyInt());
		is.setStop();
		assertEquals(-1, is.read(new byte[] { 11 }, 0, 0));
	}

	@Test
	void testReadAllBytes() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> is.readAllBytes());
	}

	@Test
	void testReadNBytesInt() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> is.readNBytes(0));
	}

	@Test
	void testReadNBytesByteArrayIntInt() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> is.readNBytes(new byte[] { 11 }, 0, 0));
	}

	@Test
	void testSkip() throws IOException {
		assertEquals(42, is.skip(11));
		Mockito.verify(in, Mockito.only()).skip(anyLong());
		is.setStop();
		assertEquals(0, is.skip(11));
	}

	@Test
	void testAvailable() throws IOException {
		assertEquals(42, is.available());
		Mockito.verify(in, Mockito.only()).available();
		is.setStop();
		assertEquals(0, is.available());
	}

	@Test
	void testStop() {
		assertFalse(is.isStopped());
		is.setStop();
		assertTrue(is.isStopped());
	}
}
