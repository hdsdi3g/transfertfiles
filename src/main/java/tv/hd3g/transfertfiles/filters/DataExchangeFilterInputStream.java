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
package tv.hd3g.transfertfiles.filters;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.transfertfiles.BufferVault;

/**
 * Not reusable
 * Buggy implementation: need to async DataExchangeFilterInputStream (real transfert pipeline)
 */
@Deprecated
public class DataExchangeFilterInputStream implements DataExchangeFilter {
	private static final Logger log = LogManager.getLogger();

	private final int defaultBufferSize;
	private final BufferVault dataDest;
	private final UnaryIOExceptionOperator<InputStream> streamProvider;
	private InternalInputStream internalStream;
	private InputStream externalStream;

	public DataExchangeFilterInputStream(final UnaryIOExceptionOperator<InputStream> streamProvider) {
		this(streamProvider, DEFAULT_BUFFER_SIZE);
	}

	public DataExchangeFilterInputStream(final UnaryIOExceptionOperator<InputStream> streamProvider,
	                                     final int defaultBufferSize) {
		this.defaultBufferSize = defaultBufferSize;
		dataDest = new BufferVault(defaultBufferSize);
		this.streamProvider = streamProvider;
		log.debug("Init filter inputstream with buffer={}", defaultBufferSize);
	}

	private class InternalInputStream extends InputStream {

		private BufferVault dataSource;
		private int readIndex;
		private boolean close;

		void appendDataSource(final BufferVault newDataSource) {
			if (readIndex == 0
			    || readIndex + 1 == dataSource.getSize()) {
				dataSource = newDataSource;
				return;
			}
			dataSource.compactAndAppend(readIndex, newDataSource);
			readIndex = 0;
		}

		@Override
		public int read() throws IOException {
			if (close) {
				return -1;
			}
			return dataSource.read(readIndex++);
		}

		@Override
		public int read(final byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		@Override
		public int read(final byte[] b, final int off, final int len) throws IOException {
			if (close) {
				return -1;
			}
			final var count = dataSource.read(b, readIndex, off, len);
			if (count == -1) {
				readIndex = 0;
				dataSource.clear();
				return 0;
			}
			readIndex += count;
			return count;
		}

		@Override
		public int available() throws IOException {
			return dataSource.getSize() - (readIndex + 1);
		}
	}

	@Override
	public BufferVault applyDataFilter(final boolean last, final BufferVault dataSource) throws IOException {
		if (internalStream == null) {
			internalStream = new InternalInputStream();
			internalStream.appendDataSource(dataSource);
			externalStream = streamProvider.apply(internalStream);
		} else {
			internalStream.appendDataSource(dataSource);
		}

		dataDest.clear();
		dataDest.write(externalStream, defaultBufferSize);
		internalStream.close = last;

		if (last) {
			log.debug("Close provided InputStream");
			externalStream.close();
		}

		if (log.isTraceEnabled()) {
			log.trace("Readed before filter {} bytes, after filter {} bytes",
			        dataSource.getSize(), dataDest.getSize());
		}
		return dataDest;
	}

	@Override
	public int ensureMinDataSourcesDataLength() {
		return defaultBufferSize;
	}

	@Override
	public void onCancelTransfert() {
		try {
			externalStream.close();
		} catch (final IOException e) {
			log.error("Can't close external stream: {}", getFilterName(), e);
		}
	}
}
