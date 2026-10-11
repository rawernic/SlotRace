import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router'
import { api, type Fahrer, type Fahrzeug, type Lauf, type RennenDetail, type SpurStand } from '../api'
import { ErgebnisTabelle } from '../komponenten/Ergebnis'
import { Fehler, Laden } from '../komponenten/Hinweis'
import { StatusMarke } from '../komponenten/Status'
import { useDaten } from '../komponenten/hooks'
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
  const { daten: stamm } = useDaten(async () => {
    const [fahrer, fahrzeuge] = await Promise.all([api.fahrer.alle(), api.fahrzeuge.alle()])
    return { fahrer, fahrzeuge }
  })

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
      // im Training folgt die Anzeige automatisch der aktuellen Sitzung
      if (r.modus === 'TRAINING') return
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

  const training = rennen?.modus === 'TRAINING'
  const abgeschlossen = rennen?.status === 'BEENDET' || rennen?.status === 'ABGEBROCHEN'
  const lauf: Lauf | undefined = rennen?.laeufe.find((l) => l.nummer === (gewaehlterLauf ?? (training ? rennen.aktuellerLauf : null)))

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
    if (!lauf || lauf.status !== 'BEREIT' || ampel !== null || rennen?.status === 'ABGEBROCHEN') return
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
    if (training) {
      void ausfuehren(() => api.rennen.beendeLauf(id, lauf.nummer)).then((r) => { if (r) setGewaehlterLauf(null) })
      return
    }
    if (!confirm(`Lauf ${lauf.nummer} jetzt beenden?`)) return
    void ausfuehren(() => api.rennen.beendeLauf(id, lauf.nummer))
  }, [ausfuehren, id, lauf, training])

  const zuruecksetzen = () => {
    const was = training ? `Sitzung ${lauf?.nummer} verwerfen` : `Lauf ${lauf?.nummer} zurücksetzen`
    if (!lauf || !confirm(`${was}? Alle Runden ${training ? 'dieser Sitzung' : 'dieses Laufs'} werden gelöscht.`)) return
    void ausfuehren(() => api.rennen.setzeLaufZurueck(id, lauf.nummer))
  }

  const pausieren = () => {
    if (!confirm('Rennen pausieren? Der laufende Lauf wird zurückgesetzt und muss später neu gestartet werden. '
      + 'Bereits beendete Läufe bleiben erhalten.')) return
    void ausfuehren(() => api.rennen.pausieren(id))
  }

  const abbrechen = () => {
    if (!rennen || !confirm(`„${rennen.name}“ abbrechen? ${training ? 'Das Training' : 'Das Rennen'} kann danach nicht `
      + 'fortgesetzt werden und zählt nicht für die Meisterschaft.')) return
    void ausfuehren(() => api.rennen.abbrechen(id))
  }

  const trainingBeenden = () => {
    if (!confirm('Training beenden?')) return
    void ausfuehren(() => api.rennen.beendeTraining(id)).then((r) => {
      if (r) setGewaehlterLauf(r.laeufe.at(-1)?.nummer ?? null)
    })
  }

  const wechsle = (spur: number, fahrerId: string | null, fahrzeugId: string | null) =>
    void ausfuehren(() => api.rennen.wechsleBelegung(id, spur, fahrerId, fahrzeugId))

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
  const laufName = training ? 'Sitzung' : 'Lauf'
  const wechselbar = training && !abgeschlossen && lauf !== undefined && lauf.nummer === rennen.aktuellerLauf
  const belegteFahrer = new Set(lauf?.spuren.map((s) => fahrerVon(rennen, s)?.fahrerId))
  const wahl = (spur: number, stand?: SpurStand) => {
    if (!wechselbar || !stamm) return undefined
    const t = stand && fahrerVon(rennen, stand)
    return (
      <BelegungWahl fahrerId={t?.fahrerId ?? null} fahrzeugId={t?.fahrzeugId ?? null} fahrer={stamm.fahrer}
        fahrzeuge={stamm.fahrzeuge} belegteFahrer={belegteFahrer}
        onWechsel={(fahrerId, fahrzeugId) => wechsle(spur, fahrerId, fahrzeugId)} />
    )
  }

  return (
    <section className="rennleitung">
      <div className="kopfzeile">
        <div>
          <p className="klein"><Link to={`/rennen/${rennen.id}`}>← {rennen.name}</Link></p>
          <h1>{training ? 'Training' : 'Rennleitung'} <StatusMarke status={rennen.status} /></h1>
          <p className="gedimmt">{modusBeschreibung(rennen.modus, rennen.laufDauerSekunden, rennen.rundenZiel)} · {rennen.bahn.name}</p>
        </div>
        <div className="aktionen">
          {!training && rennen.status === 'LAEUFT' && <button onClick={pausieren}>Pausieren</button>}
          {training && (rennen.status === 'LAEUFT' || rennen.status === 'PAUSIERT') && (
            <button onClick={trainingBeenden}>Training beenden</button>
          )}
          {!abgeschlossen && <button className="gefahr" onClick={abbrechen}>Abbrechen</button>}
          <button onClick={() => void document.documentElement.requestFullscreen?.()}>Vollbild</button>
        </div>
      </div>

      {rennen.status === 'PAUSIERT' && (
        <div className="hinweis">
          {training ? 'Das Training' : 'Das Rennen'} ist pausiert, die Bahn ist frei für andere Rennen. Zum Fortsetzen
          {training ? ' die nächste Sitzung' : ' den nächsten Lauf'} starten.
        </div>
      )}
      {rennen.status === 'ABGEBROCHEN' && <div className="hinweis">Dieses Rennen wurde abgebrochen.</div>}

      <nav className="laufwahl" aria-label={training ? 'Sitzungen' : 'Läufe'}>
        {rennen.laeufe.map((l) => (
          <button key={l.nummer} className={`lauf-${l.status.toLowerCase()} ${l.nummer === lauf?.nummer ? 'aktiv' : ''}`}
            onClick={() => setGewaehlterLauf(l.nummer)}>
            {laufName} {l.nummer}
          </button>
        ))}
      </nav>

      {lauf && (
        <>
          <div className="laufkopf">
            <div>
              <h2>
                {laufName} {lauf.nummer}{!training && <> von {rennen.laeufe.length}</>} <StatusMarke status={lauf.status} art="lauf" />
              </h2>
              {rennen.status !== 'ABGEBROCHEN' && (
                <div className="aktionen">
                  {lauf.status === 'BEREIT' && <button className="primaer gross" onClick={starten} disabled={ampel !== null}>Start (Leertaste)</button>}
                  {lauf.status === 'LAEUFT' && (
                    <button className={`${training ? '' : 'gefahr '}gross`} onClick={beenden}>
                      {training ? 'Pause (Esc)' : 'Lauf beenden (Esc)'}
                    </button>
                  )}
                  {lauf.status !== 'BEREIT' && (!training || wechselbar) && (
                    <button onClick={zuruecksetzen}>{training ? 'Sitzung verwerfen' : 'Lauf zurücksetzen'}</button>
                  )}
                  {training && lauf.nummer !== rennen.aktuellerLauf && rennen.aktuellerLauf !== null && (
                    <button className="primaer gross" onClick={() => setGewaehlterLauf(null)}>Zur aktuellen Sitzung →</button>
                  )}
                  {!training && lauf.status === 'BEENDET' && naechsterLauf && (
                    <button className="primaer gross" onClick={() => setGewaehlterLauf(naechsterLauf.nummer)}>
                      Weiter zu Lauf {naechsterLauf.nummer} →
                    </button>
                  )}
                </div>
              )}
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
                  besteImLauf={besteImLauf} training={training} wahl={wahl(spur.nummer, stand)}
                  onRunde={() => runde(spur.nummer)} onZurueck={() => rundeZurueck(spur.nummer)} />
              ) : (
                <div key={spur.nummer} className={`spurkarte ${wechselbar ? '' : 'leer'}`} style={{ borderColor: spur.farbe }}>
                  <div className="spurkopf" style={{ background: spur.farbe }}>Spur {spur.nummer}</div>
                  <p className="gedimmt">nicht belegt</p>
                  {wahl(spur.nummer)}
                </div>
              )
            })}
          </div>
          <p className="gedimmt klein">
            Tastatur: <kbd>1</kbd>–<kbd>8</kbd> Runde auf Spur · <kbd>Umschalt</kbd>+Ziffer letzte Runde entfernen ·{' '}
            <kbd>Leertaste</kbd> Start · <kbd>Esc</kbd> {training ? 'Pause' : 'Lauf beenden'} · Klick auf eine Spur zählt ebenfalls eine Runde
          </p>
        </>
      )}

      <h2>{training ? 'Bestzeiten' : 'Gesamtstand'}</h2>
      <ErgebnisTabelle ergebnis={rennen.ergebnis} training={training} />
    </section>
  )
}

