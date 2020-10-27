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
import static org.mockito.ArgumentMatchers.eq;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FTPListingTest {

	@Mock
	FTPClient client;
	@Mock
	org.apache.commons.net.ftp.FTPFile fileEntry;

	String path;
	String result;

	@BeforeEach
	void init() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		path = String.valueOf(System.nanoTime());
		result = String.valueOf(System.nanoTime());

		Mockito.when(fileEntry.getName()).thenReturn(result);
	}

	@AfterEach
	void end() throws Exception {
		Mockito.verifyNoMoreInteractions(client);
		Mockito.verifyNoMoreInteractions(fileEntry);
	}

	@Test
	void testNLST() throws IOException {
		var stream = FTPListing.NLST.listDirectory(client, path);
		assertEquals(0, stream.count());

		Mockito.when(client.listNames(eq(path))).thenReturn(new String[] { result });
		stream = FTPListing.NLST.listDirectory(client, path);
		assertEquals(result, stream.findFirst().get());

		Mockito.verify(client, Mockito.times(2)).listNames(eq(path));
	}

	@Test
	void testLIST() throws IOException {
		var stream = FTPListing.LIST.listDirectory(client, path);
		assertEquals(0, stream.count());

		Mockito.when(client.listFiles(eq(path))).thenReturn(new org.apache.commons.net.ftp.FTPFile[] { fileEntry });
		stream = FTPListing.LIST.listDirectory(client, path);
		assertEquals(result, stream.findFirst().get());

		Mockito.verify(client, Mockito.times(2)).listFiles(eq(path));
		Mockito.verify(fileEntry, Mockito.times(1)).getName();
	}

	@Test
	void testMLSD() throws IOException {
		var stream = FTPListing.MLSD.listDirectory(client, path);
		assertEquals(0, stream.count());

		Mockito.when(client.mlistDir(eq(path))).thenReturn(new org.apache.commons.net.ftp.FTPFile[] { fileEntry });
		stream = FTPListing.MLSD.listDirectory(client, path);
		assertEquals(result, stream.findFirst().get());

		Mockito.verify(client, Mockito.times(2)).mlistDir(eq(path));
		Mockito.verify(fileEntry, Mockito.times(1)).getName();
	}

}
