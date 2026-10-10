package de.slotrace.domain.service;

import java.util.UUID;

/** Stand eines Teilnehmers innerhalb eines einzelnen Laufs. */
public record SpurStand(int spurNummer, UUID teilnehmerId, int position, int runden, long zeitMs,
                        Long letzteRundeMs, Long besteRundeMs) {
}
