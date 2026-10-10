package de.slotrace.domain;

import java.util.UUID;

public class NichtGefundenException extends RuntimeException {

    public NichtGefundenException(String art, UUID id) {
        super(art + " mit ID " + id + " wurde nicht gefunden");
    }

    public NichtGefundenException(String meldung) {
        super(meldung);
    }
}
