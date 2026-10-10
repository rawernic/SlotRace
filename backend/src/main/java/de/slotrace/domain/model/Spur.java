package de.slotrace.domain.model;

import static de.slotrace.domain.Pruefung.pflichtText;
import static de.slotrace.domain.Pruefung.regel;

import java.util.regex.Pattern;

public record Spur(int nummer, String farbe) {

    private static final Pattern HEX_FARBE = Pattern.compile("#[0-9a-fA-F]{6}");

    public Spur {
        regel(nummer >= 1, "Spurnummer muss mindestens 1 sein");
        farbe = pflichtText(farbe, "Spurfarbe");
        regel(HEX_FARBE.matcher(farbe).matches(), "Spurfarbe muss im Format #RRGGBB angegeben werden");
    }
}
