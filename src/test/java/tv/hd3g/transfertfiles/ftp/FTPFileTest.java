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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import org.apache.commons.io.FileUtils;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.listener.ListenerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

import tv.hd3g.transfertfiles.AbstractFileSystem;
import tv.hd3g.transfertfiles.BaseTestSpecific;
import tv.hd3g.transfertfiles.TestFileToolkit;

class FTPFileTest extends TestFileToolkit<FTPFile> {

	static int port;
	static InetAddress host;
	static String username;
	static String password;
	static DefaultFtpServer ftpd;

	@BeforeAll
	static void load() throws IOException, FtpException {
		host = InetAddress.getLocalHost();
		username = "testusr";
		password = String.valueOf(System.nanoTime());

		final var serverFactory = new FtpServerFactory();
		serverFactory.setUserManager(new FTPLetUserManager(username, password, getRoot().getAbsoluteFile()));
		final var factory = new ListenerFactory();
		factory.setPort(0);
		factory.setServerAddress(host.getHostAddress());
		serverFactory.addListener("default", factory.createListener());
		ftpd = (DefaultFtpServer) serverFactory.createServer();
		ftpd.start();
		port = ftpd.getListener("default").getPort();
	}

	@AfterAll
	static void ends() {
		ftpd.stop();
	}

	@Override
	protected AbstractFileSystem<FTPFile> createFileSystem() {
		final var fs = new FTPFileSystem(host, port, username, password.toCharArray(), true, "");
		fs.connect();
		return fs;
	}

	@Nested
	class Specific_File extends BaseTestSpecific<FTPFileSystem, FTPFile> {

		@Override
		protected File provideRootDir() {
			return getRoot();
		}

		@Override
		protected FTPFileSystem createNewFileSystem() {
			return (FTPFileSystem) createFileSystem();
		}

		@Override
		protected void afterInit() throws IOException {
			file = new File(getRoot(), getRelativePath()).getAbsoluteFile();
			write(file);
		}

		@Override
		protected FTPFile createNewAbstractFile(final FTPFileSystem fs, final String path) {
			return new FTPFile(fs, path);
		}

		@Override
		protected String getRelativePath() {
			return "temp/existing-file2";
		}
	}

	@Nested
	class Specific_Dir extends Specific_File {

		@Override
		protected String getRelativePath() {
			return "temp/existing-dir2";
		}

		@Override
		protected void afterInit() throws IOException {
			file = new File(getRoot(), "temp/existing-dir2").getAbsoluteFile();
			FileUtils.forceMkdir(file);
		}

	}

	@Nested
	class Specific_NotExistsFile extends Specific_File {

		@Override
		protected String getRelativePath() {
			return "temp/notexisting-file2";
		}

		@Override
		protected void afterInit() throws IOException {
		}

	}
}