function fahrerVon(rennen: RennenDetail, stand: SpurStand) {
  return rennen.teilnehmer.find((t) => t.id === stand.teilnehmerId)
}

interface BelegungWahlProps {
  fahrerId: string | null
  fahrzeugId: string | null
  fahrer: Fahrer[]
  fahrzeuge: Fahrzeug[]
  belegteFahrer: Set<string | undefined>
  onWechsel: (fahrerId: string | null, fahrzeugId: string | null) => void
}

/** Auswahl von Fahrer und Fahrzeug einer Spur im Training; wirkt sofort, auch während der Sitzung. */
function BelegungWahl({ fahrerId, fahrzeugId, fahrer, fahrzeuge, belegteFahrer, onWechsel }: BelegungWahlProps) {
  return (
    <div className="belegungswahl" onClick={(e) => e.stopPropagation()}>
      <select value={fahrerId ?? ''} aria-label="Fahrer" onChange={(e) => {
        e.currentTarget.blur()
        onWechsel(e.target.value || null, e.target.value ? fahrzeugId ?? fahrzeuge[0]?.id ?? null : null)
      }}>
        <option value="">– frei –</option>
        {fahrer.filter((f) => f.id === fahrerId || !belegteFahrer.has(f.id)).map((f) => (
          <option key={f.id} value={f.id}>{f.anzeigename}</option>
        ))}
      </select>
      {fahrerId && (
        <select value={fahrzeugId ?? ''} aria-label="Fahrzeug" onChange={(e) => {
          e.currentTarget.blur()
          onWechsel(fahrerId, e.target.value)
        }}>
          {fahrzeuge.map((f) => <option key={f.id} value={f.id}>{f.bezeichnung}</option>)}
        </select>
      )}
    </div>
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
    beschriftung = rennen.modus === 'TRAINING' ? 'Sitzungszeit' : 'Laufzeit'
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
  training: boolean
  wahl?: ReactNode
  onRunde: () => void
  onZurueck: () => void
}

const ANGEZEIGTE_RUNDEN = 5

function SpurKarte({ stand, rundenZiel, aktiv, besteImLauf, training, wahl, onRunde, onZurueck }: SpurKarteProps) {
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
      {wahl}
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
      {training && stand.rundenzeitenMs.length > 0 && (
        <ol className="rundenliste" aria-label="Letzte Rundenzeiten">
          {stand.rundenzeitenMs.map((ms, i) => ({ ms, nr: i + 1 })).slice(-ANGEZEIGTE_RUNDEN).reverse().map(({ ms, nr }) => (
            <li key={nr} className={ms === stand.besteRundeMs ? 'bestzeit' : ''}>
              <span className="gedimmt">R{nr}</span> {formatZeit(ms)}
            </li>
          ))}
        </ol>
      )}
      <button className="zurueck" onClick={(e) => { e.stopPropagation(); onZurueck() }} title="Letzte Runde entfernen">
        − Runde
      </button>
    </div>
  )
}
