export interface Countdown { event: string; targetDate: string }

function calendarDate(value: string): Date | null {
  if (!/^[0-9]{4}-[0-9]{2}-[0-9]{2}$/.test(value)) return null
  const [year, month, day] = value.split('-').map(Number) as [number, number, number]
  if (year < 1) return null
  const date = new Date(0)
  date.setUTCHours(0, 0, 0, 0)
  date.setUTCFullYear(year, month - 1, day)
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day ? date : null
}

// Compare calendar days in the visitor's timezone, without UTC parsing or DST offsets.
export function daysUntil(target: string, now = new Date()): number | null {
  const date = calendarDate(target)
  if (!date || Number.isNaN(now.getTime())) return null
  const today = new Date(0)
  today.setUTCHours(0, 0, 0, 0)
  today.setUTCFullYear(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.max(0, Math.round((date.getTime() - today.getTime()) / 86_400_000))
}

// Old browser values are only an editor draft, never the public display or an automatic upload.
export function legacyCountdown(event: string | null, targetDate: string | null): Countdown | null {
  const name = event?.trim()
  return name && name.length <= 120 && targetDate && calendarDate(targetDate) ? { event: name, targetDate } : null
}
