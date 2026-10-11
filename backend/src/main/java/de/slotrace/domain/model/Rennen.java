package de.slotrace.domain.model;

import static de.slotrace.domain.Pruefung.pflicht;
import static de.slotrace.domain.Pruefung.pflichtText;
import static de.slotrace.domain.Pruefung.regel;

import de.slotrace.domain.NichtGefundenException;
import de.slotrace.domain.RegelVerletzungException;
import de.slotrace.domain.service.Laufplan;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregat-Wurzel eines Rennens. Enthält Teilnehmer, den Laufplan (Läufe mit Spurwechsel)
 * und alle gefahrenen Runden. Sämtliche Regeln der Renndurchführung sind hier gekapselt.
 */
public class Rennen {

    public static final int MAX_MINDEST_RUNDENZEIT_MS = 60_000;

    private final UUID id;
    private final Instant erstelltAm;
    private String name;
    private UUID bahnId;
    private UUID meisterschaftId;
    private Rennmodus modus;
    private Integer laufDauerSekunden;
    private Integer rundenZiel;
    private int mindestRundenzeitMs;
    private Rennstatus status;
    private List<Teilnehmer> teilnehmer;
    private List<Lauf> laeufe;

    /** Rekonstruiert ein Rennen aus dem Speicher – ohne Neuberechnung des Laufplans. */
    public Rennen(UUID id, Instant erstelltAm, String name, UUID bahnId, UUID meisterschaftId,
                  Rennmodus modus, Integer laufDauerSekunden, Integer rundenZiel, int mindestRundenzeitMs,
                  Rennstatus status, List<Teilnehmer> teilnehmer, List<Lauf> laeufe) {
        this.id = id;
        this.erstelltAm = erstelltAm;
        this.name = name;
        this.bahnId = bahnId;
        this.meisterschaftId = meisterschaftId;
        this.modus = modus;
        this.laufDauerSekunden = laufDauerSekunden;
        this.rundenZiel = rundenZiel;
        this.mindestRundenzeitMs = mindestRundenzeitMs;
        this.status = status;
        this.teilnehmer = List.copyOf(teilnehmer);
        this.laeufe = new ArrayList<>(laeufe);
        this.laeufe.sort((a, b) -> Integer.compare(a.nummer(), b.nummer()));
    }

    public static Rennen planen(Rennplanung planung, Bahn bahn, Instant jetzt) {
        Rennen rennen = new Rennen(UUID.randomUUID(), jetzt, null, null, null, null, null, null, 0,
                Rennstatus.GEPLANT, List.of(), List.of());
        rennen.uebernehmePlanung(planung, bahn);
        return rennen;
    }

    /** Ändert Einstellungen und Teilnehmer; der Laufplan wird dabei neu erzeugt. */
    public void aendern(Rennplanung planung, Bahn bahn) {
        regel(status == Rennstatus.GEPLANT, "Nur geplante Rennen können geändert werden");
        uebernehmePlanung(planung, bahn);
    }

    private void uebernehmePlanung(Rennplanung p, Bahn bahn) {
        pflicht(p, "Planung");
        pflicht(bahn, "Bahn");
        String neuerName = pflichtText(p.name(), "Name");
        Rennmodus neuerModus = pflicht(p.modus(), "Rennmodus");
        Integer dauer = null;
        Integer ziel = null;
        if (neuerModus == Rennmodus.ZEITRENNEN) {
            dauer = pflicht(p.laufDauerSekunden(), "Laufdauer");
            regel(dauer > 0, "Laufdauer muss größer als 0 sein");
        } else if (neuerModus == Rennmodus.RUNDENRENNEN) {
            ziel = pflicht(p.rundenZiel(), "Rundenziel");
            regel(ziel > 0, "Rundenziel muss größer als 0 sein");
        } else {
            regel(p.meisterschaftId() == null, "Ein Training kann keiner Meisterschaft zugeordnet werden");
        }
        regel(p.mindestRundenzeitMs() >= 0 && p.mindestRundenzeitMs() <= MAX_MINDEST_RUNDENZEIT_MS,
                "Mindestrundenzeit muss zwischen 0 und " + MAX_MINDEST_RUNDENZEIT_MS + " ms liegen");
        List<Meldung> meldungen = pflicht(p.meldungen(), "Teilnehmer");
        regel(!meldungen.isEmpty(), "Ein Rennen braucht mindestens einen Teilnehmer");
        var fahrer = new HashSet<UUID>();
        var spuren = new HashSet<Integer>();
        List<Teilnehmer> neueTeilnehmer = new ArrayList<>();
        List<Belegung> trainingsBelegung = new ArrayList<>();
        for (int i = 0; i < meldungen.size(); i++) {
            Meldung m = pflicht(meldungen.get(i), "Teilnehmer");
            regel(fahrer.add(pflicht(m.fahrerId(), "Fahrer")), "Ein Fahrer darf nur einmal pro Rennen starten");
            Teilnehmer t = new Teilnehmer(UUID.randomUUID(), m.fahrerId(), pflicht(m.fahrzeugId(), "Fahrzeug"), i + 1);
            neueTeilnehmer.add(t);
            if (neuerModus == Rennmodus.TRAINING) {
                int spur = pflicht(m.spurNummer(), "Spur");
                pruefeSpur(spur, bahn.spurAnzahl());
                regel(spuren.add(spur), "Spur " + spur + " ist doppelt belegt");
                trainingsBelegung.add(new Belegung(spur, t.id()));
            }
        }

        this.name = neuerName;
        this.bahnId = bahn.id();
        this.meisterschaftId = p.meisterschaftId();
        this.modus = neuerModus;
        this.laufDauerSekunden = dauer;
        this.rundenZiel = ziel;
        this.mindestRundenzeitMs = p.mindestRundenzeitMs();
        this.teilnehmer = List.copyOf(neueTeilnehmer);
        this.laeufe = new ArrayList<>();
        if (neuerModus == Rennmodus.TRAINING) {
            laeufe.add(Lauf.neu(1, trainingsBelegung));
            return;
        }
        List<List<Belegung>> plan = Laufplan.erzeuge(teilnehmer, bahn.spurAnzahl());
        for (int i = 0; i < plan.size(); i++) {
            laeufe.add(Lauf.neu(i + 1, plan.get(i)));
        }
    }

