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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class URLAccessTest {

	@Test
	void testGetProtocol() {
		assertEquals("uuu", new URLAccess("uuu://user:password@host/path?aa=bb#FRAG").getProtocol());
		assertEquals("uuu", new URLAccess("uuu://user:password@host/path?").getProtocol());
	}

	@Test
	void testInvalid() {
		assertThrows(IllegalArgumentException.class, () -> new URLAccess("uuu://ho|st/ddd"));
	}

	@Test
	void testGetBasePath() {
		assertEquals("/path", new URLAccess("uuu://user:password@host/path?aa=bb#FRAG").getPath());
		assertEquals("/path/dd/dddd", new URLAccess("uuu://user:password@host/path/dd/dddd?aa=bb#FRAG").getPath());
		assertEquals("/path/dd/dddd", new URLAccess("uuu://user:password@host/path/dd/dddd?").getPath());
		assertEquals("/path/d d/dd dd", new URLAccess("uuu://user:password@host/path/d%20d/dd%20dd").getPath());
		assertEquals("/path/d,d'd-d_d d", new URLAccess("uuu://user:password@host/path/d,d'd-d_d d").getPath());
		assertEquals("/c:/path/my file.txt", new URLAccess("uuu://host/c:\\path\\my file.txt").getPath());
	}

	@Test
	void testGetHost() {
		assertEquals("host", new URLAccess("uuu://user:password@host/path?aa=bb#FRAG").getHost());
		assertEquals("host", new URLAccess("uuu://user:password@host/path?").getHost());
	}

	@Test
	void testGetUsername() {
		assertEquals("user", new URLAccess("uuu://user:password@host/path?aa=bb#FRAG").getUsername());
		assertEquals("user", new URLAccess("uuu://user:password@host/path?").getUsername());
		assertEquals("user", new URLAccess("uuu://user@host/path?").getUsername());
	}

	@Test
	void testGetProtectedRessourceURL() {
		assertEquals("uuu://user:********@host/path?aa=bb#FRAG", new URLAccess(
		        "uuu://user:password@host/path?aa=bb#FRAG").getProtectedRessourceURL());
		assertEquals("uuu://user@host/path?aa=bb&password=*********", new URLAccess(
		        "uuu://user@host/path?aa=bb&password=NOPE#FRAG").getProtectedRessourceURL());
		assertEquals("uuu://user:********@host/path?", new URLAccess(
		        "uuu://user:password@host/path?").getProtectedRessourceURL());
	}

	@Test
	void testGetPassword() {
		assertEquals("password",
		        new String(new URLAccess("uuu://user:password@host/path?aa=bb#FRAG").getPassword()));
		assertEquals("password",
		        new String(new URLAccess("uuu://user:password@host/path?").getPassword()));
		assertEquals("NOPE#FRAG",
		        new String(new URLAccess("uuu://user@host/path?aa=bb&password=NOPE#FRAG").getPassword()));
		assertEquals("",
		        new String(new URLAccess("uuu://user@host/path?aa=bb#FRAG").getPassword()));
		assertEquals("password",
		        new String(new URLAccess("uuu://user:password@host/path?aa=bb&password=NOPE#FRAG").getPassword()));
		assertEquals("o&o^o0:o?o|o+o0\\o=O,O#o0@;;o&!0-o_o~oo",
		        new String(new URLAccess(
		                "uuu://user@host/path?aa=bb&password=\"o&o^o0:o?o|o+o0\\o=O,O#o0@;;o&!0-o_o~oo\"")
		                        .getPassword()));
	}

	@Test
	void testGetPort() {
		assertEquals(21, new URLAccess("uuu://user:password@host:21/path?aa=bb#FRAG").getPort());
		assertEquals(443, new URLAccess("uuu://user:password@host:443/path?").getPort());
		assertEquals(21, new URLAccess("uuu://user:password@host:21/").getPort());
		assertEquals(443, new URLAccess("uuu://user@host:443/path?").getPort());
		assertEquals(443, new URLAccess("uuu://host:443/").getPort());
	}

	@Test
	void testGetDefaultPort() {
		assertEquals(-1, new URLAccess("uuu://host").getPort());
		assertEquals(-1, new URLAccess("file://host").getPort());
		assertEquals(21, new URLAccess("ftp://host").getPort());
		assertEquals(989, new URLAccess("ftps://host").getPort());
		assertEquals(21, new URLAccess("ftpes://host").getPort());
		assertEquals(22, new URLAccess("sftp://host").getPort());
	}

	@Test
	void testGetOptionZone() {
		assertEquals(Map.of(), new URLAccess("uuu://host:443/").getOptionZone());
		assertEquals(Map.of("aa", List.of("bb")),
		        new URLAccess("uuu://host/?aa=bb").getOptionZone());
		assertEquals(Map.of("aa", List.of("b b")),
		        new URLAccess("uuu://host/?aa=b b").getOptionZone());
		assertEquals(Map.of("aa", List.of("b b"), "c", List.of("d")),
		        new URLAccess("uuu://host/?aa=b b&c=d").getOptionZone());
		assertEquals(Map.of("aa", List.of("b/b"), "c", List.of("d")),
		        new URLAccess("uuu://host/?aa=b/b&c=d").getOptionZone());
		assertEquals(Map.of("aa", List.of("b\\b"), "c", List.of("d")),
		        new URLAccess("uuu://host/?aa=b\\b&c=d").getOptionZone());
		assertEquals(Map.of("aa", List.of("bb"), "c", List.of("d")),
		        new URLAccess("uuu://host/?aa=bb&c=\"d\"").getOptionZone());
		assertEquals(Map.of("aa", List.of("bb"), "c", List.of("d&d")),
		        new URLAccess("uuu://host/?aa=bb&c=\"d&d\"").getOptionZone());
		assertEquals(Map.of("aa", List.of("bb"),
		        "c", List.of("o&o^o0:o?o|o+o0\\o=O,O#o0@;;o&!0-o_o~oo"),
		        "ee", List.of("ff")),
		        new URLAccess("uuu://host/?aa=bb&c=\"o&o^o0:o?o|o+o0\\o=O,O#o0@;;o&!0-o_o~oo\"&ee=ff").getOptionZone());
		assertThrows(IllegalArgumentException.class, () -> new URLAccess("uuu://host/?aa=bb&c=\"d&d"));
		assertEquals(Set.of("aaa"), new URLAccess("p://h/?aaa").getOptionZone().keySet());
		assertEquals(Set.of("aaa", "bbb"), new URLAccess("p://h/?aaa&bbb").getOptionZone().keySet());
		assertEquals(Set.of("aaa", "bbb", "ccc"), new URLAccess("p://h/?aaa&bbb&ccc").getOptionZone().keySet());
		assertEquals(Set.of("aaabbbccc"), new URLAccess("p://h/?aaa\"bbb\"ccc").getOptionZone().keySet());
		assertEquals(Set.of("aaab&bccc"), new URLAccess("p://h/?aaa\"b&b\"ccc").getOptionZone().keySet());
		assertEquals(Set.of("aa", "ab&bc", "cc"), new URLAccess("p://h/?aa&a\"b&b\"c&cc").getOptionZone().keySet());
		assertEquals(Set.of("bbb"), new URLAccess("p://h/?&bbb").getOptionZone().keySet());
		assertEquals(Set.of("aaa"), new URLAccess("p://h/?aaa&").getOptionZone().keySet());
	}
}
