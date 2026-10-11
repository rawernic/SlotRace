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

    Rennen pausieren(UUID rennenId);

    Rennen abbrechen(UUID rennenId);

    Rennen beendeTraining(UUID rennenId);

    /** Setzt im Training Fahrer und Fahrzeug einer Spur; ohne Fahrer wird die Spur frei. */
    Rennen wechsleBelegung(UUID rennenId, int spurNummer, UUID fahrerId, UUID fahrzeugId);
}