    public void starteLauf(int laufNummer, Instant jetzt) {
        pruefeNichtAbgeschlossen();
        pruefeZeitablauf(jetzt);
        laufendeLauf().ifPresent(l -> {
            throw new RegelVerletzungException("Lauf " + l.nummer() + " läuft noch");
        });
        Lauf lauf = lauf(laufNummer);
        regel(lauf.status() == Laufstatus.BEREIT, "Lauf " + laufNummer + " wurde bereits gefahren");
        regel(!lauf.belegungen().isEmpty(), "Es ist keine Spur belegt");
        lauf.starten(jetzt);
        status = Rennstatus.LAEUFT;
    }

    public Runde erfasseRunde(int laufNummer, int spurNummer, Instant jetzt) {
        pruefeNichtAbgebrochen();
        pruefeZeitablauf(jetzt);
        Lauf lauf = lauf(laufNummer);
        regel(lauf.status() == Laufstatus.LAEUFT, "Lauf " + laufNummer + " läuft nicht");
        UUID teilnehmerId = lauf.teilnehmerAufSpur(spurNummer)
                .orElseThrow(() -> new RegelVerletzungException("Spur " + spurNummer + " ist in diesem Lauf nicht belegt"));
        List<Runde> bisher = lauf.rundenVon(teilnehmerId);
        if (modus == Rennmodus.RUNDENRENNEN) {
            regel(bisher.size() < rundenZiel, "Spur " + spurNummer + " hat das Rundenziel bereits erreicht");
        }
        long rundenzeit = Duration.between(lauf.rundenReferenz(spurNummer), jetzt).toMillis();
        regel(rundenzeit >= mindestRundenzeitMs,
                "Runde auf Spur " + spurNummer + " zu schnell (" + rundenzeit + " ms) – ignoriert");

        Runde runde = new Runde(UUID.randomUUID(), teilnehmerId, spurNummer, bisher.size() + 1, jetzt, rundenzeit);
        lauf.rundeHinzufuegen(runde);

        if (modus == Rennmodus.RUNDENRENNEN && alleImZiel(lauf)) {
            lauf.beenden(jetzt);
            aktualisiereStatus();
        }
        return runde;
    }

    public void entferneLetzteRunde(int laufNummer, int spurNummer) {
        pruefeNichtAbgebrochen();
        Lauf lauf = lauf(laufNummer);
        regel(lauf.status() != Laufstatus.BEREIT, "Lauf " + laufNummer + " wurde noch nicht gestartet");
        UUID teilnehmerId = lauf.teilnehmerAufSpur(spurNummer)
                .orElseThrow(() -> new RegelVerletzungException("Spur " + spurNummer + " ist in diesem Lauf nicht belegt"));
        Runde letzte = lauf.letzteRundeVon(teilnehmerId)
                .orElseThrow(() -> new RegelVerletzungException("Auf Spur " + spurNummer + " gibt es keine Runde"));
        lauf.rundeEntfernen(letzte);
    }

