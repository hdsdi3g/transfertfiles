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
package tv.hd3g.transfertfiles.ftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.net.ftp.FTPSClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FTPSFileSystemTest {

	InetAddress host;
	int port;
	String username;
	String password;
	FTPFileSystem fs;

	@BeforeEach
	void init() throws UnknownHostException {
		host = InetAddress.getLocalHost();
		port = 21;
		username = "testusr";
		password = String.valueOf(System.nanoTime());
		fs = new FTPSFileSystem(host, port, username, password.toCharArray(), true);
	}

	@Test
	void testCreateFTPClient() {
		final var client = fs.createFTPClient();
		assertNotNull(client);
		assertTrue(client instanceof FTPSClient);
	}

	@Test
	void testToString() {
		assertEquals("ftps://" + username + "@" + host + ":" + port, fs.toString());
	}

}
