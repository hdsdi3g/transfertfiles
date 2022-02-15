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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class StoppableOutputStreamTest {

	@Mock
	OutputStream out;

	StoppableOutputStream os;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		os = new StoppableOutputStream(out);
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(out);
	}

	@Test
	void testWriteInt() throws IOException {
		os.write(42);
		Mockito.verify(out, Mockito.only()).write(42);
		os.setStop();
		assertThrows(IOException.class, () -> os.write(42));
		Mockito.verify(out, times(1)).flush();
		Mockito.verify(out, times(1)).close();
	}

	@Test
	void testWriteByteArray() throws IOException {
		final var v = new byte[] { 11 };
		os.write(v);
		Mockito.verify(out, Mockito.only()).write(11);
		os.setStop();
		assertThrows(IOException.class, () -> os.write(v));
		Mockito.verify(out, times(1)).flush();
		Mockito.verify(out, times(1)).close();
	}

	@Test
	void testWriteByteArrayIntInt() throws IOException {
		final var v = new byte[] { 11 };
		os.write(v, 0, 1);
		Mockito.verify(out, Mockito.only()).write(11);
		os.setStop();
		assertThrows(IOException.class, () -> os.write(v, 0, 1));
		Mockito.verify(out, times(1)).flush();
		Mockito.verify(out, times(1)).close();
	}

	@Test
	void testStop() {
		assertFalse(os.isStopped());
		os.setStop();
		assertTrue(os.isStopped());
	}
}
