package de.slotrace.domain.model;

import java.util.UUID;

/** Welcher Teilnehmer in einem Lauf auf welcher Spur fährt. */
public record Belegung(int spurNummer, UUID teilnehmerId) {
}
