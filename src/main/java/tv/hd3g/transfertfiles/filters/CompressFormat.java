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

import org.apache.commons.compress.compressors.CompressorStreamFactory;

/**
 * Shorthand for CompressorStreamFactory constants
 */
public enum CompressFormat {
	BZIP2(CompressorStreamFactory.BZIP2),
	GZIP(CompressorStreamFactory.GZIP),
	XZ(CompressorStreamFactory.XZ),
	LZMA(CompressorStreamFactory.LZMA),
	DEFLATE(CompressorStreamFactory.DEFLATE),
	LZ4_BLOCK(CompressorStreamFactory.LZ4_BLOCK),
	LZ4_FRAMED(CompressorStreamFactory.LZ4_FRAMED);

	final String constantName;

	CompressFormat(final String constantName) {
		this.constantName = constantName;
	}

}
