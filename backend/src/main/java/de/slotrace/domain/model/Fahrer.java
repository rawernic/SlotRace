package de.slotrace.domain.model;

import static de.slotrace.domain.Pruefung.optionalerText;
import static de.slotrace.domain.Pruefung.pflicht;
import static de.slotrace.domain.Pruefung.pflichtText;

import java.util.UUID;

public record Fahrer(UUID id, String vorname, String nachname, String spitzname) {

    public Fahrer {
        pflicht(id, "ID");
        vorname = pflichtText(vorname, "Vorname");
        nachname = pflichtText(nachname, "Nachname");
        spitzname = optionalerText(spitzname);
    }

    public static Fahrer neu(String vorname, String nachname, String spitzname) {
        return new Fahrer(UUID.randomUUID(), vorname, nachname, spitzname);
    }

    public String anzeigename() {
        return spitzname != null ? spitzname : vorname + " " + nachname;
    }
}
