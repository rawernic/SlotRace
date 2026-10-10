package de.slotrace.application.port.in;

import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.model.Rennplanung;
import de.slotrace.domain.service.Platzierung;
import java.util.List;
import java.util.UUID;

public interface RennenVerwaltung {

    List<Rennen> alle();

    /** Liefert den aktuellen Stand; abgelaufene Läufe von Zeitrennen werden dabei beendet. */
    Rennen hole(UUID id);

    Rennen planen(UUID bahnId, Rennplanung planung);

    Rennen aendern(UUID id, UUID bahnId, Rennplanung planung);

    void loeschen(UUID id);

    List<Platzierung> ergebnis(UUID id);
}
