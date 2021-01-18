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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tv.hd3g.transfertfiles.filters.CompressFormat.BZIP2;
import static tv.hd3g.transfertfiles.filters.CompressFormat.GZIP;
import static tv.hd3g.transfertfiles.filters.CompressFormat.LZMA;
import static tv.hd3g.transfertfiles.filters.CompressFormat.XZ;

import java.io.IOException;

import org.apache.commons.compress.MemoryLimitException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.BufferVault;

class DataExchangeFilterDecompressTest {

	static ClassLoader classLoader;
	static BufferVault randomFile;
	DataExchangeFilterDecompress filter;

	@BeforeAll
	static void globalInit() throws IOException {
		classLoader = DataExchangeFilterDecompress.class.getClassLoader();
		randomFile = readAll("random");
	}

	static BufferVault readAll(final String resourceName) throws IOException {
		final var resource = classLoader.getResourceAsStream(resourceName);
		assertNotNull(resource);
		final var result = new BufferVault();
		result.write(resource, 1024);
		resource.close();
		return result;
	}

	void checkFile(final CompressFormat format, final String fileName) throws IOException {
		filter = new DataExchangeFilterDecompress(format);
		final var dataSource = readAll(fileName);
		final var dataDest = filter.applyDataFilter(true, dataSource);
		assertEquals(randomFile, dataDest);
	}

	@Test
	void test_BZIP2() throws IOException {
		checkFile(BZIP2, "random.bz2");
	}

	@Test
	void test_GZIP() throws IOException {
		checkFile(GZIP, "random.gz");
	}

	@Test
	void test_LZMA() throws IOException {
		checkFile(LZMA, "random.lzma");
	}

	@Test
	void test_XZ() throws IOException {
		checkFile(XZ, "random.xz");
	}

	@Test
	void testMemoryLimit() throws IOException {
		filter = new DataExchangeFilterDecompress(XZ, 1);
		final var dataSource = readAll("random.xz");
		assertThrows(MemoryLimitException.class, () -> filter.applyDataFilter(true, dataSource));
	}

	@Test
	void testGetFilterName() throws IOException {
		final var formats = CompressFormat.values();
		for (var pos = 0; pos < formats.length; pos++) {
			assertEquals("decompressor:" + formats[pos].constantName,
			        new DataExchangeFilterDecompress(formats[pos]).getFilterName());
		}
	}

}
