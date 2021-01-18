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
import static tv.hd3g.transfertfiles.filters.DataExchangeFilter.DEFAULT_BUFFER_SIZE;

import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.BufferVault;

abstract class DataExchangeFilterIOStreamTest {
	static Random random = new Random();
	byte[] datas;

	@BeforeEach
	void init() throws Exception {
		datas = new byte[random.nextInt(DEFAULT_BUFFER_SIZE - 100) + 100];
		random.nextBytes(datas);
	}

	abstract DataExchangeFilter getEchangeFilter();

	abstract TestClosableStream getTestClosableStream();

	interface TestClosableStream {
		int getCloseCount();
	}

	@Test
	void testOnCancelTransfert() throws IOException {
		final var echangeFilter = getEchangeFilter();
		echangeFilter.applyDataFilter(false, new BufferVault());
		echangeFilter.onCancelTransfert();
		assertEquals(1, getTestClosableStream().getCloseCount());
	}

	@Test
	void testEnsureMinDataSourcesDataLength() {
		final var echangeFilter = getEchangeFilter();
		assertEquals(DEFAULT_BUFFER_SIZE, echangeFilter.ensureMinDataSourcesDataLength());
	}
}
