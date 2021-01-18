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
 */
public class DataExchangeFilterCompress extends DataExchangeFilterOutputStream {
	private static final Logger log = LogManager.getLogger();

	private final CompressFormat format;

	public DataExchangeFilterCompress(final CompressFormat format) {
		super(internalStream -> {
			try {
				return new CompressorStreamFactory(false, -1)
				        .createCompressorOutputStream(format.constantName, internalStream);
			} catch (final CompressorException e) {
				throw new IOException(format.toString() + ": " + e.getMessage(), e);
			}
		});
		this.format = format;
		log.debug("Init {} compression stream", format);
	}

	@Override
	public String getFilterName() {
		return "compressor:" + format.constantName;
	}

}
