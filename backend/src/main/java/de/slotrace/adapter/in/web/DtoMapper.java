package de.slotrace.adapter.in.web;

import de.slotrace.adapter.in.web.Dtos.BahnDto;
import de.slotrace.adapter.in.web.Dtos.FahrerDto;
import de.slotrace.adapter.in.web.Dtos.FahrzeugDto;
import de.slotrace.adapter.in.web.Dtos.LaufDto;
import de.slotrace.adapter.in.web.Dtos.MeisterschaftDto;
import de.slotrace.adapter.in.web.Dtos.MeisterschaftsPlatzDto;
import de.slotrace.adapter.in.web.Dtos.PlatzierungDto;
import de.slotrace.adapter.in.web.Dtos.RennenDetailDto;
import de.slotrace.adapter.in.web.Dtos.RennenUebersichtDto;
import de.slotrace.adapter.in.web.Dtos.SpurDto;
import de.slotrace.adapter.in.web.Dtos.SpurStandDto;
import de.slotrace.adapter.in.web.Dtos.TeilnehmerDto;
import de.slotrace.application.port.in.BahnVerwaltung;
import de.slotrace.application.port.in.FahrerVerwaltung;
import de.slotrace.application.port.in.FahrzeugVerwaltung;
import de.slotrace.application.port.in.MeisterschaftVerwaltung;
import de.slotrace.domain.model.Bahn;
import de.slotrace.domain.model.Fahrer;
import de.slotrace.domain.model.Fahrzeug;
import de.slotrace.domain.model.Lauf;
import de.slotrace.domain.model.Laufstatus;
import de.slotrace.domain.model.Meisterschaft;
import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.model.Rennmodus;
import de.slotrace.domain.model.Teilnehmer;
import de.slotrace.domain.service.Auswertung;
import de.slotrace.domain.service.MeisterschaftsPlatz;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Wandelt Domänenobjekte in API-Objekte um und löst dabei Namen von Fahrern, Fahrzeugen usw. auf. */
@Component
class DtoMapper {

    private final FahrerVerwaltung fahrerVerwaltung;
    private final FahrzeugVerwaltung fahrzeugVerwaltung;
    private final BahnVerwaltung bahnVerwaltung;
    private final MeisterschaftVerwaltung meisterschaftVerwaltung;
    private final Clock uhr;

    DtoMapper(FahrerVerwaltung fahrerVerwaltung, FahrzeugVerwaltung fahrzeugVerwaltung, BahnVerwaltung bahnVerwaltung,
              MeisterschaftVerwaltung meisterschaftVerwaltung, Clock uhr) {
        this.fahrerVerwaltung = fahrerVerwaltung;
        this.fahrzeugVerwaltung = fahrzeugVerwaltung;
        this.bahnVerwaltung = bahnVerwaltung;
        this.meisterschaftVerwaltung = meisterschaftVerwaltung;
        this.uhr = uhr;
    }

    static FahrerDto fahrer(Fahrer f) {
        return new FahrerDto(f.id(), f.vorname(), f.nachname(), f.spitzname(), f.anzeigename());
    }

    static FahrzeugDto fahrzeug(Fahrzeug f) {
        return new FahrzeugDto(f.id(), f.bezeichnung(), f.hersteller(), f.massstab());
    }

    static BahnDto bahn(Bahn b) {
        return new BahnDto(b.id(), b.name(), b.spuren().stream().map(s -> new SpurDto(s.nummer(), s.farbe())).toList());
    }

    static MeisterschaftDto meisterschaft(Meisterschaft m) {
        return new MeisterschaftDto(m.id(), m.name(), m.punkteSchema());
    }

    List<MeisterschaftsPlatzDto> wertung(List<MeisterschaftsPlatz> wertung) {
        Map<UUID, Fahrer> fahrer = fahrerNachId();
        return wertung.stream()
                .map(p -> new MeisterschaftsPlatzDto(p.platz(), p.fahrerId(), name(fahrer.get(p.fahrerId())),
                        p.punkte(), p.siege(), p.rennen()))
                .toList();
    }

