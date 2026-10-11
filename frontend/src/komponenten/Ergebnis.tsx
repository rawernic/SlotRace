import type { Platzierung } from '../api'
import { formatZeit } from '../format'

export function ErgebnisTabelle({ ergebnis, training = false }: { ergebnis: Platzierung[]; training?: boolean }) {
  const fuehrender = ergebnis[0]
  const schnellste = Math.min(...ergebnis.map((p) => p.besteRundeMs ?? Infinity))
  if (training) {
    if (ergebnis.length === 0) return <p className="gedimmt">Noch keine Runden gefahren.</p>
    return (
      <table className="rangliste">
        <thead>
          <tr>
            <th>Platz</th><th>Fahrer</th><th>Fahrzeug</th><th className="zahl">Beste Runde</th>
            <th className="zahl">Rückstand</th><th className="zahl">Runden</th><th className="zahl">Sitzungen</th>
          </tr>
        </thead>
        <tbody>
          {ergebnis.map((p) => (
            <tr key={p.teilnehmerId} className={`platz-${p.platz}`}>
              <td>{p.platz}.</td>
              <td>{p.fahrerName}</td>
              <td>{p.fahrzeugName}</td>
              <td className={`zahl ${p.besteRundeMs === schnellste ? 'bestzeit' : ''}`}><strong>{formatZeit(p.besteRundeMs)}</strong></td>
              <td className="zahl">
                {p === fuehrender || p.besteRundeMs === null ? '–' : `+${formatZeit(p.besteRundeMs - schnellste)}`}
              </td>
              <td className="zahl">{p.runden}</td>
              <td className="zahl">{p.gefahreneLaeufe}</td>
            </tr>
          ))}
        </tbody>
      </table>
    )
  }
  return (
    <table className="rangliste">
      <thead>
        <tr>
          <th>Platz</th><th>Fahrer</th><th>Fahrzeug</th><th className="zahl">Runden</th>
          <th className="zahl">Zeit</th><th className="zahl">Rückstand</th><th className="zahl">Beste Runde</th><th className="zahl">Läufe</th>
        </tr>
      </thead>
      <tbody>
        {ergebnis.map((p) => (
          <tr key={p.teilnehmerId} className={`platz-${p.platz}`}>
            <td>{p.platz}.</td>
            <td>{p.fahrerName}</td>
            <td>{p.fahrzeugName}</td>
            <td className="zahl"><strong>{p.runden}</strong></td>
            <td className="zahl">{formatZeit(p.gesamtzeitMs)}</td>
            <td className="zahl">{rueckstand(fuehrender, p)}</td>
            <td className={`zahl ${p.besteRundeMs === schnellste ? 'bestzeit' : ''}`}>{formatZeit(p.besteRundeMs)}</td>
            <td className="zahl">{p.gefahreneLaeufe}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function rueckstand(fuehrender: Platzierung, p: Platzierung): string {
  if (p === fuehrender) return '–'
  const runden = fuehrender.runden - p.runden
  if (runden > 0) return `+${runden} ${runden === 1 ? 'Runde' : 'Runden'}`
  return `+${formatZeit(p.gesamtzeitMs - fuehrender.gesamtzeitMs)}`
}
