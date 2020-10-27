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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StoppableInputStream extends FilterInputStream implements StoppableIOStream {

	private static final String NOT_IMPLEMENTED = "Not implemented";
	private static final Logger log = LogManager.getLogger();
	private volatile boolean stopped;

	public StoppableInputStream(final InputStream in) {
		super(in);
		stopped = false;
	}

	@Override
	public synchronized void setStop() {
		log.debug("Stop InputStream");
		stopped = true;
	}

	@Override
	public int read() throws IOException {
		if (stopped) {
			return -1;
		}
		return super.read();
	}

	@Override
	public int read(final byte[] b) throws IOException {
		if (stopped) {
			return -1;
		}
		return super.read(b);
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		if (stopped) {
			return -1;
		}
		return super.read(b, off, len);
	}

	@Override
	public byte[] readAllBytes() throws IOException {
		throw new IllegalArgumentException(NOT_IMPLEMENTED);
	}

	@Override
	public int readNBytes(final byte[] b, final int off, final int len) throws IOException {
		throw new IllegalArgumentException(NOT_IMPLEMENTED);
	}

	@Override
	public byte[] readNBytes(final int len) throws IOException {
		throw new IllegalArgumentException(NOT_IMPLEMENTED);
	}

	@Override
	public long skip(final long n) throws IOException {
		if (stopped) {
			return 0;
		}
		return super.skip(n);
	}

	@Override
	public int available() throws IOException {
		if (stopped) {
			return 0;
		}
		return super.available();
	}

	@Override
	public boolean isStopped() {
		return stopped;
	}

}
