package de.slotrace.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ein Lauf (Heat) eines Rennens mit fester Spurbelegung. Zustandsänderungen erfolgen
 * ausschließlich über das Aggregat {@link Rennen}.
 */
public class Lauf {

    private final UUID id;
    private final int nummer;
    private final List<Belegung> belegungen;
    private final List<Runde> runden;
    private Laufstatus status;
    private Instant gestartetAm;
    private Instant beendetAm;

    public Lauf(UUID id, int nummer, List<Belegung> belegungen, Laufstatus status,
                Instant gestartetAm, Instant beendetAm, List<Runde> runden) {
        this.id = id;
        this.nummer = nummer;
        this.belegungen = belegungen.stream().sorted(Comparator.comparingInt(Belegung::spurNummer)).toList();
        this.status = status;
        this.gestartetAm = gestartetAm;
        this.beendetAm = beendetAm;
        this.runden = new ArrayList<>(runden);
        this.runden.sort(Comparator.comparing(Runde::zeitpunkt));
    }

    static Lauf neu(int nummer, List<Belegung> belegungen) {
        return new Lauf(UUID.randomUUID(), nummer, belegungen, Laufstatus.BEREIT, null, null, List.of());
    }

    void starten(Instant jetzt) {
        status = Laufstatus.LAEUFT;
        gestartetAm = jetzt;
        beendetAm = null;
    }

    void beenden(Instant zeitpunkt) {
        status = Laufstatus.BEENDET;
        beendetAm = zeitpunkt;
    }

    void zuruecksetzen() {
        status = Laufstatus.BEREIT;
        gestartetAm = null;
        beendetAm = null;
        runden.clear();
    }

    void rundeHinzufuegen(Runde runde) {
        runden.add(runde);
    }

    void rundeEntfernen(Runde runde) {
        runden.remove(runde);
    }

    public Optional<UUID> teilnehmerAufSpur(int spurNummer) {
        return belegungen.stream()
                .filter(b -> b.spurNummer() == spurNummer)
                .map(Belegung::teilnehmerId)
                .findFirst();
    }

    public boolean faehrtMit(UUID teilnehmerId) {
        return belegungen.stream().anyMatch(b -> b.teilnehmerId().equals(teilnehmerId));
    }

    public List<Runde> rundenVon(UUID teilnehmerId) {
        return runden.stream().filter(r -> r.teilnehmerId().equals(teilnehmerId)).toList();
    }

    public Optional<Runde> letzteRundeVon(UUID teilnehmerId) {
        List<Runde> eigene = rundenVon(teilnehmerId);
        return eigene.isEmpty() ? Optional.empty() : Optional.of(eigene.getLast());
    }

    /** Gefahrene Zeit vom Laufstart bis zur letzten Runde des Teilnehmers (0 ohne Runden). */
    public long gefahreneZeitMs(UUID teilnehmerId) {
        return letzteRundeVon(teilnehmerId)
                .map(r -> Duration.between(gestartetAm, r.zeitpunkt()).toMillis())
                .orElse(0L);
    }

    public UUID id() {
        return id;
    }

    public int nummer() {
        return nummer;
    }

    public List<Belegung> belegungen() {
        return belegungen;
    }

    public List<Runde> runden() {
        return Collections.unmodifiableList(runden);
    }

    public Laufstatus status() {
        return status;
    }

    public Instant gestartetAm() {
        return gestartetAm;
    }

    public Instant beendetAm() {
        return beendetAm;
    }
}
