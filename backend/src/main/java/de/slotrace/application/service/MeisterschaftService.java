package de.slotrace.application.service;

import de.slotrace.application.port.in.MeisterschaftVerwaltung;
import de.slotrace.application.port.out.MeisterschaftRepository;
import de.slotrace.application.port.out.RennenRepository;
import de.slotrace.domain.NichtGefundenException;
import de.slotrace.domain.RegelVerletzungException;
import de.slotrace.domain.model.Meisterschaft;
import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.service.MeisterschaftsPlatz;
import de.slotrace.domain.service.Meisterschaftswertung;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MeisterschaftService implements MeisterschaftVerwaltung {

    private final MeisterschaftRepository meisterschaftRepository;
    private final RennenRepository rennenRepository;

    public MeisterschaftService(MeisterschaftRepository meisterschaftRepository, RennenRepository rennenRepository) {
        this.meisterschaftRepository = meisterschaftRepository;
        this.rennenRepository = rennenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Meisterschaft> alle() {
        return meisterschaftRepository.alle().stream()
                .sorted(Comparator.comparing(Meisterschaft::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Meisterschaft hole(UUID id) {
        return meisterschaftRepository.finde(id).orElseThrow(() -> new NichtGefundenException("Meisterschaft", id));
    }

    @Override
    public Meisterschaft anlegen(MeisterschaftDaten daten) {
        Meisterschaft m = Meisterschaft.neu(daten.name(), daten.punkteSchema());
        meisterschaftRepository.speichere(m);
        return m;
    }

    @Override
    public Meisterschaft aendern(UUID id, MeisterschaftDaten daten) {
        hole(id);
        Meisterschaft m = new Meisterschaft(id, daten.name(), daten.punkteSchema());
        meisterschaftRepository.speichere(m);
        return m;
    }

    @Override
    public void loeschen(UUID id) {
        hole(id);
        if (rennenRepository.existiertMitMeisterschaft(id)) {
            throw new RegelVerletzungException("Die Meisterschaft enthält Rennen und kann nicht gelöscht werden");
        }
        meisterschaftRepository.loesche(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rennen> rennen(UUID id) {
        hole(id);
        return rennenRepository.alleDerMeisterschaft(id).stream()
                .sorted(Comparator.comparing(Rennen::erstelltAm))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeisterschaftsPlatz> wertung(UUID id) {
        return Meisterschaftswertung.berechne(hole(id), rennenRepository.alleDerMeisterschaft(id));
    }
}
