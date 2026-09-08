package com.tgac.pldb;

// ABOUTME: The RENTED certify capability: the backend validates read sets at
// ABOUTME: commit itself; its refusal dialect is recognized, not prevented.

import java.sql.SQLException;

/**
 * A source whose backend certifies reads itself — a serializable
 * isolation that actually validates read sets (PostgreSQL's SSI, the
 * two-phase-locking implementations). The implementor is the adapter
 * that KNOWS its backend keeps that promise and speaks its conflict
 * dialect; a backend whose SERIALIZABLE is snapshot isolation in
 * costume must not wear this interface.
 */
public interface CertifiedReads {

	boolean conflict(SQLException failure);
}
