package de.slotrace.application.port.out;

import de.slotrace.domain.model.Fahrzeug;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FahrzeugRepository {

    List<Fahrzeug> alle();

    Optional<Fahrzeug> finde(UUID id);

    void speichere(Fahrzeug fahrzeug);

    void loesche(UUID id);
}
