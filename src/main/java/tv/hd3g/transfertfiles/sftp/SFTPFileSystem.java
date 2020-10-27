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

import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import tv.hd3g.commons.IORuntimeException;
import tv.hd3g.transfertfiles.AbstractFileSystem;

public class SFTPFileSystem implements AbstractFileSystem<SFTPFile> {
	private static final Logger log = LogManager.getLogger();

	private final SSHClient client;
	private final InetAddress host;
	private final int port;
	private final String username;
	private final Set<KeyProvider> authKeys;

	private char[] password;
	private SFTPClient sftpClient;
	private boolean statefulSFTPClient;
	private volatile boolean wasConnected;

	public SFTPFileSystem(final InetAddress host, final int port, final String username) {
		client = new SSHClient();
		wasConnected = false;
		this.host = Objects.requireNonNull(host);
		this.port = port;
		this.username = Objects.requireNonNull(username, "username");
		if (username.length() == 0) {
			throw new IllegalArgumentException("Invalid (empty) username");
		}
		authKeys = new HashSet<>();
		log.debug("Init ssh client to {}", this);

		final var defaultKhFile = System.getProperty("user.home")
		                          + File.separator + ".ssh" + File.separator + "known_hosts";
		final var knownHostFile = new File(System.getProperty("ssh.knownhosts", defaultKhFile));
		try {
			client.addHostKeyVerifier(new DefaultKnownHostsVerifier(knownHostFile));
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public boolean isReusable() {
		return false;
	}

	@Override
	public synchronized boolean isAvaliable() {
		return client.isConnected() && client.isAuthenticated();
	}

	@Override
	public int getIOBufferSize() {
		if (sftpClient != null) {
			final var system = sftpClient.getSFTPEngine().getSubsystem();
			return Math.min(system.getLocalMaxPacketSize(), system.getRemoteMaxPacketSize());
		} else {
			return -1;
		}
	}

	@Override
	public String toString() {
		return "sftp://" + username + "@" + host + ":" + port;
	}

	public void setPasswordAuth(final char[] password) {
		this.password = Objects.requireNonNull(password, "password");
		if (password.length == 0) {
			throw new IllegalArgumentException("Invalid (empty) password");
		}
		final var strStars = logPassword(password);
		log.debug("Set password auth for {}: {}", this, strStars);
	}

	private static String logPassword(final char[] password) {
		final var stars = new char[password.length];
		Arrays.fill(stars, '*');
		return String.valueOf(stars);
	}

	private KeyProvider loadPrivateKey(final File privateKey, final char[] keyPassword) {
		try {
			if (keyPassword != null && keyPassword.length > 0) {
				final var strStars = logPassword(keyPassword);
				log.debug("Add private key auth for {}: {}, with password {}", this, privateKey, strStars);
				return client.loadKeys(privateKey.getPath(), clonePassword(keyPassword));
			} else {
				log.debug("Add private key auth for {}: {}", this, privateKey);
				return client.loadKeys(privateKey.getPath());
			}
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	/**
	 * @param privateKey can be a file, or a directory of key files (like "~/.ssh/")
	 */
	public void manuallyAddPrivatekeyAuth(final File privateKey, final char[] keyPassword) {
		Objects.requireNonNull(privateKey);
		if (privateKey.exists() == false) {
			throw new IORuntimeException(new FileNotFoundException(privateKey.getPath()));
		} else if (privateKey.isDirectory()) {
			authKeys.addAll(Stream.of("id_rsa", "id_dsa", "id_ed25519", "id_ecdsa")
			        .map(f -> new File(privateKey, f))
			        .filter(File::exists)
			        .map(fPrivateKey -> loadPrivateKey(fPrivateKey, keyPassword))
			        .collect(toUnmodifiableList()));
		} else {
			authKeys.add(loadPrivateKey(privateKey, keyPassword));
		}
	}

	/**
	 * @param privateKey can be a file, or a directory of key files (like "~/.ssh/")
	 */
	public void manuallyAddPrivatekeyAuth(final File privateKey) {
		manuallyAddPrivatekeyAuth(privateKey, null);
	}

	public SSHClient getClient() {
		return client;
	}

	public SFTPClient getSFTPClient() {
		return sftpClient;
	}

	private static final char[] clonePassword(final char[] password) {
		final var disposablePassword = new char[password.length];
		System.arraycopy(password, 0, disposablePassword, 0, password.length);
		return disposablePassword;
	}

	@Override
	public synchronized void connect() {
		if (isAvaliable()) {
			return;
		} else if (wasConnected == true) {
			throw new IORuntimeException("Client is not avaliable to use");
		}
		log.debug("Start to connect to {}", this);
		wasConnected = true;
		try {
			client.connect(host, port);
			if (password != null && password.length > 0) {
				log.trace("Connect to {} with password", this);
				client.authPassword(username, clonePassword(password));
			} else {
				log.trace("Connect to {} with Publickey", this);
				client.authPublickey(username, authKeys);
			}
			log.info("Connected to {}", this);
			createANewSFTPClient();
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	public boolean isStatefulSFTPClient() {
		return statefulSFTPClient;
	}

	public void setStatefulSFTPClient(final boolean statefulSFTPClient) {
		this.statefulSFTPClient = statefulSFTPClient;
	}

	/**
	 * Needed for simultaneous transferts:
	 * a = getFromPath() + createANewSFTPClient() + b = getFromPath()
	 * &gt; a and b can do actions in same time.
	 */
	public synchronized void createANewSFTPClient() {
		try {
			if (statefulSFTPClient) {
				log.debug("Create a new stateful SFTP client for {}", this);
				sftpClient = client.newStatefulSFTPClient();
			} else {
				log.debug("Create a new SFTP client for {}", this);
				sftpClient = client.newSFTPClient();
			}
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public synchronized void close() {
		try {
			if (client.isConnected()) {
				log.info("Manually disconnect client for {}", this);
				client.disconnect();
			}
			sftpClient = null;
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public synchronized SFTPFile getFromPath(final String path) {
		if (isAvaliable() == false) {
			if (wasConnected == false) {
				throw new IORuntimeException("Non-active SSH client, try to connect before");
			} else {
				throw new IORuntimeException("SSH client was disconnected. Please retry with another instance.");
			}
		}
		log.trace("Create new SFTPFile to {}/{}", this, path);
		return new SFTPFile(this, sftpClient, path);
	}

	public InetAddress getHost() {
		return host;
	}

	public String getUsername() {
		return username;
	}

	@Override
	public int hashCode() {
		final var prime = 31;
		var result = 1;
		result = prime * result + Arrays.hashCode(password);
		result = prime * result + Objects.hash(host, port, username);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final var other = (SFTPFileSystem) obj;
		return Objects.equals(host, other.host) && Arrays.equals(password, other.password) && port == other.port
		       && Objects.equals(username, other.username);
	}

}
