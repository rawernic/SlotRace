import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { api, type RennenAnfrage, type Rennmodus } from '../api'
import { Fehler, Laden } from '../komponenten/Hinweis'
import { useDaten } from '../komponenten/hooks'

interface Meldung {
  fahrerId: string
  fahrzeugId: string
}

export function RennenFormular() {
  const { id } = useParams()
  const [suchParameter] = useSearchParams()
  const navigate = useNavigate()
  const { daten: stamm, fehler: ladeFehler } = useDaten(async () => {
    const [fahrer, fahrzeuge, bahnen, meisterschaften] = await Promise.all([
      api.fahrer.alle(), api.fahrzeuge.alle(), api.bahnen.alle(), api.meisterschaften.alle(),
    ])
    return { fahrer, fahrzeuge, bahnen, meisterschaften }
  })

  const [name, setName] = useState('')
  const [bahnId, setBahnId] = useState('')
  const [meisterschaftId, setMeisterschaftId] = useState(suchParameter.get('meisterschaft') ?? '')
  const [modus, setModus] = useState<Rennmodus>(suchParameter.get('modus') === 'TRAINING' ? 'TRAINING' : 'ZEITRENNEN')
  const [laufDauerMinuten, setLaufDauerMinuten] = useState(3)
  const [rundenZiel, setRundenZiel] = useState(20)
  const [mindestRundenzeitMs, setMindestRundenzeitMs] = useState(1000)
  const [meldungen, setMeldungen] = useState<Meldung[]>([])
  const [spurBelegung, setSpurBelegung] = useState<Record<number, Meldung>>({})
  const [fehler, setFehler] = useState<string | null>(null)
  const training = modus === 'TRAINING'

  useEffect(() => {
    if (!id) return
    api.rennen.hole(id).then((r) => {
      setName(r.name)
      setBahnId(r.bahn.id)
      setMeisterschaftId(r.meisterschaftId ?? '')
      setModus(r.modus)
      if (r.laufDauerSekunden) setLaufDauerMinuten(r.laufDauerSekunden / 60)
      if (r.rundenZiel) setRundenZiel(r.rundenZiel)
      setMindestRundenzeitMs(r.mindestRundenzeitMs)
      setMeldungen(r.teilnehmer.map((t) => ({ fahrerId: t.fahrerId, fahrzeugId: t.fahrzeugId })))
      if (r.modus === 'TRAINING') {
        const belegung: Record<number, Meldung> = {}
        for (const s of r.laeufe.at(-1)?.spuren ?? []) {
          const t = r.teilnehmer.find((x) => x.id === s.teilnehmerId)
          if (t) belegung[s.spurNummer] = { fahrerId: t.fahrerId, fahrzeugId: t.fahrzeugId }
        }
        setSpurBelegung(belegung)
      }
    }, (e: Error) => setFehler(e.message))
  }, [id])

  useEffect(() => {
    if (stamm && !bahnId && stamm.bahnen.length > 0) setBahnId(stamm.bahnen[0].id)
  }, [stamm, bahnId])

  if (ladeFehler) return <Fehler text={ladeFehler} />
  if (!stamm) return <Laden />

  const bahn = stamm.bahnen.find((b) => b.id === bahnId)
  const gewaehlteFahrer = new Set(meldungen.map((m) => m.fahrerId))
  const laufAnzahl = bahn && meldungen.length > 0 ? Math.max(meldungen.length, bahn.spuren.length) : 0
  const fehlendeStammdaten = stamm.bahnen.length === 0 || stamm.fahrer.length === 0 || stamm.fahrzeuge.length === 0

  const meldungHinzufuegen = () => {
    const freierFahrer = stamm.fahrer.find((f) => !gewaehlteFahrer.has(f.id))
    if (!freierFahrer) return
    setMeldungen([...meldungen, { fahrerId: freierFahrer.id, fahrzeugId: stamm.fahrzeuge[0]?.id ?? '' }])
  }

  const meldungAendern = (index: number, aenderung: Partial<Meldung>) =>
    setMeldungen(meldungen.map((m, i) => (i === index ? { ...m, ...aenderung } : m)))

  const verschieben = (index: number, richtung: -1 | 1) => {
    const ziel = index + richtung
    if (ziel < 0 || ziel >= meldungen.length) return
    const neu = [...meldungen]
    ;[neu[index], neu[ziel]] = [neu[ziel], neu[index]]
    setMeldungen(neu)
  }

  const spurBelegen = (spur: number, aenderung: Partial<Meldung> | null) => {
    const neu = { ...spurBelegung }
    if (aenderung === null) delete neu[spur]
    else neu[spur] = { ...(neu[spur] ?? { fahrerId: '', fahrzeugId: stamm.fahrzeuge[0]?.id ?? '' }), ...aenderung }
    setSpurBelegung(neu)
  }
  const trainingsMeldungen = Object.entries(spurBelegung)
    .filter(([spur]) => bahn?.spuren.some((s) => s.nummer === Number(spur)))
    .map(([spur, m]) => ({ ...m, spurNummer: Number(spur) }))
  const trainingsFahrer = new Set(trainingsMeldungen.map((m) => m.fahrerId))

  const absenden = async (e: FormEvent) => {
    e.preventDefault()
    const anfrage: RennenAnfrage = {
      name,
      bahnId,
      meisterschaftId: training ? null : meisterschaftId || null,
      modus,
      laufDauerSekunden: modus === 'ZEITRENNEN' ? Math.round(laufDauerMinuten * 60) : null,
      rundenZiel: modus === 'RUNDENRENNEN' ? rundenZiel : null,
      mindestRundenzeitMs,
      teilnehmer: training ? trainingsMeldungen : meldungen,
    }
    try {
      const rennen = id ? await api.rennen.aendern(id, anfrage) : await api.rennen.planen(anfrage)
      navigate(`/rennen/${rennen.id}`)
    } catch (err) {
      setFehler((err as Error).message)
    }
  }

  return (
    <section>
      <p><Link to={id ? `/rennen/${id}` : '/rennen'}>← Zurück</Link></p>
      <h1>{id ? (training ? 'Training bearbeiten' : 'Rennen bearbeiten') : (training ? 'Neues Training' : 'Neues Rennen')}</h1>
      {fehlendeStammdaten && (
        <div className="hinweis">
          Für ein Rennen werden mindestens eine <Link to="/bahnen">Bahn</Link>, ein <Link to="/fahrer">Fahrer</Link> und
          ein <Link to="/fahrzeuge">Fahrzeug</Link> benötigt.
        </div>
      )}
      <form className="karte formular" onSubmit={absenden}>
        <div className="felder">
          <label>
            Name *
            <input value={name} required onChange={(e) => setName(e.target.value)} placeholder="z. B. Clubabend März" />
          </label>
          <label>
            Bahn *
            <select value={bahnId} required onChange={(e) => setBahnId(e.target.value)}>
              {stamm.bahnen.map((b) => <option key={b.id} value={b.id}>{b.name} ({b.spuren.length} Spuren)</option>)}
            </select>
          </label>
          {!training && (
            <label>
              Meisterschaft
              <select value={meisterschaftId} onChange={(e) => setMeisterschaftId(e.target.value)}>
                <option value="">– keine –</option>
                {stamm.meisterschaften.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
              </select>
            </label>
          )}
        </div>

        <fieldset className="modus">
          <legend>Rennmodus</legend>
          <label className="auswahl">
            <input type="radio" checked={modus === 'ZEITRENNEN'} onChange={() => setModus('ZEITRENNEN')} />
            Zeitrennen – meiste Runden in vorgegebener Zeit
          </label>
          <label className="auswahl">
            <input type="radio" checked={modus === 'RUNDENRENNEN'} onChange={() => setModus('RUNDENRENNEN')} />
            Rundenrennen – vorgegebene Rundenzahl am schnellsten
          </label>
          <label className="auswahl">
            <input type="radio" checked={training} onChange={() => setModus('TRAINING')} />
            Training – open End, schnellste Runde; Fahrer und Fahrzeug jederzeit wechselbar
          </label>
        </fieldset>

        <div className="felder">
          {modus === 'ZEITRENNEN' && (
            <label>
              Laufdauer (Minuten) *
              <input type="number" min={0.25} step={0.25} value={laufDauerMinuten} required
                onChange={(e) => setLaufDauerMinuten(Number(e.target.value))} />
            </label>
          )}
          {modus === 'RUNDENRENNEN' && (
            <label>
              Runden pro Lauf *
              <input type="number" min={1} value={rundenZiel} required onChange={(e) => setRundenZiel(Number(e.target.value))} />
            </label>
          )}
          <label>
            Mindestrundenzeit (ms)
            <input type="number" min={0} max={60000} step={100} value={mindestRundenzeitMs}
              onChange={(e) => setMindestRundenzeitMs(Number(e.target.value))} />
            <small className="gedimmt">Schnellere Eingaben werden als Doppelklick verworfen.</small>
          </label>
        </div>

        {training ? (
          <>
            <h2>Spurbelegung</h2>
            {bahn?.spuren.map((s) => {
              const m = spurBelegung[s.nummer]
              return (
                <div key={s.nummer} className="meldung">
                  <span className="spurpunkt" style={{ background: s.farbe }}>{s.nummer}</span>
                  <select value={m?.fahrerId ?? ''}
                    onChange={(e) => spurBelegen(s.nummer, e.target.value ? { fahrerId: e.target.value } : null)}>
                    <option value="">– frei –</option>
                    {stamm.fahrer.filter((f) => f.id === m?.fahrerId || !trainingsFahrer.has(f.id)).map((f) => (
                      <option key={f.id} value={f.id}>{f.anzeigename}</option>
                    ))}
                  </select>
                  {m && (
                    <select value={m.fahrzeugId} onChange={(e) => spurBelegen(s.nummer, { fahrzeugId: e.target.value })}>
                      {stamm.fahrzeuge.map((f) => <option key={f.id} value={f.id}>{f.bezeichnung}</option>)}
                    </select>
                  )}
                </div>
              )
            })}
            <p className="gedimmt">Fahrer und Fahrzeuge lassen sich während des Trainings jederzeit wechseln.</p>
          </>
        ) : (
          <>
            <h2>Teilnehmer</h2>
            {meldungen.length === 0 && <p className="gedimmt">Noch keine Teilnehmer gemeldet.</p>}
            {meldungen.map((m, i) => (
              <div key={i} className="meldung">
                <span className="startnummer">{i + 1}</span>
                <select value={m.fahrerId} onChange={(e) => meldungAendern(i, { fahrerId: e.target.value })}>
                  {stamm.fahrer.filter((f) => f.id === m.fahrerId || !gewaehlteFahrer.has(f.id)).map((f) => (
                    <option key={f.id} value={f.id}>{f.anzeigename}</option>
                  ))}
                </select>
                <select value={m.fahrzeugId} onChange={(e) => meldungAendern(i, { fahrzeugId: e.target.value })}>
                  {stamm.fahrzeuge.map((f) => <option key={f.id} value={f.id}>{f.bezeichnung}</option>)}
                </select>
                <button type="button" onClick={() => verschieben(i, -1)} title="nach oben">↑</button>
                <button type="button" onClick={() => verschieben(i, 1)} title="nach unten">↓</button>
                <button type="button" className="gefahr" onClick={() => setMeldungen(meldungen.filter((_, j) => j !== i))}>
                  Entfernen
                </button>
              </div>
            ))}
            <p>
              <button type="button" onClick={meldungHinzufuegen} disabled={gewaehlteFahrer.size >= stamm.fahrer.length || stamm.fahrzeuge.length === 0}>
                + Teilnehmer hinzufügen
              </button>
            </p>
            {laufAnzahl > 0 && (
              <p className="gedimmt">
                Laufplan: {laufAnzahl} Läufe mit Spurwechsel – jeder Teilnehmer fährt jede der {bahn?.spuren.length} Spuren genau einmal.
              </p>
            )}
          </>
        )}

        <Fehler text={fehler} />
        <div className="aktionen">
          <button type="submit" className="primaer"
            disabled={(training ? trainingsMeldungen.length === 0 : meldungen.length === 0) || !bahnId}>
            {id ? 'Speichern' : training ? 'Training anlegen' : 'Rennen anlegen'}
          </button>
        </div>
      </form>
    </section>
  )
}
