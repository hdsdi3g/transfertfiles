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
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.ADLER32;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.BLAKE2B_384;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.BLAKE2B_512;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.BLAKE2S_224;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.BLAKE2S_256;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.CRC32;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.CRC32C;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.MD5;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.SHA3_224;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.SHA3_256;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.SHA3_384;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.SHA3_512;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.SHA_1;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.SHA_224;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.SHA_256;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.SHA_384;
import static tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.SHA_512;

import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

class DigestFilterHashExtractionTest {

	private static final String THE_FOX = "The quick brown fox jumps over the lazy dog";
	private static final DigestFilterHashExtraction[] items = DigestFilterHashExtraction.values();

	@Test
	void testToString() {
		for (var pos = 0; pos < items.length; pos++) {
			assertEquals(items[pos].getDigestName(), items[pos].toString());
		}
	}

	@Test
	void testIsAvaliable() {
		for (var pos = 0; pos < items.length; pos++) {
			assertTrue(items[pos].isAvaliable(), items[pos].getDigestName());
		}
	}

	@Test
	void testGetDigestName() {
		assertEquals("CRC-32", CRC32.getDigestName());
		assertEquals("CRC-32C", CRC32C.getDigestName());
		assertEquals("ADLER-32", ADLER32.getDigestName());
		assertEquals("MD5", MD5.getDigestName());
		assertEquals("SHA-1", SHA_1.getDigestName());
		assertEquals("SHA-224", SHA_224.getDigestName());
		assertEquals("SHA-256", SHA_256.getDigestName());
		assertEquals("SHA-384", SHA_384.getDigestName());
		assertEquals("SHA-512", SHA_512.getDigestName());
		assertEquals("SHA3-224", SHA3_224.getDigestName());
		assertEquals("SHA3-256", SHA3_256.getDigestName());
		assertEquals("SHA3-384", SHA3_384.getDigestName());
		assertEquals("SHA3-512", SHA3_512.getDigestName());
		assertEquals("BLAKE2s-224", BLAKE2S_224.getDigestName());
		assertEquals("BLAKE2s-256", BLAKE2S_256.getDigestName());
		assertEquals("BLAKE2b-384", BLAKE2B_384.getDigestName());
		assertEquals("BLAKE2b-512", BLAKE2B_512.getDigestName());
	}

	static String compute(final String value, final DigestFilterHashExtraction item) {
		final var instance = item.createInstance();
		assertNotNull(instance);
		instance.update(ByteBuffer.wrap(value.getBytes()));
		final var result = instance.digest();
		assertNotNull(result);
		assertTrue(result.length > 1);
		return new String(Hex.encodeHex(result));
	}

	@Test
	void testCreateInstance() {
		assertEquals(
		        "9e107d9d372bb6826bd81d3542a419d6",
		        compute(THE_FOX, MD5));
		assertEquals("cbf43926", compute("123456789", CRC32));
		assertEquals("ee0c002b", compute("Lorem ipsum dolor sit amet", CRC32C));
		assertEquals("5bdc0fda", compute(THE_FOX, ADLER32));
		assertEquals(
		        "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12",
		        compute(THE_FOX,
		                SHA_1));
		assertEquals(
		        "730e109bd7a8a32b1cb9d9a09aa2325d2430587ddbc0c38bad911525",
		        compute(THE_FOX,
		                SHA_224));
		assertEquals(
		        "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
		        compute("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
		                SHA_256));
		assertEquals(
		        "09330c33f71147e83d192fc782cd1b4753111b173b3b05d22fa08086e3b0f712fcc7c71a557e2db966c3e9fa91746039",
		        compute("abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu",
		                SHA_384));
		assertEquals(
		        "8e959b75dae313da8cf4f72814fc143f8f7779c6eb9f7fa17299aeadb6889018501d289e4900f7e4331b99dec4b5433ac7d329eeb6dd26545e96e55b874be909",
		        compute("abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu",
		                SHA_512));
		assertEquals(
		        "d15dadceaa4d5d7bb3b48f446421d542e08ad8887305e28d58335795",
		        compute(THE_FOX,
		                SHA3_224));
		assertEquals(
		        "69070dda01975c8c120c3aada1b282394e7f032fa9cf32f4cb2259a0897dfc04",
		        compute(THE_FOX,
		                SHA3_256));
		assertEquals(
		        "7063465e08a93bce31cd89d2e3ca8f602498696e253592ed26f07bf7e703cf328581e1471a7ba7ab119b1a9ebdf8be41",
		        compute(THE_FOX,
		                SHA3_384));
		assertEquals(
		        "01dedd5de4ef14642445ba5f5b97c15e47b9ad931326e4b0727cd94cefc44fff23f07bf543139939b49128caf436dc1bdee54fcb24023a08d9403f9b4bf0d450",
		        compute(THE_FOX,
		                SHA3_512));
		assertEquals(
		        "e4e5cb6c7cae41982b397bf7b7d2d9d1949823ae78435326e8db4912",
		        compute(THE_FOX,
		                BLAKE2S_224));
		assertEquals(
		        "606beeec743ccbeff6cbcdf5d5302aa855c256c29b88c8ed331ea1a6bf3c8812",
		        compute(THE_FOX,
		                BLAKE2S_256));
		assertEquals(
		        "b7c81b228b6bd912930e8f0b5387989691c1cee1e65aade4da3b86a3c9f678fc8018f6ed9e2906720c8d2a3aeda9c03d",
		        compute(THE_FOX,
		                BLAKE2B_384));
		assertEquals(
		        "a8add4bdddfd93e4877d2746e62817b116364a1fa7bc148d95090bc7333b3673f82401cf7aa2e4cb1ecd90296e3f14cb5413f8ed77be73045b13914cdcd6a918",
		        compute(THE_FOX,
		                BLAKE2B_512));
	}

}
