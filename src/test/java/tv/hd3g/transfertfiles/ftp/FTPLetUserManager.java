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

import java.io.File;
import java.util.List;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;

class FTPLetUserManager implements UserManager, User {

	private final String username;
	private final String password;
	private final File homeDir;

	public FTPLetUserManager(final String username, final String password, final File homeDir) {
		this.username = username;
		this.password = password;
		this.homeDir = homeDir;
	}

	@Override
	public User authenticate(final Authentication authentication) throws AuthenticationFailedException {
		if (authentication instanceof UsernamePasswordAuthentication) {
			final var userPasswordAuth = (UsernamePasswordAuthentication) authentication;
			if (username.equals(userPasswordAuth.getUsername()) == false) {
				throw new AuthenticationFailedException("Unknown user");
			} else if (password.equals(userPasswordAuth.getPassword()) == false) {
				throw new AuthenticationFailedException("Invalid password");
			}
			return this;
		}
		throw new UnsupportedOperationException("authentication is not an UsernamePasswordAuthentication");
	}

	@Override
	public User getUserByName(final String username) throws FtpException {
		if (this.username.equals(username)) {
			return this;
		}
		return null;
	}

	@Override
	public String[] getAllUserNames() throws FtpException {
		return new String[] { username };
	}

	@Override
	public void delete(final String username) throws FtpException {
		throw new UnsupportedOperationException("delete");
	}

	@Override
	public void save(final User user) throws FtpException {
		throw new UnsupportedOperationException("save");
	}

	@Override
	public boolean doesExist(final String username) throws FtpException {
		return this.username.equals(username);
	}

	@Override
	public String getAdminName() throws FtpException {
		throw new UnsupportedOperationException("getAdminName");
	}

	@Override
	public boolean isAdmin(final String username) throws FtpException {
		throw new UnsupportedOperationException("isAdmin");
	}

	@Override
	public String getName() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public List<? extends Authority> getAuthorities() {
		return null;
	}

	@Override
	public List<? extends Authority> getAuthorities(final Class<? extends Authority> clazz) {
		return null;
	}

	@Override
	public AuthorizationRequest authorize(final AuthorizationRequest request) {
		return request;
	}

	@Override
	public int getMaxIdleTime() {
		return 60;
	}

	@Override
	public boolean getEnabled() {
		return true;
	}

	@Override
	public String getHomeDirectory() {
		return homeDir.getAbsolutePath();
	}
}
