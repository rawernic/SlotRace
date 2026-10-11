package de.slotrace.adapter.in.web;

import de.slotrace.domain.model.Laufstatus;
import de.slotrace.domain.model.Rennmodus;
import de.slotrace.domain.model.Rennstatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Antwort- und Anfrageobjekte der REST-API. */
final class Dtos {

    private Dtos() {
    }

    record FahrerDto(UUID id, String vorname, String nachname, String spitzname, String anzeigename) {
    }

    record FahrzeugDto(UUID id, String bezeichnung, String hersteller, String massstab) {
    }

    record SpurDto(int nummer, String farbe) {
    }

    record BahnDto(UUID id, String name, List<SpurDto> spuren) {
    }

    record MeisterschaftDto(UUID id, String name, List<Integer> punkteSchema) {
    }

    record MeisterschaftsPlatzDto(int platz, UUID fahrerId, String fahrerName, int punkte, int siege, int rennen) {
    }

    record MeldungDto(UUID fahrerId, UUID fahrzeugId, Integer spurNummer) {
    }

    record BelegungAnfrage(UUID fahrerId, UUID fahrzeugId) {
    }

    record RennenAnfrage(String name, UUID bahnId, UUID meisterschaftId, Rennmodus modus, Integer laufDauerSekunden,
                         Integer rundenZiel, Integer mindestRundenzeitMs, List<MeldungDto> teilnehmer) {
    }

    record RundeAnfrage(int spur) {
    }

    record RennenUebersichtDto(UUID id, String name, UUID bahnId, String bahnName, UUID meisterschaftId,
                               Rennmodus modus, Integer laufDauerSekunden, Integer rundenZiel, Rennstatus status,
                               Instant erstelltAm, int teilnehmerAnzahl, int laufAnzahl, int beendeteLaeufe) {
    }

    record TeilnehmerDto(UUID id, int startnummer, UUID fahrerId, String fahrerName, UUID fahrzeugId,
                         String fahrzeugName) {
    }

    record SpurStandDto(int spurNummer, String farbe, UUID teilnehmerId, String fahrerName, String fahrzeugName,
                        int position, int runden, long zeitMs, Long letzteRundeMs, Long besteRundeMs,
                        List<Long> rundenzeitenMs) {
    }

    /** @param endetAm geplantes Ende bei Zeitrennen (sonst {@code null}) */
    record LaufDto(int nummer, Laufstatus status, Instant gestartetAm, Instant beendetAm, Instant endetAm,
                   List<SpurStandDto> spuren) {
    }

    record PlatzierungDto(int platz, UUID teilnehmerId, int startnummer, String fahrerName, String fahrzeugName,
                          int runden, long gesamtzeitMs, Long besteRundeMs, int gefahreneLaeufe) {
    }

    /**
     * Vollständiger Stand eines Rennens.
     *
     * @param serverZeit aktuelle Serverzeit, damit Clients Uhrabweichungen ausgleichen können
     * @param aktuellerLauf laufender bzw. nächster zu fahrender Lauf ({@code null}, wenn alle gefahren)
     */
    record RennenDetailDto(UUID id, String name, BahnDto bahn, UUID meisterschaftId, String meisterschaftName,
                           Rennmodus modus, Integer laufDauerSekunden, Integer rundenZiel, int mindestRundenzeitMs,
                           Rennstatus status, Instant erstelltAm, Instant serverZeit, List<TeilnehmerDto> teilnehmer,
                           List<LaufDto> laeufe, Integer aktuellerLauf, List<PlatzierungDto> ergebnis) {
    }
}
