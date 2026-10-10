package de.slotrace.domain.model;

import static de.slotrace.domain.Pruefung.pflicht;
import static de.slotrace.domain.Pruefung.pflichtText;
import static de.slotrace.domain.Pruefung.regel;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Eine Rennbahn mit ihren Spuren (durchgehend nummeriert ab 1). */
public record Bahn(UUID id, String name, List<Spur> spuren) {

    public static final int MAX_SPUREN = 8;

    public Bahn {
        pflicht(id, "ID");
        name = pflichtText(name, "Name");
        pflicht(spuren, "Spuren");
        regel(!spuren.isEmpty(), "Eine Bahn braucht mindestens eine Spur");
        regel(spuren.size() <= MAX_SPUREN, "Eine Bahn darf höchstens " + MAX_SPUREN + " Spuren haben");
        spuren = spuren.stream().sorted(Comparator.comparingInt(Spur::nummer)).toList();
        for (int i = 0; i < spuren.size(); i++) {
            regel(spuren.get(i).nummer() == i + 1, "Spuren müssen fortlaufend ab 1 nummeriert sein");
        }
    }

    public static Bahn neu(String name, List<Spur> spuren) {
        return new Bahn(UUID.randomUUID(), name, spuren);
    }

    public int spurAnzahl() {
        return spuren.size();
    }
}
