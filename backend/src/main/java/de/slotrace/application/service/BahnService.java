package de.slotrace.application.service;

import de.slotrace.application.port.in.BahnVerwaltung;
import de.slotrace.application.port.out.BahnRepository;
import de.slotrace.application.port.out.RennenRepository;
import de.slotrace.domain.NichtGefundenException;
import de.slotrace.domain.Pruefung;
import de.slotrace.domain.RegelVerletzungException;
import de.slotrace.domain.model.Bahn;
import de.slotrace.domain.model.Spur;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BahnService implements BahnVerwaltung {

    private final BahnRepository bahnRepository;
    private final RennenRepository rennenRepository;

    public BahnService(BahnRepository bahnRepository, RennenRepository rennenRepository) {
        this.bahnRepository = bahnRepository;
        this.rennenRepository = rennenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bahn> alle() {
        return bahnRepository.alle().stream()
                .sorted(Comparator.comparing(Bahn::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Bahn hole(UUID id) {
        return bahnRepository.finde(id).orElseThrow(() -> new NichtGefundenException("Bahn", id));
    }

    @Override
    public Bahn anlegen(BahnDaten daten) {
        Bahn bahn = Bahn.neu(daten.name(), spuren(daten));
        bahnRepository.speichere(bahn);
        return bahn;
    }

    @Override
    public Bahn aendern(UUID id, BahnDaten daten) {
        Bahn alt = hole(id);
        Bahn neu = new Bahn(id, daten.name(), spuren(daten));
        if (neu.spurAnzahl() != alt.spurAnzahl() && rennenRepository.existiertMitBahn(id)) {
            throw new RegelVerletzungException("Die Spuranzahl einer Bahn mit Rennen kann nicht geändert werden");
        }
        bahnRepository.speichere(neu);
        return neu;
    }

    @Override
    public void loeschen(UUID id) {
        hole(id);
        if (rennenRepository.existiertMitBahn(id)) {
            throw new RegelVerletzungException("Auf der Bahn wurden Rennen angelegt; sie kann nicht gelöscht werden");
        }
        bahnRepository.loesche(id);
    }

    private static List<Spur> spuren(BahnDaten daten) {
        List<String> farben = Pruefung.pflicht(daten.spurFarben(), "Spuren");
        List<Spur> spuren = new ArrayList<>();
        for (int i = 0; i < farben.size(); i++) {
            spuren.add(new Spur(i + 1, farben.get(i)));
        }
        return spuren;
    }
}
