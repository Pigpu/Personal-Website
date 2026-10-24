import axios from 'axios'
import type { Countdown } from '../utils/countdown'

export async function getCountdown(): Promise<Countdown | null> {
  const response = await axios.get<Countdown>('/api/home/countdown')
  return response.status === 204 ? null : response.data
}

export async function saveCountdown(value: Countdown): Promise<Countdown> {
  return (await axios.put<Countdown>('/api/home/admin/countdown', value)).data
}
