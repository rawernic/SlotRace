import type { Laufstatus, Rennmodus, Rennstatus } from './api'

/** Formatiert Millisekunden als Rundenzeit, z. B. 4321 → "4,321" und 83456 → "1:23,456". */
export function formatZeit(ms: number | null | undefined): string {
  if (ms === null || ms === undefined) return '–'
  const negativ = ms < 0
  const wert = Math.abs(Math.round(ms))
  const minuten = Math.floor(wert / 60000)
  const sekunden = Math.floor((wert % 60000) / 1000)
  const millis = wert % 1000
  const rest = `${sekunden},${String(millis).padStart(3, '0')}`
  const text = minuten > 0 ? `${minuten}:${rest.padStart(6, '0')}` : rest
  return negativ ? `-${text}` : text
}

/** Formatiert eine Restzeit/Laufzeit als Uhr, z. B. 83456 → "1:23,4". */
export function formatUhr(ms: number): string {
  const wert = Math.max(0, ms)
  const minuten = Math.floor(wert / 60000)
  const sekunden = Math.floor((wert % 60000) / 1000)
  const zehntel = Math.floor((wert % 1000) / 100)
  return `${minuten}:${String(sekunden).padStart(2, '0')},${zehntel}`
}

export function formatDatum(iso: string): string {
  return new Date(iso).toLocaleString('de-DE', { dateStyle: 'medium', timeStyle: 'short' })
}

export const modusText: Record<Rennmodus, string> = {
  ZEITRENNEN: 'Zeitrennen',
  RUNDENRENNEN: 'Rundenrennen',
}

export const rennstatusText: Record<Rennstatus, string> = {
  GEPLANT: 'Geplant',
  LAEUFT: 'Läuft',
  BEENDET: 'Beendet',
}

export const laufstatusText: Record<Laufstatus, string> = {
  BEREIT: 'Bereit',
  LAEUFT: 'Läuft',
  BEENDET: 'Beendet',
}

export function modusBeschreibung(modus: Rennmodus, laufDauerSekunden: number | null, rundenZiel: number | null) {
  return modus === 'ZEITRENNEN'
    ? `Zeitrennen · ${formatUhr((laufDauerSekunden ?? 0) * 1000).replace(/,\d$/, '')} min pro Lauf`
    : `Rundenrennen · ${rundenZiel} Runden pro Lauf`
}
