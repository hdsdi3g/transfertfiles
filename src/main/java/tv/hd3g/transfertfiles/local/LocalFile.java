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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.time.temporal.ChronoUnit.MILLIS;
import static tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection.DISTANTTOLOCAL;
import static tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection.LOCALTODISTANT;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.commons.IORuntimeException;
import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.CannotDeleteException;
import tv.hd3g.transfertfiles.CommonAbstractFile;
import tv.hd3g.transfertfiles.TransfertObserver;
import tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection;

public class LocalFile extends CommonAbstractFile<LocalFileSystem> {// NOSONAR S2160
	private static final Logger log = LogManager.getLogger();

	private final File internalFile;

	private static String makePath(final LocalFileSystem fileSystem, final File internalFile) {
		final var size = fileSystem.getRelativePath().getPath().length();
		return internalFile.getPath().substring(size).replace('\\', '/');
	}

	LocalFile(final File internalFile, final LocalFileSystem fileSystem) {
		super(fileSystem, makePath(fileSystem, internalFile));
		this.internalFile = internalFile;
	}

	File getInternalFile() {
		return internalFile;
	}

	@Override
	public void copyAbstractToLocal(final File localFile, final TransfertObserver observer) {
		copy(internalFile, localFile, localFile, observer, DISTANTTOLOCAL);
	}

	@Override
	public void sendLocalToAbstract(final File localFile, final TransfertObserver observer) {
		copy(localFile, internalFile, localFile, observer, LOCALTODISTANT);
	}

	private void copy(final File source,
	                  final File fileDirDest,
	                  final File localFile,
	                  final TransfertObserver observer,
	                  final TransfertDirection transfertDirection) {
		if (source.isDirectory()) {
			throw new IORuntimeException("Can't copy directories directly");
		}

		File dest;
		if (fileDirDest.isDirectory()) {
			dest = new File(fileDirDest, source.getName());
		} else {
			dest = fileDirDest;
		}

		final var now = System.currentTimeMillis();
		observer.beforeTransfert(localFile, this, transfertDirection);
		log.info("Copy local file from \"{}\" to \"{}\" ({} bytes)", source, dest, source.length());

		var stopped = false;
		final var buffer = ByteBuffer.allocateDirect(fileSystem.getIOBufferSize());
		try (var reader = Files.newByteChannel(source.toPath(), READ);
		     var writer = Files.newByteChannel(dest.toPath(), WRITE, CREATE, TRUNCATE_EXISTING)) {

			var dataTransferred = 0L;
			while (reader.read(buffer) >= 0 || buffer.position() != 0) {
				buffer.flip();
				dataTransferred += buffer.remaining();
				writer.write(buffer);
				buffer.compact();

				if (log.isTraceEnabled()) {
					log.trace("Copy local progression from \"{}\" ({} bytes) to \"{}\": {} bytes",
					        source, source.length(), dest, dataTransferred);
				}
				stopped = observer.onTransfertProgress(
				        localFile, this, transfertDirection, now, dataTransferred) == false;
				if (stopped) {
					log.info("Stop copy local file from \"{}\" ({} bytes) to \"{}\": {} bytes",
					        source, source.length(), dest, dataTransferred);
					break;
				}
			}
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}

		if (stopped) {
			return;
		}
		observer.afterTransfert(localFile, this, transfertDirection,
		        Duration.of(System.currentTimeMillis() - now, MILLIS));
	}

	@Override
	public long length() {
		return internalFile.length();
	}

	@Override
	public boolean exists() {
		return internalFile.exists();
	}

	@Override
	public void delete() {
		log.debug("Delete local file \"{}\"", internalFile);
		if (internalFile.delete() == false) {
			if (isDirectory()) {
				throw new CannotDeleteException(this, true, new IOException("Can't delete directory"));
			} else {
				throw new CannotDeleteException(this, false, new IOException("Can't delete file"));
			}
		}
	}

	@Override
	public boolean isDirectory() {
		return internalFile.isDirectory();
	}

	@Override
	public boolean isFile() {
		return internalFile.isFile();
	}

	@Override
	public boolean isLink() {
		return FileUtils.isSymlink(internalFile);
	}

	@Override
	public boolean isSpecial() {
		if (exists() == false) {
			return false;
		}
		try {
			return Optional.ofNullable(Files.readAttributes(internalFile.toPath(), BasicFileAttributes.class))
			        .map(BasicFileAttributes::isOther)
			        .orElse(false);
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public boolean isHidden() {
		return internalFile.isHidden() || super.isHidden();
	}

	@Override
	public long lastModified() {
		return internalFile.lastModified();
	}

	@Override
	public Stream<AbstractFile> list() {
		final var rootLen = fileSystem.getRelativePath().getPath().length();
		return Optional.ofNullable(internalFile.listFiles()).stream()
		        .flatMap(Stream::of)
		        .map(f -> fileSystem.getFromPath(f.getPath().substring(rootLen)));
	}

	@Override
	public void mkdir() {
		log.debug("Mkdir local file \"{}\"", internalFile);
		try {
			FileUtils.forceMkdir(internalFile);
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public AbstractFile renameTo(final String path) {
		if (exists() == false) {
			throw new IORuntimeException("Can't move non-existent file/dir \"" + internalFile + "\"");
		}
		final var newRef = fileSystem.getFromPath(path);
		final var dest = newRef.internalFile;
		log.debug("Rename local file \"{}\" to \"{}\", as \"{}\"", internalFile, dest, path);
		if (internalFile.renameTo(dest) == false) {
			throw new IORuntimeException("Can't move \"" + internalFile + "\" to \"" + dest + "\"");
		}
		return newRef;
	}

}
