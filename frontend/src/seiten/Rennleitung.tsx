import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, type Lauf, type RennenDetail, type SpurStand } from '../api'
import { ErgebnisTabelle } from '../komponenten/Ergebnis'
import { Fehler, Laden } from '../komponenten/Hinweis'
import { StatusMarke } from '../komponenten/Status'
import { formatUhr, formatZeit, modusBeschreibung } from '../format'

const ABFRAGE_INTERVALL_MS = 500
const AMPEL_SEKUNDEN = 3

interface Meldung {
  text: string
  art: 'fehler' | 'info'
}

/** Live-Ansicht zur Durchführung eines Rennens: Läufe starten, Runden per Klick/Tastatur erfassen. */
export function Rennleitung() {
  const { id = '' } = useParams()
  const [rennen, setRennen] = useState<RennenDetail | null>(null)
  const [ladeFehler, setLadeFehler] = useState<string | null>(null)
  const [gewaehlterLauf, setGewaehlterLauf] = useState<number | null>(null)
  const [ampel, setAmpel] = useState<number | null>(null)
  const [meldung, setMeldung] = useState<Meldung | null>(null)
  const [jetzt, setJetzt] = useState(() => Date.now())
  const [versatzMs, setVersatzMs] = useState(0)
  const anfrageNr = useRef(0)
  const angewendetNr = useRef(0)
  const meldungTimer = useRef<number | undefined>(undefined)

  const zeigeMeldung = useCallback((text: string, art: Meldung['art'] = 'fehler') => {
    setMeldung({ text, art })
    window.clearTimeout(meldungTimer.current)
    meldungTimer.current = window.setTimeout(() => setMeldung(null), 2500)
  }, [])

  /** Übernimmt eine Serverantwort – ältere Antworten (z. B. überholte Abfragen) werden verworfen. */
  const ausfuehren = useCallback(async (aufruf: () => Promise<RennenDetail>) => {
    const nr = ++anfrageNr.current
    try {
      const antwort = await aufruf()
      if (nr < angewendetNr.current) return antwort
      angewendetNr.current = nr
      setVersatzMs(new Date(antwort.serverZeit).getTime() - Date.now())
      setRennen(antwort)
      setLadeFehler(null)
      return antwort
    } catch (e) {
      zeigeMeldung((e as Error).message)
      return null
    }
  }, [zeigeMeldung])

  useEffect(() => {
    let aktiv = true
    api.rennen.hole(id).then((r) => {
      if (!aktiv) return
      setVersatzMs(new Date(r.serverZeit).getTime() - Date.now())
      setRennen(r)
      setGewaehlterLauf(r.laeufe.find((l) => l.status === 'LAEUFT')?.nummer ?? r.aktuellerLauf ?? r.laeufe.at(-1)?.nummer ?? null)
    }, (e: Error) => setLadeFehler(e.message))
    const abfrage = window.setInterval(() => void ausfuehren(() => api.rennen.hole(id)), ABFRAGE_INTERVALL_MS)
    const uhr = window.setInterval(() => setJetzt(Date.now()), 100)
    return () => {
      aktiv = false
      window.clearInterval(abfrage)
      window.clearInterval(uhr)
    }
  }, [id, ausfuehren])

  const lauf: Lauf | undefined = rennen?.laeufe.find((l) => l.nummer === gewaehlterLauf)

  const runde = useCallback((spur: number) => {
    if (!lauf || lauf.status !== 'LAEUFT') return
    void ausfuehren(() => api.rennen.runde(id, lauf.nummer, spur))
  }, [ausfuehren, id, lauf])

  const rundeZurueck = useCallback((spur: number) => {
    if (!lauf || lauf.status === 'BEREIT') return
    void ausfuehren(() => api.rennen.rundeZurueck(id, lauf.nummer, spur)).then((r) => {
      if (r) zeigeMeldung(`Letzte Runde auf Spur ${spur} entfernt`, 'info')
    })
  }, [ausfuehren, id, lauf, zeigeMeldung])

  const starten = useCallback(() => {
    if (!lauf || lauf.status !== 'BEREIT' || ampel !== null) return
    if (rennen?.laeufe.some((l) => l.status === 'LAEUFT')) {
      zeigeMeldung('Es läuft bereits ein anderer Lauf')
      return
    }
    let rest = AMPEL_SEKUNDEN
    setAmpel(rest)
    const timer = window.setInterval(() => {
      rest -= 1
      if (rest > 0) {
        setAmpel(rest)
        return
      }
      window.clearInterval(timer)
      setAmpel(null)
      void ausfuehren(() => api.rennen.starteLauf(id, lauf.nummer))
    }, 1000)
  }, [ampel, ausfuehren, id, lauf, rennen, zeigeMeldung])

  const beenden = useCallback(() => {
    if (!lauf || lauf.status !== 'LAEUFT') return
    if (!confirm(`Lauf ${lauf.nummer} jetzt beenden?`)) return
    void ausfuehren(() => api.rennen.beendeLauf(id, lauf.nummer))
  }, [ausfuehren, id, lauf])

  const zuruecksetzen = () => {
    if (!lauf || !confirm(`Lauf ${lauf.nummer} zurücksetzen? Alle Runden dieses Laufs werden gelöscht.`)) return
    void ausfuehren(() => api.rennen.setzeLaufZurueck(id, lauf.nummer))
  }

  useEffect(() => {
    const taste = (e: KeyboardEvent) => {
      if (e.repeat || (e.target as HTMLElement).closest('input, select, textarea')) return
      const ziffer = /^(Digit|Numpad)([1-8])$/.exec(e.code)
      if (ziffer) {
        e.preventDefault()
        const spur = Number(ziffer[2])
        if (e.shiftKey) rundeZurueck(spur)
        else runde(spur)
      } else if (e.code === 'Space') {
        e.preventDefault()
        starten()
      } else if (e.code === 'Escape') {
        beenden()
      }
    }
    window.addEventListener('keydown', taste)
    return () => window.removeEventListener('keydown', taste)
  }, [runde, rundeZurueck, starten, beenden])

  if (ladeFehler) return <Fehler text={ladeFehler} />
  if (!rennen) return <Laden />

  const serverJetzt = jetzt + versatzMs
  const naechsterLauf = rennen.laeufe.find((l) => l.status === 'BEREIT' && l.nummer !== lauf?.nummer)
  const besteImLauf = Math.min(...(lauf?.spuren.map((s) => s.besteRundeMs ?? Infinity) ?? [Infinity]))

  return (
    <section className="rennleitung">
      <div className="kopfzeile">
        <div>
          <p className="klein"><Link to={`/rennen/${rennen.id}`}>← {rennen.name}</Link></p>
          <h1>Rennleitung <StatusMarke status={rennen.status} /></h1>
          <p className="gedimmt">{modusBeschreibung(rennen.modus, rennen.laufDauerSekunden, rennen.rundenZiel)} · {rennen.bahn.name}</p>
        </div>
        <button onClick={() => void document.documentElement.requestFullscreen?.()}>Vollbild</button>
      </div>

      <nav className="laufwahl" aria-label="Läufe">
        {rennen.laeufe.map((l) => (
          <button key={l.nummer} className={`lauf-${l.status.toLowerCase()} ${l.nummer === lauf?.nummer ? 'aktiv' : ''}`}
            onClick={() => setGewaehlterLauf(l.nummer)}>
            Lauf {l.nummer}
          </button>
        ))}
      </nav>

      {lauf && (
        <>
          <div className="laufkopf">
            <div>
              <h2>Lauf {lauf.nummer} von {rennen.laeufe.length} <StatusMarke status={lauf.status} art="lauf" /></h2>
              <div className="aktionen">
                {lauf.status === 'BEREIT' && <button className="primaer gross" onClick={starten} disabled={ampel !== null}>Start (Leertaste)</button>}
                {lauf.status === 'LAEUFT' && <button className="gefahr gross" onClick={beenden}>Lauf beenden (Esc)</button>}
                {lauf.status !== 'BEREIT' && <button onClick={zuruecksetzen}>Lauf zurücksetzen</button>}
                {lauf.status === 'BEENDET' && naechsterLauf && (
                  <button className="primaer gross" onClick={() => setGewaehlterLauf(naechsterLauf.nummer)}>
                    Weiter zu Lauf {naechsterLauf.nummer} →
                  </button>
                )}
              </div>
            </div>
            <Uhr rennen={rennen} lauf={lauf} jetzt={serverJetzt} />
          </div>

          {ampel !== null && <div className="ampel" aria-live="assertive">{ampel}</div>}
          {meldung && <div className={`toast ${meldung.art}`} role="status">{meldung.text}</div>}

          <div className="spuren">
            {rennen.bahn.spuren.map((spur) => {
              const stand = lauf.spuren.find((s) => s.spurNummer === spur.nummer)
              return stand ? (
                <SpurKarte key={spur.nummer} stand={stand} rundenZiel={rennen.rundenZiel} aktiv={lauf.status === 'LAEUFT'}
                  besteImLauf={besteImLauf} onRunde={() => runde(spur.nummer)} onZurueck={() => rundeZurueck(spur.nummer)} />
              ) : (
                <div key={spur.nummer} className="spurkarte leer" style={{ borderColor: spur.farbe }}>
                  <div className="spurkopf" style={{ background: spur.farbe }}>Spur {spur.nummer}</div>
                  <p className="gedimmt">nicht belegt</p>
                </div>
              )
            })}
          </div>
          <p className="gedimmt klein">
            Tastatur: <kbd>1</kbd>–<kbd>8</kbd> Runde auf Spur · <kbd>Umschalt</kbd>+Ziffer letzte Runde entfernen ·{' '}
            <kbd>Leertaste</kbd> Start · <kbd>Esc</kbd> Lauf beenden · Klick auf eine Spur zählt ebenfalls eine Runde
          </p>
        </>
      )}

      <h2>Gesamtstand</h2>
      <ErgebnisTabelle ergebnis={rennen.ergebnis} />
    </section>
  )
}

