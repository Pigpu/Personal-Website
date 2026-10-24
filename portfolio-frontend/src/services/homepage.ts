import axios from 'axios'

export type SiteLanguage = 'zh' | 'en' | 'ja'
export type EditorSection = 'profile' | 'quotes' | 'listening'
export interface HomeTranslation { desc1: string; desc2: string; contactPublic: string; contactProf: string }
export interface Listening { artist: string; title: string; url: string; enabled: boolean }
export interface HomeContent { translations: Partial<Record<SiteLanguage, HomeTranslation>>; listening: Listening | null }
export interface Quote { id: number; content: string; source: string; language: SiteLanguage | null; enabled: boolean }
export type QuoteDraft = Omit<Quote, 'id'> & { id?: number }

export const siteLanguages: { value: SiteLanguage; label: string }[] = [
  { value: 'zh', label: '中文' }, { value: 'en', label: 'English' }, { value: 'ja', label: '日本語' },
]
export function siteLanguage(value: string): SiteLanguage {
  return value === 'en' || value === 'ja' ? value : 'zh'
}

// Kept in memory so navigation preserves the quote; a full page refresh selects anew.
const quoteCache = new Map<SiteLanguage, Promise<Quote | null>>()
export function getRandomQuote(language: SiteLanguage): Promise<Quote | null> {
  const cached = quoteCache.get(language)
  if (cached) return cached
  const request = axios.get<Quote>('/api/home/quote', { params: { lang: language } })
    .then(response => response.status === 204 ? null : response.data)
    .catch(error => { quoteCache.delete(language); throw error })
  quoteCache.set(language, request)
  return request
}
export function clearQuoteCache() { quoteCache.clear() }
export async function getHomeContent() { return (await axios.get<HomeContent>('/api/home')).data }
export async function getListening() { return (await axios.get<Listening>('/api/home/admin/listening')).data }
export async function getQuotes() { return (await axios.get<Quote[]>('/api/home/admin/quotes')).data }
export async function saveProfile(translations: Record<SiteLanguage, HomeTranslation>) {
  return (await axios.put<HomeContent>('/api/home/admin/profile', { translations })).data
}
export async function saveListening(value: Listening) {
  return (await axios.put<Listening>('/api/home/admin/listening', value)).data
}
export async function saveQuote(value: QuoteDraft) {
  const { id, ...body } = value
  const response = id === undefined
    ? await axios.post<Quote>('/api/home/admin/quotes', body)
    : await axios.put<Quote>(`/api/home/admin/quotes/${id}`, body)
  clearQuoteCache()
  return response.data
}

export function songLink(value: string | undefined): string | null {
  if (!value) return null
  try {
    const url = new URL(value)
    return url.protocol === 'https:' && !url.username && !url.password
      && ['music.163.com', 'y.music.163.com'].includes(url.hostname)
      && (!url.port || url.port === '443') ? url.href : null
  } catch { return null }
}
