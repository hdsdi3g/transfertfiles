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
package tv.hd3g.transfertfiles;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toUnmodifiableList;
import static tv.hd3g.transfertfiles.AbstractFile.normalizePath;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.UnknownHostException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.commons.IORuntimeException;
import tv.hd3g.transfertfiles.ftp.FTPESFileSystem;
import tv.hd3g.transfertfiles.ftp.FTPFileSystem;
import tv.hd3g.transfertfiles.ftp.FTPSFileSystem;
import tv.hd3g.transfertfiles.local.LocalFileSystem;
import tv.hd3g.transfertfiles.sftp.SFTPFileSystem;

public class AbstractFileSystemURL implements Closeable {
	private static final Logger log = LogManager.getLogger();

	private static class CustomURLStreamHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(final URL url) throws IOException {
			throw new IllegalAccessError("Can't open Connection with " + url.getProtocol());
		}
	}

	static {
		final var customURLStreamHandler = new CustomURLStreamHandler();
		URL.setURLStreamHandlerFactory(protocol -> {
			if (protocol.equalsIgnoreCase("sftp")
			    || protocol.equalsIgnoreCase("ftp")
			    || protocol.equalsIgnoreCase("ftps")
			    || protocol.equalsIgnoreCase("ftpes")) {
				return customURLStreamHandler;
			}
			return null;
		});
	}

	private final String protectedRessourceURL;
	private final AbstractFileSystem<?> fileSystem;
	private final String basePath;

	protected AbstractFileSystemURL(final String protectedRessourceURL,
	                                final AbstractFileSystem<?> fileSystem,
	                                final String basePath) {
		this.protectedRessourceURL = protectedRessourceURL;
		this.fileSystem = fileSystem;
		this.basePath = basePath;
	}

	/**
	 * @param ressourceURL like "protocol://user:password@host/basePath?password=secret&active&key=/path/to/privateOpenSSHkey"
	 *        query must not be encoded
	 *        use ":password" or "password=" as you want.
	 *        Never set an "@" in the user:password place. Quotation mark here are ignored (simply used as quotation marks).
	 *        Never add directly an "&" in the password in query, but you can use " (quotation mark) like password="s&e?c=r+t"
	 *        key password can be set by... set password
	 *        FTP(S|ES) is passive by default.
	 *        -
	 *        Add "ignoreInvalidCertificates" to bypass TLS verification with FTPS/FTPES clients.
	 */
	public AbstractFileSystemURL(final String ressourceURL) {
		try {
			final var url = new URL(ressourceURL);

			final var protocol = url.getProtocol();
			basePath = normalizePath(Optional.ofNullable(url.getPath()).orElse("/"));
			final var host = InetAddress.getByName(Optional.ofNullable(url.getHost()).orElse("localhost"));
			final var query = splitURLQuery(url);
			final var userPasswordEntry = parseUserInfo(url.getUserInfo(), getFirstKeyValue(query, "password", ""));
			final var username = userPasswordEntry.getKey();
			final var passwordStr = userPasswordEntry.getValue();
			protectedRessourceURL = url.toString().replace(passwordStr, logPassword(passwordStr.length()));
			final var password = passwordStr.toCharArray();

			final var passive = query.containsKey("active") == false;
			final var ignoreInvalidCertificates = query.containsKey("ignoreInvalidCertificates");
			final var keys = query.getOrDefault("key", List.of()).stream()
			        .map(File::new)
			        .filter(File::exists)
			        .collect(toUnmodifiableList());

			if (protocol.contentEquals("file")) {
				log.debug("Init URL LocalFileSystem: {}", this::toString);
				fileSystem = new LocalFileSystem(new File(basePath));
			} else if (protocol.contentEquals("sftp")) {
				log.debug("Init URL SFTPFileSystem: {}", this::toString);
				fileSystem = new SFTPFileSystem(host, getPort(url, 22), username, basePath);
				final var sFTPfileSystem = (SFTPFileSystem) fileSystem;

				if (keys.isEmpty()) {
					if (password.length > 0) {
						sFTPfileSystem.setPasswordAuth(password);
					}
				} else {
					keys.forEach(privateKey -> sFTPfileSystem.manuallyAddPrivatekeyAuth(privateKey, password));
				}
			} else if (protocol.contentEquals("ftp")) {
				log.debug("Init URL FTPFileSystem: {}", this::toString);
				fileSystem = new FTPFileSystem(host, getPort(url, 21), username, password, passive, basePath);
			} else if (protocol.contentEquals("ftps")) {
				log.debug("Init URL FTPSFileSystem: {}", this::toString);
				fileSystem = new FTPSFileSystem(host, getPort(url, 990), username, password, passive,
				        ignoreInvalidCertificates, basePath);
			} else if (protocol.contentEquals("ftpes")) {
				log.debug("Init URL FTPESFileSystem: {}", this::toString);
				fileSystem = new FTPESFileSystem(host, getPort(url, 21), username, password, passive,
				        ignoreInvalidCertificates, basePath);
			} else {
				throw new IORuntimeException("Can't manage protocol \"" + protocol + "\" in URL: " + toString());
			}
		} catch (final MalformedURLException e) {
			throw new IORuntimeException("Invalid URL parsing: \"" + ressourceURL + "\"", e);
		} catch (final UnknownHostException e) {
			throw new IORuntimeException("Invalid URL host: \"" + toString() + "\"", e);
		}
	}

	private static String getFirstKeyValue(final Map<String, List<String>> query,
	                                       final String key,
	                                       final String defaultValue) {
		return query.entrySet().stream()
		        .filter(es -> key.equalsIgnoreCase(es.getKey()))
		        .flatMap(es -> Optional.ofNullable(es.getValue()).stream())
		        .findFirst()
		        .flatMap(l -> l.stream().findFirst())
		        .orElse(defaultValue);
	}

	public static Stream<String> protectedSplit(final String text) {
		final var list = new ArrayList<String>();
		var isInEscape = false;
		var data = new StringBuilder();
		for (var pos = 0; pos < text.length(); pos++) {
			final var chr = text.charAt(pos);
			if (chr == '"') {
				isInEscape = isInEscape == false;
			} else if (isInEscape || chr == '&' == false) {
				data.append(chr);
			} else {
				list.add(data.toString());
				data = new StringBuilder();
			}
		}
		if (data.length() > 0) {
			list.add(data.toString());
		}
		return list.stream().filter(not(String::isEmpty));
	}

	static Map<String, List<String>> splitURLQuery(final URL url) {
		final var qList = Optional.ofNullable(url.getQuery()).orElse("")
		                  + Optional.ofNullable(url.getRef()).map(r -> "#" + r).orElse("");
		return protectedSplit(qList)
		        .map(it -> {
			        final var idx = it.indexOf('=');
			        final var key = idx > 0 ? it.substring(0, idx) : it;
			        final var value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
			        return new SimpleImmutableEntry<>(key, value);
		        })
		        .collect(Collectors.groupingBy(SimpleImmutableEntry::getKey,
		                LinkedHashMap::new, mapping(Map.Entry::getValue, Collectors.toList())));
	}

	private static int getPort(final URL url, final int defaultPort) {
		final var port = url.getPort();
		if (port == -1 || port == 0) {
			return defaultPort;
		}
		return port;
	}

	static SimpleImmutableEntry<String, String> parseUserInfo(final String userInfo,
	                                                          final String defaultPassword) {
		if (userInfo == null || userInfo.equals("")) {
			return new SimpleImmutableEntry<>(null, defaultPassword);
		}
		if (userInfo.contains(":") == false) {
			return new SimpleImmutableEntry<>(userInfo, defaultPassword);
		}
		final var pos = userInfo.indexOf(':');
		if (pos == 0) {
			return new SimpleImmutableEntry<>(null, userInfo);
		} else if (pos == userInfo.length() - 1) {
			return new SimpleImmutableEntry<>(userInfo, defaultPassword);
		}
		return new SimpleImmutableEntry<>(userInfo.substring(0, pos), userInfo.substring(pos + 1));
	}

	/**
	 * Passwords will be replaced by "*"
	 */
	@Override
	public String toString() {
		return protectedRessourceURL;
	}

	private static String logPassword(final int size) {
		final var stars = new char[size];
		Arrays.fill(stars, '*');
		return String.valueOf(stars);
	}

	public AbstractFileSystem<?> getFileSystem() {// NOSONAR S1452
		return fileSystem;
	}

	/**
	 * @return after fileSystem.connect
	 */
	public AbstractFile getFromPath(final String path) {
		fileSystem.connect();
		return fileSystem.getFromPath(normalizePath(path));
	}

	/**
	 * @return after fileSystem.connect
	 */
	public AbstractFile getRootPath() {
		return getFromPath("");
	}

	@Override
	public void close() throws IOException {
		fileSystem.close();
	}

	/**
	 * If disconnected, can we re-connect after ?
	 */
	public boolean isReusable() {
		return fileSystem.isReusable();
	}

	public String getBasePath() {
		return basePath;
	}
}
