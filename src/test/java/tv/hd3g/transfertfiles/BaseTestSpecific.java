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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTestSpecific<Fs extends AbstractFileSystem<Af>, Af extends AbstractFile> {// NOSONAR S5786

	protected File file;
	protected Af f;
	protected Fs fs;

	protected abstract File provideRootDir();

	protected abstract Fs createNewFileSystem();

	protected abstract void afterInit() throws IOException;

	protected abstract Af createNewAbstractFile(Fs fs, String path);

	protected abstract String getRelativePath();

	@BeforeEach
	void init() throws IOException {
		FileUtils.cleanDirectory(provideRootDir());
		fs = createNewFileSystem();
		f = createNewAbstractFile(fs, getRelativePath());
		afterInit();
	}

	@AfterEach
	void end() throws IOException {
		fs.close();
	}

}
