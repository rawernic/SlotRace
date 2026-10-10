package de.slotrace.domain;

/** Hilfsmethoden für fachliche Eingabeprüfungen. */
public final class Pruefung {

    private Pruefung() {
    }

    public static String pflichtText(String wert, String feld) {
        if (wert == null || wert.isBlank()) {
            throw new RegelVerletzungException(feld + " darf nicht leer sein");
        }
        return wert.strip();
    }

    public static String optionalerText(String wert) {
        return wert == null || wert.isBlank() ? null : wert.strip();
    }

    public static <T> T pflicht(T wert, String feld) {
        if (wert == null) {
            throw new RegelVerletzungException(feld + " muss angegeben werden");
        }
        return wert;
    }

    public static void regel(boolean bedingung, String meldung) {
        if (!bedingung) {
            throw new RegelVerletzungException(meldung);
        }
    }
}
