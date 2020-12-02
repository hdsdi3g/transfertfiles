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

import static java.io.File.separator;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection.DISTANTTOLOCAL;
import static tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection.LOCALTODISTANT;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.mina.core.RuntimeIoException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

import tv.hd3g.commons.IORuntimeException;
import tv.hd3g.transfertfiles.TransfertObserver.TransfertDirection;

public abstract class TestFileToolkit<T extends AbstractFile> { // NOSONAR S5786

	private static final int LOOP_COPIES_BUFFER = 4;

	static final LinkedBlockingQueue<AbstractFileSystem<?>> createdFS = new LinkedBlockingQueue<>();
	static File root;

	@BeforeAll
	static void initBaseWorkingDir() throws IOException {
		root = new File("target/testfs");
		FileUtils.forceMkdir(root);
		FileUtils.cleanDirectory(root);
	}

	@AfterAll
	static void removeBaseWorkingDir() throws Exception {
		createdFS.forEach(t -> {
			try {
				t.close();
			} catch (final IOException e) {
				throw new RuntimeIoException(e);
			}
		});
		createdFS.clear();
		FileUtils.cleanDirectory(root);
	}

	protected abstract AbstractFileSystem<T> createFileSystem();

	public static File getRoot() {
		return root;
	}

	public static void write(final File dest) {
		try {
			FileUtils.write(dest, "A", Charset.defaultCharset());
		} catch (final IOException e) {
			throw new RuntimeIoException(e);
		}
	}

	@FunctionalInterface
	interface Executable1<A> {
		void execute(A ab) throws Throwable;
	}

	private void addRegularTests(final Map<String, Executable1<AbstractFile>> tests,
	                             final AbstractFileSystem<T> fs,
	                             final boolean isADir,
	                             final boolean isAFile,
	                             final long length) {
		if (isAFile) {
			tests.put("testLength", f -> assertEquals(length, f.length()));
		}
		tests.put("testExists",
		        f -> assertEquals(isADir || isAFile, f.exists()));
		tests.put("testIsDirectory",
		        f -> assertEquals(isADir, f.isDirectory()));
		tests.put("testIsFile",
		        f -> assertEquals(isAFile, f.isFile()));
		tests.put("testIsLink",
		        f -> assertFalse(f.isLink()));
		tests.put("testIsSpecial",
		        f -> assertFalse(f.isSpecial()));
		if (isADir == false && isAFile == false) {
			tests.put("testLastModified",
			        f -> assertEquals(0, f.lastModified()));
			tests.put("testToCache",
			        f -> {
				        final var c = f.toCache();
				        assertFalse(c.exists());
				        assertFalse(c.isDirectory());
				        assertFalse(c.isFile());
				        assertFalse(c.isLink());
				        assertFalse(c.isSpecial());
				        assertEquals(0, c.lastModified());
				        assertEquals(0, c.length());
				        assertEquals(f.getName(), c.getName());
				        assertEquals(f.getPath(), c.getPath());
			        });
		} else {
			tests.put("testLastModified-notTooYoung",
			        f -> assertTrue(f.lastModified() < System.currentTimeMillis() + 1_000L));
			tests.put("testLastModified-notTooOld",
			        f -> assertTrue(f.lastModified() > System.currentTimeMillis() - 300_000L));
			tests.put("testToCache", f -> compareCache(f, f.toCache()));
		}
	}

	private static void compareCache(final AbstractFile f, final CachedFileAttributes c) {
		assertEquals(f.toCache(), c);
		assertEquals(f.exists(), c.exists());
		assertEquals(f.isDirectory(), c.isDirectory());
		assertEquals(f.isFile(), c.isFile());
		assertEquals(f.isLink(), c.isLink());
		assertEquals(f.isSpecial(), c.isSpecial());
		assertEquals(f.lastModified(), c.lastModified());
		if (f.isDirectory() == false) {
			assertEquals(f.length(), c.length());
		}
		assertEquals(f.isHidden(), c.isHidden());
		assertEquals(f.getName(), c.getName());
		assertEquals(f.getPath(), c.getPath());
	}

