import { BrowserRouter, NavLink, Route, Routes } from 'react-router'
import { BahnenSeite } from './seiten/BahnenSeite'
import { FahrerSeite } from './seiten/FahrerSeite'
import { FahrzeugeSeite } from './seiten/FahrzeugeSeite'
import { MeisterschaftDetail } from './seiten/MeisterschaftDetail'
import { MeisterschaftenSeite } from './seiten/MeisterschaftenSeite'
import { RennenDetail } from './seiten/RennenDetail'
import { RennenFormular } from './seiten/RennenFormular'
import { RennenListe } from './seiten/RennenListe'
import { Rennleitung } from './seiten/Rennleitung'
import { Start } from './seiten/Start'

const navigation = [
  { ziel: '/rennen', text: 'Rennen' },
  { ziel: '/meisterschaften', text: 'Meisterschaften' },
  { ziel: '/fahrer', text: 'Fahrer' },
  { ziel: '/fahrzeuge', text: 'Fahrzeuge' },
  { ziel: '/bahnen', text: 'Bahnen' },
]

export default function App() {
  return (
    <BrowserRouter>
      <header className="kopf">
        <NavLink to="/" className="logo">🏁 SlotRace</NavLink>
        <nav>
          {navigation.map((n) => <NavLink key={n.ziel} to={n.ziel}>{n.text}</NavLink>)}
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Start />} />
          <Route path="/fahrer" element={<FahrerSeite />} />
          <Route path="/fahrzeuge" element={<FahrzeugeSeite />} />
          <Route path="/bahnen" element={<BahnenSeite />} />
          <Route path="/meisterschaften" element={<MeisterschaftenSeite />} />
          <Route path="/meisterschaften/:id" element={<MeisterschaftDetail />} />
          <Route path="/rennen" element={<RennenListe />} />
          <Route path="/rennen/neu" element={<RennenFormular />} />
          <Route path="/rennen/:id" element={<RennenDetail />} />
          <Route path="/rennen/:id/bearbeiten" element={<RennenFormular />} />
          <Route path="/rennen/:id/live" element={<Rennleitung />} />
          <Route path="*" element={<p>Seite nicht gefunden.</p>} />
        </Routes>
      </main>
    </BrowserRouter>
  )
}
