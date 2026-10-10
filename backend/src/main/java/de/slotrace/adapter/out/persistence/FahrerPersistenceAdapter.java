package de.slotrace.adapter.out.persistence;

import de.slotrace.application.port.out.FahrerRepository;
import de.slotrace.domain.model.Fahrer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class FahrerPersistenceAdapter implements FahrerRepository {

    private final FahrerJpaRepository jpa;

    FahrerPersistenceAdapter(FahrerJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Fahrer> alle() {
        return jpa.findAll().stream().map(FahrerPersistenceAdapter::zuDomain).toList();
    }

    @Override
    public Optional<Fahrer> finde(UUID id) {
        return jpa.findById(id).map(FahrerPersistenceAdapter::zuDomain);
    }

    @Override
    public void speichere(Fahrer fahrer) {
        FahrerEntity e = jpa.findById(fahrer.id()).orElseGet(FahrerEntity::new);
        e.id = fahrer.id();
        e.vorname = fahrer.vorname();
        e.nachname = fahrer.nachname();
        e.spitzname = fahrer.spitzname();
        jpa.save(e);
    }

    @Override
    public void loesche(UUID id) {
        jpa.deleteById(id);
    }

    private static Fahrer zuDomain(FahrerEntity e) {
        return new Fahrer(e.id, e.vorname, e.nachname, e.spitzname);
    }
}
