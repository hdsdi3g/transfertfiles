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

import static org.apache.sshd.common.config.keys.KeyUtils.RSA_ALGORITHM;
import static org.apache.sshd.server.auth.BuiltinUserAuthFactories.PASSWORD;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.UnknownCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

import tv.hd3g.transfertfiles.AbstractFileSystem;
import tv.hd3g.transfertfiles.BaseTestSpecific;
import tv.hd3g.transfertfiles.TestFileToolkit;

class SFTPFileTest extends TestFileToolkit<SFTPFile> {

	static SshServer sshd;
	static String password;
	static InetAddress host;

	@BeforeAll
	static void load() throws Exception {
		/**
		 * Init server
		 */
		final var serverKeys = new File("target/testfs-scp/testserverkey.ser");
		FileUtils.forceMkdirParent(serverKeys);
		FileUtils.deleteQuietly(serverKeys);

		host = InetAddress.getLocalHost();
		final var username = "testusr";
		password = String.valueOf(System.nanoTime());

		sshd = SshServer.setUpDefaultServer();
		sshd.setPort(0);
		sshd.setHost(host.getHostAddress());

		final var hostKeyProvider = new SimpleGeneratorHostKeyProvider(serverKeys.toPath());
		hostKeyProvider.setAlgorithm(RSA_ALGORITHM);
		sshd.setKeyPairProvider(hostKeyProvider);

		sshd.setUserAuthFactories(List.of(PASSWORD.create()));
		sshd.setPasswordAuthenticator(
		        (usr, passw, session) -> (usr.equalsIgnoreCase(username)
		                                  && passw.equals(password)));
		sshd.setCommandFactory(UnknownCommandFactory.INSTANCE);
		sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()));

		final var fileSystemFactory = new VirtualFileSystemFactory();
		fileSystemFactory.setDefaultHomeDir(getRoot().getAbsoluteFile().toPath());
		sshd.setFileSystemFactory(fileSystemFactory);

		sshd.start();

		final var savedFile = File.createTempFile("testKnownHosts", ".txt");
		System.setProperty("ssh.knownhosts", savedFile.getAbsolutePath());
	}

	@AfterAll
	static void ends() throws Exception {
		sshd.stop(true);
	}

	@Override
	protected AbstractFileSystem<SFTPFile> createFileSystem() {
		final var fs = new SFTPFileSystem(host, sshd.getPort(), "testusr");
		fs.setPasswordAuth(password.toCharArray());
		fs.connect();
		return fs;
	}

	@Nested
	class Specific_File extends BaseTestSpecific<SFTPFileSystem, SFTPFile> {

		@Override
		protected File provideRootDir() {
			return getRoot();
		}

		@Override
		protected SFTPFileSystem createNewFileSystem() {
			return (SFTPFileSystem) createFileSystem();
		}

		@Override
		protected void afterInit() throws IOException {
			file = new File(getRoot(), getRelativePath()).getAbsoluteFile();
			write(file);
		}

		@Override
		protected SFTPFile createNewAbstractFile(final SFTPFileSystem fs, final String path) {
			return new SFTPFile(fs, fs.getSFTPClient(), path);
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
