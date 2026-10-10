package de.slotrace.domain.model;

import static de.slotrace.domain.Pruefung.pflicht;

import java.util.UUID;

/** Ein Fahrer mit seinem Fahrzeug in einem bestimmten Rennen. */
public record Teilnehmer(UUID id, UUID fahrerId, UUID fahrzeugId, int startnummer) {

    public Teilnehmer {
        pflicht(id, "ID");
        pflicht(fahrerId, "Fahrer");
        pflicht(fahrzeugId, "Fahrzeug");
    }
}
