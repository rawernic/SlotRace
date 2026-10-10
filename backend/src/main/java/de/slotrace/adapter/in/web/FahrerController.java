package de.slotrace.adapter.in.web;

import de.slotrace.adapter.in.web.Dtos.FahrerDto;
import de.slotrace.application.port.in.FahrerVerwaltung;
import de.slotrace.application.port.in.FahrerVerwaltung.FahrerDaten;
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
@RequestMapping("/api/fahrer")
class FahrerController {

    private final FahrerVerwaltung verwaltung;

    FahrerController(FahrerVerwaltung verwaltung) {
        this.verwaltung = verwaltung;
    }

    @GetMapping
    List<FahrerDto> alle() {
        return verwaltung.alle().stream().map(DtoMapper::fahrer).toList();
    }

    @GetMapping("/{id}")
    FahrerDto hole(@PathVariable UUID id) {
        return DtoMapper.fahrer(verwaltung.hole(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FahrerDto anlegen(@RequestBody FahrerDaten daten) {
        return DtoMapper.fahrer(verwaltung.anlegen(daten));
    }

    @PutMapping("/{id}")
    FahrerDto aendern(@PathVariable UUID id, @RequestBody FahrerDaten daten) {
        return DtoMapper.fahrer(verwaltung.aendern(id, daten));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void loeschen(@PathVariable UUID id) {
        verwaltung.loeschen(id);
    }
}
