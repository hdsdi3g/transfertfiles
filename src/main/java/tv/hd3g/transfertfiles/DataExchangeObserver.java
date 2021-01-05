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
package tv.hd3g.transfertfiles;

import java.time.Duration;

/**
 * Expected to be thread safe.
 */
public interface DataExchangeObserver {

	/**
	 * Called after each copy loop ! Please do a quick answer !
	 * @return true for keep transfert, false to cancel it
	 */
	default boolean onTransfertProgressFromSource(final AbstractFile source,
	                                              final long startDate,
	                                              final long dataTransferred) {
		return true;
	}

	/**
	 * Called after each copy loop ! Please do a quick answer !
	 * @return true for keep transfert, false to cancel it
	 */
	default boolean onTransfertProgressToDestination(final AbstractFile destination,
	                                                 final long startDate,
	                                                 final long dataTransferred) {
		return true;
	}

	default void beforeTransfert(final AbstractFile source,
	                             final AbstractFile destination) {
	}

	default void afterTransfert(final AbstractFile source,
	                            final AbstractFile destination,
	                            final long dataSizeTranferedFromSource,
	                            final long dataSizeTranferedToDestination,
	                            final Duration transfertDuration) {
	}
}
