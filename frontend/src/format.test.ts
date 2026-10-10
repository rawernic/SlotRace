import { describe, expect, it } from 'vitest'
import { formatUhr, formatZeit } from './format'

describe('formatZeit', () => {
  it('zeigt Sekunden mit Millisekunden', () => {
    expect(formatZeit(4321)).toBe('4,321')
    expect(formatZeit(50)).toBe('0,050')
  })
  it('zeigt Minuten ab 60 Sekunden', () => {
    expect(formatZeit(83456)).toBe('1:23,456')
    expect(formatZeit(60000)).toBe('1:00,000')
  })
  it('zeigt Strich ohne Wert', () => {
    expect(formatZeit(null)).toBe('–')
  })
})

describe('formatUhr', () => {
  it('zeigt Minuten, Sekunden und Zehntel', () => {
    expect(formatUhr(83456)).toBe('1:23,4')
    expect(formatUhr(-5)).toBe('0:00,0')
  })
})
