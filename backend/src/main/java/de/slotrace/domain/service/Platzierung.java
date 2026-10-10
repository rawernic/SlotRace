package de.slotrace.domain.service;

import java.util.UUID;

/**
 * Gesamtergebnis eines Teilnehmers in einem Rennen.
 *
 * @param gesamtzeitMs Summe je Lauf der Zeit vom Start bis zur letzten gefahrenen Runde
 * @param besteRundeMs schnellste Runde oder {@code null}, falls noch keine Runde gefahren wurde
 */
public record Platzierung(int platz, UUID teilnehmerId, int runden, long gesamtzeitMs, Long besteRundeMs,
                          int gefahreneLaeufe) {
}
