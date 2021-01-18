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
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.transfertfiles.BufferVault;

/**
 * Not reusable
 */
public class DataExchangeFilterOutputStream implements DataExchangeFilter {
	private static final Logger log = LogManager.getLogger();

	private final int defaultBufferSize;
	private final BufferVault dataDest;
	private final UnaryIOExceptionOperator<OutputStream> streamProvider;
	private OutputStream externalOutputStream;

	public DataExchangeFilterOutputStream(final UnaryIOExceptionOperator<OutputStream> streamProvider,
	                                      final int defaultBufferSize) {
		this.defaultBufferSize = defaultBufferSize;
		dataDest = new BufferVault(defaultBufferSize);
		this.streamProvider = streamProvider;
		log.debug("Init filter outputstream with buffer={}", defaultBufferSize);
	}

	public DataExchangeFilterOutputStream(final UnaryIOExceptionOperator<OutputStream> streamProvider) {
		this(streamProvider, DEFAULT_BUFFER_SIZE);
	}

	@Override
	public BufferVault applyDataFilter(final boolean last, final BufferVault dataSource) throws IOException {
		dataDest.clear();
		if (externalOutputStream == null) {
			externalOutputStream = streamProvider.apply(dataDest.asOutputStream());
		}

		dataSource.read(externalOutputStream);
		externalOutputStream.flush();
		if (last) {
			log.debug("Close internal channel, close provided OutputStream");
			externalOutputStream.close();
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
			dataDest.clear();
			externalOutputStream.close();
		} catch (final IOException e) {
			log.error("Can't close external stream: {}", getFilterName(), e);
		}
	}
}
