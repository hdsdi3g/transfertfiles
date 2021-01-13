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
 * Copyright (C) hdsdi3g for hd3g.tv 2021
 *
 */
package tv.hd3g.transfertfiles.filters;

import java.io.IOException;

import tv.hd3g.transfertfiles.BufferVault;

@FunctionalInterface
public interface DataExchangeFilter {
	int DEFAULT_BUFFER_SIZE = 32768;

	/**
	 * @param dataSource can be empty after call
	 * @param last if true, you should use flush/close internal needs and return last responses,
	 *        because applyDataFilter will never be called for the current instance.
	 * @return empty for "ignore" filter/data collector filter
	 *         null for stop data transfert operation
	 *         returned BufferVault can be re-used here after, and can be empty after call
	 */
	BufferVault applyDataFilter(final boolean last,
	                            final BufferVault dataSources) throws IOException;

	default void onCancelTransfert() {
	}

	/**
	 * Ensure to all applyDataFilter call will contain a total dataSources size equals/more than with this value.
	 * Else applyDataFilter with last = true will be called
	 * @return byte count
	 */
	default int ensureMinDataSourcesDataLength() {
		return DEFAULT_BUFFER_SIZE;
	}

	default String getFilterName() {
		return getClass().getSimpleName();
	}

}
