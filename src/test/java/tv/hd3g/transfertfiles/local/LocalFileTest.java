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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.AbstractFileSystem;
import tv.hd3g.transfertfiles.TestFileToolkit;

class LocalFileTest extends TestFileToolkit<LocalFile> {

	@Override
	protected AbstractFileSystem<LocalFile> createFileSystem() {
		return new LocalFileSystem(getRoot());
	}

	@Nested
	class Specific_File {
		File file;
		LocalFile f;
		LocalFileSystem fs;

		@BeforeEach
		void init() throws IOException {
			FileUtils.cleanDirectory(getRoot());
			file = new File(getRoot(), "temp/existing-file2").getAbsoluteFile();
			write(file);
			fs = (LocalFileSystem) createFileSystem();
			f = fs.getFromPath("temp/existing-file2");
		}

		@Test
		void testGetInternalFile() {
			assertEquals(file, f.getInternalFile());
		}
	}

	@Nested
	class Specific_Dir {
		File file;
		LocalFile f;
		LocalFileSystem fs;

		@BeforeEach
		void init() throws IOException {
			FileUtils.cleanDirectory(getRoot());
			file = new File(getRoot(), "temp/existing-dir2").getAbsoluteFile();
			FileUtils.forceMkdir(file);
			fs = (LocalFileSystem) createFileSystem();
			f = fs.getFromPath("temp/existing-dir2");
		}

		@Test
		void testGetInternalFile() {
			assertEquals(file, f.getInternalFile());
		}
	}

	@Nested
	class Specific_NotExistsFile {
		File file;
		LocalFile f;
		LocalFileSystem fs;

		@BeforeEach
		void init() throws IOException {
			FileUtils.cleanDirectory(getRoot());
			file = new File(getRoot(), "temp/notexisting-file2").getAbsoluteFile();
			fs = (LocalFileSystem) createFileSystem();
			f = fs.getFromPath("temp/notexisting-file2");
		}

		@Test
		void testGetInternalFile() {
			assertEquals(file, f.getInternalFile());
		}
	}
}
