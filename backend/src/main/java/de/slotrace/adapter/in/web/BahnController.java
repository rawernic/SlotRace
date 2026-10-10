package de.slotrace.adapter.in.web;

import de.slotrace.adapter.in.web.Dtos.BahnDto;
import de.slotrace.application.port.in.BahnVerwaltung;
import de.slotrace.application.port.in.BahnVerwaltung.BahnDaten;
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
@RequestMapping("/api/bahnen")
class BahnController {

    private final BahnVerwaltung verwaltung;

    BahnController(BahnVerwaltung verwaltung) {
        this.verwaltung = verwaltung;
    }

    @GetMapping
    List<BahnDto> alle() {
        return verwaltung.alle().stream().map(DtoMapper::bahn).toList();
    }

    @GetMapping("/{id}")
    BahnDto hole(@PathVariable UUID id) {
        return DtoMapper.bahn(verwaltung.hole(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BahnDto anlegen(@RequestBody BahnDaten daten) {
        return DtoMapper.bahn(verwaltung.anlegen(daten));
    }

    @PutMapping("/{id}")
    BahnDto aendern(@PathVariable UUID id, @RequestBody BahnDaten daten) {
        return DtoMapper.bahn(verwaltung.aendern(id, daten));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void loeschen(@PathVariable UUID id) {
        verwaltung.loeschen(id);
    }
}
