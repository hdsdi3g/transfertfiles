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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.listener.ListenerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tv.hd3g.commons.IORuntimeException;

class FTPFileSystemBadPassiveTest {

	static File root;

	static int port;
	static InetAddress host;
	static String username;
	static String password;
	static DefaultFtpServer ftpd;

	@BeforeAll
	static void load() throws IOException, FtpException {
		root = new File("target/testfs-ftp");
		FileUtils.forceMkdir(root);
		FileUtils.cleanDirectory(root);

		host = InetAddress.getLocalHost();
		username = "testusr";
		password = String.valueOf(System.nanoTime());

		final var serverFactory = new FtpServerFactory();
		serverFactory.setUserManager(new FTPLetUserManager(username, password, new File("")));
		final var factory = new ListenerFactory();
		factory.setPort(0);
		factory.setServerAddress(host.getHostAddress());
		serverFactory.setFtplets(Map.of("noPasv", new Ftplet() {

			@Override
			public FtpletResult beforeCommand(final FtpSession session,
			                                  final FtpRequest request) throws FtpException, IOException {
				if ("PASV".equals(request.getCommand().toUpperCase())) {
					session.write(new FtpReply() {

						@Override
						public int getCode() {
							return FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED;
						}

						@Override
						public boolean isPositive() {
							return false;
						}

						@Override
						public long getSentTime() {
							return 0;
						}

						@Override
						public String getMessage() {
							return "";
						}

					});
					return FtpletResult.DISCONNECT;
				}
				return FtpletResult.DEFAULT;
			}

			@Override
			public FtpletResult onDisconnect(final FtpSession session) throws FtpException, IOException {
				return FtpletResult.DEFAULT;
			}

			@Override
			public FtpletResult onConnect(final FtpSession session) throws FtpException, IOException {
				return FtpletResult.DEFAULT;
			}

			@Override
			public void init(final FtpletContext ftpletContext) throws FtpException {
			}

			@Override
			public void destroy() {
			}

			@Override
			public FtpletResult afterCommand(final FtpSession session,
			                                 final FtpRequest request,
			                                 final FtpReply reply) throws FtpException, IOException {
				return FtpletResult.DEFAULT;
			}
		}));

		serverFactory.addListener("default", factory.createListener());
		ftpd = (DefaultFtpServer) serverFactory.createServer();
		ftpd.start();
		port = ftpd.getListener("default").getPort();
	}

	@AfterAll
	static void ends() {
		try {
			ftpd.stop();
		} catch (final UnsupportedOperationException e) {
		}
	}

	FTPFileSystem fs;
	char[] goodPassword;

	@BeforeEach
	void init() throws IOException {
		goodPassword = password.toCharArray();
		fs = new FTPFileSystem(host, port, username, goodPassword, false);
	}

	@AfterEach
	void end() {
		if (fs != null) {
			fs.close();
		}
	}

	@Test
	void testIsPassiveMode() {
		assertFalse(fs.isPassiveMode());
	}

	@Test
	void testConnectIsAvaliable() {
		assertFalse(fs.isAvaliable());
		fs.connect();
		assertTrue(fs.isAvaliable());
		fs.connect();
		assertTrue(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testGetFromPath() {
		fs.connect();
		final var f = fs.getFromPath("target");
		assertTrue(f.list().count() > 0);
		fs.close();
	}

	@Test
	void testGetFromPath_ForcePassive() {
		fs = new FTPFileSystem(host, port, username, goodPassword, true);
		assertThrows(IORuntimeException.class, () -> fs.getFromPath("target"));
		fs.connect();
		final var f = fs.getFromPath("target");
		assertThrows(IORuntimeException.class, () -> f.list());
		fs.close();
	}

}
