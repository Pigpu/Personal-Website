import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import axios from 'axios'
import { clearQuoteCache, getRandomQuote, saveQuote, siteLanguage, songLink } from '../src/services/homepage.ts'

const originalGet = axios.get
const originalPost = axios.post
const originalPut = axios.put
afterEach(() => {
  axios.get = originalGet
  axios.post = originalPost
  axios.put = originalPut
  clearQuoteCache()
})

test('keeps one quote per language during navigation and refreshes after cache reset', async () => {
  let calls = 0
  axios.get = async (_url, { params }) => ({ status: 200, data: { id: ++calls, language: params.lang } })
  const first = await getRandomQuote('zh')
  assert.equal(await getRandomQuote('zh'), first)
  assert.equal(calls, 1)
  assert.equal((await getRandomQuote('en')).language, 'en')
  assert.equal(calls, 2)
  clearQuoteCache()
  assert.notEqual((await getRandomQuote('zh')).id, first.id)
})

test('an empty quote stays empty for this visit without repeated requests', async () => {
  let calls = 0
  axios.get = async () => { calls++; return { status: 204 } }
  assert.equal(await getRandomQuote('ja'), null)
  assert.equal(await getRandomQuote('ja'), null)
  assert.equal(calls, 1)
})

test('a failed quote request can be retried', async () => {
  let calls = 0
  axios.get = async () => {
    if (++calls === 1) throw new Error('unavailable')
    return { status: 200, data: { id: 1 } }
  }
  await assert.rejects(getRandomQuote('zh'))
  assert.equal((await getRandomQuote('zh')).id, 1)
})

test('editing a quote uses its update endpoint and invalidates cached selection', async () => {
  let reads = 0
  axios.get = async () => ({ status: 200, data: { id: ++reads } })
  const value = { id: 42, content: 'A quote', source: '', language: null, enabled: false }
  axios.put = async (url, body) => {
    assert.equal(url, '/api/home/admin/quotes/42')
    assert.equal('id' in body, false)
    assert.equal(body.enabled, false)
    return { data: value }
  }
  await getRandomQuote('zh')
  assert.equal(await saveQuote(value), value)
  await getRandomQuote('zh')
  assert.equal(reads, 2)
})

test('creating a quote uses the create endpoint', async () => {
  axios.post = async (url, body) => {
    assert.equal(url, '/api/home/admin/quotes')
    assert.equal(body.content, 'A new quote')
    return { data: { id: 43, ...body } }
  }
  assert.equal((await saveQuote({ content: 'A new quote', source: '', language: 'en', enabled: true })).id, 43)
})

test('song links only point to official HTTPS NetEase origins', () => {
  assert.equal(songLink('https://music.163.com/#/song?id=123'), 'https://music.163.com/#/song?id=123')
  for (const value of ['javascript:alert(1)', 'http://music.163.com/song?id=123', 'https://music.163.com.evil.test/song?id=123', 'https://user@music.163.com/song?id=123', 'https://music.163.com:9999/song?id=123', '']) {
    assert.equal(songLink(value), null)
  }
})

test('unsupported site languages fall back to Chinese', () => {
  assert.equal(siteLanguage('en'), 'en')
  assert.equal(siteLanguage('ja'), 'ja')
  assert.equal(siteLanguage('unknown'), 'zh')
})
