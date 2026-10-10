package de.slotrace.domain.model;

import static de.slotrace.domain.TestDaten.START;
import static de.slotrace.domain.TestDaten.plusMs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.slotrace.domain.RegelVerletzungException;
import de.slotrace.domain.TestDaten;
import java.util.List;
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
}
