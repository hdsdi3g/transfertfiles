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
package tv.hd3g.transfertfiles;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tv.hd3g.transfertfiles.DataExchangeObserver.createLogger;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.MD5;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tv.hd3g.transfertfiles.filters.DataExchangeFilterHashExtraction;
import tv.hd3g.transfertfiles.randomfs.RandomFile;

class DataExchangePipelineTest {

	int buffer;
	int passes;
	int maxTransfert;
	RandomFile from;
	RandomFile to;
	DataExchangeFilterHashExtraction dig0;
	DataExchangeFilterHashExtraction dig1;
	DataExchangeFilterHashExtraction dig2;

	@BeforeEach
	void init() throws Exception {
		dig0 = new DataExchangeFilterHashExtraction(MD5);
		dig1 = new DataExchangeFilterHashExtraction(MD5);
		dig2 = new DataExchangeFilterHashExtraction(MD5);
	}

	@Test
	void testTransfert() throws InterruptedException, ExecutionException, TimeoutException {
		buffer = 1024;
		passes = 1_000_000;
		maxTransfert = buffer * passes;
		from = new RandomFile(maxTransfert, buffer);
		to = new RandomFile(maxTransfert, buffer);

		assertTrue(from.getTransfered().isEmpty());
		assertTrue(to.getTransfered().isEmpty());

		final var cfTransfert = doTransfer();
		from.copyAbstractToAbstract(to, DataExchangeObserver.createLogger());

		final var copied = cfTransfert.get(1, SECONDS);
		assertEquals(maxTransfert, copied);
	}

	void checkHashes() {
		final var hash0 = dig0.getResults().get(MD5);
		final var hash1 = dig1.getResults().get(MD5);
		final var hash2 = dig2.getResults().get(MD5);
		assertTrue(Arrays.equals(hash0, hash2));
		assertTrue(Arrays.equals(hash0, hash1));
	}

	@Test
	void testTransfert_filter_hashes() throws InterruptedException, ExecutionException, TimeoutException {
		buffer = 1024;
		passes = 100_000;
		maxTransfert = buffer * passes;
		from = new RandomFile(maxTransfert, buffer);
		to = new RandomFile(maxTransfert, buffer);

		assertTrue(from.getTransfered().isEmpty());
		assertTrue(to.getTransfered().isEmpty());

		final var cfTransfert = doTransfer();

		from.copyAbstractToAbstract(to, createLogger(), dig0, dig1, dig2);

		final var copied = cfTransfert.get(1, SECONDS);
		assertEquals(maxTransfert, copied);

		checkHashes();
	}

	/*@Test
	void testTransfert_filter_simpleCompress() throws InterruptedException, ExecutionException, TimeoutException {
		buffer = 1024;
		passes = 1_000_000;
		maxTransfert = buffer * passes;
		from = new ZeroFile(maxTransfert, buffer);
		to = new ZeroFile(maxTransfert, buffer);

		assertTrue(from.getTransfered().isEmpty());
		assertTrue(to.getTransfered().isEmpty());

		final var cfTransfert = doTransfer();
		final var compress = new DataExchangeFilterCompress(CompressFormat.DEFLATE);
		final var unCompress = new DataExchangeFilterDecompress(CompressFormat.DEFLATE);

		final var transfert = from.copyAbstractToAbstract(to, createLogger(), compress, unCompress);

		final var copied = cfTransfert.get(1, SECONDS);
		assertEquals(maxTransfert, copied);

		assertNotNull(transfert);
		final var compressTS = transfert.getTransfertStats(compress);
		final var unCompressTS = transfert.getTransfertStats(unCompress);
		assertNotNull(compressTS);
		assertNotNull(unCompressTS);

		final var cDeltaTranfered = compressTS.getDeltaTranfered();
		final var unCDeltaTranfered = unCompressTS.getDeltaTranfered();
		assertEquals(-cDeltaTranfered, unCDeltaTranfered);
		assertTrue(compressTS.getTotalDuration() > 0);
		assertTrue(unCompressTS.getTotalDuration() > 0);
		assertTrue(transfert.getIoWaitTime() >= 0);
	}*/

	CompletableFuture<Integer> doTransfer() {
		return CompletableFuture.supplyAsync(() -> {
			final var tFrom = from.getTransfered();
			final var tTo = to.getTransfered();

			var count = 0;
			try {
				while (count < maxTransfert) {
					final var nextPush = tFrom.take();
					final var nextPull = tTo.take();
					assertNotNull(nextPush);
					assertNotNull(nextPull);
					count += nextPull.getSize();
					assertEquals(nextPush.getSize(), nextPull.getSize());
					assertEquals(nextPush, nextPull);
				}
			} catch (final InterruptedException e) {
				throw new AssertionError("Can't take", e);
			}

			assertTrue(tFrom.isEmpty(), "Size: " + tFrom.size());
			assertTrue(tTo.isEmpty(), "Size: " + tTo.size());
			return count;
		});
	}

	/*class ZeroFile extends RandomFile {
	
		public ZeroFile(final int maxTransfert, final int bufferSize) {
			super(maxTransfert, bufferSize);
		}
	
		@Override
		protected void nextBytes(final byte[] buffer) {
		}
	}*/

}
