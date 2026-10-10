import { useCallback, useEffect, useRef, useState } from 'react'

/** Lädt Daten asynchron und stellt Lade-/Fehlerzustand sowie eine Neu-Laden-Funktion bereit. */
export function useDaten<T>(laden: () => Promise<T>, abhaengigkeiten: unknown[] = []) {
  const [daten, setDaten] = useState<T | null>(null)
  const [fehler, setFehler] = useState<string | null>(null)
  const ladenRef = useRef(laden)
  useEffect(() => {
    ladenRef.current = laden
  })

  const neuLaden = useCallback(async () => {
    try {
      setDaten(await ladenRef.current())
      setFehler(null)
    } catch (e) {
      setFehler((e as Error).message)
    }
  }, [])

  const schluessel = JSON.stringify(abhaengigkeiten)
  useEffect(() => {
    void neuLaden()
  }, [neuLaden, schluessel])

  return { daten, fehler, neuLaden, setDaten }
}
