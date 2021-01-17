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

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.transfertfiles.BufferVault;
import tv.hd3g.transfertfiles.filters.DigestFilterHashExtraction.ExtractionInstance;

/**
 * Not reusable
 */
public class DataExchangeFilterHashExtraction implements DataExchangeFilter {
	private static final Logger log = LogManager.getLogger();

	private static final Set<DigestFilterHashExtraction> avaliableDigests;

	static {
		avaliableDigests = Stream.of(DigestFilterHashExtraction.values())
		        .filter(DigestFilterHashExtraction::isAvaliable)
		        .collect(toUnmodifiableSet());

		final var notAvaliableDigests = Stream.of(DigestFilterHashExtraction.values())
		        .filter(not(avaliableDigests::contains))
		        .collect(toUnmodifiableSet());
		log.debug("Avaliable digests: {}, not avaliable digests: {}", avaliableDigests, notAvaliableDigests);
	}

	private final Map<DigestFilterHashExtraction, ExtractionInstance> currentDigests;

	public DataExchangeFilterHashExtraction(final DigestFilterHashExtraction... digests) {
		this(Stream.of(digests)
		        .filter(Objects::nonNull)
		        .collect(toUnmodifiableSet()));
	}

	public DataExchangeFilterHashExtraction(final Collection<DigestFilterHashExtraction> digests) {
		currentDigests = digests.stream()
		        .filter(avaliableDigests::contains)
		        .collect(toUnmodifiableMap(d -> d,
		                DigestFilterHashExtraction::createInstance));
		if (currentDigests.isEmpty()) {
			throw new IllegalArgumentException("Can't init instances of " + digests);
		}
	}

	@Override
	public BufferVault applyDataFilter(final boolean last, final BufferVault dataSources) throws IOException {
		currentDigests.values()
		        .forEach(md -> md.update(dataSources.readAllToByteBuffer()));
		return new BufferVault();
	}

	public Map<DigestFilterHashExtraction, byte[]> getResults() {
		return currentDigests.keySet().stream()
		        .collect(toUnmodifiableMap(d -> d,
		                d -> currentDigests.get(d).digest()));
	}

	@Override
	public String getFilterName() {
		return "HashExtraction:" + currentDigests.keySet().stream()
		        .map(DigestFilterHashExtraction::toString)
		        .collect(Collectors.joining("+"));
	}

}
