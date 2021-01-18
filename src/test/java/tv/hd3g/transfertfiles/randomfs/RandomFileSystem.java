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
package tv.hd3g.transfertfiles.randomfs;

import java.io.IOException;
import java.util.Random;

import tv.hd3g.transfertfiles.CommonAbstractFileSystem;

public class RandomFileSystem extends CommonAbstractFileSystem<RandomFile> {

	protected RandomFileSystem() {
		super("/");
	}

	@Override
	public void connect() {
	}

	@Override
	public RandomFile getFromPath(final String path) {
		return null;
	}

	@Override
	public boolean isReusable() {
		return true;
	}

	@Override
	public boolean isAvaliable() {
		return true;
	}

	@Override
	public int reusableHashCode() {
		return new Random().nextInt();
	}

	@Override
	public void close() throws IOException {
	}
}