	@TestFactory
	final Stream<DynamicNode> testExistingDir() throws IOException {
		final var baseDirName = "existing-dir";
		final var baseSubFileName = "existing-file";
		final var dir = new File(root, baseDirName).getAbsoluteFile();
		final var subFile = new File(root, baseDirName + separator + baseSubFileName).getAbsoluteFile();
		final var fs = createFileSystem();
		createdFS.add(fs);

		final Map<String, Executable1<AbstractFile>> tests = new LinkedHashMap<>();
		addRegularTests(tests, fs, true, false, dir.length());
		final var to = new TransfertObserver() {};
		tests.put("testList",
		        f -> {
			        final var sf = fs.getFromPath(baseDirName + "/" + baseSubFileName);
			        final var list = f.list().collect(toUnmodifiableList());
			        assertEquals(1, list.size());
			        assertEquals(sf, list.get(0));
			        assertEquals(subFile.getName(), list.get(0).getName());
		        });
		tests.put("testToCachedList",
		        f -> {
			        final var list = f.list().collect(toUnmodifiableList());
			        final var clist = f.toCachedList().collect(toUnmodifiableList());
			        assertEquals(1, list.size());
			        assertEquals(1, clist.size());
			        compareCache(list.get(0), clist.get(0));
		        });
		tests.put("testDelete",
		        f -> {
			        assertTrue(dir.exists());
			        assertTrue(subFile.exists());
			        assertThrows(CannotDeleteException.class, () -> f.delete());
			        assertTrue(dir.exists());
			        assertTrue(subFile.exists());
			        FileUtils.cleanDirectory(dir);
			        f.delete();
			        assertFalse(dir.exists());
		        });
		tests.put("testMkdir",
		        f -> {
			        FileUtils.cleanDirectory(root);
			        assertFalse(dir.exists());
			        assertFalse(subFile.exists());
			        f.mkdir();
			        assertTrue(dir.exists());
			        assertTrue(dir.isDirectory());
		        });
		tests.put("testRenameTo",
		        f -> {
			        assertTrue(f.exists());
			        final var newF = f.renameTo("/thisDir");
			        assertFalse(f.exists());
			        assertFalse(dir.exists());

			        assertTrue(newF.exists());
			        assertFalse(newF.isFile());
			        assertTrue(newF.isDirectory());
		        });
		tests.put("testCopyAbstractToLocal",
		        d -> {
			        final var dest = new File(root, "destfileCopy");
			        assertThrows(IORuntimeException.class, () -> d.copyAbstractToLocal(dest, to));
		        });
		tests.put("testSendLocalToAbstract",
		        d -> {
			        final var localFileCopy = new File(root, "localfileCopy");
			        assertFalse(localFileCopy.exists());
			        FileUtils.write(localFileCopy, "BB", defaultCharset());
			        assertTrue(localFileCopy.exists());
			        d.sendLocalToAbstract(localFileCopy, to);
			        assertTrue(d.exists());

			        final var expectedFile = new File(dir, "localfileCopy");
			        assertTrue(expectedFile.exists());
			        assertEquals(2, expectedFile.length());
			        assertEquals("BB", FileUtils.readFileToString(expectedFile, defaultCharset()));
		        });

		return tests.entrySet().stream().map(entry -> {
			final var testName = entry.getKey();
			final var testAction = entry.getValue();
			return dynamicTest(testName,
			        () -> {
				        FileUtils.cleanDirectory(root);
				        FileUtils.forceMkdir(dir);
				        write(subFile);
				        final var f = fs.getFromPath(baseDirName);
				        testAction.execute(f);
			        });
		});
	}

