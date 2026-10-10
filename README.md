# SlotRace

Eine Software, um Slotcar-Rennen zu verwalten und durchzuführen.

## Funktionen

- **Stammdaten:** Fahrer, Fahrzeuge und Bahnen (1–8 Spuren mit eigener Spurfarbe)
- **Rennen** im Modus *Zeitrennen* (feste Laufdauer) oder *Rundenrennen* (feste Rundenzahl pro Lauf)
- **Läufe mit Spurrotation:** Jeder Fahrer fährt jede Spur einmal; der Laufplan wird automatisch erzeugt
- **Rennleitung (live):** Runden per Klick auf die Spur oder per Tastatur erfassen (`1`–`8`, `Umschalt`+Ziffer = Korrektur, `Leertaste` = Start mit Startampel, `Esc` = Laufende), mit Mindestrundenzeit gegen Doppelklicks und Vollbildmodus
- **Ergebnisse:** Rangliste nach Runden, danach Gesamtzeit, mit bester Runde und Rückstand
- **Meisterschaften:** Punkteschema frei konfigurierbar (Standard 10-8-6-5-4-3-2-1); es zählen nur beendete Rennen, bei Punktgleichheit entscheiden die Siege

## Architektur

| Teil | Technik |
|------|---------|
| `backend/` | Spring Boot 4, Java 25, Maven, hexagonale Architektur, Flyway |
| `frontend/` | React 19, TypeScript, Vite, React Router |
| Datenbank | PostgreSQL 17 |

Das Backend ist nach Ports & Adapters aufgebaut (`de.slotrace`):

- `domain` – Fachmodell und Regeln (`Rennen` als Aggregat, `Laufplan`, `Auswertung`, `Meisterschaftswertung`), ohne Framework-Abhängigkeiten
- `application` – Anwendungsfälle (`port.in`), Repository-Schnittstellen (`port.out`), Services
- `adapter.in.web` – REST-API unter `/api`, Fehler als `ProblemDetail`
- `adapter.out.persistence` – JPA-Entities und Repositories

Die Schichtregeln prüft `ArchitekturTest` (ArchUnit).

## Starten

Komplett mit Docker:

```bash
docker compose up --build      # Oberfläche: http://localhost:8081
```

Für die Entwicklung:

```bash
docker compose up -d db                 # PostgreSQL
cd backend && ./mvnw spring-boot:run    # API auf :8080
cd frontend && npm install && npm run dev   # Oberfläche auf :5173 (leitet /api weiter)
```

Datenbankzugang über `SLOTRACE_DB_URL`, `SLOTRACE_DB_USER`, `SLOTRACE_DB_PASSWORD` (Standard: lokale DB `slotrace`/`slotrace`).

## Tests

```bash
cd backend && ./mvnw test                          # alle Backend-Tests (H2, keine DB nötig)
cd backend && ./mvnw test -Dtest=RennenTest        # einzelne Testklasse
cd backend && ./mvnw test -Dtest=RennenTest#methode
cd frontend && npm test                            # Vitest
cd frontend && npm run lint                        # oxlint
cd frontend && npm run build                       # Typprüfung + Build
```
