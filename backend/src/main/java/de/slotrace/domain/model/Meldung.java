package de.slotrace.domain.model;

import java.util.UUID;

/** Anmeldung eines Fahrers mit Fahrzeug zu einem Rennen. */
public record Meldung(UUID fahrerId, UUID fahrzeugId) {
}
