package de.slotrace.application.port.out;

import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.model.Rennstatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RennenRepository {

    List<Rennen> alle();

    List<Rennen> alleDerMeisterschaft(UUID meisterschaftId);

    Optional<Rennen> finde(UUID id);

    /** Lädt das Rennen mit Schreibsperre, damit parallele Rundenerfassungen sich nicht überschreiben. */
    Optional<Rennen> findeZurBearbeitung(UUID id);

    List<Rennen> alleAufBahnMitStatus(UUID bahnId, Rennstatus status);

    void speichere(Rennen rennen);

    void loesche(UUID id);

    boolean existiertMitFahrer(UUID fahrerId);

    boolean existiertMitFahrzeug(UUID fahrzeugId);

    boolean existiertMitBahn(UUID bahnId);

    boolean existiertMitMeisterschaft(UUID meisterschaftId);
}
