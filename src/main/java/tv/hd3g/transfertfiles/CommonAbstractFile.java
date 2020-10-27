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

import java.util.Objects;

import org.apache.commons.io.FilenameUtils;

public abstract class CommonAbstractFile<T extends AbstractFileSystem<?>> implements AbstractFile {

	protected final T fileSystem;
	protected final String path;

	protected CommonAbstractFile(final T fileSystem, final String path) {
		this.fileSystem = fileSystem;
		this.path = AbstractFile.normalizePath(path);
	}

	@Override
	public AbstractFileSystem<?> getFileSystem() {
		return fileSystem;
	}

	@Override
	public String toString() {
		return getPath();
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getName() {
		return FilenameUtils.getName(path);
	}

	@Override
	public AbstractFile getParent() {
		return fileSystem.getFromPath(FilenameUtils.getFullPathNoEndSeparator(path));
	}

	@Override
	public boolean isHidden() {
		return getName().startsWith(".");
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileSystem, path);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (CommonAbstractFile<?>) obj;
		return Objects.equals(fileSystem, other.fileSystem) && Objects.equals(path, other.path);
	}

}
