package de.slotrace.adapter.out.persistence;

import de.slotrace.application.port.out.MeisterschaftRepository;
import de.slotrace.domain.model.Meisterschaft;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class MeisterschaftPersistenceAdapter implements MeisterschaftRepository {

    private final MeisterschaftJpaRepository jpa;

    MeisterschaftPersistenceAdapter(MeisterschaftJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Meisterschaft> alle() {
        return jpa.findAll().stream().map(MeisterschaftPersistenceAdapter::zuDomain).toList();
    }

    @Override
    public Optional<Meisterschaft> finde(UUID id) {
        return jpa.findById(id).map(MeisterschaftPersistenceAdapter::zuDomain);
    }

    @Override
    public void speichere(Meisterschaft meisterschaft) {
        MeisterschaftEntity e = jpa.findById(meisterschaft.id()).orElseGet(MeisterschaftEntity::new);
        e.id = meisterschaft.id();
        e.name = meisterschaft.name();
        e.punkte.clear();
        e.punkte.addAll(meisterschaft.punkteSchema());
        jpa.save(e);
    }

    @Override
    public void loesche(UUID id) {
        jpa.deleteById(id);
    }

    private static Meisterschaft zuDomain(MeisterschaftEntity e) {
        return new Meisterschaft(e.id, e.name, List.copyOf(e.punkte));
    }
}
