export type Rennmodus = 'ZEITRENNEN' | 'RUNDENRENNEN' | 'TRAINING'
export type Rennstatus = 'GEPLANT' | 'LAEUFT' | 'PAUSIERT' | 'BEENDET' | 'ABGEBROCHEN'
export type Laufstatus = 'BEREIT' | 'LAEUFT' | 'BEENDET'

export interface Fahrer {
  id: string
  vorname: string
  nachname: string
  spitzname: string | null
  anzeigename: string
}

export interface Fahrzeug {
  id: string
  bezeichnung: string
  hersteller: string | null
  massstab: string | null
}

export interface Spur {
  nummer: number
  farbe: string
}

export interface Bahn {
  id: string
  name: string
  spuren: Spur[]
}

export interface Meisterschaft {
  id: string
  name: string
  punkteSchema: number[]
}

export interface MeisterschaftsPlatz {
  platz: number
  fahrerId: string
  fahrerName: string
  punkte: number
  siege: number
  rennen: number
}

export interface RennenUebersicht {
  id: string
  name: string
  bahnId: string
  bahnName: string | null
  meisterschaftId: string | null
  modus: Rennmodus
  laufDauerSekunden: number | null
  rundenZiel: number | null
  status: Rennstatus
  erstelltAm: string
  teilnehmerAnzahl: number
  laufAnzahl: number
  beendeteLaeufe: number
}

export interface Teilnehmer {
  id: string
  startnummer: number
  fahrerId: string
  fahrerName: string
  fahrzeugId: string
  fahrzeugName: string
}

export interface SpurStand {
  spurNummer: number
  farbe: string
  teilnehmerId: string
  fahrerName: string
  fahrzeugName: string
  position: number
  runden: number
  zeitMs: number
  letzteRundeMs: number | null
  besteRundeMs: number | null
  rundenzeitenMs: number[]
}

export interface Lauf {
  nummer: number
  status: Laufstatus
  gestartetAm: string | null
  beendetAm: string | null
  endetAm: string | null
  spuren: SpurStand[]
}

export interface Platzierung {
  platz: number
  teilnehmerId: string
  startnummer: number
  fahrerName: string
  fahrzeugName: string
  runden: number
  gesamtzeitMs: number
  besteRundeMs: number | null
  gefahreneLaeufe: number
}

export interface RennenDetail {
  id: string
  name: string
  bahn: Bahn
  meisterschaftId: string | null
  meisterschaftName: string | null
  modus: Rennmodus
  laufDauerSekunden: number | null
  rundenZiel: number | null
  mindestRundenzeitMs: number
  status: Rennstatus
  erstelltAm: string
  serverZeit: string
  teilnehmer: Teilnehmer[]
  laeufe: Lauf[]
  aktuellerLauf: number | null
  ergebnis: Platzierung[]
}

export interface RennenAnfrage {
  name: string
  bahnId: string
  meisterschaftId: string | null
  modus: Rennmodus
  laufDauerSekunden: number | null
  rundenZiel: number | null
  mindestRundenzeitMs: number
  teilnehmer: { fahrerId: string; fahrzeugId: string; spurNummer?: number | null }[]
}

export class ApiFehler extends Error {}

