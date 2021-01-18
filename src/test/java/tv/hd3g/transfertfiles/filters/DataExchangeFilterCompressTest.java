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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.compress.compressors.CompressorException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import tv.hd3g.transfertfiles.BufferVault;

class DataExchangeFilterCompressTest {

	static Random random = new Random();
	static BufferVault randomFile;
	static byte[] randomBytes;

	DataExchangeFilterCompress filter;
	DataExchangeFilterDecompress decompress;

	@BeforeAll
	static void globalInit() throws IOException {
		randomBytes = new byte[random.nextInt(1_000) + 100];
		for (var pos = 0; pos < randomBytes.length / 2; pos++) {
			randomBytes[pos] = (byte) random.nextInt();
		}
		randomFile = new BufferVault().write(randomBytes);
	}

	@TestFactory
	Stream<DynamicNode> Compressors() throws IOException {
		return Stream.of(CompressFormat.values())
		        .map(format -> dynamicTest(format.toString(),
		                () -> checkCompressor(format)));
	}

	void checkCompressor(final CompressFormat format) throws IOException {
		filter = new DataExchangeFilterCompress(format);
		final var resultCompress = filter.applyDataFilter(true, randomFile);

		decompress = new DataExchangeFilterDecompress(format);

		final var resultDeCompress = decompress.applyDataFilter(true, resultCompress);
		assertEquals(randomBytes.length, resultDeCompress.getSize());
		assertEquals(randomFile, resultDeCompress);
	}

	@Test
	void testGetFilterName() throws CompressorException, IOException {
		final var formats = CompressFormat.values();
		for (var pos = 0; pos < formats.length; pos++) {
			assertEquals("compressor:" + formats[pos].constantName,
			        new DataExchangeFilterCompress(formats[pos]).getFilterName());
		}
	}

}