	@TestFactory
	final Stream<DynamicNode> testExistingFile() throws IOException {
		final var baseName = "existing-file";
		final var file = new File(root, baseName).getAbsoluteFile();
		final var fs = createFileSystem();
		createdFS.add(fs);

		final Map<String, Executable1<AbstractFile>> tests = new LinkedHashMap<>();
		addRegularTests(tests, fs, false, true, 1);
		tests.put("testList",
		        f -> assertEquals(0, f.list().count()));
		tests.put("testToCachedList",
		        f -> assertEquals(0, f.toCachedList().count()));
		tests.put("testDelete", f -> {
			assertTrue(file.exists());
			f.delete();
			assertFalse(file.exists());
		});
		tests.put("testMkdir", f -> {
			assertTrue(file.exists());
			assertFalse(file.isDirectory());
			assertThrows(IORuntimeException.class, () -> f.mkdir());
		});
		tests.put("testRenameTo", f -> {
			assertTrue(f.exists());
			final var newF = f.renameTo("/thisFile");
			assertFalse(f.exists());
			assertFalse(file.exists());

			assertTrue(newF.exists());
			assertTrue(newF.isFile());
			assertEquals(1, newF.length());
		});
		tests.put("testCopyAbstractToLocal", f -> {
			final var localFileCopy = new File(root, "destfileCopy");
			assertFalse(localFileCopy.exists());

			final var to = Mockito.mock(TransfertObserver.class);
			when(to.onTransfertProgress(
			        any(File.class), any(AbstractFile.class), any(TransfertDirection.class), anyLong(),
			        anyLong()))
			                .thenReturn(true);

			f.copyAbstractToLocal(localFileCopy, to);
			assertTrue(localFileCopy.exists());
			assertTrue(f.exists());
			assertEquals("A", FileUtils.readFileToString(localFileCopy, defaultCharset()));

			verify(to, Mockito.times(1)).onTransfertProgress(
			        eq(localFileCopy),
			        eq(f),
			        eq(DISTANTTOLOCAL),
			        anyLong(),
			        eq(localFileCopy.length()));
			verify(to, Mockito.times(1))
			        .beforeTransfert(eq(localFileCopy), eq(f), eq(DISTANTTOLOCAL));
			verify(to, Mockito.times(1))
			        .afterTransfert(eq(localFileCopy), eq(f), eq(DISTANTTOLOCAL), any(
			                Duration.class));
		});
		tests.put("testSendLocalToAbstract", f -> {
			final var localFileCopy = new File(root, "destfileCopy");
			assertFalse(localFileCopy.exists());
			FileUtils.write(localFileCopy, "BB", defaultCharset());
			assertTrue(localFileCopy.exists());

			final var to = Mockito.mock(TransfertObserver.class);
			when(to.onTransfertProgress(
			        any(File.class), any(AbstractFile.class), any(TransfertDirection.class), anyLong(),
			        anyLong()))
			                .thenReturn(true);

			f.sendLocalToAbstract(localFileCopy, to);
			assertTrue(f.exists());
			assertEquals(2, f.length());
			assertEquals("BB", FileUtils.readFileToString(file, defaultCharset()));

			verify(to, Mockito.times(1)).onTransfertProgress(
			        eq(localFileCopy),
			        eq(f),
			        eq(LOCALTODISTANT),
			        anyLong(),
			        eq(localFileCopy.length()));
			verify(to, Mockito.times(1))
			        .beforeTransfert(eq(localFileCopy), eq(f), eq(LOCALTODISTANT));
			verify(to, Mockito.times(1))
			        .afterTransfert(eq(localFileCopy), eq(f), eq(LOCALTODISTANT), any(Duration.class));
		});

		return tests.entrySet().stream().map(entry -> {
			final var testName = entry.getKey();
			final var testAction = entry.getValue();
			return dynamicTest(testName,
			        () -> {
				        FileUtils.cleanDirectory(root);
				        write(file);
				        final var f = fs.getFromPath(baseName);
				        testAction.execute(f);
			        });
		});
	}

