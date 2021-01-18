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
 * Copyright (C) hdsdi3g for hd3g.tv 2021
 *
 */
package tv.hd3g.transfertfiles.randomfs;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.transfertfiles.AbstractFile;
import tv.hd3g.transfertfiles.BufferVault;
import tv.hd3g.transfertfiles.CommonAbstractFile;
import tv.hd3g.transfertfiles.SizedStoppableCopyCallback;
import tv.hd3g.transfertfiles.TransfertObserver;

public class RandomFile extends CommonAbstractFile<RandomFileSystem> {
	private static final Logger log = LogManager.getLogger();

	private final Random r = ThreadLocalRandom.current();
	private final LinkedBlockingQueue<BufferVault> transfered;
	private final int maxTransfert;
	private final int globalBufferSize;

	public RandomFile(final int maxTransfert,
	                  final int bufferSize) {
		super(new RandomFileSystem(), "/");
		globalBufferSize = bufferSize;
		transfered = new LinkedBlockingQueue<>(maxTransfert / bufferSize);
		this.maxTransfert = maxTransfert;
	}

	protected void nextBytes(final byte[] buffer) {
		r.nextBytes(buffer);
	}

	@Override
	public long downloadAbstract(final OutputStream outputStream,
	                             final int bufferSize,
	                             final SizedStoppableCopyCallback copyCallback) {
		final var buffer = new byte[globalBufferSize];
		var count = 0L;
		try {
			while (count < maxTransfert) {
				nextBytes(buffer);
				outputStream.write(buffer);
				transfered.put(new BufferVault(buffer.length).write(buffer));
				count += buffer.length;

				if (copyCallback.apply(count) == false) {
					outputStream.close();
					break;
				}
			}
			outputStream.close();
		} catch (final Exception e) {
			log.error("Can't push datas", e);
		}

		return count;
	}

	@Override
	public long uploadAbstract(final InputStream inputStream,
	                           final int bufferSize,
	                           final SizedStoppableCopyCallback copyCallback) {
		final var buffer = new byte[globalBufferSize];
		var count = 0L;
		try {
			int read;
			while ((read = inputStream.read(buffer, 0, buffer.length)) >= 0) {
				if (copyCallback.apply(count) == false) {
					break;
				} else if (read == 0) {
					continue;
				}
				transfered.put(new BufferVault(read).write(buffer, 0, read));
				count += read;
			}
		} catch (final Exception e) {
			log.error("Can't pull datas", e);
		}
		return count;
	}

	public LinkedBlockingQueue<BufferVault> getTransfered() {
		return transfered;
	}

	@Override
	public void copyAbstractToLocal(final File localFile, final TransfertObserver observer) {
	}

	@Override
	public void sendLocalToAbstract(final File localFile, final TransfertObserver observer) {
	}

	@Override
	public long length() {
		return -1;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public void delete() {
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public boolean isLink() {
		return false;
	}

	@Override
	public boolean isSpecial() {
		return false;
	}

	@Override
	public long lastModified() {
		return 0;
	}

	@Override
	public Stream<AbstractFile> list() {
		return Stream.empty();
	}

	@Override
	public void mkdir() {
	}

	@Override
	public AbstractFile renameTo(final String path) {
		return this;
	}

}
