import axios from 'axios'
import type { SiteLanguage } from './homepage'

export interface AboutTranslation { title: string; purpose1: string; purpose2: string }
export interface FriendLink { id: number; name: string; url: string; description: string }
export type FriendLinkDraft = Omit<FriendLink, 'id'> & { id?: number }
export interface AboutContent { translations: Partial<Record<SiteLanguage, AboutTranslation>>; links: FriendLink[] }

export async function getAboutContent(): Promise<AboutContent> {
  return (await axios.get<AboutContent>('/api/about')).data
}
export async function saveAboutProfile(translations: Record<SiteLanguage, AboutTranslation>): Promise<AboutContent> {
  return (await axios.put<AboutContent>('/api/about/admin/profile', { translations })).data
}
export async function saveFriendLink(value: FriendLinkDraft): Promise<FriendLink> {
  const { id, ...body } = value
  return id === undefined ? (await axios.post<FriendLink>('/api/about/admin/links', body)).data
    : (await axios.put<FriendLink>('/api/about/admin/links/' + id, body)).data
}
export async function deleteFriendLink(id: number) { await axios.delete('/api/about/admin/links/' + id) }