	@TestFactory
	final Stream<DynamicNode> testNonExisting() throws IOException {
		final var baseName = "non-existing";
		final var file = new File(root, baseName).getAbsoluteFile();
		final var fs = createFileSystem();
		createdFS.add(fs);
		final var to = new TransfertObserver() {};

		final Map<String, Executable1<AbstractFile>> tests = new LinkedHashMap<>();
		addRegularTests(tests, fs, false, false, 0);
		tests.put("testIsHidden-reallyhidded",
		        f -> assertTrue(fs.getFromPath(".dontseeme").isHidden()));
		tests.put("testList",
		        f -> assertEquals(0, f.list().count()));
		tests.put("testToCachedList",
		        f -> assertEquals(0, f.toCachedList().count()));
		tests.put("testDelete", f -> {
			assertThrows(IORuntimeException.class, () -> f.delete());
		});
		tests.put("testMkdir", f -> {
			assertFalse(file.exists());
			assertFalse(file.isDirectory());
			f.mkdir();
			assertTrue(file.exists());
			assertTrue(file.isDirectory());
		});
		tests.put("testRenameTo", f -> {
			assertFalse(f.exists());
			assertThrows(IORuntimeException.class, () -> f.renameTo("/thisnewdir"));
		});
		tests.put("testCopyAbstractToLocal", f -> {
			final var localFileCopy = new File("target/destfile");
			assertThrows(IORuntimeException.class,
			        () -> f.copyAbstractToLocal(localFileCopy, to));
		});
		tests.put("testSendLocalToAbstract", f -> {
			final var localFileCopy = new File("target/destfile");
			assertThrows(IORuntimeException.class,
			        () -> f.sendLocalToAbstract(localFileCopy, to));
		});

		return tests.entrySet().stream().map(entry -> {
			final var testName = entry.getKey();
			final var testAction = entry.getValue();
			return dynamicTest(testName,
			        () -> {
				        FileUtils.cleanDirectory(root);
				        final var f = fs.getFromPath(baseName);
				        testAction.execute(f);
			        });
		});
	}

