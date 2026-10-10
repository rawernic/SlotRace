package de.slotrace.domain.service;

import de.slotrace.domain.model.Meisterschaft;
import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.model.Rennstatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Berechnet den Meisterschaftsstand aus allen beendeten Rennen der Meisterschaft.
 * Rangfolge: Punkte, dann Anzahl Siege.
 */
public final class Meisterschaftswertung {

    private Meisterschaftswertung() {
    }

    public static List<MeisterschaftsPlatz> berechne(Meisterschaft meisterschaft, List<Rennen> rennen) {
        Map<UUID, int[]> summen = new LinkedHashMap<>();
        for (Rennen r : rennen) {
            if (r.status() != Rennstatus.BEENDET || !meisterschaft.id().equals(r.meisterschaftId())) {
                continue;
            }
            for (Platzierung p : Auswertung.rennergebnis(r)) {
                UUID fahrerId = r.teilnehmer(p.teilnehmerId()).fahrerId();
                int[] s = summen.computeIfAbsent(fahrerId, k -> new int[3]);
                s[0] += meisterschaft.punkteFuerPlatz(p.platz());
                s[1] += p.platz() == 1 ? 1 : 0;
                s[2]++;
            }
        }
        List<Map.Entry<UUID, int[]>> sortiert = new ArrayList<>(summen.entrySet());
        sortiert.sort(Comparator.<Map.Entry<UUID, int[]>>comparingInt(e -> e.getValue()[0]).reversed()
                .thenComparing(Comparator.<Map.Entry<UUID, int[]>>comparingInt(e -> e.getValue()[1]).reversed()));

        List<MeisterschaftsPlatz> stand = new ArrayList<>();
        for (int i = 0; i < sortiert.size(); i++) {
            var e = sortiert.get(i);
            int platz = i + 1;
            if (i > 0) {
                int[] vorher = sortiert.get(i - 1).getValue();
                if (vorher[0] == e.getValue()[0] && vorher[1] == e.getValue()[1]) {
                    platz = stand.get(i - 1).platz();
                }
            }
            stand.add(new MeisterschaftsPlatz(platz, e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]));
        }
        return List.copyOf(stand);
    }
}
