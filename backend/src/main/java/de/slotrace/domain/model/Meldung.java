package de.slotrace.domain.model;

import java.util.UUID;

/** Anmeldung eines Fahrers mit Fahrzeug zu einem Rennen; {@code spurNummer} nur im Training. */
public record Meldung(UUID fahrerId, UUID fahrzeugId, Integer spurNummer) {

    public Meldung(UUID fahrerId, UUID fahrzeugId) {
        this(fahrerId, fahrzeugId, null);
    }
}
