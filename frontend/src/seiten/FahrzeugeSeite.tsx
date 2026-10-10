import { api, type Fahrzeug } from '../api'
import { Stammdaten } from '../komponenten/Stammdaten'

export function FahrzeugeSeite() {
  return (
    <Stammdaten<Fahrzeug>
      titel="Fahrzeuge"
      einzahl="Fahrzeug"
      felder={[
        { name: 'bezeichnung', label: 'Bezeichnung', pflicht: true, platzhalter: 'z. B. Porsche 917K' },
        { name: 'hersteller', label: 'Hersteller', platzhalter: 'z. B. Carrera' },
        { name: 'massstab', label: 'Maßstab', platzhalter: 'z. B. 1:32' },
      ]}
      spalten={[
        { titel: 'Bezeichnung', wert: (f) => f.bezeichnung },
        { titel: 'Hersteller', wert: (f) => f.hersteller ?? '–' },
        { titel: 'Maßstab', wert: (f) => f.massstab ?? '–' },
      ]}
      laden={api.fahrzeuge.alle}
      zuFormular={(f) => ({ bezeichnung: f.bezeichnung, hersteller: f.hersteller ?? '', massstab: f.massstab ?? '' })}
      speichern={(w, id) => (id ? api.fahrzeuge.aendern(id, w) : api.fahrzeuge.anlegen(w))}
      loeschen={api.fahrzeuge.loeschen}
      bezeichnung={(f) => f.bezeichnung}
    />
  )
}
