# SlotRace – Hinweise für Copilot

Software zum Verwalten und Durchführen von Slotcar-Rennen. **Alles ist auf Deutsch**: UI-Texte, Fehlermeldungen, Doku und auch Bezeichner im Code (`Rennen`, `Lauf`, `Runde`, `Fahrer`, `erfasseRunde` …). Neue Bezeichner ebenfalls deutsch benennen.

## Befehle

- Backend (`backend/`, Maven Wrapper, Java 25):
  - `./mvnw test`: alle Tests; sie nutzen H2 im PostgreSQL-Modus über `src/test/resources/application.properties` und brauchen keine DB
  - Einzeltest: `./mvnw test -Dtest=RennenTest` oder `-Dtest=RennenTest#methode`
  - Starten: `./mvnw spring-boot:run` (vorher `docker compose up -d db`)
- Frontend (`frontend/`):
  - `npm run dev`: Vite auf :5173, leitet `/api` an :8080 weiter
  - `npm run build`: `tsc -b` und Vite
  - `npm run lint`: oxlint
  - `npm test`: Vitest; einzeln mit `npx vitest run src/format.test.ts`
- Alles zusammen: `docker compose up --build` (Oberfläche auf :8081, nginx leitet `/api` an das Backend weiter)

## Backend-Architektur (hexagonal, Paket `de.slotrace`)

- `domain`: reines Java, keine Abhängigkeit auf Spring, Jakarta, Hibernate, `application` oder `adapter`.
  - `model.Rennen` ist das Aggregat und enthält alle Rennregeln: Laufstart, Rundenerfassung, Mindestrundenzeit, automatisches Laufende bei Zeitablauf oder erreichtem Rundenziel, Status.
  - Services: `Laufplan` (Spurrotation), `Auswertung` (Rangliste), `Meisterschaftswertung`.
  - Regelverstöße werfen `RegelVerletzungException` (→ HTTP 422), fehlende Objekte `NichtGefundenException` (→ 404).
- `application`: Ports `port.in` (Anwendungsfälle) und `port.out` (Repositories), Services mit `@Service`/`@Transactional`.
  - `RennenService` lädt Rennen zur Bearbeitung mit pessimistischer Sperre (`findeZurBearbeitung`), damit parallele Rundenklicks nicht verloren gehen.
  - Die Zeit kommt über eine injizierte `Clock`.
- `adapter.in.web`: REST-Controller unter `/api`.
  - DTOs liegen in `Dtos.java`, die Abbildung in `DtoMapper`.
  - Fehler laufen über `FehlerBehandlung` als `ProblemDetail`; die deutsche Meldung steht in `detail`.
- `adapter.out.persistence`: JPA-Entities und Spring-Data-Repositories (`JpaRepositories.java`), je Port ein Adapter.
- `ArchitekturTest` (ArchUnit) prüft die Schichtregeln: `adapter.in` darf nicht auf `adapter.out`/`port.out` zugreifen und umgekehrt.

## Konventionen

- IDs sind UUIDs und werden in der Domäne erzeugt; Entities haben kein `@GeneratedValue`.
- Kind-Tabellen hängen über unidirektionales `@OneToMany` mit `@JoinColumn(nullable = false)` und Orphan-Removal; die FKs in der DB haben `ON DELETE CASCADE`.
- Schemaänderungen nur über neue Flyway-Migrationen in `src/main/resources/db/migration` (`ddl-auto=validate`). Enums als `@Enumerated(STRING)` mit `@JdbcTypeCode(SqlTypes.VARCHAR)`.
- Rangliste: mehr Runden, dann geringere Gesamtzeit (Summe je Lauf von Start bis letzte Runde).
- Frontend:
  - Typen und API-Client in `src/api.ts` müssen zu den Backend-DTOs passen.
  - Seiten liegen in `src/seiten`, wiederverwendbare Teile in `src/komponenten`.
  - Formatierung (deutsche Zeiten wie `1:23,456`) in `src/format.ts`.
  - CSS-Klassen deutsch benannt in `src/index.css`.
  - TypeScript mit `erasableSyntaxOnly`, also keine `enum`s.
