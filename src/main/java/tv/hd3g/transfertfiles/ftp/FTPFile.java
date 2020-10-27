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
package tv.hd3g.transfertfiles.ftp;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.apache.commons.io.FilenameUtils.getFullPathNoEndSeparator;
import static tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection.DISTANTTOLOCAL;
import static tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection.LOCALTODISTANT;
import static tv.hd3g.transfertfiles.ftp.StoppableOutputStream.MANUALLY_STOP_WRITING;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.commons.IORuntimeException;
import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.AbstractFileSystem;
import tv.hd3g.transfertfiles.CannotDeleteException;
import tv.hd3g.transfertfiles.CommonAbstractFile;
import tv.hd3g.transfertfiles.TransfertObserver;
import tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection;

public class FTPFile extends CommonAbstractFile<FTPFileSystem> {// NOSONAR S2160
	private static final Logger log = LogManager.getLogger();

	private final FTPClient ftpClient;
	private String actualCWD;

	FTPFile(final FTPFileSystem fileSystem, final String path) {
		super(fileSystem, path);
		ftpClient = fileSystem.getClient();
	}

	@Override
	public AbstractFileSystem<?> getFileSystem() {
		return fileSystem;
	}

	private Optional<org.apache.commons.net.ftp.FTPFile> getCurrentFile() {
		try {
			final var preferList = FTPListing.LIST.equals(fileSystem.getFtpListing());
			if (preferList || ftpClient.hasFeature("MLST") == false) {
				return Stream.of(ftpClient.listFiles(getFullPathNoEndSeparator(path)))
				        .filter(f -> f.getName().equalsIgnoreCase(getName()))
				        .findFirst();
			} else {
				return Optional.ofNullable(ftpClient.mlistFile(path));
			}
		} catch (final IOException e) {
			throw new IORuntimeException("FTP error during list \"" + path + "\"", e);
		}
	}

