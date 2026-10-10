package de.slotrace.application.port.in;

import de.slotrace.domain.model.Fahrzeug;
import java.util.List;
import java.util.UUID;

public interface FahrzeugVerwaltung {

    record FahrzeugDaten(String bezeichnung, String hersteller, String massstab) {
    }

    List<Fahrzeug> alle();

    Fahrzeug hole(UUID id);

    Fahrzeug anlegen(FahrzeugDaten daten);

    Fahrzeug aendern(UUID id, FahrzeugDaten daten);

    void loeschen(UUID id);
}
