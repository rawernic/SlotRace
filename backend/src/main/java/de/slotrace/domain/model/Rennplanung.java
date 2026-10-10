package de.slotrace.domain.model;

import java.util.List;
import java.util.UUID;

/** Einstellungen und Meldungen, aus denen ein Rennen samt Laufplan erzeugt wird. */
public record Rennplanung(String name, UUID meisterschaftId, Rennmodus modus, Integer laufDauerSekunden,
                          Integer rundenZiel, int mindestRundenzeitMs, List<Meldung> meldungen) {
}
