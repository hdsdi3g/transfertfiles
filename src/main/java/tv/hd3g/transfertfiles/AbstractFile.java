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
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import tv.hd3g.commons.IORuntimeException;
import tv.hd3g.transfertfiles.local.LocalFileSystem;

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

	/**
	 * Only use with a regular file. Type will not be checked before copy action.
	 * @param bufferSize can be used on internal stream transfert, but it's not mandated.
	 * @return data size readed from this
	 */
	long downloadAbstract(final OutputStream outputStream,
	                      int bufferSize,
	                      final SizedStoppableCopyCallback copyCallback);

	/**
	 * Only use with a regular file. Type will not be checked before copy action.
	 * @param bufferSize can be used on internal stream transfert, but it's not mandated.
	 * @return data size writer to this
	 */
	long uploadAbstract(final InputStream inputStream,
	                    int bufferSize,
	                    final SizedStoppableCopyCallback copyCallback);

	static void checkIsSameFileSystem(final AbstractFile from,
	                                  final AbstractFile destination) {
		final var fromFs = from.getFileSystem();
		final var toFs = destination.getFileSystem();
		if (fromFs.equals(toFs) == false
		    || fromFs instanceof LocalFileSystem
		    || toFs instanceof LocalFileSystem) {
			return;
		}
		if (fromFs.reusableHashCode() == toFs.reusableHashCode()) {
			throw new IORuntimeException(
			        "Can't use same FileSystem instances between to AbstractFiles. Please start a new FS instance for one of the two AbstractFile");
		}
	}

	default void copyAbstractToAbstract(final AbstractFile destination,
	                                    final DataExchangeObserver dataExchangeObserver) {
		final var bufferSize = Math.max(8192,
		        Math.max(destination.getFileSystem().getIOBufferSize(),
		                getFileSystem().getIOBufferSize()));
		copyAbstractToAbstract(destination, dataExchangeObserver, new DataExchangeInOutStream(bufferSize));
	}

	default void copyAbstractToAbstract(final AbstractFile destination,
	                                    final DataExchangeObserver dataExchangeObserver,
	                                    final DataExchangeInOutStream exchange) {
		checkIsSameFileSystem(this, destination);

		final var bufferSize = exchange.getBufferSize();
		final var sourceStream = exchange.getSourceOriginStream();
		final var destStream = exchange.getDestTargetStream();
		final var startDate = System.currentTimeMillis();

		final SizedStoppableCopyCallback readCallback = copied -> dataExchangeObserver
		        .onTransfertProgressFromSource(this, startDate, copied);
		final SizedStoppableCopyCallback writeCallback = copied -> dataExchangeObserver
		        .onTransfertProgressToDestination(destination, startDate, copied);

		dataExchangeObserver.beforeTransfert(this, destination);
		final var downloader = CompletableFuture.supplyAsync(() -> downloadAbstract(destStream, bufferSize,
		        readCallback));

		final var writed = destination.uploadAbstract(sourceStream, bufferSize, writeCallback);

		long readed;
		try {
			readed = downloader.get();
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
		dataExchangeObserver.afterTransfert(this, destination, readed, writed,
		        Duration.ofMillis(System.currentTimeMillis() - startDate));
	}
}
