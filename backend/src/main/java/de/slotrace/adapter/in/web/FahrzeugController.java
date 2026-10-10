package de.slotrace.adapter.in.web;

import de.slotrace.adapter.in.web.Dtos.FahrzeugDto;
import de.slotrace.application.port.in.FahrzeugVerwaltung;
import de.slotrace.application.port.in.FahrzeugVerwaltung.FahrzeugDaten;
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
@RequestMapping("/api/fahrzeuge")
class FahrzeugController {

    private final FahrzeugVerwaltung verwaltung;

    FahrzeugController(FahrzeugVerwaltung verwaltung) {
        this.verwaltung = verwaltung;
    }

    @GetMapping
    List<FahrzeugDto> alle() {
        return verwaltung.alle().stream().map(DtoMapper::fahrzeug).toList();
    }

    @GetMapping("/{id}")
    FahrzeugDto hole(@PathVariable UUID id) {
        return DtoMapper.fahrzeug(verwaltung.hole(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FahrzeugDto anlegen(@RequestBody FahrzeugDaten daten) {
        return DtoMapper.fahrzeug(verwaltung.anlegen(daten));
    }

    @PutMapping("/{id}")
    FahrzeugDto aendern(@PathVariable UUID id, @RequestBody FahrzeugDaten daten) {
        return DtoMapper.fahrzeug(verwaltung.aendern(id, daten));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void loeschen(@PathVariable UUID id) {
        verwaltung.loeschen(id);
    }
}
