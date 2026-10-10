import { Link, useNavigate, useParams } from 'react-router'
import { api } from '../api'
import { ErgebnisTabelle } from '../komponenten/Ergebnis'
import { Fehler, Laden } from '../komponenten/Hinweis'
import { StatusMarke } from '../komponenten/Status'
import { useDaten } from '../komponenten/hooks'
import { formatDatum, formatZeit, modusBeschreibung } from '../format'

export function RennenDetail() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { daten: rennen, fehler } = useDaten(() => api.rennen.hole(id), [id])

  if (fehler) return <Fehler text={fehler} />
  if (!rennen) return <Laden />

  const loeschen = async () => {
    if (!confirm(`Rennen „${rennen.name}“ mit allen Ergebnissen löschen?`)) return
    await api.rennen.loeschen(rennen.id)
    navigate('/rennen')
  }

  return (
    <section>
      <p><Link to="/rennen">← Rennen</Link></p>
      <div className="kopfzeile">
        <h1>{rennen.name} <StatusMarke status={rennen.status} /></h1>
        <div className="aktionen">
          {rennen.status !== 'BEENDET' && <Link className="knopf primaer" to={`/rennen/${rennen.id}/live`}>Rennleitung öffnen</Link>}
          {rennen.status === 'GEPLANT' && <Link className="knopf" to={`/rennen/${rennen.id}/bearbeiten`}>Bearbeiten</Link>}
          <button className="gefahr" onClick={loeschen}>Löschen</button>
        </div>
      </div>
      <p className="gedimmt">
        {modusBeschreibung(rennen.modus, rennen.laufDauerSekunden, rennen.rundenZiel)} · Bahn „{rennen.bahn.name}“
        {rennen.meisterschaftId && <> · Meisterschaft <Link to={`/meisterschaften/${rennen.meisterschaftId}`}>{rennen.meisterschaftName}</Link></>}
        {' '}· angelegt {formatDatum(rennen.erstelltAm)}
      </p>

      <h2>{rennen.status === 'BEENDET' ? 'Endergebnis' : 'Zwischenstand'}</h2>
      <ErgebnisTabelle ergebnis={rennen.ergebnis} />

      <h2>Laufplan</h2>
      <table className="laufplan">
        <thead>
          <tr>
            <th>Lauf</th>
            {rennen.bahn.spuren.map((s) => (
              <th key={s.nummer}><span className="spurpunkt" style={{ background: s.farbe }}>{s.nummer}</span> Spur {s.nummer}</th>
            ))}
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {rennen.laeufe.map((l) => (
            <tr key={l.nummer}>
              <td>{l.nummer}</td>
              {rennen.bahn.spuren.map((s) => {
                const stand = l.spuren.find((x) => x.spurNummer === s.nummer)
                return (
                  <td key={s.nummer}>
                    {stand ? (
                      <>
                        {stand.fahrerName}
                        {l.status !== 'BEREIT' && (
                          <div className="klein gedimmt">{stand.runden} Rd. · beste {formatZeit(stand.besteRundeMs)}</div>
                        )}
                      </>
                    ) : <span className="gedimmt">frei</span>}
                  </td>
                )
              })}
              <td><StatusMarke status={l.status} art="lauf" /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
