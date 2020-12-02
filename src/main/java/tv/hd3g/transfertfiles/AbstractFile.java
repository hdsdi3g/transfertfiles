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
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Expected to be stateless (store statuses in corresponding AbstractFileSystem) and thread safe.
 * Don't forget to implements hashCode, equals and toString.
 */
public interface AbstractFile {

	AbstractFileSystem<?> getFileSystem();// NOSONAR S1452

	void copyAbstractToLocal(File localFile, TransfertObserver observer);

	void sendLocalToAbstract(File localFile, TransfertObserver observer);

	/**
	 * @return always with '/' as directory separators
	 */
	String getPath();

	String getName();

	/**
	 * @return null if not parent (this is root dir)
	 */
	AbstractFile getParent();

	long length();

	boolean exists();

	/**
	 * Not recursive.
	 */
	void delete();

	boolean isDirectory();

	boolean isFile();

	boolean isLink();

	boolean isSpecial();

	/**
	 * @return marked "hidden" or is a dotfile
	 */
	boolean isHidden();

	long lastModified();

	Stream<AbstractFile> list();

	void mkdir();

	/**
	 * @return moved file
	 */
	AbstractFile renameTo(String path);

	/**
	 * @return a read-only cached data version of this AbstractFile
	 */
	default CachedFileAttributes toCache() {
		return new CachedFileAttributes(this);
	}

	/**
	 * @return a read-only cached list data version of AbstractFile.list
	 */
	default Stream<CachedFileAttributes> toCachedList() {
		return list().map(CachedFileAttributes::new);
	}

	static String normalizePath(final String path) {
		Objects.requireNonNull(path, "path can't be null");
		var p = path;
		if (p.equals("") || p.equals("/")) {
			return "/";
		}
		while (p.contains("//")) {
			p = p.replace("//", "/");
		}
		if (p.contains("../")
		    || p.contains("./")
		    || p.contains("/~/")
		    || p.startsWith("~/")
		    || p.equals("..")
		    || p.equals(".")
		    || p.equals("~")) {
			throw new IllegalArgumentException("Invalid path: \"" + path + "\"");
		}

		if (p.startsWith("/") == false) {
			p = "/" + p;
		}
		if (p.endsWith("/")) {
			return p.substring(0, p.length() - 1);
		} else {
			return p;
		}
	}
}
