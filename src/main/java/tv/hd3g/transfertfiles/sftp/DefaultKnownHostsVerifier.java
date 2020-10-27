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
package tv.hd3g.transfertfiles.sftp;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import tv.hd3g.commons.IORuntimeException;

class DefaultKnownHostsVerifier extends OpenSSHKnownHosts {
	private static final Logger currentlog = LogManager.getLogger();

	public DefaultKnownHostsVerifier(final File khFile) throws IOException {
		super(khFile);
		currentlog.debug("Set SSH known hosts file : \"{}\"", getFile().getAbsolutePath());
	}

	@Override
	protected boolean hostKeyUnverifiableAction(final String hostname, final PublicKey key) {
		final var type = KeyType.fromKey(key);
		currentlog.info("The authenticity of host '{}' can't be established. {} key fingerprint is {}.",
		        hostname, type, SecurityUtils.getFingerprint(key));
		try {
			entries().add(new HostEntry(null, hostname, KeyType.fromKey(key), key));
			write();
			currentlog.info("SSH: Permanently added '{}' ({}) to the list of known hosts.", hostname, type);
		} catch (final IOException e) {
			throw new IORuntimeException(e);
		}
		return true;
	}

	@Override
	protected boolean hostKeyChangedAction(final String hostname, final PublicKey key) {
		final var type = KeyType.fromKey(key);
		final var fp = SecurityUtils.getFingerprint(key);
		final var path = getFile().getAbsolutePath();
		currentlog.error(
		        "REMOTE HOST IDENTIFICATION HAS CHANGED! IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY! Someone could be eavesdropping on you right now (man-in-the-middle attack)! It is also possible that the host key has just been changed. The fingerprint for the {} key sent by the remote host is {}. Please contact your system administrator or add correct host key in {} to get rid of this message.",
		        type, fp, path);
		return false;
	}
}
