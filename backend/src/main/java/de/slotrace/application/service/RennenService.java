package de.slotrace.application.service;

import de.slotrace.application.port.in.RennenVerwaltung;
import de.slotrace.application.port.in.Renndurchfuehrung;
import de.slotrace.application.port.out.BahnRepository;
import de.slotrace.application.port.out.FahrerRepository;
import de.slotrace.application.port.out.FahrzeugRepository;
import de.slotrace.application.port.out.MeisterschaftRepository;
import de.slotrace.application.port.out.RennenRepository;
import de.slotrace.domain.NichtGefundenException;
import de.slotrace.domain.Pruefung;
import de.slotrace.domain.model.Bahn;
import de.slotrace.domain.model.Meldung;
import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.model.Rennplanung;
import de.slotrace.domain.model.Rennstatus;
import de.slotrace.domain.service.Auswertung;
import de.slotrace.domain.service.Platzierung;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RennenService implements RennenVerwaltung, Renndurchfuehrung {

    private final RennenRepository rennenRepository;
    private final BahnRepository bahnRepository;
    private final FahrerRepository fahrerRepository;
    private final FahrzeugRepository fahrzeugRepository;
    private final MeisterschaftRepository meisterschaftRepository;
    private final Clock uhr;

    public RennenService(RennenRepository rennenRepository, BahnRepository bahnRepository,
                         FahrerRepository fahrerRepository, FahrzeugRepository fahrzeugRepository,
                         MeisterschaftRepository meisterschaftRepository, Clock uhr) {
        this.rennenRepository = rennenRepository;
        this.bahnRepository = bahnRepository;
        this.fahrerRepository = fahrerRepository;
        this.fahrzeugRepository = fahrzeugRepository;
        this.meisterschaftRepository = meisterschaftRepository;
        this.uhr = uhr;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rennen> alle() {
        return rennenRepository.alle().stream()
                .sorted(Comparator.comparing(Rennen::erstelltAm).reversed())
                .toList();
    }

    @Override
    public Rennen hole(UUID id) {
        Rennen rennen = ladenZurBearbeitung(id);
        if (rennen.pruefeZeitablauf(jetzt())) {
            rennenRepository.speichere(rennen);
        }
        return rennen;
    }

    @Override
    public Rennen planen(UUID bahnId, Rennplanung planung) {
        Rennen rennen = Rennen.planen(planung, pruefeReferenzen(bahnId, planung), jetzt());
        rennenRepository.speichere(rennen);
        return rennen;
    }

    @Override
    public Rennen aendern(UUID id, UUID bahnId, Rennplanung planung) {
        Rennen rennen = ladenZurBearbeitung(id);
        rennen.aendern(planung, pruefeReferenzen(bahnId, planung));
        rennenRepository.speichere(rennen);
        return rennen;
    }

    @Override
    public void loeschen(UUID id) {
        laden(id);
        rennenRepository.loesche(id);
    }

    @Override
    public List<Platzierung> ergebnis(UUID id) {
        return Auswertung.rennergebnis(hole(id));
    }

    @Override
    public Rennen starteLauf(UUID rennenId, int laufNummer) {
        Instant zeitpunkt = jetzt();
        return veraendere(rennenId, r -> {
            pruefeBahnFrei(r, zeitpunkt);
            r.starteLauf(laufNummer, zeitpunkt);
        });
    }

    @Override
    public Rennen erfasseRunde(UUID rennenId, int laufNummer, int spurNummer) {
        Instant zeitpunkt = jetzt();
        return veraendere(rennenId, r -> r.erfasseRunde(laufNummer, spurNummer, zeitpunkt));
    }

    @Override
    public Rennen entferneLetzteRunde(UUID rennenId, int laufNummer, int spurNummer) {
        return veraendere(rennenId, r -> r.entferneLetzteRunde(laufNummer, spurNummer));
    }

    @Override
    public Rennen beendeLauf(UUID rennenId, int laufNummer) {
        return veraendere(rennenId, r -> r.beendeLauf(laufNummer, jetzt()));
    }

    @Override
    public Rennen setzeLaufZurueck(UUID rennenId, int laufNummer) {
        return veraendere(rennenId, r -> r.setzeLaufZurueck(laufNummer));
    }

    @Override
    public Rennen pausieren(UUID rennenId) {
        return veraendere(rennenId, r -> r.pausieren(jetzt()));
    }

    @Override
    public Rennen abbrechen(UUID rennenId) {
        return veraendere(rennenId, r -> r.abbrechen(jetzt()));
    }

    @Override
    public Rennen beendeTraining(UUID rennenId) {
        return veraendere(rennenId, r -> r.beendeTraining(jetzt()));
    }

    @Override
    public Rennen wechsleBelegung(UUID rennenId, int spurNummer, UUID fahrerId, UUID fahrzeugId) {
        if (fahrerId != null && fahrerRepository.finde(fahrerId).isEmpty()) {
            throw new NichtGefundenException("Fahrer", fahrerId);
        }
        if (fahrzeugId != null && fahrzeugRepository.finde(fahrzeugId).isEmpty()) {
            throw new NichtGefundenException("Fahrzeug", fahrzeugId);
        }
        return veraendere(rennenId, r -> {
            Bahn bahn = bahnRepository.finde(r.bahnId()).orElseThrow(() -> new NichtGefundenException("Bahn", r.bahnId()));
            r.wechsleBelegung(spurNummer, fahrerId, fahrzeugId, bahn.spurAnzahl(), jetzt());
        });
    }

    /** Auf einer Bahn kann immer nur ein Rennen gleichzeitig laufen. */
    private void pruefeBahnFrei(Rennen rennen, Instant zeitpunkt) {
        for (Rennen anderes : rennenRepository.alleAufBahnMitStatus(rennen.bahnId(), Rennstatus.LAEUFT)) {
            if (anderes.id().equals(rennen.id())) {
                continue;
            }
            anderes.pruefeZeitablauf(zeitpunkt);
            Pruefung.regel(anderes.status() != Rennstatus.LAEUFT, "Auf dieser Bahn läuft bereits „" + anderes.name()
                    + "“ – bitte zuerst pausieren, beenden oder abbrechen");
        }
    }

    private Rennen veraendere(UUID rennenId, Consumer<Rennen> aenderung) {
        Rennen rennen = ladenZurBearbeitung(rennenId);
        aenderung.accept(rennen);
        rennenRepository.speichere(rennen);
        return rennen;
    }

    private Bahn pruefeReferenzen(UUID bahnId, Rennplanung planung) {
        Pruefung.pflicht(bahnId, "Bahn");
        Bahn bahn = bahnRepository.finde(bahnId).orElseThrow(() -> new NichtGefundenException("Bahn", bahnId));
        if (planung.meisterschaftId() != null && meisterschaftRepository.finde(planung.meisterschaftId()).isEmpty()) {
            throw new NichtGefundenException("Meisterschaft", planung.meisterschaftId());
        }
        if (planung.meldungen() != null) {
            for (Meldung m : planung.meldungen()) {
                if (m.fahrerId() != null && fahrerRepository.finde(m.fahrerId()).isEmpty()) {
                    throw new NichtGefundenException("Fahrer", m.fahrerId());
                }
                if (m.fahrzeugId() != null && fahrzeugRepository.finde(m.fahrzeugId()).isEmpty()) {
                    throw new NichtGefundenException("Fahrzeug", m.fahrzeugId());
                }
            }
        }
        return bahn;
    }

    private Rennen laden(UUID id) {
        return rennenRepository.finde(id).orElseThrow(() -> new NichtGefundenException("Rennen", id));
    }

    private Rennen ladenZurBearbeitung(UUID id) {
        return rennenRepository.findeZurBearbeitung(id).orElseThrow(() -> new NichtGefundenException("Rennen", id));
    }

    private Instant jetzt() {
        return uhr.instant();
    }
}
