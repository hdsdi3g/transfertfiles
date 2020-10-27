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

import java.io.Closeable;

/**
 * Expected to be thread safe.
 * Don't forget to implements hashCode, equals and toString.
 */
public interface AbstractFileSystem<T extends AbstractFile> extends Closeable {

	void connect();

	T getFromPath(String path);

	/**
	 * 65535 bytes
	 */
	default int getIOBufferSize() {
		return 0xFFFF;
	}

	/**
	 * If disconnected, can we re-connect after ?
	 */
	boolean isReusable();

	boolean isAvaliable();
}
