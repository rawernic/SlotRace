import { Link } from 'react-router'
import { api, type Meisterschaft } from '../api'
import { Stammdaten } from '../komponenten/Stammdaten'

function punkteLesen(text: string): number[] {
  return text.split(/[,;\s]+/).filter(Boolean).map(Number)
}

export function MeisterschaftenSeite() {
  return (
    <Stammdaten<Meisterschaft>
      titel="Meisterschaften"
      einzahl="Meisterschaft"
      felder={[
        { name: 'name', label: 'Name', pflicht: true, platzhalter: 'z. B. Winterpokal 2026' },
        { name: 'punkte', label: 'Punkte je Platz', platzhalter: 'Standard: 10, 8, 6, 5, 4, 3, 2, 1' },
      ]}
      spalten={[
        { titel: 'Name', wert: (m) => <Link to={`/meisterschaften/${m.id}`}>{m.name}</Link> },
        { titel: 'Punkte je Platz', wert: (m) => m.punkteSchema.join(', ') },
      ]}
      laden={api.meisterschaften.alle}
      zuFormular={(m) => ({ name: m.name, punkte: m.punkteSchema.join(', ') })}
      speichern={(w, id) => {
        const daten = { name: w.name, punkteSchema: punkteLesen(w.punkte ?? '') }
        return id ? api.meisterschaften.aendern(id, daten) : api.meisterschaften.anlegen(daten)
      }}
      loeschen={api.meisterschaften.loeschen}
      bezeichnung={(m) => m.name}
    />
  )
}
