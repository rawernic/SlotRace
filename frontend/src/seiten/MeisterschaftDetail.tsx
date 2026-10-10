import { Link, useParams } from 'react-router'
import { api } from '../api'
import { Fehler, Laden } from '../komponenten/Hinweis'
import { useDaten } from '../komponenten/hooks'
import { RennenTabelle } from './RennenListe'

export function MeisterschaftDetail() {
  const { id = '' } = useParams()
  const { daten, fehler } = useDaten(
    async () => {
      const [meisterschaft, wertung, rennen] = await Promise.all([
        api.meisterschaften.hole(id),
        api.meisterschaften.wertung(id),
        api.meisterschaften.rennen(id),
      ])
      return { meisterschaft, wertung, rennen }
    },
    [id],
  )

  if (fehler) return <Fehler text={fehler} />
  if (!daten) return <Laden />
  const { meisterschaft, wertung, rennen } = daten

  return (
    <section>
      <p><Link to="/meisterschaften">← Meisterschaften</Link></p>
      <h1>{meisterschaft.name}</h1>
      <p className="gedimmt">Punkte je Platz: {meisterschaft.punkteSchema.join(', ')} · gewertet werden beendete Rennen</p>

      <h2>Gesamtwertung</h2>
      {wertung.length === 0 ? (
        <p className="gedimmt">Noch keine beendeten Rennen.</p>
      ) : (
        <table className="rangliste">
          <thead>
            <tr><th>Platz</th><th>Fahrer</th><th className="zahl">Punkte</th><th className="zahl">Siege</th><th className="zahl">Rennen</th></tr>
          </thead>
          <tbody>
            {wertung.map((p) => (
              <tr key={p.fahrerId} className={`platz-${p.platz}`}>
                <td>{p.platz}.</td>
                <td>{p.fahrerName}</td>
                <td className="zahl"><strong>{p.punkte}</strong></td>
                <td className="zahl">{p.siege}</td>
                <td className="zahl">{p.rennen}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Rennen</h2>
      <RennenTabelle rennen={rennen} />
      <p><Link className="knopf" to={`/rennen/neu?meisterschaft=${meisterschaft.id}`}>+ Rennen zur Meisterschaft anlegen</Link></p>
    </section>
  )
}
