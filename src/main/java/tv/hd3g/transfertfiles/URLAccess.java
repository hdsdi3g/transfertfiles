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
import static tv.hd3g.transfertfiles.AbstractFile.normalizePath;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class URLAccess {

	private final Map<String, List<String>> optionZone;
	private final String protocol;
	private final String path;
	private final String host;
	private final String username;
	private final String protectedRessourceURL;
	private final char[] password;
	private final int port;

	public static final Map<String, Integer> defaultPorts = Map.of(
	        "file", -1,
	        "ftp", 21,
	        "ftps", 989,
	        "ftpes", 21,
	        "sftp", 22);

	/**
	 * @param url like "uuu://user:password@host:port/path/dir/subdir?option0=value0&option1&password="o&^o:o?o|o+0\\o=O,O#o0@;;o&!0-o_o~o\"&option3=ff" ...
	 *        Don't add "?" in the first password aera (and neither on protocol/user/host/path zone).
	 */
	public URLAccess(final String url) {
		final var qmarkPos = url.indexOf('?');
		final URI internalURI;
		try {
			if (qmarkPos == -1) {
				internalURI = new URI(protectPath(url));
				optionZone = Map.of();
			} else if (qmarkPos == url.length() - 1) {
				internalURI = new URI(protectPath(url.substring(0, qmarkPos)));
				optionZone = Map.of();
			} else {
				optionZone = splitURLQuery(url.substring(qmarkPos + 1));
				internalURI = new URI(protectPath(url.substring(0, qmarkPos)));
			}
		} catch (final URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL: \"" + url + "\"", e);
		}

		protocol = Objects.requireNonNull(internalURI.getScheme(), "Missing protocol");
		path = normalizePath(Optional.ofNullable(internalURI.getPath()).orElse("/"));
		host = Optional.ofNullable(internalURI.getHost()).orElse("localhost");
		final var userPasswordEntry = parseUserInfo(internalURI.getUserInfo(),
		        getFirstKeyValue(optionZone, "password", ""));
		if (internalURI.getPort() < 1) {
			port = defaultPorts.getOrDefault(protocol, -1);
		} else {
			port = internalURI.getPort();
		}
		username = userPasswordEntry.getKey();
		final var passwordStr = userPasswordEntry.getValue();
		protectedRessourceURL = url.replace(passwordStr, logPassword(passwordStr.length()));
		password = passwordStr.toCharArray();
	}

	private static String protectPath(final String urlWithoutOptions) {
		return urlWithoutOptions.replace(" ", "%20").replace('\\', '/');
	}

	private static Stream<String> protectedSplit(final String text) {
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
		if (isInEscape) {
			throw new IllegalArgumentException("Missing \" on \"" + text + "\"");
		} else if (data.length() > 0) {
			list.add(data.toString());
		}
		return list.stream().filter(not(String::isEmpty));
	}

	private static Map<String, List<String>> splitURLQuery(final String qList) {
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

	private static SimpleImmutableEntry<String, String> parseUserInfo(final String userInfo,
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

	private static String logPassword(final int size) {
		final var stars = new char[size];
		Arrays.fill(stars, '*');
		return String.valueOf(stars);
	}

	public String getProtocol() {
		return protocol;
	}

	public String getPath() {
		return path;
	}

	public String getHost() {
		return host;
	}

	public String getUsername() {
		return username;
	}

	public String getProtectedRessourceURL() {
		return protectedRessourceURL;
	}

	public char[] getPassword() {
		return password;
	}

	public int getPort() {
		return port;
	}

	public Map<String, List<String>> getOptionZone() {
		return optionZone;
	}
}
