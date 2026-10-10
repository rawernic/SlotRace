package de.slotrace.application.port.in;

import de.slotrace.domain.model.Fahrer;
import java.util.List;
import java.util.UUID;

public interface FahrerVerwaltung {

    record FahrerDaten(String vorname, String nachname, String spitzname) {
    }

    List<Fahrer> alle();

    Fahrer hole(UUID id);

    Fahrer anlegen(FahrerDaten daten);

    Fahrer aendern(UUID id, FahrerDaten daten);

    void loeschen(UUID id);
}
