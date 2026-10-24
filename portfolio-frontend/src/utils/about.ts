// Navigation only; never load an external site on the server or insert link text as HTML.
export function friendLinkUrl(value: string | null | undefined): string | null {
  if (!value) return null
  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol) && !!url.hostname && !url.username && !url.password ? url.href : null
  } catch { return null }
}
export function friendLinkHost(value: string): string {
  const url = friendLinkUrl(value)
  return url ? new URL(url).hostname.replace(/^www\./, '') : ''
}
