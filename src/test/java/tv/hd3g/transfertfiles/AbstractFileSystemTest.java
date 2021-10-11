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
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AbstractFileSystemTest {

	static class DemoAFS implements AbstractFileSystem {

		@Override
		public void close() throws IOException {
		}

		@Override
		public void connect() {
		}

		@Override
		public AbstractFile getFromPath(final String path) {
			final var af = Mockito.mock(AbstractFile.class);
			when(af.getPath()).thenReturn(path);
			return af;
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
		public int reusableHashCode() {
			return 0;
		}

	}

	DemoAFS demoAFS;

	@BeforeEach
	void init() throws Exception {
		demoAFS = new DemoAFS();
	}

	@Test
	void testGetFromPathStringStringArray() {
		final var a = String.valueOf(System.nanoTime());
		final var b = String.valueOf(System.nanoTime());
		final var c = String.valueOf(System.nanoTime());
		final var result = demoAFS.getFromPath(a, b, c);
		assertNotNull(result);
		assertEquals(a + "/" + b + "/" + c, result.getPath());
	}

	@Test
	void testGetFromPathStringNull() {
		final var a = String.valueOf(System.nanoTime());
		final var result = demoAFS.getFromPath(a, null);
		assertNotNull(result);
		assertEquals(a, result.getPath());
	}

	@Test
	void testGetIOBufferSize() {
		assertEquals(0xFFFF, demoAFS.getIOBufferSize());
	}

}
