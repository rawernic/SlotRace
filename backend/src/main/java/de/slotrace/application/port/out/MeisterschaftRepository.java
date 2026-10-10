package de.slotrace.application.port.out;

import de.slotrace.domain.model.Meisterschaft;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeisterschaftRepository {

    List<Meisterschaft> alle();

    Optional<Meisterschaft> finde(UUID id);

    void speichere(Meisterschaft meisterschaft);

    void loesche(UUID id);
}
