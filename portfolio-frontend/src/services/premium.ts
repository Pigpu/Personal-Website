import axios from 'axios'

export interface PremiumAudio {
  id: number
  uuid: string
  title: string
  audioUrl: string
  createAt: string | null
}
export interface PremiumDraft { title: string; audioUrl: string }
export type PremiumEpisode = Omit<PremiumAudio, 'audioUrl'>
export interface PremiumAccess { allowed: boolean; admin: boolean }
export interface PremiumListener { userId: number; username: string; grantedAt: string }
export interface PremiumUser { userId: number; username: string; authorized: boolean; admin: boolean }

export async function getPremiumAccess(): Promise<PremiumAccess> {
  return (await axios.get<PremiumAccess>('/api/premium/access')).data
}
export async function getPremiumList(): Promise<PremiumEpisode[]> {
  return (await axios.get<PremiumEpisode[]>('/api/premium/list')).data
}
export async function getPremiumListeners(): Promise<PremiumListener[]> {
  return (await axios.get<PremiumListener[]>('/api/premium/admin/listeners')).data
}
export async function searchPremiumUsers(query: string): Promise<PremiumUser[]> {
  return (await axios.get<PremiumUser[]>('/api/premium/admin/users', { params: { query: query.trim() } })).data
}
export async function grantPremiumListener(value: { userId: number } | { username: string }): Promise<PremiumListener> {
  return (await axios.post<PremiumListener>('/api/premium/admin/listeners', value)).data
}
export async function revokePremiumListener(userId: number): Promise<void> {
  await axios.delete('/api/premium/admin/listeners/' + userId)
}
export async function getPremiumAudio(uuid: string): Promise<PremiumAudio> {
  return (await axios.get<PremiumAudio>('/api/premium/play/' + encodeURIComponent(uuid))).data
}
export async function publishPremiumAudio(value: PremiumDraft): Promise<void> {
  // Publishing only submits editable fields; existing UUIDs and records are never rewritten.
  await axios.post('/api/premium/admin/save', { title: value.title.trim(), audioUrl: value.audioUrl.trim() })
}
export async function uploadPremiumAudio(file: File): Promise<string> {
  const body = new FormData()
  body.append('file', file)
  body.append('type', 'media')
  return (await axios.post<string>('/api/upload/project', body)).data
}
