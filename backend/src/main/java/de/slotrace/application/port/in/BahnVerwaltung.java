package de.slotrace.application.port.in;

import de.slotrace.domain.model.Bahn;
import java.util.List;
import java.util.UUID;

public interface BahnVerwaltung {

    /** @param spurFarben Farben der Spuren 1..n im Format #RRGGBB */
    record BahnDaten(String name, List<String> spurFarben) {
    }

    List<Bahn> alle();

    Bahn hole(UUID id);

    Bahn anlegen(BahnDaten daten);

    Bahn aendern(UUID id, BahnDaten daten);

    void loeschen(UUID id);
}
