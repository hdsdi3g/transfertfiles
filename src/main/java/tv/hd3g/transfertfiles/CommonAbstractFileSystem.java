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

import static tv.hd3g.transfertfiles.AbstractFile.normalizePath;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public abstract class CommonAbstractFileSystem<T extends AbstractFile> implements AbstractFileSystem<T> {

	private final String basePath;
	protected long timeoutDuration;

	protected CommonAbstractFileSystem(final String basePath) {
		this.basePath = normalizePath(Objects.requireNonNull(basePath, "basePath"));
		timeoutDuration = 0;
	}

	protected String getPathFromRelative(final String path) {
		return normalizePath(basePath + normalizePath(path));
	}

	public String getBasePath() {
		return basePath;
	}

	@Override
	public int hashCode() {
		return Objects.hash(basePath);
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
		final var other = (CommonAbstractFileSystem<?>) obj;
		return Objects.equals(basePath, other.basePath);
	}

	@Override
	public void setTimeout(final long duration, final TimeUnit unit) {
		if (duration > 0) {
			timeoutDuration = unit.toMillis(duration);
		} else {
			throw new IllegalArgumentException("Can't set a timeoutDuration to " + timeoutDuration);
		}
		if (timeoutDuration > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Can't set a timeoutDuration > Integer.MAX_VALUE: "
			                                   + timeoutDuration);
		}
	}

	public long getTimeout() {
		return timeoutDuration;
	}
}
