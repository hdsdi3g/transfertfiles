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

import java.net.InetAddress;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;

/**
 * FTP TLS/SSL explicit client
 */
public class FTPESFileSystem extends FTPFileSystem {

	public FTPESFileSystem(final InetAddress host,
	                      final int port,
	                      final String username,
	                      final char[] password,
	                      final boolean passiveMode) {
		super(host, port, username, password, passiveMode);
	}

	@Override
	protected FTPClient createFTPClient() {
		return new FTPSClient(false);
	}

	@Override
	public String toString() {
		return "ftpes://" + username + "@" + host + ":" + port;
	}

}
