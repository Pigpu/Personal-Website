export interface Article {
  id: number
  title: string
  content?: string | null
  summary?: string | null
  category: string
  coverUrl?: string | null
  viewCount?: number | null
  likeCount?: number | null
  createdAt?: string | null
}
export type ArticleSort = 'time' | 'likes'
export type SortDirection = 'asc' | 'desc'
export interface ArticleDraft { title: string; content: string; category: string }
export const DRAFT_KEY = 'blog_draft_data'
export const emptyArticle = (): ArticleDraft => ({ title: '', content: '', category: '技术' })

function timestamp(value: string | null | undefined) {
  const parsed = value ? new Date(value).getTime() : 0
  return Number.isFinite(parsed) ? parsed : 0
}
export function sortedArticles(articles: Article[], key: ArticleSort, direction: SortDirection): Article[] {
  const sign = direction === 'asc' ? 1 : -1
  return [...articles].sort((a, b) => {
    const first = key === 'likes' ? Number(a.likeCount || 0) - Number(b.likeCount || 0) : timestamp(a.createdAt) - timestamp(b.createdAt)
    return first * sign || timestamp(b.createdAt) - timestamp(a.createdAt) || b.id - a.id
  })
}
export function plainExcerpt(value: string | null | undefined, length = 160): string {
  const text = (value || '')
    .replace(/<!--[\s\S]*?-->/g, ' ')
    .replace(/```[\s\S]*?```|~~~[\s\S]*?~~~/g, ' ')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/!\[[^\]]*\]\[[^\]]*\]/g, ' ')
    .replace(/^\s*\[[^\]]+\]:\s*\S+.*$/gm, ' ')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/<[^>]*>/g, ' ')
    .replace(/^\s{0,3}(?:#{1,6}\s+|>\s*|[-+*]\s+|\d+\.\s+)/gm, '')
    .replace(/[*_~`]/g, '')
    .replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'")
    .replace(/\s+/g, ' ').replace(/\s+([,.;:!?，。；：！？、])/g, '$1').trim()
  const characters = Array.from(text)
  return characters.length > length ? characters.slice(0, length).join('') + '…' : text
}
export function articleChanged(value: ArticleDraft, baseline: ArticleDraft): boolean {
  return value.title !== baseline.title || value.content !== baseline.content || value.category !== baseline.category
}
export function parseArticleDraft(value: string | null): ArticleDraft | null {
  if (!value) return null
  try {
    const parsed: unknown = JSON.parse(value)
    if (!parsed || typeof parsed !== 'object') return null
    const draft = parsed as Record<string, unknown>
    if (typeof draft.title !== 'string' || typeof draft.content !== 'string') return null
    if (!draft.title.trim() && (!draft.content.trim() || draft.content.trim() === '# 开始你的写作...') && (typeof draft.category !== 'string' || draft.category === '技术')) return null
    return { title: draft.title, content: draft.content, category: typeof draft.category === 'string' ? draft.category : '技术' }
  } catch { return null }
}
export function articlePayload(value: ArticleDraft) {
  const match = value.content.match(/!\[[^\]]*\]\(\s*<?([^\s)>]+)>?(?:\s+["'][^)]*)?\s*\)/)
  return { ...value, title: value.title.trim(), coverUrl: match?.[1] || '', summary: plainExcerpt(value.content) }
}
