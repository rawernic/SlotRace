package de.slotrace.adapter.in.web;

import de.slotrace.adapter.in.web.Dtos.BelegungAnfrage;
import de.slotrace.adapter.in.web.Dtos.MeldungDto;
import de.slotrace.adapter.in.web.Dtos.RennenAnfrage;
import de.slotrace.adapter.in.web.Dtos.RennenDetailDto;
import de.slotrace.adapter.in.web.Dtos.RennenUebersichtDto;
import de.slotrace.adapter.in.web.Dtos.RundeAnfrage;
import de.slotrace.application.port.in.RennenVerwaltung;
import de.slotrace.application.port.in.Renndurchfuehrung;
import de.slotrace.domain.model.Meldung;
import de.slotrace.domain.model.Rennplanung;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rennen")
class RennenController {

    private static final int STANDARD_MINDEST_RUNDENZEIT_MS = 1000;

    private final RennenVerwaltung verwaltung;
    private final Renndurchfuehrung durchfuehrung;
    private final DtoMapper mapper;

    RennenController(RennenVerwaltung verwaltung, Renndurchfuehrung durchfuehrung, DtoMapper mapper) {
        this.verwaltung = verwaltung;
        this.durchfuehrung = durchfuehrung;
        this.mapper = mapper;
    }

    @GetMapping
    List<RennenUebersichtDto> alle() {
        return mapper.uebersicht(verwaltung.alle());
    }

    @GetMapping("/{id}")
    RennenDetailDto hole(@PathVariable UUID id) {
        return mapper.detail(verwaltung.hole(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RennenDetailDto planen(@RequestBody RennenAnfrage anfrage) {
        return mapper.detail(verwaltung.planen(anfrage.bahnId(), planung(anfrage)));
    }

    @PutMapping("/{id}")
    RennenDetailDto aendern(@PathVariable UUID id, @RequestBody RennenAnfrage anfrage) {
        return mapper.detail(verwaltung.aendern(id, anfrage.bahnId(), planung(anfrage)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void loeschen(@PathVariable UUID id) {
        verwaltung.loeschen(id);
    }

    @PostMapping("/{id}/laeufe/{nummer}/start")
    RennenDetailDto starteLauf(@PathVariable UUID id, @PathVariable int nummer) {
        return mapper.detail(durchfuehrung.starteLauf(id, nummer));
    }

    @PostMapping("/{id}/laeufe/{nummer}/runden")
    RennenDetailDto erfasseRunde(@PathVariable UUID id, @PathVariable int nummer, @RequestBody RundeAnfrage anfrage) {
        return mapper.detail(durchfuehrung.erfasseRunde(id, nummer, anfrage.spur()));
    }

    @DeleteMapping("/{id}/laeufe/{nummer}/runden/letzte")
    RennenDetailDto entferneLetzteRunde(@PathVariable UUID id, @PathVariable int nummer, @RequestParam int spur) {
        return mapper.detail(durchfuehrung.entferneLetzteRunde(id, nummer, spur));
    }

    @PostMapping("/{id}/laeufe/{nummer}/ende")
    RennenDetailDto beendeLauf(@PathVariable UUID id, @PathVariable int nummer) {
        return mapper.detail(durchfuehrung.beendeLauf(id, nummer));
    }

    @PostMapping("/{id}/laeufe/{nummer}/zuruecksetzen")
    RennenDetailDto setzeLaufZurueck(@PathVariable UUID id, @PathVariable int nummer) {
        return mapper.detail(durchfuehrung.setzeLaufZurueck(id, nummer));
    }

    @PostMapping("/{id}/pause")
    RennenDetailDto pausieren(@PathVariable UUID id) {
        return mapper.detail(durchfuehrung.pausieren(id));
    }

    @PostMapping("/{id}/abbruch")
    RennenDetailDto abbrechen(@PathVariable UUID id) {
        return mapper.detail(durchfuehrung.abbrechen(id));
    }

    @PostMapping("/{id}/ende")
    RennenDetailDto beendeTraining(@PathVariable UUID id) {
        return mapper.detail(durchfuehrung.beendeTraining(id));
    }

    @PutMapping("/{id}/spuren/{spur}")
    RennenDetailDto wechsleBelegung(@PathVariable UUID id, @PathVariable int spur, @RequestBody BelegungAnfrage anfrage) {
        return mapper.detail(durchfuehrung.wechsleBelegung(id, spur, anfrage.fahrerId(), anfrage.fahrzeugId()));
    }

    private static Rennplanung planung(RennenAnfrage a) {
        List<Meldung> meldungen = a.teilnehmer() == null ? null
                : a.teilnehmer().stream().map(RennenController::meldung).toList();
        int mindestRundenzeit = a.mindestRundenzeitMs() == null ? STANDARD_MINDEST_RUNDENZEIT_MS : a.mindestRundenzeitMs();
        return new Rennplanung(a.name(), a.meisterschaftId(), a.modus(), a.laufDauerSekunden(), a.rundenZiel(),
                mindestRundenzeit, meldungen);
    }

    private static Meldung meldung(MeldungDto m) {
        return m == null ? null : new Meldung(m.fahrerId(), m.fahrzeugId(), m.spurNummer());
    }
}