function Uhr({ rennen, lauf, jetzt }: { rennen: RennenDetail; lauf: Lauf; jetzt: number }) {
  const start = lauf.gestartetAm ? new Date(lauf.gestartetAm).getTime() : null
  const ende = lauf.beendetAm ? new Date(lauf.beendetAm).getTime() : null
  let ms: number
  let beschriftung: string
  if (rennen.modus === 'ZEITRENNEN') {
    const dauer = (rennen.laufDauerSekunden ?? 0) * 1000
    beschriftung = 'Restzeit'
    ms = start === null ? dauer : Math.max(0, start + dauer - (ende ?? jetzt))
  } else {
    beschriftung = 'Laufzeit'
    ms = start === null ? 0 : (ende ?? jetzt) - start
  }
  const knapp = rennen.modus === 'ZEITRENNEN' && lauf.status === 'LAEUFT' && ms <= 10_000
  return (
    <div className={`uhr ${knapp ? 'knapp' : ''}`}>
      <span className="klein">{beschriftung}</span>
      <span className="ziffern">{formatUhr(ms)}</span>
    </div>
  )
}

interface SpurKarteProps {
  stand: SpurStand
  rundenZiel: number | null
  aktiv: boolean
  besteImLauf: number
  onRunde: () => void
  onZurueck: () => void
}

