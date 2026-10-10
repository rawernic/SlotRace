package de.slotrace.application.port.in;

import de.slotrace.domain.model.Meisterschaft;
import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.service.MeisterschaftsPlatz;
import java.util.List;
import java.util.UUID;

public interface MeisterschaftVerwaltung {

    record MeisterschaftDaten(String name, List<Integer> punkteSchema) {
    }

    List<Meisterschaft> alle();

    Meisterschaft hole(UUID id);

    Meisterschaft anlegen(MeisterschaftDaten daten);

    Meisterschaft aendern(UUID id, MeisterschaftDaten daten);

    void loeschen(UUID id);

    List<Rennen> rennen(UUID id);

    List<MeisterschaftsPlatz> wertung(UUID id);
}
