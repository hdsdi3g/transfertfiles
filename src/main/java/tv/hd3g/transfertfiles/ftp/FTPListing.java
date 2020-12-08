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
package tv.hd3g.transfertfiles.ftp;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public enum FTPListing {

	NLST {
		@Override
		Stream<String> listDirectory(final FTPClient ftpClient, final String path) throws IOException {
			final var rawList = Optional.ofNullable(ftpClient.listNames(path)).orElse(new String[] {});
			return Stream.of(rawList).filter(f -> f.endsWith(".") == false);
		}

		@Override
		Stream<FTPFile> rawListDirectory(final FTPClient ftpClient, final String path) throws IOException {
			throw new IllegalArgumentException("Not supported with NLST");
		}
	},
	LIST {
		@Override
		Stream<String> listDirectory(final FTPClient ftpClient, final String path) throws IOException {
			return rawListDirectory(ftpClient, path).map(org.apache.commons.net.ftp.FTPFile::getName);
		}

		@Override
		Stream<FTPFile> rawListDirectory(final FTPClient ftpClient, final String path) throws IOException {
			return rawFTPFileListToStream(ftpClient.listFiles(path));
		}
	},
	MLSD {
		@Override
		Stream<String> listDirectory(final FTPClient ftpClient, final String path) throws IOException {
			return rawListDirectory(ftpClient, path).map(org.apache.commons.net.ftp.FTPFile::getName);
		}

		@Override
		Stream<FTPFile> rawListDirectory(final FTPClient ftpClient, final String path) throws IOException {
			return rawFTPFileListToStream(ftpClient.mlistDir(path));
		}
	};

	private static Stream<org.apache.commons.net.ftp.FTPFile> rawFTPFileListToStream(final org.apache.commons.net.ftp.FTPFile[] rawList) {
		final var protectedList = Optional.ofNullable(rawList)
		        .orElse(new org.apache.commons.net.ftp.FTPFile[] {});
		return Stream.of(protectedList).filter(f -> f.getName().endsWith(".") == false);
	}

	abstract Stream<String> listDirectory(final FTPClient ftpClient, final String path) throws IOException;

	abstract Stream<org.apache.commons.net.ftp.FTPFile> rawListDirectory(final FTPClient ftpClient,
	                                                                     final String path) throws IOException;

}
