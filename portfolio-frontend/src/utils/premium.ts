// Keep uploaded files and ordinary external audio URLs, including signed query strings.
export function premiumAudioUrl(value: unknown): string | null {
  if (typeof value !== 'string') return null
  const url = value.trim()
  if (!url || /[\u0000-\u001f\u007f\\]/.test(url)) return null
  if (url.startsWith('/uploads/')) return url
  try {
    const parsed = new URL(url)
    return ['https:', 'http:'].includes(parsed.protocol) && !parsed.username && !parsed.password ? url : null
  } catch { return null }
}

export function premiumUuid(value: unknown): string | null {
  return typeof value === 'string' && /^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(value) ? value : null
}
