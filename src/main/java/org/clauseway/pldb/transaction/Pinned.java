package org.clauseway.pldb.transaction;

// ABOUTME: A value with the pin naming the world it was read from — data and
// ABOUTME: validator minted together, so no window can tear them apart.

import lombok.Value;

/**
 * A value paired with the {@link Pin} naming the world it came from.
 * This is the sync projection of the async base seam's shape — a
 * produce's emissions materialized, with the completion pin beside
 * them: pin and data arrive as ONE result, so a source whose medium
 * mints them together (a snapshot, one HTTP response) has no two-call
 * window to tear, and a source whose medium splits them must capture
 * the pin BEFORE the data it certifies.
 */
@Value(staticConstructor = "of")
public class Pinned<T> {
	T value;
	Pin pin;
}
