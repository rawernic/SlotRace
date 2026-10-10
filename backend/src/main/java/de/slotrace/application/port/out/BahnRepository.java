package de.slotrace.application.port.out;

import de.slotrace.domain.model.Bahn;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BahnRepository {

    List<Bahn> alle();

    Optional<Bahn> finde(UUID id);

    void speichere(Bahn bahn);

    void loesche(UUID id);
}