async function anfrage<T>(methode: string, pfad: string, body?: unknown): Promise<T> {
  const antwort = await fetch(`/api${pfad}`, {
    method: methode,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!antwort.ok) {
    let meldung = `Fehler ${antwort.status}`
    try {
      const problem = await antwort.json()
      meldung = problem.detail ?? problem.title ?? meldung
    } catch {
      // keine JSON-Antwort
    }
    throw new ApiFehler(meldung)
  }
  return antwort.status === 204 ? (undefined as T) : antwort.json()
}

const get = <T>(pfad: string) => anfrage<T>('GET', pfad)
const post = <T>(pfad: string, body?: unknown) => anfrage<T>('POST', pfad, body ?? {})
const put = <T>(pfad: string, body: unknown) => anfrage<T>('PUT', pfad, body)
const del = <T = void>(pfad: string) => anfrage<T>('DELETE', pfad)

export const api = {
  fahrer: {
    alle: () => get<Fahrer[]>('/fahrer'),
    anlegen: (d: Partial<Fahrer>) => post<Fahrer>('/fahrer', d),
    aendern: (id: string, d: Partial<Fahrer>) => put<Fahrer>(`/fahrer/${id}`, d),
    loeschen: (id: string) => del(`/fahrer/${id}`),
  },
  fahrzeuge: {
    alle: () => get<Fahrzeug[]>('/fahrzeuge'),
    anlegen: (d: Partial<Fahrzeug>) => post<Fahrzeug>('/fahrzeuge', d),
    aendern: (id: string, d: Partial<Fahrzeug>) => put<Fahrzeug>(`/fahrzeuge/${id}`, d),
    loeschen: (id: string) => del(`/fahrzeuge/${id}`),
  },
  bahnen: {
    alle: () => get<Bahn[]>('/bahnen'),
    anlegen: (d: { name: string; spurFarben: string[] }) => post<Bahn>('/bahnen', d),
    aendern: (id: string, d: { name: string; spurFarben: string[] }) => put<Bahn>(`/bahnen/${id}`, d),
    loeschen: (id: string) => del(`/bahnen/${id}`),
  },
  meisterschaften: {
    alle: () => get<Meisterschaft[]>('/meisterschaften'),
    hole: (id: string) => get<Meisterschaft>(`/meisterschaften/${id}`),
    anlegen: (d: Partial<Meisterschaft>) => post<Meisterschaft>('/meisterschaften', d),
    aendern: (id: string, d: Partial<Meisterschaft>) => put<Meisterschaft>(`/meisterschaften/${id}`, d),
    loeschen: (id: string) => del(`/meisterschaften/${id}`),
    rennen: (id: string) => get<RennenUebersicht[]>(`/meisterschaften/${id}/rennen`),
    wertung: (id: string) => get<MeisterschaftsPlatz[]>(`/meisterschaften/${id}/wertung`),
  },
  rennen: {
    alle: () => get<RennenUebersicht[]>('/rennen'),
    hole: (id: string) => get<RennenDetail>(`/rennen/${id}`),
    planen: (d: RennenAnfrage) => post<RennenDetail>('/rennen', d),
    aendern: (id: string, d: RennenAnfrage) => put<RennenDetail>(`/rennen/${id}`, d),
    loeschen: (id: string) => del(`/rennen/${id}`),
    starteLauf: (id: string, lauf: number) => post<RennenDetail>(`/rennen/${id}/laeufe/${lauf}/start`),
    runde: (id: string, lauf: number, spur: number) =>
      post<RennenDetail>(`/rennen/${id}/laeufe/${lauf}/runden`, { spur }),
    rundeZurueck: (id: string, lauf: number, spur: number) =>
      del<RennenDetail>(`/rennen/${id}/laeufe/${lauf}/runden/letzte?spur=${spur}`),
    beendeLauf: (id: string, lauf: number) => post<RennenDetail>(`/rennen/${id}/laeufe/${lauf}/ende`),
    setzeLaufZurueck: (id: string, lauf: number) =>
      post<RennenDetail>(`/rennen/${id}/laeufe/${lauf}/zuruecksetzen`),
    pausieren: (id: string) => post<RennenDetail>(`/rennen/${id}/pause`),
    abbrechen: (id: string) => post<RennenDetail>(`/rennen/${id}/abbruch`),
    beendeTraining: (id: string) => post<RennenDetail>(`/rennen/${id}/ende`),
    wechsleBelegung: (id: string, spur: number, fahrerId: string | null, fahrzeugId: string | null) =>
      put<RennenDetail>(`/rennen/${id}/spuren/${spur}`, { fahrerId, fahrzeugId }),
  },
}
