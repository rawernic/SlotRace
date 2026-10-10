package de.slotrace.domain;

import de.slotrace.domain.model.Bahn;
import de.slotrace.domain.model.Meldung;
import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.model.Rennmodus;
import de.slotrace.domain.model.Rennplanung;
import de.slotrace.domain.model.Spur;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TestDaten {

    public static final Instant START = Instant.parse("2026-01-01T10:00:00Z");

    private TestDaten() {
    }

    public static Bahn bahn(int spuren) {
        List<Spur> liste = new ArrayList<>();
        for (int i = 1; i <= spuren; i++) {
            liste.add(new Spur(i, "#00000" + i));
        }
        return Bahn.neu("Testbahn", liste);
    }

    public static List<Meldung> meldungen(int anzahl) {
        List<Meldung> liste = new ArrayList<>();
        for (int i = 0; i < anzahl; i++) {
            liste.add(new Meldung(UUID.randomUUID(), UUID.randomUUID()));
        }
        return liste;
    }

    public static Rennen zeitrennen(int teilnehmer, int spuren, int sekunden, int mindestRundenzeitMs) {
        return Rennen.planen(new Rennplanung("Zeitrennen", null, Rennmodus.ZEITRENNEN, sekunden, null,
                mindestRundenzeitMs, meldungen(teilnehmer)), bahn(spuren), START);
    }

    public static Rennen rundenrennen(int teilnehmer, int spuren, int runden) {
        return Rennen.planen(new Rennplanung("Rundenrennen", null, Rennmodus.RUNDENRENNEN, null, runden,
                0, meldungen(teilnehmer)), bahn(spuren), START);
    }

    public static Instant plusMs(long ms) {
        return START.plusMillis(ms);
    }
}
