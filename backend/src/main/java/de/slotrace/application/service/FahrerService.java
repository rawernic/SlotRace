package de.slotrace.application.service;

import de.slotrace.application.port.in.FahrerVerwaltung;
import de.slotrace.application.port.out.FahrerRepository;
import de.slotrace.application.port.out.RennenRepository;
import de.slotrace.domain.NichtGefundenException;
import de.slotrace.domain.RegelVerletzungException;
import de.slotrace.domain.model.Fahrer;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FahrerService implements FahrerVerwaltung {

    private final FahrerRepository fahrerRepository;
    private final RennenRepository rennenRepository;

    public FahrerService(FahrerRepository fahrerRepository, RennenRepository rennenRepository) {
        this.fahrerRepository = fahrerRepository;
        this.rennenRepository = rennenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fahrer> alle() {
        return fahrerRepository.alle().stream()
                .sorted(Comparator.comparing(Fahrer::nachname, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Fahrer::vorname, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Fahrer hole(UUID id) {
        return fahrerRepository.finde(id).orElseThrow(() -> new NichtGefundenException("Fahrer", id));
    }

    @Override
    public Fahrer anlegen(FahrerDaten daten) {
        Fahrer fahrer = Fahrer.neu(daten.vorname(), daten.nachname(), daten.spitzname());
        fahrerRepository.speichere(fahrer);
        return fahrer;
    }

    @Override
    public Fahrer aendern(UUID id, FahrerDaten daten) {
        hole(id);
        Fahrer fahrer = new Fahrer(id, daten.vorname(), daten.nachname(), daten.spitzname());
        fahrerRepository.speichere(fahrer);
        return fahrer;
    }

    @Override
    public void loeschen(UUID id) {
        hole(id);
        if (rennenRepository.existiertMitFahrer(id)) {
            throw new RegelVerletzungException("Der Fahrer ist in einem Rennen gemeldet und kann nicht gelöscht werden");
        }
        fahrerRepository.loesche(id);
    }
}
