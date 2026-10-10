export function Fehler({ text }: { text: string | null }) {
  if (!text) return null
  return <div className="hinweis fehler" role="alert">{text}</div>
}

export function Laden() {
  return <p className="gedimmt">Lade …</p>
}
