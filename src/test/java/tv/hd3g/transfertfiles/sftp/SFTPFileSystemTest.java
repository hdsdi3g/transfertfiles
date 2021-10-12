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

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.sshd.common.config.keys.KeyUtils.RSA_ALGORITHM;
import static org.apache.sshd.server.auth.BuiltinUserAuthFactories.PASSWORD;
import static org.apache.sshd.server.auth.BuiltinUserAuthFactories.PUBLICKEY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.UnknownCommandFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.password.PasswordUtils;
import tv.hd3g.commons.IORuntimeException;
import tv.hd3g.transfertfiles.AbstractFile;

class SFTPFileSystemTest {

	static File root;
	static SshServer sshd;
	static int port;
	static InetAddress host;
	static String username;
	static String password;

	static File clientPrivateKeyFile;
	static File clientPasswordProtectedPrivateKeyFile;
	static String privateKeyPassword;

	@BeforeAll
	static void load() throws Exception {
		root = new File("target/testfs-ssh/working");
		FileUtils.forceMkdir(root.getParentFile());
		FileUtils.cleanDirectory(root.getParentFile());
		FileUtils.forceMkdir(root);

		/**
		 * Init client needs
		 */
		final var clientPublicKeyFile = new File("target/testfs-ssh/id_rsa.pub");
		clientPrivateKeyFile = new File("target/testfs-ssh/id_rsa");
		final var clientPasswordProtectedPublicKeyFile = new File("target/testfs-ssh/id_rsaP.pub");
		clientPasswordProtectedPrivateKeyFile = new File("target/testfs-ssh/id_rsaP");

		final var jsch = new JSch();
		final var kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
		kpair.writePublicKey(clientPublicKeyFile.getAbsolutePath(),
		        "Created for test purpose, can be deleted");
		kpair.writePrivateKey(clientPrivateKeyFile.getAbsolutePath());
		final var clientPublicKey = parseIdRSAkey(clientPrivateKeyFile, clientPublicKeyFile, null);
		kpair.dispose();

		privateKeyPassword = String.valueOf(System.nanoTime());
		final var kpairP = KeyPair.genKeyPair(jsch, KeyPair.RSA);
		kpairP.writePublicKey(clientPasswordProtectedPublicKeyFile.getAbsolutePath(),
		        "Created for test purpose, can be deleted");
		kpairP.writePrivateKey(clientPasswordProtectedPrivateKeyFile.getAbsolutePath(), privateKeyPassword.getBytes());
		final var clientPasswordProtectedPublicKey = parseIdRSAkey(clientPasswordProtectedPrivateKeyFile,
		        clientPasswordProtectedPublicKeyFile, privateKeyPassword);
		kpairP.dispose();

		/**
		 * Init server
		 */
		final var serverKeys = new File("target/testfs-ssh/testserverkey.ser");
		FileUtils.forceMkdirParent(serverKeys);
		FileUtils.deleteQuietly(serverKeys);

		host = InetAddress.getLocalHost();
		username = "testusr";
		password = String.valueOf(System.nanoTime());

		sshd = SshServer.setUpDefaultServer();
		sshd.setPort(0);
		sshd.setHost(host.getHostAddress());

		final var hostKeyProvider = new SimpleGeneratorHostKeyProvider(serverKeys.toPath());
		hostKeyProvider.setAlgorithm(RSA_ALGORITHM);

		sshd.setKeyPairProvider(hostKeyProvider);

		sshd.setUserAuthFactories(List.of(
		        PASSWORD.create(),
		        PUBLICKEY.create()));
		sshd.setPasswordAuthenticator(
		        (usr, passw, session) -> (usr.equalsIgnoreCase(username)
		                                  && passw.equals(password)));
		sshd.setPublickeyAuthenticator(
		        (usr, key, session) -> (usr.equalsIgnoreCase(username)
		                                && (clientPublicKey.equals(key)
		                                    || clientPasswordProtectedPublicKey.equals(key))));

		sshd.setCommandFactory(UnknownCommandFactory.INSTANCE);
		sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()));

		final var fileSystemFactory = new VirtualFileSystemFactory();
		fileSystemFactory.setDefaultHomeDir(new File("").getAbsoluteFile().toPath());
		sshd.setFileSystemFactory(fileSystemFactory);

		sshd.start();

		port = sshd.getPort();
	}

	private static final PublicKey parseIdRSAkey(final File privateKeyFile,
	                                             final File publicKeyFile,
	                                             final String password) throws GeneralSecurityException, IOException {
		final var parser = new OpenSSHKeyFile();
		if (password != null) {
			parser.init(readFileToString(privateKeyFile, StandardCharsets.US_ASCII),
			        readFileToString(publicKeyFile, StandardCharsets.US_ASCII),
			        PasswordUtils.createOneOff(password.toCharArray()));
		} else {
			parser.init(readFileToString(privateKeyFile, StandardCharsets.US_ASCII),
			        readFileToString(publicKeyFile, StandardCharsets.US_ASCII));
		}
		return parser.getPublic();
	}

	@AfterAll
	static void ends() throws Exception {
		sshd.stop(true);
	}

	SFTPFileSystem fs;
	char[] noPassword;
	char[] badPassword;

	@BeforeEach
	void init() throws IOException {
		final var savedFile = File.createTempFile("testKnownHosts", ".txt");
		System.setProperty("ssh.knownhosts", savedFile.getAbsolutePath());
		fs = new SFTPFileSystem(host, port, username, "");
		noPassword = "".toCharArray();
		badPassword = "nope".toCharArray();
	}

	@AfterEach
	void end() {
		if (fs != null) {
			fs.close();
		}
		System.clearProperty("ssh.knownhosts");
	}

	@Test
	void testSFTPFileSystem_noUsername() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> {
			new SFTPFileSystem(host, port, "", "");
		});
	}

	@Test
	void testSetPasswordAuth() {
		assertDoesNotThrow(() -> fs.setPasswordAuth(badPassword));

		final var empty = "".toCharArray();
		assertThrows(IllegalArgumentException.class, () -> fs.setPasswordAuth(empty));
		assertThrows(NullPointerException.class, () -> fs.setPasswordAuth(null));
	}

	@Test
	void testManuallyAddPrivatekeyAuthFile_password() {
		assertDoesNotThrow(() -> fs.manuallyAddPrivatekeyAuth(clientPasswordProtectedPrivateKeyFile,
		        noPassword));
		assertDoesNotThrow(() -> fs.manuallyAddPrivatekeyAuth(clientPasswordProtectedPrivateKeyFile.getParentFile(),
		        badPassword));
	}

	@Test
	void testManuallyAddPrivatekeyAuthFile() {
		fs.manuallyAddPrivatekeyAuth(clientPrivateKeyFile);
		fs.manuallyAddPrivatekeyAuth(clientPrivateKeyFile.getParentFile());
		assertThrows(NullPointerException.class, () -> fs.manuallyAddPrivatekeyAuth(null));
		final var notExists = new File("nopeFile");
		assertThrows(IORuntimeException.class, () -> fs.manuallyAddPrivatekeyAuth(notExists));
	}

	@Test
	void testGetClient() {
		assertNotNull(fs.getClient());
	}

	@Test
	void testIsStatefulSFTPClient() {
		assertFalse(fs.isStatefulSFTPClient());
	}

	@Test
	void testSetStatefulSFTPClient() {
		fs.setStatefulSFTPClient(true);
		assertTrue(fs.isStatefulSFTPClient());
	}

	@Test
	void testGetHost() throws UnknownHostException {
		assertEquals(InetAddress.getLocalHost(), fs.getHost());
	}

	@Test
	void testGetUsername() {
		assertEquals(username, fs.getUsername());
	}

	@Test
	void testConnect_badHostPort() throws UnknownHostException {
		fs = new SFTPFileSystem(InetAddress.getByName("127.255.255.255"), port, username, "");
		fs.getClient().setTimeout(1);
		fs.getClient().setConnectTimeout(1);
		assertThrows(IORuntimeException.class, () -> fs.connect());
		assertFalse(fs.isAvaliable());

		fs = new SFTPFileSystem(host, 1, username, "");
		fs.getClient().setTimeout(1);
		fs.getClient().setConnectTimeout(1);
		assertThrows(IORuntimeException.class, () -> fs.connect());
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testIsReusable() {
		assertFalse(fs.isReusable());
	}

	@Test
	void testConnect_auth_noAuth() {
		assertTrue(port > 0);
		assertThrows(IORuntimeException.class, () -> fs.connect());
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testConnect_auth_badPassword() {
		fs.setPasswordAuth(badPassword);
		assertThrows(IORuntimeException.class, () -> fs.connect());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testConnect_auth_password() {
		fs.setPasswordAuth(password.toCharArray());
		fs.connect();
		assertTrue(fs.isAvaliable());
		fs.connect();
		assertTrue(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testConnect_auth_pubkey() {
		fs.manuallyAddPrivatekeyAuth(clientPrivateKeyFile);
		fs.connect();
		assertTrue(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testConnect_auth_pubkey_password_bad() throws IOException {
		fs.manuallyAddPrivatekeyAuth(clientPasswordProtectedPrivateKeyFile, badPassword);
		assertThrows(IORuntimeException.class, () -> fs.connect());
		assertFalse(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testConnect_auth_pubkey_password() throws IOException {
		fs.manuallyAddPrivatekeyAuth(clientPasswordProtectedPrivateKeyFile, privateKeyPassword.toCharArray());
		fs.connect();
		assertTrue(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testConnect_auth_pubkey_password_no() throws IOException {
		fs.manuallyAddPrivatekeyAuth(clientPasswordProtectedPrivateKeyFile);
		assertThrows(IORuntimeException.class, () -> fs.connect());
		assertFalse(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testIsAvaliable() {
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testIsAvaliable_badLogin() {
		fs.setPasswordAuth(badPassword);
		assertThrows(IORuntimeException.class, () -> fs.connect());
		assertFalse(fs.isAvaliable());

		fs.setPasswordAuth(password.toCharArray());
		assertThrows(IORuntimeException.class, () -> fs.connect());
		assertFalse(fs.isAvaliable());
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testCreateANewSFTPClient() {
		fs.setPasswordAuth(password.toCharArray());
		assertNull(fs.getSFTPClient());
		fs.connect();
		final var client0 = fs.getSFTPClient();
		assertNotNull(client0);
		final var client1 = fs.getSFTPClient();
		assertEquals(client0, client1);
		fs.createANewSFTPClient();
		final var client3 = fs.getSFTPClient();
		assertNotEquals(client0, client3);
		fs.close();
		assertNull(fs.getSFTPClient());
	}

	@Test
	void testCreateANewSFTPClient_stateFull() {
		fs.setPasswordAuth(password.toCharArray());
		fs.setStatefulSFTPClient(true);
		fs.connect();
		final var client0 = fs.getSFTPClient();
		assertNotNull(client0);
		final var client1 = fs.getSFTPClient();
		assertEquals(client0, client1);
		fs.createANewSFTPClient();
		final var client3 = fs.getSFTPClient();
		assertNotEquals(client0, client3);
		fs.close();
		assertNull(fs.getSFTPClient());
	}

	@Test
	void testClose() {
		fs.close();
		assertFalse(fs.isAvaliable());
	}

	@Test
	void testGetFromPath() throws IOException {
		fs.setPasswordAuth(password.toCharArray());
		fs.connect();
		final var f = fs.getFromPath("target");
		assertTrue(f.list()
		        .map(AbstractFile::getName)
		        .anyMatch(sf -> sf.equalsIgnoreCase("testfs-ssh")));
		fs.close();
	}

	@Test
	void testGetFromPath_disconnected() {
		assertThrows(IORuntimeException.class, () -> fs.getFromPath("."));
		fs.setPasswordAuth(password.toCharArray());
		fs.connect();
		fs.close();
		assertThrows(IORuntimeException.class, () -> fs.getFromPath("."));
	}

	@Test
	void testGetSftpClient() {
		assertNull(fs.getSFTPClient());
	}

	@Test
	void testEqualshashCode() {
		final var lfs = new SFTPFileSystem(host, port, username, "");
		assertEquals(lfs, fs);
		assertEquals(lfs.hashCode(), fs.hashCode());
	}

	@Test
	void testToString() {
		assertEquals("sftp://" + username + "@" + host.getHostName() + ":" + port + "/", fs.toString());
	}

	@Test
	void testTimeout() {
		fs.setTimeout(2000, TimeUnit.MILLISECONDS);
		fs.setPasswordAuth(password.toCharArray());
		fs.connect();
		final var client = fs.getClient();
		assertEquals(fs.getTimeout(), client.getConnectTimeout());
		assertEquals(fs.getTimeout(), client.getTimeout());
	}

	@Test
	void testReusableHashCode() {
		fs.setPasswordAuth(password.toCharArray());
		fs.connect();
		final var code0 = fs.reusableHashCode();
		assertNotEquals(0, code0);

		final var lfs = new SFTPFileSystem(host, port, username, "");
		lfs.setPasswordAuth(password.toCharArray());
		lfs.connect();
		assertNotEquals(code0, lfs.reusableHashCode());

		lfs.close();
	}

}