    public void beendeLauf(int laufNummer, Instant jetzt) {
        pruefeNichtAbgebrochen();
        boolean abgelaufen = pruefeZeitablauf(jetzt);
        Lauf lauf = lauf(laufNummer);
        if (abgelaufen && lauf.status() == Laufstatus.BEENDET) {
            return;
        }
        regel(lauf.status() == Laufstatus.LAEUFT, "Lauf " + laufNummer + " läuft nicht");
        lauf.beenden(jetzt);
        if (modus == Rennmodus.TRAINING) {
            neueTrainingssitzung();
        }
        aktualisiereStatus();
    }

    public void setzeLaufZurueck(int laufNummer) {
        pruefeNichtAbgebrochen();
        Lauf lauf = lauf(laufNummer);
        regel(lauf.status() != Laufstatus.BEREIT, "Lauf " + laufNummer + " wurde noch nicht gestartet");
        if (modus == Rennmodus.TRAINING) {
            regel(status != Rennstatus.BEENDET && lauf == laeufe.getLast(),
                    "Nur die aktuelle Trainingssitzung kann zurückgesetzt werden");
        }
        lauf.zuruecksetzen();
        aktualisiereStatus();
    }

    /**
     * Unterbricht das Rennen, damit die Bahn für ein anderes Rennen frei wird. Ein laufender Lauf
     * wird zurückgesetzt und später neu gefahren; im Training bleiben die Runden der Sitzung erhalten.
     */
    public void pausieren(Instant jetzt) {
        pruefeZeitablauf(jetzt);
        regel(status == Rennstatus.LAEUFT, "Nur laufende Rennen können pausiert werden");
        Optional<Lauf> laufend = laufendeLauf();
        if (modus == Rennmodus.TRAINING) {
            laufend.ifPresent(l -> l.beenden(jetzt));
            neueTrainingssitzung();
        } else {
            laufend.ifPresent(Lauf::zuruecksetzen);
        }
        status = laeufe.stream().allMatch(l -> l.status() == Laufstatus.BEREIT) ? Rennstatus.GEPLANT : Rennstatus.PAUSIERT;
    }

    /** Bricht das Rennen endgültig ab; es wird nicht gewertet. */
    public void abbrechen(Instant jetzt) {
        pruefeNichtAbgeschlossen();
        laufendeLauf().ifPresent(l -> l.beenden(jetzt));
        status = Rennstatus.ABGEBROCHEN;
    }

    /** Beendet ein Training; nicht gestartete Sitzungen entfallen. */
    public void beendeTraining(Instant jetzt) {
        regel(modus == Rennmodus.TRAINING, "Nur ein Training kann jederzeit beendet werden");
        pruefeNichtAbgeschlossen();
        regel(laeufe.stream().anyMatch(l -> l.status() != Laufstatus.BEREIT), "Das Training wurde noch nicht gestartet");
        laufendeLauf().ifPresent(l -> l.beenden(jetzt));
        laeufe.removeIf(l -> l.status() == Laufstatus.BEREIT);
        status = Rennstatus.BEENDET;
    }

    /**
     * Setzt im Training Fahrer und Fahrzeug einer Spur für die aktuelle Sitzung, auch während sie läuft.
     * Ohne Fahrer wird die Spur frei. Die nächste Runde zählt ab dem Wechsel.
     */
    public void wechsleBelegung(int spurNummer, UUID fahrerId, UUID fahrzeugId, int spurAnzahl, Instant jetzt) {
        regel(modus == Rennmodus.TRAINING, "Fahrer und Fahrzeuge können nur im Training gewechselt werden");
        pruefeNichtAbgeschlossen();
        pruefeSpur(spurNummer, spurAnzahl);
        Lauf sitzung = laeufe.getLast();
        if (fahrerId == null) {
            sitzung.raeumeSpur(spurNummer);
            return;
        }
        pflicht(fahrzeugId, "Fahrzeug");
        for (Belegung b : sitzung.belegungen()) {
            if (b.spurNummer() != spurNummer && teilnehmer(b.teilnehmerId()).fahrerId().equals(fahrerId)) {
                throw new RegelVerletzungException("Der Fahrer fährt bereits auf Spur " + b.spurNummer());
            }
        }
        Teilnehmer neu = teilnehmer.stream()
                .filter(t -> t.fahrerId().equals(fahrerId) && t.fahrzeugId().equals(fahrzeugId))
                .findFirst()
                .orElseGet(() -> neuerTeilnehmer(fahrerId, fahrzeugId));
        if (sitzung.teilnehmerAufSpur(spurNummer).filter(neu.id()::equals).isPresent()) {
            return;
        }
        sitzung.belegeSpur(spurNummer, neu.id(), sitzung.status() == Laufstatus.LAEUFT ? jetzt : null);
    }

    private Teilnehmer neuerTeilnehmer(UUID fahrerId, UUID fahrzeugId) {
        int startnummer = teilnehmer.stream().mapToInt(Teilnehmer::startnummer).max().orElse(0) + 1;
        Teilnehmer t = new Teilnehmer(UUID.randomUUID(), fahrerId, fahrzeugId, startnummer);
        List<Teilnehmer> liste = new ArrayList<>(teilnehmer);
        liste.add(t);
        teilnehmer = List.copyOf(liste);
        return t;
    }

