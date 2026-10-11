package de.slotrace.domain.service;

import static de.slotrace.domain.TestDaten.START;
import static de.slotrace.domain.TestDaten.plusMs;
import static org.assertj.core.api.Assertions.assertThat;

import de.slotrace.domain.TestDaten;
import de.slotrace.domain.model.Meisterschaft;
import de.slotrace.domain.model.Rennen;
import de.slotrace.domain.model.Rennplanung;
import de.slotrace.domain.model.Rennmodus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuswertungTest {

    @Test
    void mehrRundenVorWenigerRundenUndBeiGleichstandKuerzereZeit() {
        Rennen rennen = TestDaten.zeitrennen(3, 3, 60, 0);
        rennen.starteLauf(1, START);
        // Lauf 1: Spur 1 -> 2 Runden in 8 s, Spur 2 -> 2 Runden in 7 s, Spur 3 -> 1 Runde
        rennen.erfasseRunde(1, 1, plusMs(4_000));
        rennen.erfasseRunde(1, 1, plusMs(8_000));
        rennen.erfasseRunde(1, 2, plusMs(3_000));
        rennen.erfasseRunde(1, 2, plusMs(7_000));
        rennen.erfasseRunde(1, 3, plusMs(2_000));
        rennen.beendeLauf(1, plusMs(60_000));

        List<Platzierung> ergebnis = Auswertung.rennergebnis(rennen);

        UUID spur1 = rennen.lauf(1).teilnehmerAufSpur(1).orElseThrow();
        UUID spur2 = rennen.lauf(1).teilnehmerAufSpur(2).orElseThrow();
        UUID spur3 = rennen.lauf(1).teilnehmerAufSpur(3).orElseThrow();
        assertThat(ergebnis).extracting(Platzierung::teilnehmerId).containsExactly(spur2, spur1, spur3);
        assertThat(ergebnis.getFirst().besteRundeMs()).isEqualTo(3_000);
        assertThat(ergebnis.getFirst().gesamtzeitMs()).isEqualTo(7_000);
        assertThat(ergebnis).extracting(Platzierung::platz).containsExactly(1, 2, 3);
    }

    @Test
    void gleichstandErgibtGleichenPlatz() {
        Rennen rennen = TestDaten.zeitrennen(2, 2, 60, 0);

        List<Platzierung> ergebnis = Auswertung.rennergebnis(rennen);

        assertThat(ergebnis).extracting(Platzierung::platz).containsExactly(1, 1);
    }

    @Test
    void laufstandSortiertPositionen() {
        Rennen rennen = TestDaten.rundenrennen(2, 2, 5);
        rennen.starteLauf(1, START);
        rennen.erfasseRunde(1, 2, plusMs(3_000));

        List<SpurStand> stand = Auswertung.laufstand(rennen.lauf(1), rennen.modus());

        assertThat(stand).extracting(SpurStand::spurNummer).containsExactly(1, 2);
        assertThat(stand).extracting(SpurStand::position).containsExactly(2, 1);
        assertThat(stand.get(1).letzteRundeMs()).isEqualTo(3_000);
    }

    @Test
    void meisterschaftVergibtPunkteNachSchema() {
        Meisterschaft meisterschaft = Meisterschaft.neu("Winterpokal", List.of(5, 3));
        var meldungen = TestDaten.meldungen(2);
        Rennen rennen = Rennen.planen(new Rennplanung("R1", meisterschaft.id(), Rennmodus.RUNDENRENNEN, null, 1, 0,
                meldungen), TestDaten.bahn(2), START);
        rennen.starteLauf(1, START);
        rennen.erfasseRunde(1, 1, plusMs(2_000));
        rennen.erfasseRunde(1, 2, plusMs(3_000));
        rennen.starteLauf(2, plusMs(10_000));
        rennen.erfasseRunde(2, 1, plusMs(12_000));
        rennen.erfasseRunde(2, 2, plusMs(13_000));

        List<MeisterschaftsPlatz> stand = Meisterschaftswertung.berechne(meisterschaft, List.of(rennen));

        UUID ersterTeilnehmer = rennen.lauf(1).teilnehmerAufSpur(1).orElseThrow();
        UUID sieger = rennen.teilnehmer(ersterTeilnehmer).fahrerId();
        // beide Fahrer: 2 Runden; Teilnehmer auf Spur 1 in Lauf 1 fährt in Lauf 2 auf Spur 2 -> 2 s + 3 s vs 3 s + 2 s
        assertThat(stand).hasSize(2);
        assertThat(stand).allSatisfy(p -> assertThat(p.rennen()).isEqualTo(1));
        assertThat(stand).extracting(MeisterschaftsPlatz::punkte).containsExactly(5, 5);
        assertThat(stand).extracting(MeisterschaftsPlatz::fahrerId).contains(sieger);
    }
}
