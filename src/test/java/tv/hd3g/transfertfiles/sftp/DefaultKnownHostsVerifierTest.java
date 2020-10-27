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
 * Copyright (C) hdsdi3g for hd3g.tv 2020
 *
 */
package tv.hd3g.transfertfiles.sftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.schmizz.sshj.common.KeyType;

class DefaultKnownHostsVerifierTest {

	static PrivateKey privateKey;
	static PublicKey publicKey;

	@BeforeAll
	static void load() throws GeneralSecurityException {
		final var keyGen = KeyPairGenerator.getInstance("RSA");

		SecureRandom random;
		try {
			random = SecureRandom.getInstance("NATIVEPRNGNONBLOCKING");
		} catch (final NoSuchAlgorithmException e) {
			random = SecureRandom.getInstanceStrong();
		}

		keyGen.initialize(2048, random);
		final var pair = keyGen.generateKeyPair();
		privateKey = pair.getPrivate();
		publicKey = pair.getPublic();
	}

	File savedFile;
	DefaultKnownHostsVerifier d;

	@BeforeEach()
	void init() throws IOException {
		savedFile = File.createTempFile("testKnownHosts", ".txt");
		d = new DefaultKnownHostsVerifier(savedFile);
	}

	@Test
	void testDefaultKnownHostsVerifier() throws IOException {
		assertEquals(savedFile, d.getFile());
		assertEquals(0, d.entries().size());
	}

	@Test
	void testHostKeyUnverifiableActionStringPublicKey() throws IOException {
		final var result = d.hostKeyUnverifiableAction("test-host-name", publicKey);
		assertTrue(result);
		assertEquals(1, d.entries().size());
		assertTrue(d.entries().get(0).appliesTo(KeyType.RSA, "test-host-name"));
	}

	@Test
	void testHostKeyChangedActionStringPublicKey() throws IOException {
		final var result = d.hostKeyChangedAction("test-host-name", publicKey);
		assertFalse(result);
	}

}
