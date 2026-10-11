import { Link } from 'react-router'
import { api, type RennenUebersicht } from '../api'
import { Fehler, Laden } from '../komponenten/Hinweis'
import { StatusMarke } from '../komponenten/Status'
import { useDaten } from '../komponenten/hooks'
import { formatDatum, modusBeschreibung } from '../format'

export function RennenTabelle({ rennen }: { rennen: RennenUebersicht[] }) {
  if (rennen.length === 0) return <p className="gedimmt">Keine Rennen vorhanden.</p>
  return (
    <table>
      <thead>
        <tr><th>Name</th><th>Bahn</th><th>Modus</th><th className="zahl">Teilnehmer</th><th>Läufe</th><th>Status</th><th>Angelegt</th><th /></tr>
      </thead>
      <tbody>
        {rennen.map((r) => (
          <tr key={r.id}>
            <td><Link to={`/rennen/${r.id}`}>{r.name}</Link></td>
            <td>{r.bahnName ?? '–'}</td>
            <td>{modusBeschreibung(r.modus, r.laufDauerSekunden, r.rundenZiel)}</td>
            <td className="zahl">{r.teilnehmerAnzahl}</td>
            <td>{r.beendeteLaeufe} / {r.laufAnzahl}</td>
            <td><StatusMarke status={r.status} /></td>
            <td>{formatDatum(r.erstelltAm)}</td>
            <td className="rechts">
              {r.status !== 'BEENDET' && r.status !== 'ABGEBROCHEN' && (
                <Link className="knopf primaer" to={`/rennen/${r.id}/live`}>{r.modus === 'TRAINING' ? 'Training' : 'Rennleitung'}</Link>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export function RennenListe() {
  const { daten, fehler } = useDaten(api.rennen.alle)
  return (
    <section>
      <div className="kopfzeile">
        <h1>Rennen</h1>
        <div className="aktionen">
          <Link className="knopf" to="/rennen/neu?modus=TRAINING">+ Training</Link>
          <Link className="knopf primaer" to="/rennen/neu">+ Neues Rennen</Link>
        </div>
      </div>
      <Fehler text={fehler} />
      {daten ? <RennenTabelle rennen={daten} /> : <Laden />}
    </section>
  )
}
