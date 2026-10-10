package de.slotrace.domain;

/** Eine fachliche Regel wurde verletzt (z. B. ungültige Eingabe oder unzulässiger Zustandswechsel). */
public class RegelVerletzungException extends RuntimeException {

    public RegelVerletzungException(String meldung) {
        super(meldung);
    }
}
