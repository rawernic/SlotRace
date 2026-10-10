package de.slotrace.domain.model;

import static de.slotrace.domain.Pruefung.optionalerText;
import static de.slotrace.domain.Pruefung.pflicht;
import static de.slotrace.domain.Pruefung.pflichtText;

import java.util.UUID;

public record Fahrzeug(UUID id, String bezeichnung, String hersteller, String massstab) {

    public Fahrzeug {
        pflicht(id, "ID");
        bezeichnung = pflichtText(bezeichnung, "Bezeichnung");
        hersteller = optionalerText(hersteller);
        massstab = optionalerText(massstab);
    }

    public static Fahrzeug neu(String bezeichnung, String hersteller, String massstab) {
        return new Fahrzeug(UUID.randomUUID(), bezeichnung, hersteller, massstab);
    }
}
