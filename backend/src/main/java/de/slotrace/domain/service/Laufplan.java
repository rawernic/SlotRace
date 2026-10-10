package de.slotrace.domain.service;

import de.slotrace.domain.model.Belegung;
import de.slotrace.domain.model.Teilnehmer;
import java.util.ArrayList;
import java.util.List;

/**
 * Erzeugt einen Laufplan mit Spurwechsel: Jeder Teilnehmer fährt genau einmal auf jeder Spur.
 *
 * <p>Die Teilnehmer werden (ggf. mit leeren Plätzen aufgefüllt) auf {@code P = max(Teilnehmer, Spuren)}
 * Positionen rotiert. In Lauf {@code h} fährt auf Spur {@code s} die Position {@code (h + s) mod P}.
 * Damit ergeben sich {@code P} Läufe, in denen jeder Teilnehmer jede Spur genau einmal belegt.
 */
public final class Laufplan {

    private Laufplan() {
    }

    public static List<List<Belegung>> erzeuge(List<Teilnehmer> teilnehmer, int spurAnzahl) {
        if (teilnehmer.isEmpty() || spurAnzahl < 1) {
            return List.of();
        }
        int positionen = Math.max(teilnehmer.size(), spurAnzahl);
        List<List<Belegung>> laeufe = new ArrayList<>();
        for (int lauf = 0; lauf < positionen; lauf++) {
            List<Belegung> belegungen = new ArrayList<>();
            for (int spur = 0; spur < spurAnzahl; spur++) {
                int position = (lauf + spur) % positionen;
                if (position < teilnehmer.size()) {
                    belegungen.add(new Belegung(spur + 1, teilnehmer.get(position).id()));
                }
            }
            laeufe.add(List.copyOf(belegungen));
        }
        return List.copyOf(laeufe);
    }
}
