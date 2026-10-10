package de.slotrace.adapter.in.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Testuhr, die manuell vorgestellt werden kann. */
class StellbareUhr extends Clock {

    private Instant jetzt = Instant.parse("2026-01-01T10:00:00Z");

    void vorstellen(Duration dauer) {
        jetzt = jetzt.plus(dauer);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return jetzt;
    }
}
