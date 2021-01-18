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

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Not reusable
 * Based on DataExchangeFilterInputStream buggy implementation.
 */
@Deprecated
public class DataExchangeFilterDecompress extends DataExchangeFilterInputStream {
	private static final Logger log = LogManager.getLogger();

	private final CompressFormat format;

	public DataExchangeFilterDecompress(final CompressFormat format,
	                                    final int maxMemoryInKb) {
		super(internalStream -> {
			try {
				return new CompressorStreamFactory(false, maxMemoryInKb)
				        .createCompressorInputStream(format.constantName, internalStream);
			} catch (final CompressorException e) {
				throw new IOException(format.toString() + ": " + e.getMessage(), e);
			}
		}, /** One MB */
		        0xFFFFF);
		this.format = format;
		log.debug("Init {} decompression stream with maxMemory set to {}", format, maxMemoryInKb);
	}

	public DataExchangeFilterDecompress(final CompressFormat format) {
		this(format, -1);
	}

	@Override
	public String getFilterName() {
		return "decompressor:" + format.constantName;
	}

}
