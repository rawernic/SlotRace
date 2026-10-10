package de.slotrace.adapter.out.persistence;

import de.slotrace.application.port.out.BahnRepository;
import de.slotrace.domain.model.Bahn;
import de.slotrace.domain.model.Spur;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class BahnPersistenceAdapter implements BahnRepository {

    private final BahnJpaRepository jpa;

    BahnPersistenceAdapter(BahnJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Bahn> alle() {
        return jpa.findAll().stream().map(BahnPersistenceAdapter::zuDomain).toList();
    }

    @Override
    public Optional<Bahn> finde(UUID id) {
        return jpa.findById(id).map(BahnPersistenceAdapter::zuDomain);
    }

    @Override
    public void speichere(Bahn bahn) {
        BahnEntity e = jpa.findById(bahn.id()).orElseGet(BahnEntity::new);
        e.id = bahn.id();
        e.name = bahn.name();
        e.spuren.clear();
        bahn.spuren().forEach(s -> e.spuren.add(new BahnEntity.SpurEmbeddable(s.nummer(), s.farbe())));
        jpa.save(e);
    }

    @Override
    public void loesche(UUID id) {
        jpa.deleteById(id);
    }

    private static Bahn zuDomain(BahnEntity e) {
        return new Bahn(e.id, e.name, e.spuren.stream().map(s -> new Spur(s.nummer, s.farbe)).toList());
    }
}
