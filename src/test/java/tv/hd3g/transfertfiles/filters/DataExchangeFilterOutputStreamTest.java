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

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.BufferVault;

class DataExchangeFilterOutputStreamTest extends DataExchangeFilterIOStreamTest {

	DataExchangeFilterOutputStream echangeFilter;
	OutputStream internalStream;
	ByteArrayOutputStream datasStream;
	TestOutputStream externalStream;

	@Override
	@BeforeEach
	void init() throws Exception {
		super.init();
		datasStream = new ByteArrayOutputStream();
		externalStream = new TestOutputStream(datasStream);

		echangeFilter = new DataExchangeFilterOutputStream(internalStream -> {
			this.internalStream = internalStream;
			return externalStream;
		});
	}

	@Override
	DataExchangeFilter getEchangeFilter() {
		return echangeFilter;
	}

	enum CheckWrite {
		SINGLE_BYTE,
		BYTES,
		BYTES_OFF_LEN;
	}

	class TestOutputStream extends FilterOutputStream implements TestClosableStream {
		int closed;
		CheckWrite cw;

		public TestOutputStream(final OutputStream out) {
			super(out);
			closed = 0;
			cw = CheckWrite.BYTES_OFF_LEN;
		}

		@Override
		public void write(final byte[] b, final int off, final int len) throws IOException {
			switch (cw) {
			case SINGLE_BYTE:
				for (var i = 0; i < len; i++) {
					datasStream.write(b[off + i]);
					internalStream.write(b[off + i]);
				}
				break;
			case BYTES:
				final var newArray = Arrays.copyOfRange(b, off, len);
				datasStream.write(newArray);
				internalStream.write(newArray);
				break;
			case BYTES_OFF_LEN:
				datasStream.write(b, off, len);
				internalStream.write(b, off, len);
				break;
			}
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

	@Test
	void testApplyDataFilter() throws IOException {
		final var result = echangeFilter.applyDataFilter(false, new BufferVault().write(datas));
		assertNotNull(result);

		/**
		 * Check result
		 */
		assertEquals(datas.length, result.getSize());

		/**
		 * Assert provided externalStream == result
		 */
		assertTrue(Arrays.equals(datas, result.readAll()));

		/**
		 * Assert dataSource == provided internalStream
		 */
		var capturedData = datasStream.toByteArray();
		assertEquals(datas.length, capturedData.length);
		assertTrue(Arrays.equals(datas, capturedData));

		/**
		 * Check InternalOutputStream (remaining) behaviors
		 */
		datasStream.reset();
		externalStream.cw = CheckWrite.BYTES;
		echangeFilter.applyDataFilter(false, new BufferVault().write(new byte[10]));
		capturedData = datasStream.toByteArray();
		assertEquals(10, capturedData.length);

		/**
		 * Check close
		 * Check InternalOutputStream (remaining) behaviors
		 */
		assertEquals(0, externalStream.closed);
		datasStream.reset();
		externalStream.cw = CheckWrite.SINGLE_BYTE;
		echangeFilter.applyDataFilter(true, new BufferVault().write(new byte[DEFAULT_BUFFER_SIZE + 1]));
		assertEquals(1, externalStream.closed);

		capturedData = datasStream.toByteArray();
		assertEquals(DEFAULT_BUFFER_SIZE + 1, capturedData.length);
	}

	@Override
	TestClosableStream getTestClosableStream() {
		return externalStream;
	}

}
