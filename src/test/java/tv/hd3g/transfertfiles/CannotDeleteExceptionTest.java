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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class CannotDeleteExceptionTest {

	@Mock
	AbstractFile file;
	@Mock
	IOException e;
	@SuppressWarnings("rawtypes")
	@Mock
	AbstractFileSystem fs;

	CannotDeleteException cde;
	String path;
	String fsName;

	@BeforeEach
	@SuppressWarnings({ "unchecked" })
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();

		path = String.valueOf(System.nanoTime());
		fsName = String.valueOf(System.nanoTime());
		Mockito.when(file.getPath()).thenReturn(path);
		Mockito.when(file.getFileSystem()).thenReturn(fs);
		Mockito.when(fs.toString()).thenReturn(fsName);

		cde = new CannotDeleteException(file, true, e);
	}

	@Test
	void testGetFileSystemName() {
		assertEquals(fsName, cde.getFileSystemName());

	}

	@Test
	void testGetPath() {
		assertEquals(path, cde.getPath());
	}

	@Test
	void testIsDirectory() {
		assertTrue(cde.isDirectory());
		cde = new CannotDeleteException(file, false, e);
		assertFalse(cde.isDirectory());
	}
}
