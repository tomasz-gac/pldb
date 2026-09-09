package com.tgac.pldb.transaction;

// ABOUTME: An opaque snapshot token: minted by a source, meaningful only when
// ABOUTME: handed back to that source with a certify question.

/**
 * A source-owned snapshot token. Nothing outside the minting source may
 * interpret it — the source answers every question about it; outsiders only carry it.
 */
public interface Pin {
}
