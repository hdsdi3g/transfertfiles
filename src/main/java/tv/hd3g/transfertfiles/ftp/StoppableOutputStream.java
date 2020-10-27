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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StoppableOutputStream extends FilterOutputStream implements StoppableIOStream {

	static final String MANUALLY_STOP_WRITING = "Manually stop writing";
	private static final Logger log = LogManager.getLogger();
	private volatile boolean stopped;

	public StoppableOutputStream(final OutputStream out) {
		super(out);
		stopped = false;
	}

	@Override
	public synchronized void setStop() {
		log.debug("Stop OutputStream");
		stopped = true;
	}

	private void checkStop() throws IOException {
		if (stopped) {
			super.close();
			throw new IOException(MANUALLY_STOP_WRITING);
		}
	}

	@Override
	public void write(final byte[] b) throws IOException {
		checkStop();
		super.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		checkStop();
		super.write(b, off, len);
	}

	@Override
	public void write(final int b) throws IOException {
		checkStop();
		super.write(b);
	}

	@Override
	public boolean isStopped() {
		return stopped;
	}

}
