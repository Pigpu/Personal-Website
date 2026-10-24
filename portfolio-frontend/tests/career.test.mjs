import { test } from 'node:test'
import assert from 'node:assert/strict'
import { careerPayload, careerPeriod, careerTags, careerToForm, isCurrentCareer } from '../src/utils/career.ts'

const experience = {
  id: 12, period: '2026-09-15 - 至今', startDate: '2026-09-15', endDate: null, isCurrent: true,
  company: 'Test university', position: 'Research student', description: 'First line\nSecond line', tags: 'Java, Vue,数据库',
}

test('editing preserves the record identity and all editable fields', () => {
  const form = careerToForm(experience)
  assert.equal(form.id, 12)
  assert.equal(form.startDate, '2026-09-15')
  assert.equal(form.endDate, '')
  assert.equal(form.isCurrent, true)
  assert.equal(form.company, experience.company)
  assert.equal(form.position, experience.position)
  assert.equal(form.description, experience.description)
  assert.deepEqual(form.tagsList, ['Java', 'Vue', '数据库'])
})

test('older records can still be edited using their period field', () => {
  const form = careerToForm({ ...experience, startDate: null, endDate: null, isCurrent: null, period: '2024-10-21 - 2026-02-13' })
  assert.equal(form.startDate, '2024-10-21')
  assert.equal(form.endDate, '2026-02-13')
  assert.equal(form.isCurrent, false)
})

test('saving a current experience clears old end dates and preserves the existing backend contract', () => {
  const form = { ...careerToForm(experience), endDate: '2027-01-01' }
  const payload = careerPayload(form)
  assert.equal(payload.id, 12)
  assert.equal(payload.endDate, '')
  assert.equal(payload.period, '2026-09-15 - 至今')
  assert.equal(payload.tags, 'Java,Vue,数据库')
  assert.equal('tagsList' in payload, false)
})

test('a current experience can be changed to a completed one', () => {
  const payload = careerPayload({ ...careerToForm(experience), isCurrent: false, endDate: '2027-03-31' })
  assert.equal(payload.isCurrent, false)
  assert.equal(payload.endDate, '2027-03-31')
  assert.equal(payload.period, '2026-09-15 - 2027-03-31')
})

test('localized dates only affect display and never mutate saved data', () => {
  assert.equal(careerPeriod(experience, 'Present'), '2026-09-15 — Present')
  assert.equal(careerPeriod(experience, '現在'), '2026-09-15 — 現在')
  assert.equal(experience.period, '2026-09-15 - 至今')
  assert.equal(isCurrentCareer({ ...experience, isCurrent: null }), true)
})

test('tag edits support whitespace, missing values and duplicates', () => {
  assert.deepEqual(careerTags(' Java, , Vue,Java, C++ '), ['Java', 'Vue', 'C++'])
  assert.deepEqual(careerTags(null), [])
})
