package de.slotrace.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Eine gefahrene Runde; {@code rundenzeitMs} ist die Zeit seit der vorherigen Runde bzw. dem Laufstart. */
public record Runde(UUID id, UUID teilnehmerId, int spurNummer, int nummer, Instant zeitpunkt, long rundenzeitMs) {
}
