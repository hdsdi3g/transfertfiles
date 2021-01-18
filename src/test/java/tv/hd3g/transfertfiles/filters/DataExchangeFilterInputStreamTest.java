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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tv.hd3g.transfertfiles.filters.DataExchangeFilter.DEFAULT_BUFFER_SIZE;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.BufferVault;

class DataExchangeFilterInputStreamTest extends DataExchangeFilterIOStreamTest {
	DataExchangeFilterInputStream echangeFilter;
	TestInputStream externalStream;
	InputStream internalStream;

	@Override
	@BeforeEach
	void init() throws Exception {
		super.init();
		externalStream = new TestInputStream(new ByteArrayInputStream(datas));

		echangeFilter = new DataExchangeFilterInputStream(internalStream -> {
			this.internalStream = internalStream;
			return externalStream;
		});
	}

	class TestInputStream extends FilterInputStream implements TestClosableStream {

		int closed;

		protected TestInputStream(final InputStream in) {
			super(in);
			closed = 0;
		}

		@Override
		public void close() throws IOException {
			super.close();
			closed++;
		}

		@Override
		public int getCloseCount() {
			return closed;
		}
	}

	@Override
	DataExchangeFilter getEchangeFilter() {
		return echangeFilter;
	}

	@Test
	void testApplyDataFilter() throws IOException {
		final var dataSourceBytes = new byte[datas.length];
		random.nextBytes(dataSourceBytes);
		final var dataSource = new BufferVault().write(dataSourceBytes);

		final var result = echangeFilter.applyDataFilter(false, dataSource);
		assertNotNull(result);

		/**
		 * Check result
		 */
		assertEquals(datas.length, result.getSize());

		/**
		 * Assert provided externalStream == result
		 */
		final var resultBytes = result.readAll();
		assertTrue(Arrays.equals(datas, resultBytes));

		/**
		 * Assert dataSource == provided internalStream
		 */
		final var readed = internalStream.read(resultBytes);
		assertEquals(resultBytes.length, readed);
		assertEquals(-1, internalStream.read());
		assertTrue(Arrays.equals(dataSourceBytes, resultBytes));

		/**
		 * Check close
		 */
		assertEquals(0, externalStream.closed);
		echangeFilter.applyDataFilter(true, new BufferVault().write(new byte[DEFAULT_BUFFER_SIZE + 2]));
		assertEquals(1, externalStream.closed);

		/**
		 * Check InternalInputStream (remaining) behaviors
		 */
		externalStream.reset();
		assertEquals(0, internalStream.read());
		assertEquals(1, internalStream.read(new byte[1], 0, 1));
		assertEquals(DEFAULT_BUFFER_SIZE,
		        internalStream.read(new byte[DEFAULT_BUFFER_SIZE], 0, DEFAULT_BUFFER_SIZE));
		assertEquals(-1, internalStream.read(new byte[1], 0, 1));
	}

	@Override
	TestClosableStream getTestClosableStream() {
		return externalStream;
	}

}
