export interface Project {
  id: number
  title: string
  description?: string | null
  category?: string | null
  coverUrl?: string | null
  mediaUrl?: string | null
  mediaType?: string | null
  attachmentUrl?: string | null
  viewCount?: number | null
  likeCount?: number | null
  createdAt?: string | null
}
export interface ProjectDraft {
  title: string; description: string; category: string; coverUrl: string
  mediaUrl: string; mediaType: string; attachmentUrl: string
}
export type ProjectSort = 'latest' | 'likes'
export type UploadRole = 'cover' | 'media' | 'attachment'
export const projectCategories = ['视频剪辑', '音乐相关', '技术开发', '其他']
export const emptyProject = (): ProjectDraft => ({ title: '', description: '', category: '视频剪辑', coverUrl: '', mediaUrl: '', mediaType: 'VIDEO', attachmentUrl: '' })
export function projectDraft(value: Project): ProjectDraft {
  return {
    title: value.title || '', description: value.description || '', category: value.category || '视频剪辑',
    coverUrl: value.coverUrl || '', mediaUrl: value.mediaUrl || '', mediaType: value.mediaType || 'VIDEO', attachmentUrl: value.attachmentUrl || '',
  }
}
export function projectChanged(value: ProjectDraft, baseline: ProjectDraft): boolean {
  return (Object.keys(baseline) as (keyof ProjectDraft)[]).some(key => value[key] !== baseline[key])
}
export function projectPayload(value: ProjectDraft): ProjectDraft {
  return { ...value, title: value.title.trim(), coverUrl: value.coverUrl.trim(), mediaUrl: value.mediaUrl.trim(), attachmentUrl: value.attachmentUrl.trim() }
}
export function uploadedMediaType(file: { type: string; name: string }): 'AUDIO' | 'VIDEO' {
  return file.type.startsWith('audio/') || /\.(mp3|wav|flac|m4a|ogg|aac|aiff|opus)$/i.test(file.name) ? 'AUDIO' : 'VIDEO'
}
function timestamp(value: string | null | undefined): number {
  const parsed = value ? new Date(value).getTime() : 0
  return Number.isFinite(parsed) ? parsed : 0
}
// Search responses are not ordered by the API; keep the selected order for both search and browsing.
export function sortedProjects(values: Project[], sort: ProjectSort): Project[] {
  return [...values].sort((a, b) => (sort === 'likes' ? (b.likeCount || 0) - (a.likeCount || 0) : 0)
    || timestamp(b.createdAt) - timestamp(a.createdAt) || b.id - a.id)
}
export function attachmentTarget(value: string): { external: boolean; url: string } {
  return /^https?:\/\//i.test(value)
    ? { external: true, url: value }
    : { external: false, url: `/api/download?fileUrl=${encodeURIComponent(value)}` }
}
