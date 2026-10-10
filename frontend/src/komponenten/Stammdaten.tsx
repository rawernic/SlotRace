import { useState, type FormEvent, type ReactNode } from 'react'
import { Fehler, Laden } from './Hinweis'
import { useDaten } from './hooks'

export interface Feld {
  name: string
  label: string
  pflicht?: boolean
  platzhalter?: string
}

interface Props<T extends { id: string }> {
  titel: string
  einzahl: string
  felder: Feld[]
  spalten: { titel: string; wert: (eintrag: T) => ReactNode }[]
  laden: () => Promise<T[]>
  zuFormular: (eintrag: T) => Record<string, string>
  speichern: (werte: Record<string, string>, id?: string) => Promise<unknown>
  loeschen: (id: string) => Promise<unknown>
  bezeichnung: (eintrag: T) => string
}

/** Generische Verwaltungsseite (Liste + Formular) für einfache Stammdaten. */
export function Stammdaten<T extends { id: string }>(props: Props<T>) {
  const { daten, fehler: ladeFehler, neuLaden } = useDaten(props.laden)
  const leer = Object.fromEntries(props.felder.map((f) => [f.name, '']))
  const [werte, setWerte] = useState<Record<string, string>>(leer)
  const [bearbeiteId, setBearbeiteId] = useState<string | null>(null)
  const [fehler, setFehler] = useState<string | null>(null)

  const zuruecksetzen = () => {
    setWerte(leer)
    setBearbeiteId(null)
    setFehler(null)
  }

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    try {
      await props.speichern(werte, bearbeiteId ?? undefined)
      zuruecksetzen()
      await neuLaden()
    } catch (err) {
      setFehler((err as Error).message)
    }
  }

  const loeschen = async (eintrag: T) => {
    if (!confirm(`${props.einzahl} „${props.bezeichnung(eintrag)}“ wirklich löschen?`)) return
    try {
      await props.loeschen(eintrag.id)
      if (bearbeiteId === eintrag.id) zuruecksetzen()
      await neuLaden()
    } catch (err) {
      setFehler((err as Error).message)
    }
  }

  return (
    <section>
      <h1>{props.titel}</h1>
      <form className="karte formular" onSubmit={absenden}>
        <h2>{bearbeiteId ? `${props.einzahl} bearbeiten` : `${props.einzahl} anlegen`}</h2>
        <div className="felder">
          {props.felder.map((f) => (
            <label key={f.name}>
              {f.label}
              {f.pflicht && ' *'}
              <input
                value={werte[f.name] ?? ''}
                required={f.pflicht}
                placeholder={f.platzhalter}
                onChange={(e) => setWerte({ ...werte, [f.name]: e.target.value })}
              />
            </label>
          ))}
        </div>
        <Fehler text={fehler} />
        <div className="aktionen">
          <button type="submit" className="primaer">{bearbeiteId ? 'Speichern' : 'Anlegen'}</button>
          {bearbeiteId && <button type="button" onClick={zuruecksetzen}>Abbrechen</button>}
        </div>
      </form>

      <Fehler text={ladeFehler} />
      {!daten ? <Laden /> : daten.length === 0 ? (
        <p className="gedimmt">Noch keine Einträge vorhanden.</p>
      ) : (
        <table>
          <thead>
            <tr>
              {props.spalten.map((s) => <th key={s.titel}>{s.titel}</th>)}
              <th />
            </tr>
          </thead>
          <tbody>
            {daten.map((eintrag) => (
              <tr key={eintrag.id}>
                {props.spalten.map((s) => <td key={s.titel}>{s.wert(eintrag)}</td>)}
                <td className="rechts">
                  <button onClick={() => { setBearbeiteId(eintrag.id); setWerte(props.zuFormular(eintrag)); setFehler(null) }}>
                    Bearbeiten
                  </button>{' '}
                  <button className="gefahr" onClick={() => loeschen(eintrag)}>Löschen</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
