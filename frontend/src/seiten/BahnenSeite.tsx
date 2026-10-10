import { useState, type FormEvent } from 'react'
import { api, type Bahn } from '../api'
import { Fehler, Laden } from '../komponenten/Hinweis'
import { useDaten } from '../komponenten/hooks'

const STANDARD_FARBEN = ['#e53935', '#1e88e5', '#43a047', '#fdd835', '#8e24aa', '#fb8c00', '#00acc1', '#f5f5f5']
const MAX_SPUREN = 8

export function BahnenSeite() {
  const { daten: bahnen, fehler: ladeFehler, neuLaden } = useDaten(api.bahnen.alle)
  const [name, setName] = useState('')
  const [farben, setFarben] = useState<string[]>(STANDARD_FARBEN.slice(0, 4))
  const [bearbeiteId, setBearbeiteId] = useState<string | null>(null)
  const [fehler, setFehler] = useState<string | null>(null)

  const zuruecksetzen = () => {
    setName('')
    setFarben(STANDARD_FARBEN.slice(0, 4))
    setBearbeiteId(null)
    setFehler(null)
  }

  const spurAnzahlSetzen = (anzahl: number) => {
    const n = Math.min(MAX_SPUREN, Math.max(1, anzahl))
    setFarben(Array.from({ length: n }, (_, i) => farben[i] ?? STANDARD_FARBEN[i]))
  }

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    try {
      const daten = { name, spurFarben: farben }
      if (bearbeiteId) await api.bahnen.aendern(bearbeiteId, daten)
      else await api.bahnen.anlegen(daten)
      zuruecksetzen()
      await neuLaden()
    } catch (err) {
      setFehler((err as Error).message)
    }
  }

  const bearbeiten = (b: Bahn) => {
    setBearbeiteId(b.id)
    setName(b.name)
    setFarben(b.spuren.map((s) => s.farbe))
    setFehler(null)
  }

  const loeschen = async (b: Bahn) => {
    if (!confirm(`Bahn „${b.name}“ wirklich löschen?`)) return
    try {
      await api.bahnen.loeschen(b.id)
      await neuLaden()
    } catch (err) {
      setFehler((err as Error).message)
    }
  }

  return (
    <section>
      <h1>Bahnen</h1>
      <form className="karte formular" onSubmit={absenden}>
        <h2>{bearbeiteId ? 'Bahn bearbeiten' : 'Bahn anlegen'}</h2>
        <div className="felder">
          <label>
            Name *
            <input value={name} required onChange={(e) => setName(e.target.value)} />
          </label>
          <label>
            Anzahl Spuren
            <input
              type="number"
              min={1}
              max={MAX_SPUREN}
              value={farben.length}
              onChange={(e) => spurAnzahlSetzen(Number(e.target.value))}
            />
          </label>
        </div>
        <div className="spurfarben">
          {farben.map((farbe, i) => (
            <label key={i} className="spurfarbe">
              Spur {i + 1}
              <input
                type="color"
                value={farbe}
                onChange={(e) => setFarben(farben.map((f, j) => (j === i ? e.target.value : f)))}
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
      {!bahnen ? <Laden /> : bahnen.length === 0 ? (
        <p className="gedimmt">Noch keine Bahnen vorhanden.</p>
      ) : (
        <table>
          <thead>
            <tr><th>Name</th><th>Spuren</th><th /></tr>
          </thead>
          <tbody>
            {bahnen.map((b) => (
              <tr key={b.id}>
                <td>{b.name}</td>
                <td>
                  {b.spuren.map((s) => (
                    <span key={s.nummer} className="spurpunkt" style={{ background: s.farbe }} title={`Spur ${s.nummer}`}>
                      {s.nummer}
                    </span>
                  ))}
                </td>
                <td className="rechts">
                  <button onClick={() => bearbeiten(b)}>Bearbeiten</button>{' '}
                  <button className="gefahr" onClick={() => loeschen(b)}>Löschen</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