	@TestFactory
	final Stream<DynamicNode> testObserver() throws IOException {
		final var to = Mockito.mock(TransfertObserver.class);
		final var internalFile = new File(root, "transfertSource").getAbsoluteFile();

		final var fs = createFileSystem();
		createdFS.add(fs);
		final var baseBufferSize = fs.getIOBufferSize();

		final var stringContent = IntStream.range(0, baseBufferSize * LOOP_COPIES_BUFFER)
		        .mapToObj(i -> (char) (i % 93 + 33))
		        .map(String::valueOf)
		        .collect(joining());
		final var localFileCopy = new File(root, "transfertDest");

		final Map<String, Executable1<AbstractFile>> tests = new LinkedHashMap<>();
		tests.put("testObserverStop_DISTANTTOLOCAL",
		        f -> {
			        final var direction = DISTANTTOLOCAL;
			        when(to.onTransfertProgress(
			                any(File.class), any(AbstractFile.class), eq(direction), anyLong(), anyLong()))
			                        .thenReturn(false);
			        f.copyAbstractToLocal(localFileCopy, to);
			        final long expectedSize = baseBufferSize;

			        verify(to, Mockito.times(1)).onTransfertProgress(eq(localFileCopy),
			                eq(f), eq(direction), anyLong(), longThat(l -> l > 0 && l <= baseBufferSize));
			        verify(to, Mockito.never())
			                .afterTransfert(any(File.class), any(AbstractFile.class), eq(direction), any(
			                        Duration.class));

			        assertTrue(localFileCopy.exists());
			        assertTrue(localFileCopy.length() > 0);
			        assertTrue(localFileCopy.length() <= expectedSize);
			        assertEquals(stringContent.substring(0, (int) localFileCopy.length()),
			                FileUtils.readFileToString(localFileCopy, defaultCharset()));
			        verify(to, Mockito.times(1))
			                .beforeTransfert(eq(localFileCopy), eq(f), eq(direction));
		        });
		tests.put("testObserverStop_LOCALTODISTANT",
		        f -> {
			        final var direction = LOCALTODISTANT;
			        internalFile.renameTo(localFileCopy);

			        when(to.onTransfertProgress(
			                any(File.class), any(AbstractFile.class), eq(direction), anyLong(), anyLong()))
			                        .thenReturn(false);
			        f.sendLocalToAbstract(localFileCopy, to);
			        final long expectedSize = baseBufferSize;

			        verify(to, Mockito.times(1)).onTransfertProgress(
			                eq(localFileCopy), eq(f), eq(direction), anyLong(),
			                longThat(l -> l > 0 && l <= baseBufferSize));
			        verify(to, Mockito.never())
			                .afterTransfert(any(File.class), any(AbstractFile.class), eq(direction),
			                        any(Duration.class));

			        assertTrue(internalFile.exists());
			        assertTrue(internalFile.length() <= expectedSize);
			        assertEquals(stringContent.substring(0, (int) internalFile.length()),
			                FileUtils.readFileToString(internalFile, defaultCharset()));
			        verify(to, Mockito.times(1))
			                .beforeTransfert(eq(localFileCopy), eq(f), eq(direction));
		        });
		tests.put("testObserverFull_DISTANTTOLOCAL",
		        f -> {
			        final var direction = DISTANTTOLOCAL;
			        final var expectedSize = stringContent.length();
			        when(to.onTransfertProgress(
			                any(File.class), any(AbstractFile.class), eq(direction), anyLong(), anyLong()))
			                        .thenReturn(true);
			        f.copyAbstractToLocal(localFileCopy, to);

			        verify(to, atLeastOnce()).onTransfertProgress(
			                eq(localFileCopy), eq(f), eq(direction), anyLong(),
			                longThat(l -> l > 0 && l <= expectedSize));
			        verify(to, Mockito.times(1))
			                .afterTransfert(eq(localFileCopy), eq(f), eq(direction), any(Duration.class));

			        assertTrue(localFileCopy.exists());
			        assertEquals(expectedSize, localFileCopy.length());
			        assertEquals(stringContent.substring(0, expectedSize),
			                FileUtils.readFileToString(localFileCopy, defaultCharset()));
			        verify(to, Mockito.times(1))
			                .beforeTransfert(eq(localFileCopy), eq(f), eq(direction));
		        });
		tests.put("testObserverFull_LOCALTODISTANT",
		        f -> {
			        final var direction = LOCALTODISTANT;
			        final var expectedSize = stringContent.length();
			        internalFile.renameTo(localFileCopy);

			        when(to.onTransfertProgress(
			                any(File.class), any(AbstractFile.class), eq(direction), anyLong(), anyLong()))
			                        .thenReturn(true);
			        f.sendLocalToAbstract(localFileCopy, to);

			        verify(to, atLeastOnce()).onTransfertProgress(
			                eq(localFileCopy), eq(f), eq(direction), anyLong(),
			                longThat(l -> l > 0 && l <= expectedSize));
			        verify(to, Mockito.times(1))
			                .afterTransfert(eq(localFileCopy), eq(f), eq(direction), any(Duration.class));

			        assertTrue(internalFile.exists());
			        assertEquals(expectedSize, internalFile.length());
			        assertEquals(stringContent.substring(0, expectedSize),
			                FileUtils.readFileToString(internalFile, defaultCharset()));
			        verify(to, Mockito.times(1))
			                .beforeTransfert(eq(localFileCopy), eq(f), eq(direction));
		        });

		return tests.entrySet().stream().map(entry -> {
			final var testName = entry.getKey();
			final var testAction = entry.getValue();
			return dynamicTest(testName,
			        () -> {
				        FileUtils.cleanDirectory(root);
				        Mockito.reset(to);
				        FileUtils.write(internalFile, stringContent, defaultCharset());

				        final var f = fs.getFromPath("transfertSource");
				        testAction.execute(f);
			        });
		});
	}

	/*@TestFactory
	Stream<DynamicNode> normalizedTests() throws IOException {
		return Stream.of(
		        DynamicContainer.dynamicContainer("Container test",
		                Stream.of(DynamicTest.dynamicTest("sub test",
		                        () -> {
			                        assertEquals(2, Math.addExact(1, 1));
		                        }))),
		        DynamicTest.dynamicTest("Add test",
		                () -> assertEquals(2, Math.addExact(1, 1))),
		        DynamicTest.dynamicTest("Multiply Test",
		                () -> assertEquals(4, Math.multiplyExact(2, 2))));
	}*/

}
