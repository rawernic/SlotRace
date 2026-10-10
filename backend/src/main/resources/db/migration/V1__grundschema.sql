CREATE TABLE fahrer (
    id        UUID PRIMARY KEY,
    vorname   VARCHAR(100) NOT NULL,
    nachname  VARCHAR(100) NOT NULL,
    spitzname VARCHAR(100)
);

CREATE TABLE fahrzeug (
    id          UUID PRIMARY KEY,
    bezeichnung VARCHAR(200) NOT NULL,
    hersteller  VARCHAR(100),
    massstab    VARCHAR(20)
);

CREATE TABLE bahn (
    id   UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE spur (
    bahn_id UUID        NOT NULL REFERENCES bahn (id) ON DELETE CASCADE,
    nummer  INTEGER     NOT NULL,
    farbe   VARCHAR(7)  NOT NULL,
    PRIMARY KEY (bahn_id, nummer)
);

CREATE TABLE meisterschaft (
    id   UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE meisterschaft_punkte (
    meisterschaft_id UUID    NOT NULL REFERENCES meisterschaft (id) ON DELETE CASCADE,
    position         INTEGER NOT NULL,
    punkte           INTEGER NOT NULL,
    PRIMARY KEY (meisterschaft_id, position)
);

CREATE TABLE rennen (
    id                    UUID PRIMARY KEY,
    name                  VARCHAR(100)             NOT NULL,
    bahn_id               UUID                     NOT NULL REFERENCES bahn (id),
    meisterschaft_id      UUID REFERENCES meisterschaft (id),
    modus                 VARCHAR(20)              NOT NULL,
    lauf_dauer_sekunden   INTEGER,
    runden_ziel           INTEGER,
    mindest_rundenzeit_ms INTEGER                  NOT NULL,
    status                VARCHAR(20)              NOT NULL,
    erstellt_am           TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_rennen_meisterschaft ON rennen (meisterschaft_id);

CREATE TABLE teilnehmer (
    id          UUID PRIMARY KEY,
    rennen_id   UUID    NOT NULL REFERENCES rennen (id) ON DELETE CASCADE,
    fahrer_id   UUID    NOT NULL REFERENCES fahrer (id),
    fahrzeug_id UUID    NOT NULL REFERENCES fahrzeug (id),
    startnummer INTEGER NOT NULL
);
CREATE INDEX idx_teilnehmer_rennen ON teilnehmer (rennen_id);

CREATE TABLE lauf (
    id           UUID PRIMARY KEY,
    rennen_id    UUID        NOT NULL REFERENCES rennen (id) ON DELETE CASCADE,
    nummer       INTEGER     NOT NULL,
    status       VARCHAR(20) NOT NULL,
    gestartet_am TIMESTAMP WITH TIME ZONE,
    beendet_am   TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_lauf_rennen ON lauf (rennen_id);

-- Kindtabellen kaskadieren auch über teilnehmer_id, damit Hibernate beim Neuerzeugen
-- des Laufplans Teilnehmer und Läufe in beliebiger Reihenfolge löschen kann.
CREATE TABLE belegung (
    lauf_id       UUID    NOT NULL REFERENCES lauf (id) ON DELETE CASCADE,
    spur_nummer   INTEGER NOT NULL,
    teilnehmer_id UUID    NOT NULL REFERENCES teilnehmer (id) ON DELETE CASCADE,
    PRIMARY KEY (lauf_id, spur_nummer)
);

CREATE TABLE runde (
    id            UUID PRIMARY KEY,
    lauf_id       UUID                     NOT NULL REFERENCES lauf (id) ON DELETE CASCADE,
    teilnehmer_id UUID                     NOT NULL REFERENCES teilnehmer (id) ON DELETE CASCADE,
    spur_nummer   INTEGER                  NOT NULL,
    nummer        INTEGER                  NOT NULL,
    zeitpunkt     TIMESTAMP WITH TIME ZONE NOT NULL,
    rundenzeit_ms BIGINT                   NOT NULL
);
CREATE INDEX idx_runde_lauf ON runde (lauf_id);