    private void neueTrainingssitzung() {
        Lauf letzte = laeufe.getLast();
        if (letzte.status() == Laufstatus.BEENDET) {
            List<Belegung> belegung = letzte.belegungen().stream()
                    .map(b -> new Belegung(b.spurNummer(), b.teilnehmerId()))
                    .toList();
            laeufe.add(Lauf.neu(letzte.nummer() + 1, belegung));
        }
    }

    private static void pruefeSpur(int spurNummer, int spurAnzahl) {
        regel(spurNummer >= 1 && spurNummer <= spurAnzahl, "Spur " + spurNummer + " existiert auf dieser Bahn nicht");
    }

    private void pruefeNichtAbgebrochen() {
        regel(status != Rennstatus.ABGEBROCHEN, "Das Rennen wurde abgebrochen");
    }

    private void pruefeNichtAbgeschlossen() {
        pruefeNichtAbgebrochen();
        regel(status != Rennstatus.BEENDET, "Das Rennen ist bereits beendet");
    }

    /**
     * Beendet bei Zeitrennen einen laufenden Lauf, dessen Zeit abgelaufen ist.
     *
     * @return {@code true}, wenn sich dadurch der Zustand geändert hat
     */
    public boolean pruefeZeitablauf(Instant jetzt) {
        if (modus != Rennmodus.ZEITRENNEN) {
            return false;
        }
        Optional<Lauf> laufend = laufendeLauf();
        if (laufend.isEmpty()) {
            return false;
        }
        Instant ende = laufend.get().gestartetAm().plusSeconds(laufDauerSekunden);
        if (jetzt.isBefore(ende)) {
            return false;
        }
        laufend.get().beenden(ende);
        aktualisiereStatus();
        return true;
    }

    private boolean alleImZiel(Lauf lauf) {
        return lauf.belegungen().stream()
                .allMatch(b -> lauf.rundenVon(b.teilnehmerId()).size() >= rundenZiel);
    }

    private void aktualisiereStatus() {
        if (laeufe.stream().allMatch(l -> l.status() == Laufstatus.BEREIT)) {
            status = Rennstatus.GEPLANT;
        } else if (laufendeLauf().isPresent()) {
            status = Rennstatus.LAEUFT;
        } else if (modus == Rennmodus.TRAINING) {
            status = status == Rennstatus.BEENDET ? Rennstatus.BEENDET : Rennstatus.PAUSIERT;
        } else if (laeufe.stream().allMatch(l -> l.status() == Laufstatus.BEENDET)) {
            status = Rennstatus.BEENDET;
        } else if (status != Rennstatus.PAUSIERT) {
            status = Rennstatus.LAEUFT;
        }
    }

    public Lauf lauf(int laufNummer) {
        return laeufe.stream()
                .filter(l -> l.nummer() == laufNummer)
                .findFirst()
                .orElseThrow(() -> new NichtGefundenException("Lauf " + laufNummer + " existiert nicht"));
    }

    public Optional<Lauf> laufendeLauf() {
        return laeufe.stream().filter(l -> l.status() == Laufstatus.LAEUFT).findFirst();
    }

    public Teilnehmer teilnehmer(UUID teilnehmerId) {
        return teilnehmer.stream()
                .filter(t -> t.id().equals(teilnehmerId))
                .findFirst()
                .orElseThrow(() -> new NichtGefundenException("Teilnehmer", teilnehmerId));
    }

    public boolean verwendetFahrer(UUID fahrerId) {
        return teilnehmer.stream().anyMatch(t -> t.fahrerId().equals(fahrerId));
    }

    public boolean verwendetFahrzeug(UUID fahrzeugId) {
        return teilnehmer.stream().anyMatch(t -> t.fahrzeugId().equals(fahrzeugId));
    }

    public UUID id() {
        return id;
    }

    public Instant erstelltAm() {
        return erstelltAm;
    }

    public String name() {
        return name;
    }

    public UUID bahnId() {
        return bahnId;
    }

    public UUID meisterschaftId() {
        return meisterschaftId;
    }

    public Rennmodus modus() {
        return modus;
    }

    public Integer laufDauerSekunden() {
        return laufDauerSekunden;
    }

    public Integer rundenZiel() {
        return rundenZiel;
    }

    public int mindestRundenzeitMs() {
        return mindestRundenzeitMs;
    }

    public Rennstatus status() {
        return status;
    }

    public List<Teilnehmer> teilnehmer() {
        return teilnehmer;
    }

    public List<Lauf> laeufe() {
        return Collections.unmodifiableList(laeufe);
    }
}