    List<RennenUebersichtDto> uebersicht(List<Rennen> rennen) {
        Map<UUID, Bahn> bahnen = bahnVerwaltung.alle().stream().collect(Collectors.toMap(Bahn::id, Function.identity()));
        return rennen.stream().map(r -> new RennenUebersichtDto(r.id(), r.name(), r.bahnId(),
                bahnen.containsKey(r.bahnId()) ? bahnen.get(r.bahnId()).name() : null, r.meisterschaftId(),
                r.modus(), r.laufDauerSekunden(), r.rundenZiel(), r.status(), r.erstelltAm(), r.teilnehmer().size(),
                r.laeufe().size(),
                (int) r.laeufe().stream().filter(l -> l.status() == Laufstatus.BEENDET).count())).toList();
    }

    RennenDetailDto detail(Rennen r) {
        Map<UUID, Fahrer> fahrer = fahrerNachId();
        Map<UUID, Fahrzeug> fahrzeuge = fahrzeugVerwaltung.alle().stream()
                .collect(Collectors.toMap(Fahrzeug::id, Function.identity()));
        Bahn bahn = bahnVerwaltung.hole(r.bahnId());
        String meisterschaftName = r.meisterschaftId() == null ? null
                : meisterschaftVerwaltung.hole(r.meisterschaftId()).name();
        Map<UUID, Teilnehmer> teilnehmer = r.teilnehmer().stream()
                .collect(Collectors.toMap(Teilnehmer::id, Function.identity()));
        Function<UUID, String> fahrerName = tid -> name(fahrer.get(teilnehmer.get(tid).fahrerId()));
        Function<UUID, String> fahrzeugName = tid -> {
            Fahrzeug fz = fahrzeuge.get(teilnehmer.get(tid).fahrzeugId());
            return fz == null ? "?" : fz.bezeichnung();
        };

        List<TeilnehmerDto> teilnehmerDtos = r.teilnehmer().stream()
                .map(t -> new TeilnehmerDto(t.id(), t.startnummer(), t.fahrerId(), fahrerName.apply(t.id()),
                        t.fahrzeugId(), fahrzeugName.apply(t.id())))
                .toList();

        List<LaufDto> laeufe = r.laeufe().stream().map(l -> new LaufDto(l.nummer(), l.status(), l.gestartetAm(),
                l.beendetAm(), geplantesEnde(r, l),
                Auswertung.laufstand(l).stream().map(s -> new SpurStandDto(s.spurNummer(),
                        bahn.spuren().get(s.spurNummer() - 1).farbe(), s.teilnehmerId(),
                        fahrerName.apply(s.teilnehmerId()), fahrzeugName.apply(s.teilnehmerId()), s.position(),
                        s.runden(), s.zeitMs(), s.letzteRundeMs(), s.besteRundeMs())).toList())).toList();

        Integer aktuellerLauf = r.laufendeLauf().map(Lauf::nummer)
                .or(() -> r.laeufe().stream().filter(l -> l.status() == Laufstatus.BEREIT).map(Lauf::nummer).findFirst())
                .orElse(null);

        List<PlatzierungDto> ergebnis = Auswertung.rennergebnis(r).stream()
                .map(p -> new PlatzierungDto(p.platz(), p.teilnehmerId(), teilnehmer.get(p.teilnehmerId()).startnummer(),
                        fahrerName.apply(p.teilnehmerId()), fahrzeugName.apply(p.teilnehmerId()), p.runden(),
                        p.gesamtzeitMs(), p.besteRundeMs(), p.gefahreneLaeufe()))
                .toList();

        return new RennenDetailDto(r.id(), r.name(), bahn(bahn), r.meisterschaftId(), meisterschaftName, r.modus(),
                r.laufDauerSekunden(), r.rundenZiel(), r.mindestRundenzeitMs(), r.status(), r.erstelltAm(),
                uhr.instant(), teilnehmerDtos, laeufe, aktuellerLauf, ergebnis);
    }

    private static Instant geplantesEnde(Rennen r, Lauf l) {
        if (r.modus() != Rennmodus.ZEITRENNEN || l.gestartetAm() == null) {
            return null;
        }
        return l.gestartetAm().plusSeconds(r.laufDauerSekunden());
    }

    private Map<UUID, Fahrer> fahrerNachId() {
        return fahrerVerwaltung.alle().stream().collect(Collectors.toMap(Fahrer::id, Function.identity()));
    }

    private static String name(Fahrer f) {
        return f == null ? "?" : f.anzeigename();
    }
}
