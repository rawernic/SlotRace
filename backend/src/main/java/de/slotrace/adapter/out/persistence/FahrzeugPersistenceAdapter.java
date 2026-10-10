package de.slotrace.adapter.out.persistence;

import de.slotrace.application.port.out.FahrzeugRepository;
import de.slotrace.domain.model.Fahrzeug;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class FahrzeugPersistenceAdapter implements FahrzeugRepository {

    private final FahrzeugJpaRepository jpa;

    FahrzeugPersistenceAdapter(FahrzeugJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Fahrzeug> alle() {
        return jpa.findAll().stream().map(FahrzeugPersistenceAdapter::zuDomain).toList();
    }

    @Override
    public Optional<Fahrzeug> finde(UUID id) {
        return jpa.findById(id).map(FahrzeugPersistenceAdapter::zuDomain);
    }

    @Override
    public void speichere(Fahrzeug fahrzeug) {
        FahrzeugEntity e = jpa.findById(fahrzeug.id()).orElseGet(FahrzeugEntity::new);
        e.id = fahrzeug.id();
        e.bezeichnung = fahrzeug.bezeichnung();
        e.hersteller = fahrzeug.hersteller();
        e.massstab = fahrzeug.massstab();
        jpa.save(e);
    }

    @Override
    public void loesche(UUID id) {
        jpa.deleteById(id);
    }

    private static Fahrzeug zuDomain(FahrzeugEntity e) {
        return new Fahrzeug(e.id, e.bezeichnung, e.hersteller, e.massstab);
    }
}
