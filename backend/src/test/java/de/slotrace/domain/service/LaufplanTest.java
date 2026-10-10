package de.slotrace.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.slotrace.domain.model.Belegung;
import de.slotrace.domain.model.Teilnehmer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LaufplanTest {

    @ParameterizedTest(name = "{0} Teilnehmer auf {1} Spuren")
    @CsvSource({"1,4", "2,4", "4,4", "6,4", "3,2", "8,8", "5,1"})
    void jederTeilnehmerFaehrtJedeSpurGenauEinmal(int anzahlTeilnehmer, int spuren) {
        List<Teilnehmer> teilnehmer = new ArrayList<>();
        for (int i = 0; i < anzahlTeilnehmer; i++) {
            teilnehmer.add(new Teilnehmer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), i + 1));
        }

        List<List<Belegung>> plan = Laufplan.erzeuge(teilnehmer, spuren);

        assertThat(plan).hasSize(Math.max(anzahlTeilnehmer, spuren));
        for (Teilnehmer t : teilnehmer) {
            List<Integer> gefahreneSpuren = plan.stream().flatMap(List::stream)
                    .filter(b -> b.teilnehmerId().equals(t.id()))
                    .map(Belegung::spurNummer)
                    .sorted()
                    .toList();
            assertThat(gefahreneSpuren).containsExactlyElementsOf(
                    java.util.stream.IntStream.rangeClosed(1, spuren).boxed().toList());
        }
        for (List<Belegung> lauf : plan) {
            Set<UUID> imLauf = new HashSet<>();
            lauf.forEach(b -> assertThat(imLauf.add(b.teilnehmerId())).isTrue());
        }
    }
}
