package de.slotrace.application.port.out;

import de.slotrace.domain.model.Fahrer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FahrerRepository {

    List<Fahrer> alle();

    Optional<Fahrer> finde(UUID id);

    void speichere(Fahrer fahrer);

    void loesche(UUID id);
}
