import { api, type Fahrer } from '../api'
import { Stammdaten } from '../komponenten/Stammdaten'

export function FahrerSeite() {
  return (
    <Stammdaten<Fahrer>
      titel="Fahrer"
      einzahl="Fahrer"
      felder={[
        { name: 'vorname', label: 'Vorname', pflicht: true },
        { name: 'nachname', label: 'Nachname', pflicht: true },
        { name: 'spitzname', label: 'Spitzname', platzhalter: 'wird statt des Namens angezeigt' },
      ]}
      spalten={[
        { titel: 'Name', wert: (f) => `${f.vorname} ${f.nachname}` },
        { titel: 'Spitzname', wert: (f) => f.spitzname ?? '–' },
      ]}
      laden={api.fahrer.alle}
      zuFormular={(f) => ({ vorname: f.vorname, nachname: f.nachname, spitzname: f.spitzname ?? '' })}
      speichern={(w, id) => (id ? api.fahrer.aendern(id, w) : api.fahrer.anlegen(w))}
      loeschen={api.fahrer.loeschen}
      bezeichnung={(f) => f.anzeigename}
    />
  )
}
