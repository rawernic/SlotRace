package de.slotrace.domain.service;

import de.slotrace.domain.model.Belegung;
import de.slotrace.domain.model.Lauf;
import de.slotrace.domain.model.Laufstatus;
import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.model.Runde;
import de.slotrace.domain.model.Teilnehmer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Wertet Rennen und Läufe aus. Für beide Rennmodi gilt dieselbe Rangfolge:
 * mehr Runden vor weniger Runden, bei Gleichstand die kürzere gefahrene Zeit.
 * Im Rundenrennen haben alle Zielankömmlinge dieselbe Rundenzahl, sodass die Zeit entscheidet.
 */
public final class Auswertung {

    private Auswertung() {
    }

    public static List<Platzierung> rennergebnis(Rennen rennen) {
        List<Wertung> wertungen = new ArrayList<>();
        for (Teilnehmer t : rennen.teilnehmer()) {
            int runden = 0;
            long zeit = 0;
            Long beste = null;
            int laeufe = 0;
            for (Lauf lauf : rennen.laeufe()) {
                if (lauf.status() == Laufstatus.BEREIT || !lauf.faehrtMit(t.id())) {
                    continue;
                }
                laeufe++;
                List<Runde> eigene = lauf.rundenVon(t.id());
                runden += eigene.size();
                zeit += lauf.gefahreneZeitMs(t.id());
                beste = min(beste, besteRunde(eigene));
            }
            wertungen.add(new Wertung(t.id(), t.startnummer(), runden, zeit, beste, laeufe));
        }
        wertungen.sort(Comparator.comparingInt(Wertung::runden).reversed()
                .thenComparingLong(Wertung::zeit)
                .thenComparingInt(Wertung::startnummer));

        List<Platzierung> ergebnis = new ArrayList<>();
        for (int i = 0; i < wertungen.size(); i++) {
            Wertung w = wertungen.get(i);
            int platz = i + 1;
            if (i > 0 && gleichauf(w, wertungen.get(i - 1))) {
                platz = ergebnis.get(i - 1).platz();
            }
            ergebnis.add(new Platzierung(platz, w.teilnehmerId(), w.runden(), w.zeit(), w.beste(), w.laeufe()));
        }
        return List.copyOf(ergebnis);
    }

    public static List<SpurStand> laufstand(Lauf lauf) {
        record Zwischen(Belegung belegung, int runden, long zeit, Long letzte, Long beste) {
        }
        List<Zwischen> werte = new ArrayList<>();
        for (Belegung b : lauf.belegungen()) {
            List<Runde> eigene = lauf.rundenVon(b.teilnehmerId());
            Long letzte = eigene.isEmpty() ? null : eigene.getLast().rundenzeitMs();
            werte.add(new Zwischen(b, eigene.size(), lauf.gefahreneZeitMs(b.teilnehmerId()), letzte, besteRunde(eigene)));
        }
        List<Zwischen> sortiert = werte.stream()
                .sorted(Comparator.comparingInt(Zwischen::runden).reversed().thenComparingLong(Zwischen::zeit))
                .toList();
        return werte.stream()
                .map(z -> new SpurStand(z.belegung().spurNummer(), z.belegung().teilnehmerId(),
                        sortiert.indexOf(z) + 1, z.runden(), z.zeit(), z.letzte(), z.beste()))
                .toList();
    }

    private static boolean gleichauf(Wertung a, Wertung b) {
        return a.runden() == b.runden() && a.zeit() == b.zeit();
    }

    private static Long besteRunde(List<Runde> runden) {
        return runden.stream().map(Runde::rundenzeitMs).min(Long::compare).orElse(null);
    }

    private static Long min(Long a, Long b) {
        if (a == null) {
            return b;
        }
        return b == null ? a : Math.min(a, b);
    }

    private record Wertung(UUID teilnehmerId, int startnummer, int runden, long zeit, Long beste, int laeufe) {
    }
}
