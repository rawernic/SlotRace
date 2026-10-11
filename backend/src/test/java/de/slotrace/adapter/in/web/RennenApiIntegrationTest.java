package de.slotrace.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Spielt ein komplettes Rennen über die REST-API gegen eine H2-Datenbank (mit Flyway-Schema) durch. */
@SpringBootTest
@AutoConfigureMockMvc
class RennenApiIntegrationTest {

    @TestConfiguration
    static class TestUhr {
        @Bean
        @Primary
        StellbareUhr stellbareUhr() {
            return new StellbareUhr();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    StellbareUhr uhr;

    @Test
    void komplettesZeitrennenMitMeisterschaft() throws Exception {
        String anna = id(senden("/api/fahrer", """
                {"vorname":"Anna","nachname":"Schnell","spitzname":"Blitz"}"""));
        String bernd = id(senden("/api/fahrer", """
                {"vorname":"Bernd","nachname":"Kurve"}"""));
        String auto1 = id(senden("/api/fahrzeuge", """
                {"bezeichnung":"Porsche 917","hersteller":"Carrera","massstab":"1:32"}"""));
        String auto2 = id(senden("/api/fahrzeuge", """
                {"bezeichnung":"Ford GT40"}"""));
        String bahn = id(senden("/api/bahnen", """
                {"name":"Kellerbahn","spurFarben":["#e53935","#1e88e5"]}"""));
        String meisterschaft = id(senden("/api/meisterschaften", """
                {"name":"Winterpokal"}"""));

        String rennenJson = senden("/api/rennen", """
                {"name":"Lauf 1","bahnId":"%s","meisterschaftId":"%s","modus":"ZEITRENNEN","laufDauerSekunden":60,
                 "mindestRundenzeitMs":1000,
                 "teilnehmer":[{"fahrerId":"%s","fahrzeugId":"%s"},{"fahrerId":"%s","fahrzeugId":"%s"}]}"""
                .formatted(bahn, meisterschaft, anna, auto1, bernd, auto2));
        String rennen = id(rennenJson);
        String basis = "/api/rennen/" + rennen + "/laeufe/";

        mvc.perform(get("/api/rennen/" + rennen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GEPLANT"))
                .andExpect(jsonPath("$.laeufe", hasSize(2)))
                .andExpect(jsonPath("$.aktuellerLauf").value(1))
                .andExpect(jsonPath("$.laeufe[0].spuren[0].fahrerName").value("Blitz"))
                .andExpect(jsonPath("$.laeufe[0].spuren[0].farbe").value("#e53935"));

        // Lauf 1: Blitz auf Spur 1 fährt 2 Runden, Bernd auf Spur 2 eine
        aktion(post(basis + "1/start")).andExpect(jsonPath("$.status").value("LAEUFT"));
        uhr.vorstellen(Duration.ofSeconds(5));
        runde(basis + "1", 1).andExpect(status().isOk());
        uhr.vorstellen(Duration.ofMillis(500));
        runde(basis + "1", 1).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("zu schnell")));
        uhr.vorstellen(Duration.ofSeconds(4));
        runde(basis + "1", 1);
        runde(basis + "1", 2);
        runde(basis + "1", 2).andExpect(status().isUnprocessableContent());
        uhr.vorstellen(Duration.ofSeconds(2));
        runde(basis + "1", 2);
        aktion(delete(basis + "1/runden/letzte").param("spur", "2"))
                .andExpect(jsonPath("$.laeufe[0].spuren[1].runden").value(1));

        // Zeit läuft ab -> Lauf wird beim Abruf automatisch beendet
        uhr.vorstellen(Duration.ofSeconds(60));
        mvc.perform(get("/api/rennen/" + rennen))
                .andExpect(jsonPath("$.laeufe[0].status").value("BEENDET"))
                .andExpect(jsonPath("$.aktuellerLauf").value(2));

        // Lauf 2: Spurwechsel, Bernd fährt nun auf Spur 1
        aktion(post(basis + "2/start"))
                .andExpect(jsonPath("$.laeufe[1].spuren[0].fahrerName").value("Bernd Kurve"));
        uhr.vorstellen(Duration.ofSeconds(3));
        runde(basis + "2", 2);
        aktion(post(basis + "2/ende"))
                .andExpect(jsonPath("$.status").value("BEENDET"))
                .andExpect(jsonPath("$.ergebnis[0].fahrerName").value("Blitz"))
                .andExpect(jsonPath("$.ergebnis[0].runden").value(3))
                .andExpect(jsonPath("$.ergebnis[0].besteRundeMs").value(3000))
                .andExpect(jsonPath("$.ergebnis[1].runden").value(1));

        mvc.perform(get("/api/meisterschaften/" + meisterschaft + "/wertung"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fahrerName").value("Blitz"))
                .andExpect(jsonPath("$[0].punkte").value(10))
                .andExpect(jsonPath("$[1].punkte").value(8));

        mvc.perform(delete("/api/fahrer/" + anna))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/rennen"))
                .andExpect(jsonPath("$[?(@.id=='" + rennen + "')].beendeteLaeufe").value(2));
    }

    @Test
    void trainingMitFahrerwechselUndPauseFuerAnderesRennen() throws Exception {
        String clara = id(senden("/api/fahrer", """
                {"vorname":"Clara","nachname":"Gas"}"""));
        String dirk = id(senden("/api/fahrer", """
                {"vorname":"Dirk","nachname":"Bremse"}"""));
        String auto = id(senden("/api/fahrzeuge", """
                {"bezeichnung":"Mini Cooper"}"""));
        String auto2 = id(senden("/api/fahrzeuge", """
                {"bezeichnung":"BMW M1"}"""));
        String bahn = id(senden("/api/bahnen", """
                {"name":"Trainingsbahn","spurFarben":["#e53935","#1e88e5"]}"""));

        String training = id(senden("/api/rennen", """
                {"name":"Freies Training","bahnId":"%s","modus":"TRAINING","mindestRundenzeitMs":0,
                 "teilnehmer":[{"fahrerId":"%s","fahrzeugId":"%s","spurNummer":2}]}"""
                .formatted(bahn, clara, auto)));
        String rennen = id(senden("/api/rennen", """
                {"name":"Sprint","bahnId":"%s","modus":"RUNDENRENNEN","rundenZiel":5,
                 "teilnehmer":[{"fahrerId":"%s","fahrzeugId":"%s"}]}""".formatted(bahn, dirk, auto2)));

        aktion(post("/api/rennen/" + training + "/laeufe/1/start"))
                .andExpect(jsonPath("$.modus").value("TRAINING"))
                .andExpect(jsonPath("$.laeufe[0].spuren[0].spurNummer").value(2));
        uhr.vorstellen(Duration.ofSeconds(7));
        runde("/api/rennen/" + training + "/laeufe/1", 2);

        // Fahrerwechsel w\u00e4hrend der Sitzung
        aktion(put("/api/rennen/" + training + "/spuren/2").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fahrerId":"%s","fahrzeugId":"%s"}""".formatted(dirk, auto2)))
                .andExpect(jsonPath("$.laeufe[0].spuren[0].fahrerName").value("Dirk Bremse"))
                .andExpect(jsonPath("$.teilnehmer", hasSize(2)));
        uhr.vorstellen(Duration.ofSeconds(6));
        runde("/api/rennen/" + training + "/laeufe/1", 2)
                .andExpect(jsonPath("$.laeufe[0].spuren[0].letzteRundeMs").value(6000))
                .andExpect(jsonPath("$.ergebnis[0].fahrerName").value("Dirk Bremse"));

        // Bahn ist belegt, bis das Training pausiert wird
        mvc.perform(post("/api/rennen/" + rennen + "/laeufe/1/start"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Freies Training")));
        aktion(post("/api/rennen/" + training + "/pause"))
                .andExpect(jsonPath("$.status").value("PAUSIERT"))
                .andExpect(jsonPath("$.aktuellerLauf").value(2));
        aktion(post("/api/rennen/" + rennen + "/laeufe/1/start"));
        aktion(post("/api/rennen/" + rennen + "/abbruch"))
                .andExpect(jsonPath("$.status").value("ABGEBROCHEN"));

        aktion(post("/api/rennen/" + training + "/laeufe/2/start"));
        aktion(post("/api/rennen/" + training + "/ende"))
                .andExpect(jsonPath("$.status").value("BEENDET"))
                .andExpect(jsonPath("$.laeufe", hasSize(2)))
                .andExpect(jsonPath("$.ergebnis", hasSize(2)));
    }

    @Test
    void unbekanntesRennenLiefert404() throws Exception {
        mvc.perform(get("/api/rennen/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void ungueltigeEingabenLiefern422() throws Exception {
        mvc.perform(post("/api/fahrer").contentType(MediaType.APPLICATION_JSON).content("""
                        {"vorname":"","nachname":"X"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Vorname darf nicht leer sein"));
        mvc.perform(post("/api/bahnen").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"B","spurFarben":["rot"]}"""))
                .andExpect(status().isUnprocessableContent());
    }

    private String senden(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions aktion(org.springframework.test.web.servlet.RequestBuilder anfrage) throws Exception {
        return mvc.perform(anfrage).andExpect(status().isOk());
    }

    private ResultActions runde(String lauf, int spur) throws Exception {
        return mvc.perform(post(lauf + "/runden").contentType(MediaType.APPLICATION_JSON)
                .content("{\"spur\":" + spur + "}"));
    }

    private static String id(String json) {
        return JsonPath.read(json, "$.id");
    }
}
