package de.slotrace.application.service;

import de.slotrace.application.port.in.FahrzeugVerwaltung;
import de.slotrace.application.port.out.FahrzeugRepository;
import de.slotrace.application.port.out.RennenRepository;
import de.slotrace.domain.NichtGefundenException;
import de.slotrace.domain.RegelVerletzungException;
import de.slotrace.domain.model.Fahrzeug;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FahrzeugService implements FahrzeugVerwaltung {

    private final FahrzeugRepository fahrzeugRepository;
    private final RennenRepository rennenRepository;

    public FahrzeugService(FahrzeugRepository fahrzeugRepository, RennenRepository rennenRepository) {
        this.fahrzeugRepository = fahrzeugRepository;
        this.rennenRepository = rennenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fahrzeug> alle() {
        return fahrzeugRepository.alle().stream()
                .sorted(Comparator.comparing(Fahrzeug::bezeichnung, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Fahrzeug hole(UUID id) {
        return fahrzeugRepository.finde(id).orElseThrow(() -> new NichtGefundenException("Fahrzeug", id));
    }

    @Override
    public Fahrzeug anlegen(FahrzeugDaten daten) {
        Fahrzeug fahrzeug = Fahrzeug.neu(daten.bezeichnung(), daten.hersteller(), daten.massstab());
        fahrzeugRepository.speichere(fahrzeug);
        return fahrzeug;
    }

    @Override
    public Fahrzeug aendern(UUID id, FahrzeugDaten daten) {
        hole(id);
        Fahrzeug fahrzeug = new Fahrzeug(id, daten.bezeichnung(), daten.hersteller(), daten.massstab());
        fahrzeugRepository.speichere(fahrzeug);
        return fahrzeug;
    }

    @Override
    public void loeschen(UUID id) {
        hole(id);
        if (rennenRepository.existiertMitFahrzeug(id)) {
            throw new RegelVerletzungException("Das Fahrzeug ist in einem Rennen gemeldet und kann nicht gelöscht werden");
        }
        fahrzeugRepository.loesche(id);
    }
}
