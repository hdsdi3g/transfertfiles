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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.MD5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.BufferVault;

class DataExchangeFilterHashExtractionTest {

	DataExchangeFilterHashExtraction filter;

	@Test
	void testDataExchangeFilterHashExtraction_empty() {
		assertThrows(IllegalArgumentException.class, DataExchangeFilterHashExtraction::new);
		final Collection<DigestFilterHashExtraction> empty = List.of();
		assertThrows(IllegalArgumentException.class, () -> new DataExchangeFilterHashExtraction(empty));
	}

	@Test
	void testGetFilterName() {
		filter = new DataExchangeFilterHashExtraction(MD5);
		assertEquals("HashExtraction:MD5", filter.getFilterName());
	}

	@Test
	void testApplyDataFilter() throws IOException {
		filter = new DataExchangeFilterHashExtraction(MD5);
		assertTrue(Arrays.equals(new byte[0], filter.applyDataFilter(false, new BufferVault()).readAll()));
	}

	@Test
	void testGetResults() throws IOException {
		filter = new DataExchangeFilterHashExtraction(MD5);

		final var random = new Random();
		final var buffer = new byte[random.nextInt(1000)];
		random.nextBytes(buffer);
		filter.applyDataFilter(false, new BufferVault().write(buffer));

		final var results = filter.getResults();
		assertNotNull(results);
		assertEquals(1, results.size());
		assertTrue(results.containsKey(MD5));
		results.get(MD5);

		final var instance = MD5.createInstance();
		instance.update(ByteBuffer.wrap(buffer));

		assertTrue(Arrays.equals(instance.digest(), results.get(MD5)));
	}
}
