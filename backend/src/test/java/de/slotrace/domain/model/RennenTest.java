package de.slotrace.domain.model;

import static de.slotrace.domain.TestDaten.START;
import static de.slotrace.domain.TestDaten.plusMs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.slotrace.domain.RegelVerletzungException;
import de.slotrace.domain.TestDaten;
import de.slotrace.domain.service.Auswertung;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RennenTest {

    @Test
    void planenErzeugtLaufplanMitSpurwechsel() {
        Rennen rennen = TestDaten.zeitrennen(3, 2, 60, 0);

        assertThat(rennen.status()).isEqualTo(Rennstatus.GEPLANT);
        assertThat(rennen.laeufe()).hasSize(3);
        assertThat(rennen.teilnehmer()).extracting(Teilnehmer::startnummer).containsExactly(1, 2, 3);
    }

    @Test
    void fahrerDarfNurEinmalStarten() {
        List<Meldung> meldungen = TestDaten.meldungen(1);
        Meldung doppelt = new Meldung(meldungen.getFirst().fahrerId(), meldungen.getFirst().fahrzeugId());
        assertThatThrownBy(() -> Rennen.planen(new Rennplanung("X", null, Rennmodus.ZEITRENNEN, 60, null, 0,
                List.of(meldungen.getFirst(), doppelt)), TestDaten.bahn(2), START))
                .isInstanceOf(RegelVerletzungException.class)
                .hasMessageContaining("nur einmal");
    }

    @Test
    void zeitrennenBrauchtLaufdauer() {
        assertThatThrownBy(() -> Rennen.planen(new Rennplanung("X", null, Rennmodus.ZEITRENNEN, null, null, 0,
                TestDaten.meldungen(2)), TestDaten.bahn(2), START))
                .isInstanceOf(RegelVerletzungException.class)
                .hasMessageContaining("Laufdauer");
    }

    @Test
    void rundenzeitWirdAbLaufstartBzwVorherigerRundeGemessen() {
        Rennen rennen = TestDaten.zeitrennen(2, 2, 60, 0);
        rennen.starteLauf(1, START);

        Runde erste = rennen.erfasseRunde(1, 1, plusMs(5_000));
        Runde zweite = rennen.erfasseRunde(1, 1, plusMs(9_500));

        assertThat(erste.rundenzeitMs()).isEqualTo(5_000);
        assertThat(erste.nummer()).isEqualTo(1);
        assertThat(zweite.rundenzeitMs()).isEqualTo(4_500);
        assertThat(zweite.nummer()).isEqualTo(2);
        assertThat(rennen.status()).isEqualTo(Rennstatus.LAEUFT);
    }

    @Test
    void zuSchnelleRundeWirdAbgelehnt() {
        Rennen rennen = TestDaten.zeitrennen(2, 2, 60, 2_000);
        rennen.starteLauf(1, START);
        rennen.erfasseRunde(1, 1, plusMs(3_000));

        assertThatThrownBy(() -> rennen.erfasseRunde(1, 1, plusMs(3_500)))
                .isInstanceOf(RegelVerletzungException.class)
                .hasMessageContaining("zu schnell");
        assertThat(rennen.lauf(1).runden()).hasSize(1);
    }

    @Test
    void unbelegteSpurWirdAbgelehnt() {
        Rennen rennen = TestDaten.zeitrennen(1, 2, 60, 0);
        rennen.starteLauf(1, START);
        int freieSpur = rennen.lauf(1).teilnehmerAufSpur(1).isPresent() ? 2 : 1;

        assertThatThrownBy(() -> rennen.erfasseRunde(1, freieSpur, plusMs(5_000)))
                .isInstanceOf(RegelVerletzungException.class)
                .hasMessageContaining("nicht belegt");
    }

    @Test
    void nurEinLaufGleichzeitig() {
        Rennen rennen = TestDaten.zeitrennen(2, 2, 60, 0);
        rennen.starteLauf(1, START);

        assertThatThrownBy(() -> rennen.starteLauf(2, plusMs(1_000)))
                .isInstanceOf(RegelVerletzungException.class)
                .hasMessageContaining("läuft noch");
    }

    @Test
    void zeitrennenEndetAutomatischNachAblaufDerLaufzeit() {
        Rennen rennen = TestDaten.zeitrennen(2, 2, 60, 0);
        rennen.starteLauf(1, START);
        rennen.erfasseRunde(1, 1, plusMs(10_000));

        assertThat(rennen.pruefeZeitablauf(plusMs(59_999))).isFalse();
        assertThat(rennen.pruefeZeitablauf(plusMs(61_000))).isTrue();
        assertThat(rennen.lauf(1).status()).isEqualTo(Laufstatus.BEENDET);
        assertThat(rennen.lauf(1).beendetAm()).isEqualTo(plusMs(60_000));
        assertThatThrownBy(() -> rennen.erfasseRunde(1, 1, plusMs(62_000)))
                .isInstanceOf(RegelVerletzungException.class);
    }

    @Test
    void rundenrennenEndetWennAlleImZielSind() {
        Rennen rennen = TestDaten.rundenrennen(2, 2, 2);
        rennen.starteLauf(1, START);
        rennen.erfasseRunde(1, 1, plusMs(1_000));
        rennen.erfasseRunde(1, 1, plusMs(2_000));

        assertThatThrownBy(() -> rennen.erfasseRunde(1, 1, plusMs(3_000)))
                .hasMessageContaining("Rundenziel");
        assertThat(rennen.lauf(1).status()).isEqualTo(Laufstatus.LAEUFT);

        rennen.erfasseRunde(1, 2, plusMs(3_000));
        rennen.erfasseRunde(1, 2, plusMs(4_000));

        assertThat(rennen.lauf(1).status()).isEqualTo(Laufstatus.BEENDET);
    }

    @Test
    void rennenIstBeendetWennAlleLaeufeBeendetSind() {
        Rennen rennen = TestDaten.zeitrennen(2, 2, 60, 0);
        rennen.starteLauf(1, START);
        rennen.beendeLauf(1, plusMs(1_000));
        rennen.starteLauf(2, plusMs(2_000));
        rennen.beendeLauf(2, plusMs(3_000));

        assertThat(rennen.status()).isEqualTo(Rennstatus.BEENDET);
        assertThatThrownBy(() -> rennen.aendern(null, null)).hasMessageContaining("geplante");
    }

    @Test
    void letzteRundeEntfernenUndLaufZuruecksetzen() {
        Rennen rennen = TestDaten.zeitrennen(2, 2, 60, 0);
        rennen.starteLauf(1, START);
        rennen.erfasseRunde(1, 1, plusMs(1_000));
        rennen.erfasseRunde(1, 1, plusMs(2_000));

        rennen.entferneLetzteRunde(1, 1);
        assertThat(rennen.lauf(1).runden()).hasSize(1);

        rennen.setzeLaufZurueck(1);
        assertThat(rennen.lauf(1).status()).isEqualTo(Laufstatus.BEREIT);
        assertThat(rennen.lauf(1).runden()).isEmpty();
        assertThat(rennen.status()).isEqualTo(Rennstatus.GEPLANT);
    }

    @Test
    void pausierenSetztLaufendenLaufZurueckUndBehaeltBeendete() {
        Rennen rennen = TestDaten.zeitrennen(2, 2, 60, 0);
        rennen.starteLauf(1, START);
        rennen.erfasseRunde(1, 1, plusMs(5_000));
        rennen.beendeLauf(1, plusMs(10_000));
        rennen.starteLauf(2, plusMs(20_000));
        rennen.erfasseRunde(2, 1, plusMs(25_000));

        rennen.pausieren(plusMs(26_000));

        assertThat(rennen.status()).isEqualTo(Rennstatus.PAUSIERT);
        assertThat(rennen.lauf(1).runden()).hasSize(1);
        assertThat(rennen.lauf(2).status()).isEqualTo(Laufstatus.BEREIT);
        assertThat(rennen.lauf(2).runden()).isEmpty();

        rennen.starteLauf(2, plusMs(60_000));
        assertThat(rennen.status()).isEqualTo(Rennstatus.LAEUFT);
    }

    @Test
    void abgebrochenesRennenKannNichtWeitergefahrenWerden() {
        Rennen rennen = TestDaten.zeitrennen(2, 2, 60, 0);
        rennen.starteLauf(1, START);

        rennen.abbrechen(plusMs(1_000));

        assertThat(rennen.status()).isEqualTo(Rennstatus.ABGEBROCHEN);
        assertThat(rennen.lauf(1).status()).isEqualTo(Laufstatus.BEENDET);
        assertThatThrownBy(() -> rennen.starteLauf(2, plusMs(2_000))).hasMessageContaining("abgebrochen");
        assertThatThrownBy(() -> rennen.pausieren(plusMs(2_000))).isInstanceOf(RegelVerletzungException.class);
    }

    @Test
    void trainingBelegtSpurenNachWahlUndLaeuftOhneEnde() {
        List<Meldung> m = TestDaten.meldungen(2);
        Rennen training = TestDaten.training(List.of(
                new Meldung(m.get(0).fahrerId(), m.get(0).fahrzeugId(), 3),
                new Meldung(m.get(1).fahrerId(), m.get(1).fahrzeugId(), 1)), 4);

        assertThat(training.laeufe()).hasSize(1);
        assertThat(training.lauf(1).belegungen()).extracting(Belegung::spurNummer).containsExactly(1, 3);

        training.starteLauf(1, START);
        training.erfasseRunde(1, 3, plusMs(8_000));
        training.erfasseRunde(1, 3, plusMs(15_000));
        assertThat(training.pruefeZeitablauf(plusMs(3_600_000))).isFalse();
        assertThat(training.lauf(1).status()).isEqualTo(Laufstatus.LAEUFT);
        assertThat(training.lauf(1).rundenVon(training.lauf(1).teilnehmerAufSpur(3).orElseThrow()))
                .extracting(Runde::rundenzeitMs).containsExactly(8_000L, 7_000L);
    }

    @Test
    void fahrerwechselImTrainingStartetRundenzeitNeu() {
        List<Meldung> m = TestDaten.meldungen(2);
        Rennen training = TestDaten.training(List.of(new Meldung(m.get(0).fahrerId(), m.get(0).fahrzeugId(), 1)), 2);
        training.starteLauf(1, START);
        training.erfasseRunde(1, 1, plusMs(5_000));

        training.wechsleBelegung(1, m.get(1).fahrerId(), m.get(1).fahrzeugId(), 2, plusMs(20_000));
        Runde ersteNachWechsel = training.erfasseRunde(1, 1, plusMs(26_000));

        assertThat(ersteNachWechsel.rundenzeitMs()).isEqualTo(6_000);
        assertThat(ersteNachWechsel.nummer()).isEqualTo(1);
        assertThat(training.teilnehmer()).hasSize(2);
        assertThat(training.lauf(1).teilnehmerAufSpur(1)).contains(ersteNachWechsel.teilnehmerId());
        // beide Fahrer bleiben in der Wertung, auch der ausgewechselte
        assertThat(Auswertung.rennergebnis(training))
                .extracting(p -> p.besteRundeMs()).containsExactly(5_000L, 6_000L);
    }

    @Test
    void fahrzeugwechselUndDoppelteFahrerImTraining() {
        List<Meldung> m = TestDaten.meldungen(2);
        Rennen training = TestDaten.training(List.of(
                new Meldung(m.get(0).fahrerId(), m.get(0).fahrzeugId(), 1),
                new Meldung(m.get(1).fahrerId(), m.get(1).fahrzeugId(), 2)), 2);

        assertThatThrownBy(() -> training.wechsleBelegung(2, m.get(0).fahrerId(), m.get(1).fahrzeugId(), 2, START))
                .hasMessageContaining("bereits auf Spur 1");
        assertThatThrownBy(() -> training.wechsleBelegung(3, m.get(0).fahrerId(), m.get(1).fahrzeugId(), 2, START))
                .hasMessageContaining("existiert");

        training.wechsleBelegung(1, m.get(0).fahrerId(), m.get(1).fahrzeugId(), 2, START);
        assertThat(training.teilnehmer(training.lauf(1).teilnehmerAufSpur(1).orElseThrow()).fahrzeugId())
                .isEqualTo(m.get(1).fahrzeugId());

        training.wechsleBelegung(2, null, null, 2, START);
        assertThat(training.lauf(1).teilnehmerAufSpur(2)).isEmpty();
    }

    @Test
    void trainingPausierenBehaeltRundenUndLegtNeueSitzungAn() {
        List<Meldung> m = TestDaten.meldungen(1);
        Rennen training = TestDaten.training(List.of(new Meldung(m.get(0).fahrerId(), m.get(0).fahrzeugId(), 1)), 2);
        training.starteLauf(1, START);
        training.erfasseRunde(1, 1, plusMs(5_000));

        training.pausieren(plusMs(6_000));

        assertThat(training.status()).isEqualTo(Rennstatus.PAUSIERT);
        assertThat(training.lauf(1).runden()).hasSize(1);
        assertThat(training.lauf(2).status()).isEqualTo(Laufstatus.BEREIT);

        training.starteLauf(2, plusMs(100_000));
        training.beendeTraining(plusMs(110_000));
        assertThat(training.status()).isEqualTo(Rennstatus.BEENDET);
        assertThat(training.laeufe()).hasSize(2);
    }

    @Test
    void trainingBrauchtGueltigeSpurenUndKeineMeisterschaft() {
        List<Meldung> m = TestDaten.meldungen(2);
        assertThatThrownBy(() -> TestDaten.training(List.of(
                new Meldung(m.get(0).fahrerId(), m.get(0).fahrzeugId(), 1),
                new Meldung(m.get(1).fahrerId(), m.get(1).fahrzeugId(), 1)), 2))
                .hasMessageContaining("doppelt");
        assertThatThrownBy(() -> Rennen.planen(new Rennplanung("T", UUID.randomUUID(), Rennmodus.TRAINING,
                null, null, 0, List.of(new Meldung(m.get(0).fahrerId(), m.get(0).fahrzeugId(), 1))),
                TestDaten.bahn(2), START))
                .hasMessageContaining("Meisterschaft");
    }
}
