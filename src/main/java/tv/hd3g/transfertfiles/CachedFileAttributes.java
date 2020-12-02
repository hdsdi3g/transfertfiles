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

public class CachedFileAttributes {

	private final AbstractFile abstractFile;
	private final String path;
	private final long length;
	private final long lastModified;
	private final boolean exists;
	private final boolean directory;
	private final boolean file;
	private final boolean link;
	private final boolean special;

	/**
	 * Attributes and AbstractFile attributes shoud be the same values (at the least on the creation).
	 */
	public CachedFileAttributes(final AbstractFile abstractFile,
	                            final long length,
	                            final long lastModified,
	                            final boolean exists,
	                            final boolean directory,
	                            final boolean file,
	                            final boolean link,
	                            final boolean special) {
		this.abstractFile = Objects.requireNonNull(abstractFile);
		path = abstractFile.getPath();
		this.length = length;
		this.lastModified = lastModified;
		this.exists = exists;
		this.directory = directory;
		this.file = file;
		this.link = link;
		this.special = special;
	}

	/**
	 * Not optimized approach directly derived from AbstractFile.
	 */
	public CachedFileAttributes(final AbstractFile abstractFile) {
		this.abstractFile = Objects.requireNonNull(abstractFile);
		path = abstractFile.getPath();
		length = abstractFile.length();
		lastModified = abstractFile.lastModified();
		exists = abstractFile.exists();
		directory = abstractFile.isDirectory();
		file = abstractFile.isFile();
		link = abstractFile.isLink();
		special = abstractFile.isSpecial();
	}

	public static CachedFileAttributes notExists(final AbstractFile abstractFile) {
		return new CachedFileAttributes(abstractFile, 0, 0, false, false, false, false, false);
	}

	/**
	 * @return the original data source. Warning: linked AbstractFileSystem may be disconnected.
	 */
	public AbstractFile getAbstractFile() {
		return abstractFile;
	}

	public String getName() {
		return FilenameUtils.getName(path);
	}

	public String getPath() {
		return path;
	}

	@Override
	public String toString() {
		return getPath();
	}

	public String getParentPath() {
		return FilenameUtils.getFullPathNoEndSeparator(path);
	}

	public boolean isHidden() {
		return getName().startsWith(".");
	}

	public long length() {
		return length;
	}

	public long lastModified() {
		return lastModified;
	}

	public boolean exists() {
		return exists;
	}

	public boolean isDirectory() {
		return directory;
	}

	public boolean isFile() {
		return file;
	}

	public boolean isLink() {
		return link;
	}

	public boolean isSpecial() {
		return special;
	}

	@Override
	public int hashCode() {
		return Objects.hash(abstractFile);
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
		final var other = (CachedFileAttributes) obj;
		return Objects.equals(abstractFile, other.abstractFile);
	}

}
