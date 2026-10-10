import { Link } from 'react-router'
import { api } from '../api'
import { Fehler, Laden } from '../komponenten/Hinweis'
import { useDaten } from '../komponenten/hooks'
import { RennenTabelle } from './RennenListe'

export function Start() {
  const { daten, fehler } = useDaten(async () => {
    const [fahrer, fahrzeuge, bahnen, rennen, meisterschaften] = await Promise.all([
      api.fahrer.alle(), api.fahrzeuge.alle(), api.bahnen.alle(), api.rennen.alle(), api.meisterschaften.alle(),
    ])
    return { fahrer, fahrzeuge, bahnen, rennen, meisterschaften }
  })

  if (fehler) return <Fehler text={fehler} />
  if (!daten) return <Laden />

  const offen = daten.rennen.filter((r) => r.status !== 'BEENDET')
  const kacheln = [
    { titel: 'Fahrer', anzahl: daten.fahrer.length, ziel: '/fahrer' },
    { titel: 'Fahrzeuge', anzahl: daten.fahrzeuge.length, ziel: '/fahrzeuge' },
    { titel: 'Bahnen', anzahl: daten.bahnen.length, ziel: '/bahnen' },
    { titel: 'Rennen', anzahl: daten.rennen.length, ziel: '/rennen' },
    { titel: 'Meisterschaften', anzahl: daten.meisterschaften.length, ziel: '/meisterschaften' },
  ]

  return (
    <section>
      <div className="kopfzeile">
        <h1>Willkommen bei SlotRace</h1>
        <Link className="knopf primaer" to="/rennen/neu">+ Neues Rennen</Link>
      </div>
      <div className="kacheln">
        {kacheln.map((k) => (
          <Link key={k.titel} to={k.ziel} className="kachel">
            <span className="anzahl">{k.anzahl}</span>
            <span>{k.titel}</span>
          </Link>
        ))}
      </div>
      {(daten.bahnen.length === 0 || daten.fahrer.length === 0 || daten.fahrzeuge.length === 0) && (
        <div className="hinweis">
          Erste Schritte: Lege eine <Link to="/bahnen">Bahn</Link>, <Link to="/fahrer">Fahrer</Link> und{' '}
          <Link to="/fahrzeuge">Fahrzeuge</Link> an – danach kann das erste Rennen geplant werden.
        </div>
      )}
      <h2>Offene Rennen</h2>
      <RennenTabelle rennen={offen} />
    </section>
  )
}
