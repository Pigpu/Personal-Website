import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import axios from 'axios'
import { daysUntil, legacyCountdown } from '../src/utils/countdown.ts'
import { getCountdown, saveCountdown } from '../src/services/countdown.ts'

const originalGet = axios.get
const originalPut = axios.put
afterEach(() => { axios.get = originalGet; axios.put = originalPut })

test('calendar days clamp past events to zero and support leap dates', () => {
  assert.equal(daysUntil('2028-02-29', new Date(2028, 1, 28, 23, 59)), 1)
  assert.equal(daysUntil('2028-02-29', new Date(2028, 1, 29, 12)), 0)
  assert.equal(daysUntil('2028-02-29', new Date(2028, 2, 1)), 0)
  assert.equal(daysUntil('2027-01-01', new Date(2026, 11, 31)), 1)
})

test('day counts use local calendar dates across DST and negative UTC offsets', () => {
  const oldTZ = process.env.TZ
  try {
    for (const zone of ['Asia/Tokyo', 'America/Los_Angeles', 'America/New_York']) {
      process.env.TZ = zone
      assert.equal(daysUntil('2026-10-27', new Date(2026, 9, 25, 23, 59)), 2)
      assert.equal(daysUntil('2026-03-09', new Date(2026, 2, 7, 12)), 2)
      assert.equal(daysUntil('2026-11-02', new Date(2026, 9, 31, 12)), 2)
    }
  } finally { if (oldTZ === undefined) delete process.env.TZ; else process.env.TZ = oldTZ }
})

test('invalid dates never produce NaN or rolled-over calendar days', () => {
  for (const date of ['', '2026-02-29', '2026-04-31', '0000-01-01', '2026-13-01', '2026-1-01', '2026-01-01T00:00:00Z']) {
    assert.equal(daysUntil(date), null)
  }
})

test('old browser settings can be offered as a validated editor draft', () => {
  assert.deepEqual(legacyCountdown(' 修士入学考 ', '2028-02-29'), { event: '修士入学考', targetDate: '2028-02-29' })
  assert.equal(legacyCountdown(null, null), null)
  assert.equal(legacyCountdown(' ', '2028-02-29'), null)
  assert.equal(legacyCountdown('JLPT N2', 'invalid'), null)
})

test('each visitor read uses the server, and unconfigured content stays empty', async () => {
  let calls = 0
  axios.get = async url => {
    assert.equal(url, '/api/home/countdown')
    return ++calls === 1 ? { status: 204 } : { status: 200, data: { event: 'Shared event', targetDate: '2028-02-29' } }
  }
  assert.equal(await getCountdown(), null)
  assert.deepEqual(await getCountdown(), { event: 'Shared event', targetDate: '2028-02-29' })
  assert.equal(calls, 2)
})

test('saving uses the authenticated update route and failed saves are not reported as success', async () => {
  const value = { event: '修士入学考', targetDate: '2028-02-29' }
  axios.put = async (url, body) => {
    assert.equal(url, '/api/home/admin/countdown')
    assert.deepEqual(body, value)
    return { data: value }
  }
  assert.deepEqual(await saveCountdown(value), value)
  axios.put = async () => { throw new Error('forbidden') }
  await assert.rejects(saveCountdown(value), /forbidden/)
  axios.get = async () => { throw new Error('offline') }
  await assert.rejects(getCountdown(), /offline/)
})
