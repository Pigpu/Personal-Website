export interface Career {
  id?: number
  period: string
  startDate?: string | null
  endDate?: string | null
  isCurrent?: boolean | null
  company: string
  position: string
  description: string
  tags: string
}

export interface CareerForm {
  id?: number
  startDate: string
  endDate: string
  isCurrent: boolean
  company: string
  position: string
  description: string
  tagsList: string[]
}

export function careerTags(tags: string | null | undefined): string[] {
  return [...new Set((tags || '').split(',').map(tag => tag.trim()).filter(Boolean))]
}

export function isCurrentCareer(item: Career): boolean {
  return item.isCurrent ?? /(?:至今|present|current|現在)\s*$/i.test(item.period || '')
}

export function careerToForm(item: Career): CareerForm {
  const [start = '', end = ''] = (item.period || '').split(' - ')
  const current = isCurrentCareer(item)
  return {
    id: item.id,
    startDate: item.startDate || start,
    endDate: current ? '' : item.endDate || end,
    isCurrent: current,
    company: item.company || '',
    position: item.position || '',
    description: item.description || '',
    tagsList: careerTags(item.tags),
  }
}

export function careerPeriod(item: Career, presentLabel: string): string {
  const value = careerToForm(item)
  return [value.startDate, value.isCurrent ? presentLabel : value.endDate].filter(Boolean).join(' — ')
}

export function careerPayload(form: CareerForm) {
  const endDate = form.isCurrent ? '' : form.endDate
  return {
    id: form.id,
    startDate: form.startDate,
    endDate,
    isCurrent: form.isCurrent,
    period: `${form.startDate} - ${form.isCurrent ? '至今' : endDate}`,
    company: form.company,
    position: form.position,
    description: form.description,
    tags: careerTags(form.tagsList.join(',')).join(','),
  }
}