function SpurKarte({ stand, rundenZiel, aktiv, besteImLauf, onRunde, onZurueck }: SpurKarteProps) {
  const imZiel = rundenZiel !== null && stand.runden >= rundenZiel
  return (
    <div className={`spurkarte ${aktiv && !imZiel ? 'klickbar' : ''}`} style={{ borderColor: stand.farbe }}
      onClick={onRunde} role="button" tabIndex={-1} aria-label={`Runde auf Spur ${stand.spurNummer}`}>
      <div className="spurkopf" style={{ background: stand.farbe }}>
        <span>Spur {stand.spurNummer}</span>
        <span className="position">P{stand.position}</span>
      </div>
      <div className="fahrer">{stand.fahrerName}</div>
      <div className="gedimmt klein">{stand.fahrzeugName}</div>
      <div key={stand.runden} className="runden blitz">
        {stand.runden}
        {rundenZiel !== null && <span className="ziel"> / {rundenZiel}</span>}
      </div>
      {imZiel && <div className="marke status-beendet">Im Ziel</div>}
      <dl>
        <dt>Letzte</dt><dd>{formatZeit(stand.letzteRundeMs)}</dd>
        <dt>Beste</dt>
        <dd className={stand.besteRundeMs !== null && stand.besteRundeMs === besteImLauf ? 'bestzeit' : ''}>
          {formatZeit(stand.besteRundeMs)}
        </dd>
        <dt>Zeit</dt><dd>{formatZeit(stand.zeitMs)}</dd>
      </dl>
      <button className="zurueck" onClick={(e) => { e.stopPropagation(); onZurueck() }} title="Letzte Runde entfernen">
        − Runde
      </button>
    </div>
  )
}
