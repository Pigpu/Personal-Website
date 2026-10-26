import axios from 'axios'

export interface PremiumAudio {
  id: number
  uuid: string
  title: string
  audioUrl: string
  createAt: string | null
}
export interface PremiumDraft { title: string; audioUrl: string }

export async function getPremiumList(): Promise<PremiumAudio[]> {
  return (await axios.get<PremiumAudio[]>('/api/premium/admin/list')).data
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
