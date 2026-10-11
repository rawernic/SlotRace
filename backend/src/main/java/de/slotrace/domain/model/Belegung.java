package de.slotrace.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Welcher Teilnehmer in einem Lauf auf welcher Spur fährt.
 *
 * @param belegtSeit Zeitpunkt eines Wechsels während des Laufs ({@code null}: seit Laufstart)
 */
public record Belegung(int spurNummer, UUID teilnehmerId, Instant belegtSeit) {

    public Belegung(int spurNummer, UUID teilnehmerId) {
        this(spurNummer, teilnehmerId, null);
    }
}
