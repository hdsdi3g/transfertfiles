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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.CommonAbstractFileSystem;

public class LocalFileSystem extends CommonAbstractFileSystem<LocalFile> {
	private static final Logger log = LogManager.getLogger();

	private final File relativePath;

	public LocalFileSystem(final File relativePath) {
		super("");
		try {
			this.relativePath = Objects.requireNonNull(relativePath).getCanonicalFile()
			        .toPath().toRealPath().normalize().toFile();
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		if (relativePath.exists() == false || relativePath.isDirectory() == false || relativePath.canRead() == false) {
			throw new UncheckedIOException(
			        new IOException("Can't access to \"" + relativePath + "\" directory"));
		}
		log.debug("Init LocalFileSystem with {}", relativePath);
	}

	@Override
	public boolean isReusable() {
		return true;
	}

	@Override
	public boolean isAvaliable() {
		return relativePath.exists() && relativePath.isDirectory() && relativePath.canRead();
	}

	@Override
	public LocalFile getFromPath(final String path) {
		final var rpath = getPathFromRelative(path.replace('\\', '/'));
		final var file = new File(relativePath, rpath)
		        .getAbsoluteFile().toPath().normalize().toFile();
		log.trace("Get LocalFile from path {}: {}", rpath, file);

		/**
		 * Check if new path is outside relativePath
		 */
		String realPath;
		try {
			if (file.exists()) {
				realPath = file.toPath().toRealPath().normalize().toFile().getAbsolutePath();
			} else {
				realPath = file.getPath();
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		final var rootPath = relativePath.getPath();
		if (realPath.startsWith(rootPath) == false) {
			throw new UncheckedIOException(
			        new IOException("Invalid root path for \"" + file.getPath() + "\""));
		}
		return new LocalFile(file, this);
	}

	File getRelativePath() {
		return relativePath;
	}

	@Override
	public void connect() {
		/**
		 * Local FS don't need a connection.
		 */
	}

	@Override
	public void close() {
		/**
		 * Local FS don't need a connection.
		 */
	}

	@Override
	public int hashCode() {
		final var prime = 31;
		var result = super.hashCode();
		result = prime * result + Objects.hash(relativePath);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (LocalFileSystem) obj;
		return Objects.equals(relativePath, other.relativePath);
	}

	@Override
	public String toString() {
		var hostName = "localhost";
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (final UnknownHostException e) {
			/***/
		}
		final var rPath = relativePath.getAbsolutePath().replace('\\', '/');
		return "file://" + hostName + AbstractFile.normalizePath(getBasePath() + rPath);
	}

	@Override
	public int reusableHashCode() {
		return relativePath.hashCode();
	}

	@Override
	public InetAddress getHost() {
		try {
			return InetAddress.getLocalHost();
		} catch (final UnknownHostException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String getUsername() {
		return System.getProperty("user.name");
	}
}
