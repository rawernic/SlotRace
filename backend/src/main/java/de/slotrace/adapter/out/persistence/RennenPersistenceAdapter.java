package de.slotrace.adapter.out.persistence;

import de.slotrace.application.port.out.RennenRepository;
import de.slotrace.domain.model.Belegung;
import de.slotrace.domain.model.Lauf;
import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.model.Rennstatus;
import de.slotrace.domain.model.Runde;
import de.slotrace.domain.model.Teilnehmer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Speichert das Rennen-Aggregat. Statt das gesamte Objektgeflecht neu zu schreiben, werden
 * Teilnehmer, Läufe und Runden anhand ihrer IDs mit dem gespeicherten Stand abgeglichen,
 * sodass beim Erfassen einer Runde nur eine Zeile eingefügt wird.
 */
@Component
class RennenPersistenceAdapter implements RennenRepository {

    private final RennenJpaRepository jpa;

    RennenPersistenceAdapter(RennenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Rennen> alle() {
        return jpa.findAll().stream().map(RennenPersistenceAdapter::zuDomain).toList();
    }

    @Override
    public List<Rennen> alleDerMeisterschaft(UUID meisterschaftId) {
        return jpa.findByMeisterschaftId(meisterschaftId).stream().map(RennenPersistenceAdapter::zuDomain).toList();
    }

    @Override
    public Optional<Rennen> finde(UUID id) {
        return jpa.findById(id).map(RennenPersistenceAdapter::zuDomain);
    }

    @Override
    public Optional<Rennen> findeZurBearbeitung(UUID id) {
        return jpa.findByIdZumSchreiben(id).map(RennenPersistenceAdapter::zuDomain);
    }

    @Override
    public List<Rennen> alleAufBahnMitStatus(UUID bahnId, Rennstatus status) {
        return jpa.findByBahnIdAndStatus(bahnId, status).stream().map(RennenPersistenceAdapter::zuDomain).toList();
    }

    @Override
    public void speichere(Rennen rennen) {
        RennenEntity e = jpa.findById(rennen.id()).orElseGet(() -> {
            RennenEntity neu = new RennenEntity();
            neu.id = rennen.id();
            return neu;
        });
        e.name = rennen.name();
        e.bahnId = rennen.bahnId();
        e.meisterschaftId = rennen.meisterschaftId();
        e.modus = rennen.modus();
        e.laufDauerSekunden = rennen.laufDauerSekunden();
        e.rundenZiel = rennen.rundenZiel();
        e.mindestRundenzeitMs = rennen.mindestRundenzeitMs();
        e.status = rennen.status();
        e.erstelltAm = rennen.erstelltAm();
        abgleichen(e.teilnehmer, rennen.teilnehmer(), t -> t.id, Teilnehmer::id,
                RennenPersistenceAdapter::neuerTeilnehmer, (t, te) -> te.startnummer = t.startnummer());
        abgleichen(e.laeufe, rennen.laeufe(), l -> l.id, Lauf::id,
                RennenPersistenceAdapter::neuerLauf, RennenPersistenceAdapter::aktualisiereLauf);
        jpa.save(e);
    }

    @Override
    public void loesche(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existiertMitFahrer(UUID fahrerId) {
        return jpa.existiertTeilnehmerMitFahrer(fahrerId);
    }

    @Override
    public boolean existiertMitFahrzeug(UUID fahrzeugId) {
        return jpa.existiertTeilnehmerMitFahrzeug(fahrzeugId);
    }

    @Override
    public boolean existiertMitBahn(UUID bahnId) {
        return jpa.existsByBahnId(bahnId);
    }

    @Override
    public boolean existiertMitMeisterschaft(UUID meisterschaftId) {
        return jpa.existsByMeisterschaftId(meisterschaftId);
    }

    private static <E, D> void abgleichen(List<E> entities, List<D> domain, Function<E, UUID> entityId,
                                          Function<D, UUID> domainId, Function<D, E> erzeuge,
                                          java.util.function.BiConsumer<D, E> aktualisiere) {
        Set<UUID> sollIds = domain.stream().map(domainId).collect(Collectors.toSet());
        entities.removeIf(e -> !sollIds.contains(entityId.apply(e)));
        Map<UUID, E> vorhanden = entities.stream().collect(Collectors.toMap(entityId, Function.identity()));
        for (D d : domain) {
            E e = vorhanden.get(domainId.apply(d));
            if (e == null) {
                entities.add(erzeuge.apply(d));
            } else {
                aktualisiere.accept(d, e);
            }
        }
    }

    private static TeilnehmerEntity neuerTeilnehmer(Teilnehmer t) {
        TeilnehmerEntity e = new TeilnehmerEntity();
        e.id = t.id();
        e.fahrerId = t.fahrerId();
        e.fahrzeugId = t.fahrzeugId();
        e.startnummer = t.startnummer();
        return e;
    }

    private static LaufEntity neuerLauf(Lauf l) {
        LaufEntity e = new LaufEntity();
        e.id = l.id();
        e.nummer = l.nummer();
        aktualisiereLauf(l, e);
        return e;
    }

    private static void aktualisiereLauf(Lauf l, LaufEntity e) {
        e.status = l.status();
        e.gestartetAm = l.gestartetAm();
        e.beendetAm = l.beendetAm();
        List<LaufEntity.BelegungEmbeddable> belegungen = l.belegungen().stream()
                .map(b -> new LaufEntity.BelegungEmbeddable(b.spurNummer(), b.teilnehmerId(), b.belegtSeit()))
                .toList();
        if (!belegungen.equals(e.belegungen)) {
            e.belegungen.clear();
            e.belegungen.addAll(belegungen);
        }
        abgleichen(e.runden, l.runden(), r -> r.id, Runde::id, RennenPersistenceAdapter::neueRunde, (r, re) -> { });
    }

    private static RundeEntity neueRunde(Runde r) {
        RundeEntity e = new RundeEntity();
        e.id = r.id();
        e.teilnehmerId = r.teilnehmerId();
        e.spurNummer = r.spurNummer();
        e.nummer = r.nummer();
        e.zeitpunkt = r.zeitpunkt();
        e.rundenzeitMs = r.rundenzeitMs();
        return e;
    }

    private static Rennen zuDomain(RennenEntity e) {
        List<Teilnehmer> teilnehmer = e.teilnehmer.stream()
                .map(t -> new Teilnehmer(t.id, t.fahrerId, t.fahrzeugId, t.startnummer))
                .toList();
        List<Lauf> laeufe = new ArrayList<>();
        for (LaufEntity l : e.laeufe) {
            List<Belegung> belegungen = l.belegungen.stream()
                    .map(b -> new Belegung(b.spurNummer, b.teilnehmerId, b.belegtSeit))
                    .toList();
            List<Runde> runden = l.runden.stream()
                    .map(r -> new Runde(r.id, r.teilnehmerId, r.spurNummer, r.nummer, r.zeitpunkt, r.rundenzeitMs))
                    .toList();
            laeufe.add(new Lauf(l.id, l.nummer, belegungen, l.status, l.gestartetAm, l.beendetAm, runden));
        }
        return new Rennen(e.id, e.erstelltAm, e.name, e.bahnId, e.meisterschaftId, e.modus, e.laufDauerSekunden,
                e.rundenZiel, e.mindestRundenzeitMs, e.status, teilnehmer, laeufe);
    }
}
