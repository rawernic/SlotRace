package de.slotrace.adapter.in.web;

import de.slotrace.adapter.in.web.Dtos.MeisterschaftDto;
import de.slotrace.adapter.in.web.Dtos.MeisterschaftsPlatzDto;
import de.slotrace.adapter.in.web.Dtos.RennenUebersichtDto;
import de.slotrace.application.port.in.MeisterschaftVerwaltung;
import de.slotrace.application.port.in.MeisterschaftVerwaltung.MeisterschaftDaten;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meisterschaften")
class MeisterschaftController {

    private final MeisterschaftVerwaltung verwaltung;
    private final DtoMapper mapper;

    MeisterschaftController(MeisterschaftVerwaltung verwaltung, DtoMapper mapper) {
        this.verwaltung = verwaltung;
        this.mapper = mapper;
    }

    @GetMapping
    List<MeisterschaftDto> alle() {
        return verwaltung.alle().stream().map(DtoMapper::meisterschaft).toList();
    }

    @GetMapping("/{id}")
    MeisterschaftDto hole(@PathVariable UUID id) {
        return DtoMapper.meisterschaft(verwaltung.hole(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MeisterschaftDto anlegen(@RequestBody MeisterschaftDaten daten) {
        return DtoMapper.meisterschaft(verwaltung.anlegen(daten));
    }

    @PutMapping("/{id}")
    MeisterschaftDto aendern(@PathVariable UUID id, @RequestBody MeisterschaftDaten daten) {
        return DtoMapper.meisterschaft(verwaltung.aendern(id, daten));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void loeschen(@PathVariable UUID id) {
        verwaltung.loeschen(id);
    }

    @GetMapping("/{id}/rennen")
    List<RennenUebersichtDto> rennen(@PathVariable UUID id) {
        return mapper.uebersicht(verwaltung.rennen(id));
    }

    @GetMapping("/{id}/wertung")
    List<MeisterschaftsPlatzDto> wertung(@PathVariable UUID id) {
        return mapper.wertung(verwaltung.wertung(id));
    }
}
