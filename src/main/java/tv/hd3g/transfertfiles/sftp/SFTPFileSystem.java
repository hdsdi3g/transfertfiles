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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import tv.hd3g.commons.IORuntimeException;
import tv.hd3g.transfertfiles.CommonAbstractFileSystem;

public class SFTPFileSystem extends CommonAbstractFileSystem<SFTPFile> {
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

	public SFTPFileSystem(final InetAddress host, final int port, final String username, final String basePath) {
		super(basePath);
		client = new SSHClient();
		wasConnected = false;
		this.host = Objects.requireNonNull(host);
		this.port = port;
		this.username = Objects.requireNonNull(username, "SSH username");
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
		return "sftp://" + username + "@" + host.getHostName() + ":" + port + getBasePath();
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
	public void setTimeout(final long duration, final TimeUnit unit) {
		if (duration > 0) {
			timeoutDuration = unit.toMillis(duration);
		}
		if (timeoutDuration > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Can't set a timeoutDuration > Integer.MAX_VALUE: "
			                                   + timeoutDuration);
		}
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

		if (timeoutDuration > 0) {
			client.setConnectTimeout((int) timeoutDuration);
			client.setTimeout((int) timeoutDuration);
		}
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
		final var rpath = getPathFromRelative(path);
		log.trace("Create new SFTPFile to {}/{}", this, rpath);
		return new SFTPFile(this, sftpClient, rpath);
	}

	public InetAddress getHost() {
		return host;
	}

	public String getUsername() {
		return username;
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + Objects.hash(host, port, username);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (super.equals(obj) == false || getClass() != obj.getClass()) {
			return false;
		}
		final var other = (SFTPFileSystem) obj;
		return Objects.equals(host, other.host)
		       && port == other.port
		       && Objects.equals(username, other.username);
	}

	@Override
	public int reusableHashCode() {
		if (sftpClient == null) {
			throw new IllegalStateException("Please connect before get reusableHashCode");
		}
		return sftpClient.hashCode();
	}
}
