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

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public enum DigestFilterHashExtraction {

	CRC32(new CRC32InstanceProvider()),
	CRC32C(new CRC32CInstanceProvider()),
	ADLER32(new ADLER32InstanceProvider()),
	MD5(new MessageDigestInstanceProvider("MD5")),
	SHA_1(new MessageDigestInstanceProvider("SHA-1")),
	/**
	 * SHA-2
	 */
	SHA_224(new MessageDigestInstanceProvider("SHA-224")),
	/**
	 * SHA-2
	 */
	SHA_256(new MessageDigestInstanceProvider("SHA-256")),
	/**
	 * SHA-2
	 */
	SHA_384(new MessageDigestInstanceProvider("SHA-384")),
	/**
	 * SHA-2
	 */
	SHA_512(new MessageDigestInstanceProvider("SHA-512")),
	SHA3_224(new MessageDigestInstanceProvider("SHA3-224")),
	SHA3_256(new MessageDigestInstanceProvider("SHA3-256")),
	SHA3_384(new MessageDigestInstanceProvider("SHA3-384")),
	SHA3_512(new MessageDigestInstanceProvider("SHA3-512")),
	BLAKE2S_224(new MessageDigestInstanceProvider("BLAKE2s-224")),
	BLAKE2S_256(new MessageDigestInstanceProvider("BLAKE2s-256")),
	BLAKE2B_384(new MessageDigestInstanceProvider("BLAKE2b-384")),
	BLAKE2B_512(new MessageDigestInstanceProvider("BLAKE2b-512"));

	/**
	 * == NOT AVALIABLE ==
	 * MD6(new MessageDigestInstanceProvider("MD6")),
	 * BLAKE_224(new MessageDigestInstanceProvider("BLAKE-224")),
	 * BLAKE_256(new MessageDigestInstanceProvider("BLAKE-256")),
	 * BLAKE_384(new MessageDigestInstanceProvider("BLAKE-384")),
	 * BLAKE_512(new MessageDigestInstanceProvider("BLAKE-512")),
	 * BLAKE3(new MessageDigestInstanceProvider("BLAKE3"))
	 */

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private final ExtractionInstanceProvider digestProvider;

	DigestFilterHashExtraction(final ExtractionInstanceProvider digestProvider) {
		this.digestProvider = digestProvider;
	}

	public String getDigestName() {
		return digestProvider.getDigestName();
	}

	public boolean isAvaliable() {
		return digestProvider.isAvaliable();
	}

	public ExtractionInstance createInstance() {
		return digestProvider.createInstance();
	}

	@Override
	public String toString() {
		return getDigestName();
	}

	private static interface ExtractionInstanceProvider {
		ExtractionInstance createInstance();

		String getDigestName();

		boolean isAvaliable();
	}

	static interface ExtractionInstance {
		void update(ByteBuffer datas);

		byte[] digest();
	}

	private static class MessageDigestInstanceProvider implements ExtractionInstanceProvider {

		private final String digestName;

		MessageDigestInstanceProvider(final String digestName) {
			this.digestName = digestName;
		}

		@Override
		public String getDigestName() {
			return digestName;
		}

		@Override
		public boolean isAvaliable() {
			try {
				MessageDigest.getInstance(digestName);
				return true;
			} catch (final NoSuchAlgorithmException e) {
				return false;
			}
		}

		@Override
		public ExtractionInstance createInstance() {
			MessageDigest mdInstance;
			try {
				mdInstance = MessageDigest.getInstance(digestName);
			} catch (final NoSuchAlgorithmException e) {
				throw new IllegalStateException(digestName, e);
			}

			return new ExtractionInstance() {
				@Override
				public void update(final ByteBuffer datas) {
					mdInstance.update(datas);
				}

				@Override
				public byte[] digest() {
					return mdInstance.digest();
				}
			};
		}
	}

	private abstract static class ChecksumInstanceProvider implements ExtractionInstanceProvider {

		@Override
		public boolean isAvaliable() {
			return true;
		}

		abstract Checksum createChecksumInstance();

		@Override
		public ExtractionInstance createInstance() {
			final var cksum = createChecksumInstance();

			return new ExtractionInstance() {

				@Override
				public void update(final ByteBuffer datas) {
					cksum.update(datas);
				}

				@Override
				public byte[] digest() {
					final var buffer = ByteBuffer.allocate(Integer.BYTES);
					buffer.putInt((int) cksum.getValue());
					return buffer.array();
				}
			};
		}
	}

	private static class CRC32InstanceProvider extends ChecksumInstanceProvider {

		@Override
		public String getDigestName() {
			return "CRC-32";
		}

		@Override
		Checksum createChecksumInstance() {
			return new CRC32();
		}
	}

	private static class CRC32CInstanceProvider extends ChecksumInstanceProvider {

		@Override
		public String getDigestName() {
			return "CRC-32C";
		}

		@Override
		Checksum createChecksumInstance() {
			return new CRC32C();
		}
	}

	private static class ADLER32InstanceProvider extends ChecksumInstanceProvider {

		@Override
		public String getDigestName() {
			return "ADLER-32";
		}

		@Override
		Checksum createChecksumInstance() {
			return new Adler32();
		}
	}
}
