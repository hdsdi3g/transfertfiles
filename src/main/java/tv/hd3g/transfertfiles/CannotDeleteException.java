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

import java.io.IOException;

import tv.hd3g.commons.IORuntimeException;

public class CannotDeleteException extends IORuntimeException {

	private final boolean isDirectory;
	private final String path;
	private final String fsName;

	public CannotDeleteException(final AbstractFile file,
	                             final boolean isDirectory,
	                             final IOException e) {
		super(e);
		fsName = file.getFileSystem().toString();
		path = file.getPath();
		this.isDirectory = isDirectory;
	}

	public String getFileSystemName() {
		return fsName;
	}

	public String getPath() {
		return path;
	}

	public boolean isDirectory() {
		return isDirectory;
	}
}
