package de.slotrace.domain.model;

import static de.slotrace.domain.Pruefung.pflicht;
import static de.slotrace.domain.Pruefung.pflichtText;
import static de.slotrace.domain.Pruefung.regel;

import java.util.List;
import java.util.UUID;

/** Eine Meisterschaft fasst mehrere Rennen zusammen und vergibt Punkte nach Platzierung. */
public record Meisterschaft(UUID id, String name, List<Integer> punkteSchema) {

    public static final List<Integer> STANDARD_PUNKTE = List.of(10, 8, 6, 5, 4, 3, 2, 1);

    public Meisterschaft {
        pflicht(id, "ID");
        name = pflichtText(name, "Name");
        if (punkteSchema == null || punkteSchema.isEmpty()) {
            punkteSchema = STANDARD_PUNKTE;
        }
        regel(punkteSchema.stream().allMatch(p -> p != null && p >= 0), "Punkte dürfen nicht negativ sein");
        punkteSchema = List.copyOf(punkteSchema);
    }

    public static Meisterschaft neu(String name, List<Integer> punkteSchema) {
        return new Meisterschaft(UUID.randomUUID(), name, punkteSchema);
    }

    public int punkteFuerPlatz(int platz) {
        return platz >= 1 && platz <= punkteSchema.size() ? punkteSchema.get(platz - 1) : 0;
    }
}
