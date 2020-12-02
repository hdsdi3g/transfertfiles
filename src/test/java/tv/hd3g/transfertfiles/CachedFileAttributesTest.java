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
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class CachedFileAttributesTest {

	static Random rnd = new Random();

	@Mock
	AbstractFile f;

	String path;
	long length;
	long lastModified;
	boolean exists;
	boolean directory;
	boolean file;
	boolean link;
	boolean special;
	CachedFileAttributes c;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();

		path = "/" + String.valueOf(rnd.nextLong());
		length = rnd.nextLong();
		lastModified = rnd.nextLong();
		exists = rnd.nextBoolean();
		directory = rnd.nextBoolean();
		file = rnd.nextBoolean();
		link = rnd.nextBoolean();
		special = rnd.nextBoolean();

		when(f.getPath()).thenReturn(path);
		when(f.length()).thenReturn(length);
		when(f.lastModified()).thenReturn(lastModified);
		when(f.exists()).thenReturn(exists);
		when(f.isDirectory()).thenReturn(directory);
		when(f.isFile()).thenReturn(file);
		when(f.isLink()).thenReturn(link);
		when(f.isSpecial()).thenReturn(special);

		c = new CachedFileAttributes(f, length, lastModified, exists, directory, file, link, special);
		Mockito.verify(f, Mockito.only()).getPath();
	}

	@AfterEach
	void end() {
		Mockito.verifyNoMoreInteractions(f);
	}

	@Test
	void testCachedFileAttributesAbstractFile() {
		c = new CachedFileAttributes(f);
		Mockito.verify(f, Mockito.times(2)).getPath();
		Mockito.verify(f, Mockito.atLeastOnce()).length();
		Mockito.verify(f, Mockito.atLeastOnce()).lastModified();
		Mockito.verify(f, Mockito.atLeastOnce()).exists();
		Mockito.verify(f, Mockito.atLeastOnce()).isDirectory();
		Mockito.verify(f, Mockito.atLeastOnce()).isFile();
		Mockito.verify(f, Mockito.atLeastOnce()).isLink();
		Mockito.verify(f, Mockito.atLeastOnce()).isSpecial();

		assertEquals(f, c.getAbstractFile());
		assertEquals(path.substring(1), c.getName());
		assertEquals(path, c.getPath());
		assertFalse(c.isHidden());
		assertEquals(length, c.length());
		assertEquals(lastModified, c.lastModified());
		assertEquals(exists, c.exists());
		assertEquals(directory, c.isDirectory());
		assertEquals(file, c.isFile());
		assertEquals(link, c.isLink());
		assertEquals(special, c.isSpecial());
	}

	@Test
	void testNotExists() {
		c = CachedFileAttributes.notExists(f);
		assertFalse(c.isHidden());
		assertFalse(c.exists());
		assertFalse(c.isDirectory());
		assertFalse(c.isFile());
		assertFalse(c.isLink());
		assertFalse(c.isSpecial());
		assertEquals(0, c.lastModified());
		assertEquals(0, c.length());
		assertEquals(path.substring(1), c.getName());
		assertEquals(path, c.getPath());

		Mockito.verify(f, Mockito.times(2)).getPath();
	}

	@Test
	void testGetAbstractFile() {
		assertEquals(f, c.getAbstractFile());
	}

	@Test
	void testGetName() {
		assertEquals(path.substring(1), c.getName());
	}

	@Test
	void testGetPath() {
		assertEquals(path, c.getPath());
	}

	@Test
	void testGetParentPath() {
		assertEquals("/", c.getParentPath());

		when(f.getPath()).thenReturn("/aaa/bbb");
		c = new CachedFileAttributes(f, length, lastModified, exists, directory, file, link, special);
		Mockito.verify(f, Mockito.times(2)).getPath();
		assertEquals("/aaa", c.getParentPath());
	}

	@Test
	void testIsHidden() {
		assertFalse(c.isHidden());
	}

	@Test
	void testLength() {
		assertEquals(length, c.length());
	}

	@Test
	void testLastModified() {
		assertEquals(lastModified, c.lastModified());
	}

	@Test
	void testExists() {
		assertEquals(exists, c.exists());
	}

	@Test
	void testIsDirectory() {
		assertEquals(directory, c.isDirectory());
	}

	@Test
	void testIsFile() {
		assertEquals(file, c.isFile());
	}

	@Test
	void testIsLink() {
		assertEquals(link, c.isLink());
	}

	@Test
	void testIsSpecial() {
		assertEquals(special, c.isSpecial());
	}

	@Test
	void testEqualsObject() {
		assertEquals(CachedFileAttributes.notExists(f), c);
		Mockito.verify(f, Mockito.times(2)).getPath();
	}

	@Test
	void testHashCode() {
		assertEquals(CachedFileAttributes.notExists(f).hashCode(), c.hashCode());
		Mockito.verify(f, Mockito.times(2)).getPath();
	}

}
