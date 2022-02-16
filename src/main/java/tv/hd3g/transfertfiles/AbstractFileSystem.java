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
import static java.util.stream.Collectors.joining;

import java.io.Closeable;
import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Expected to be thread safe.
 * Don't forget to implements hashCode, equals and toString.
 */
public interface AbstractFileSystem<T extends AbstractFile> extends Closeable {

	void connect();

	/**
	 * @param path relative path only
	 */
	T getFromPath(String path);

	/**
	 * @param path relative path only
	 */
	default T getFromPath(final String path0, final String... pathN) {
		if (pathN != null && pathN.length > 0) {
			return getFromPath(path0 + "/" + Stream.of(pathN)
			        .filter(not(Objects::isNull))
			        .collect(joining("/")));
		} else {
			return getFromPath(path0);
		}
	}

	/**
	 * 65535 bytes
	 */
	default int getIOBufferSize() {
		return 0xFFFF;
	}

	/**
	 * Set socket/connection timeout.
	 */
	default void setTimeout(final long duration, final TimeUnit unit) {
	}

	/**
	 * If disconnected, can we re-connect after ?
	 */
	boolean isReusable();

	boolean isAvaliable();

	/**
	 * @return the same code for the same internal engine instance in FileSystem.
	 *         Can be used for interract two FS and protect the both are not the same.
	 */
	int reusableHashCode();

	InetAddress getHost();

	String getUsername();

}
