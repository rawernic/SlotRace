package de.slotrace.application.port.in;

import de.slotrace.domain.model.Rennen;
import java.util.UUID;

/** Steuerung eines laufenden Rennens: Läufe starten/beenden und Runden erfassen. */
public interface Renndurchfuehrung {

    Rennen starteLauf(UUID rennenId, int laufNummer);

    Rennen erfasseRunde(UUID rennenId, int laufNummer, int spurNummer);

    Rennen entferneLetzteRunde(UUID rennenId, int laufNummer, int spurNummer);

    Rennen beendeLauf(UUID rennenId, int laufNummer);

    Rennen setzeLaufZurueck(UUID rennenId, int laufNummer);
}
