import type { Laufstatus, Rennstatus } from '../api'
import { laufstatusText, rennstatusText } from '../format'

export function StatusMarke({ status, art = 'rennen' }: { status: Rennstatus | Laufstatus; art?: 'rennen' | 'lauf' }) {
  const text = art === 'rennen' ? rennstatusText[status as Rennstatus] : laufstatusText[status as Laufstatus]
  return <span className={`marke status-${status.toLowerCase()}`}>{text}</span>
}
