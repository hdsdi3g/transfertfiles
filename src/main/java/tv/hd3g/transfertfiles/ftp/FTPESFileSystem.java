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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;

/**
 * FTP TLS/SSL explicit client
 */
public class FTPESFileSystem extends FTPFileSystem {// NOSONAR S2160

	private final boolean ignoreInvalidCertificates;
	private final FTPSClient client;

	public FTPESFileSystem(final InetAddress host,
	                       final int port,
	                       final String username,
	                       final char[] password,
	                       final boolean passiveMode,
	                       final boolean ignoreInvalidCertificates,
	                       final String basePath) {
		super(host, port, username, password, passiveMode, basePath);
		this.ignoreInvalidCertificates = ignoreInvalidCertificates;
		if (ignoreInvalidCertificates) {
			client = new FTPSClient(false, sslContextNeverCheck);
		} else {
			client = new FTPSClient(false);
		}
	}

	static final SSLContext sslContextNeverCheck;

	static {
		try {
			sslContextNeverCheck = SSLContext.getInstance("TLS");
			sslContextNeverCheck.init(null, new TrustManager[] { new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					/**
					 * Accept all
					 */
					return new X509Certificate[0];
				}

				@Override
				public void checkClientTrusted(final X509Certificate[] certs, // NOSONAR S4830
				                               final String authType) {
					/**
					 * Accept all
					 */
				}

				@Override
				public void checkServerTrusted(final X509Certificate[] certs, // NOSONAR S4830
				                               final String authType) {
					/**
					 * Accept all
					 */
				}
			} }, new SecureRandom());
		} catch (final KeyManagementException | NoSuchAlgorithmException e) {
			throw new IllegalStateException("Invalid key management", e);
		}
	}

	@Override
	public FTPClient getClient() {
		return client;
	}

	@Override
	public String toString() {
		return "ftpes://" + username + "@" + host + ":" + port + getBasePath();
	}

	public boolean isIgnoreInvalidCertificates() {
		return ignoreInvalidCertificates;
	}

}