	@Override
	public long length() {
		try {
			if (ftpClient.hasFeature("SIZE")) {
				return Optional.ofNullable(ftpClient.getSize(path))
				        .map(Long::valueOf)
				        .orElse(0L);
			} else {
				return getCurrentFile()
				        .map(org.apache.commons.net.ftp.FTPFile::getSize)
				        .orElse(0L);
			}
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public boolean exists() {
		return getCurrentFile().isPresent();
	}

	@Override
	public boolean isDirectory() {
		return getCurrentFile()
		        .map(org.apache.commons.net.ftp.FTPFile::isDirectory)
		        .orElse(false);
	}

	@Override
	public boolean isFile() {
		return getCurrentFile()
		        .map(org.apache.commons.net.ftp.FTPFile::isFile)
		        .orElse(false);
	}

	@Override
	public boolean isLink() {
		return getCurrentFile()
		        .map(org.apache.commons.net.ftp.FTPFile::isSymbolicLink)
		        .orElse(false);
	}

	@Override
	public boolean isSpecial() {
		return getCurrentFile()
		        .map(org.apache.commons.net.ftp.FTPFile::isUnknown)
		        .orElse(false);
	}

	@Override
	public long lastModified() {
		return getCurrentFile()
		        .map(f -> f.getTimestamp().getTimeInMillis())
		        .orElse(0L);
	}

	@Override
	public Stream<AbstractFile> list() {
		try {
			return Optional.ofNullable(fileSystem.getFtpListing())
			        .orElse(FTPListing.NLST)
			        .listDirectory(ftpClient, path)
			        .filter(name -> name.equalsIgnoreCase(getName()) == false)
			        .map(name -> fileSystem.getFromPath(path + "/" + name));
		} catch (final IOException e) {
			throw new IORuntimeException("FTP error during list \"" + path + "\"", e);
		}
	}

	@Override
	public void delete() {
		final var directory = isDirectory();
		try {
			boolean deleteOk;
			if (directory) {
				deleteOk = ftpClient.removeDirectory(path);
			} else {
				deleteOk = ftpClient.deleteFile(path);
			}
			if (deleteOk == false) {
				throw new CannotDeleteException(this, directory, new IOException("Can't delete " + fileSystem + path));
			}
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void mkdir() {
		try {
			final var mkdirOk = ftpClient.makeDirectory(path);
			if (mkdirOk == false) {
				throw new IOException("Can't mkdir " + fileSystem + path);
			}
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public AbstractFile renameTo(final String path) {
		try {
			final var renameOk = ftpClient.rename(this.path, path);
			if (renameOk == false) {
				throw new IOException("Can't rename form \"" + path + "\" to \"" + path + "\"");
			}
			return fileSystem.getFromPath(path);
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void copyAbstractToLocal(final File localFile, final TransfertObserver observer) {
		copy(path, localFile.getPath(), localFile, observer, DISTANTTOLOCAL);

	}

	@Override
	public void sendLocalToAbstract(final File localFile, final TransfertObserver observer) {
		copy(localFile.getPath(), path, localFile, observer, LOCALTODISTANT);
	}

	private void cwdBeforeOperation(final String newPath) throws IOException {
		actualCWD = ftpClient.printWorkingDirectory();

		if (newPath.equalsIgnoreCase(actualCWD)) {
			return;
		}

		log.debug("Do CWD to \"{}\" for {}", actualCWD, this);
		final var done = ftpClient.changeWorkingDirectory(newPath);
		if (done == false) {
			throw new IOException("Can't change working directory to " + actualCWD
			                      + ": " + ftpClient.getReplyString());
		}
	}

	private void restoreCwd() throws IOException {
		if (actualCWD == null) {
			return;
		}
		log.debug("Do CWD to \"{}\" for {}", actualCWD, this);
		final var done = ftpClient.changeWorkingDirectory(actualCWD);
		if (done == false) {
			throw new IOException("Can't change working directory to " + actualCWD
			                      + ": " + ftpClient.getReplyString());
		}
		actualCWD = null;
	}

	private void cwdToParentPath() throws IOException {
		if (path.equals("/")) {
			throw new IllegalArgumentException("Can't cwd to ../");
		}
		cwdBeforeOperation(getFullPathNoEndSeparator(path));
	}

	private class StoppableListener implements CopyStreamListener {
		final String source;
		final String dest;
		final File localFile;
		final TransfertObserver observer;
		final TransfertDirection transfertDirection;
		final FTPFile thisRef;
		final long now;
		final AtomicReference<StoppableIOStream> stoppableIOStream;
		final AtomicLong sizeToTransfert;

		StoppableListener(final String source, final String dest, final File localFile,
		                  final TransfertObserver observer,
		                  final TransfertDirection transfertDirection,
		                  final FTPFile thisRef,
		                  final long now,
		                  final AtomicReference<StoppableIOStream> stoppableIOStream,
		                  final AtomicLong sizeToTransfert) {
			this.source = source;
			this.dest = dest;
			this.localFile = localFile;
			this.observer = observer;
			this.transfertDirection = transfertDirection;
			this.thisRef = thisRef;
			this.now = now;
			this.stoppableIOStream = stoppableIOStream;
			this.sizeToTransfert = sizeToTransfert;
		}

		@Override
		public void bytesTransferred(final CopyStreamEvent event) {
			/** Should not be used */
		}

		@Override
		public void bytesTransferred(final long totalBytesTransferred,
		                             final int bytesTransferred,
		                             final long streamSize) {
			if (observer.onTransfertProgress(
			        localFile, thisRef, transfertDirection, now, totalBytesTransferred) == false) {
				final var stop = Objects.requireNonNull(stoppableIOStream.get(),
				        "Not stoppableStream set before stop transfert");
				log.info("Stop copy FTP file from \"{}\" to \"{}\", ({}/{} bytes)",
				        source, dest, totalBytesTransferred, sizeToTransfert.get());
				stop.setStop();
			}
		}
	}

	private void copy(final String source,
	                  final String dest,
	                  final File localFile,
	                  final TransfertObserver observer,
	                  final TransfertDirection transfertDirection) {
		final var localBufferSize = Math.max(8192, fileSystem.getIOBufferSize() * 2);
		final var stoppableIOStream = new AtomicReference<StoppableIOStream>();
		final var sizeToTransfert = new AtomicLong(0);
		final var thisRef = this;
		final var now = System.currentTimeMillis();

		synchronized (ftpClient) {
			var done = false;

			try {
				ftpClient.setCopyStreamListener(new StoppableListener(source, dest, localFile, observer,
				        transfertDirection, thisRef, now, stoppableIOStream, sizeToTransfert));

				final var oTargetFileRef = getCurrentFile();
				observer.beforeTransfert(localFile, this, transfertDirection);

				if (transfertDirection == DISTANTTOLOCAL) {
					final var sourceFileRef = oTargetFileRef
					        .orElseThrow(() -> new IORuntimeException("Can't access to source file in ftp server"));
					if (sourceFileRef.isDirectory()) {
						throw new IORuntimeException("Source file is a directory, can't copy from it");
					}
					sizeToTransfert.set(sourceFileRef.getSize());
					cwdToParentPath();

					try (var outputstream = new StoppableOutputStream(new BufferedOutputStream(
					        new FileOutputStream(localFile), localBufferSize))) {
						log.info("Download file from FTP \"{}@{}:{}\" to \"{}\" ({} bytes)",
						        fileSystem.getUsername(), fileSystem.getHost(), source, dest, sizeToTransfert);
						stoppableIOStream.set(outputstream);
						done = ftpClient.retrieveFile(getName(), outputstream);
					}
				} else if (transfertDirection == LOCALTODISTANT) {
					sizeToTransfert.set(localFile.length());

					var storeName = getName();
					if (oTargetFileRef.isEmpty() || oTargetFileRef.get().isFile()) {
						cwdToParentPath();
					} else if (oTargetFileRef.get().isDirectory()) {
						cwdBeforeOperation(path);
						storeName = localFile.getName();
					}

					try (var inputstream = new StoppableInputStream(new BufferedInputStream(
					        new FileInputStream(localFile), localBufferSize))) {
						log.info("Upload file \"{}\" ({} bytes) to FTP host \"{}@{}:{}\"",
						        localFile, sizeToTransfert, fileSystem.getUsername(), fileSystem.getHost(), dest);
						stoppableIOStream.set(inputstream);
						done = ftpClient.storeFile(storeName, inputstream);
					}
				}

				if (stoppableIOStream.get().isStopped()) {
					actualCWD = null;
					ftpClient.abort();
					fileSystem.close();
					fileSystem.connect();
				} else if (done == false) {
					throw new IOException("FTP server refuse the file transfert after the operation: "
					                      + ftpClient.getReplyString());
				} else {
					observer.afterTransfert(localFile, this, transfertDirection,
					        Duration.of(System.currentTimeMillis() - now, MILLIS));
					restoreCwd();
				}
			} catch (final CopyStreamException e) {
				if (e.getCause() instanceof IOException
				    && e.getCause().getMessage().equals(MANUALLY_STOP_WRITING)) {
					try {
						actualCWD = null;
						ftpClient.abort();
						fileSystem.close();
						fileSystem.connect();
					} catch (final IOException e1) {
						throw new IORuntimeException("Can't abort transfert after manual stop", e);
					}
					return;
				}
				throw new IORuntimeException(e);
			} catch (final IOException e) {
				throw new IORuntimeException(e);
			} finally {
				ftpClient.setCopyStreamListener(null);
			}
		}
	}

}
