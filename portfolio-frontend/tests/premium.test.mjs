import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import axios from 'axios'
import { premiumAudioUrl, premiumUuid } from '../src/utils/premium.ts'
import { getPremiumAccess, getPremiumList, getPremiumAudio, publishPremiumAudio, uploadPremiumAudio, getPremiumListeners, searchPremiumUsers, grantPremiumListener, revokePremiumListener } from '../src/services/premium.ts'

const original = { get: axios.get, post: axios.post, delete: axios.delete }
afterEach(() => Object.assign(axios, original))
const uuid = '11111111-2222-4333-8444-555555555555'

test('existing UUID links keep their exact identity and do not require a login-specific endpoint', async () => {
  const record = { id: 17, uuid, title: 'An existing episode', audioUrl: 'https://example.test/voice.wav' }
  axios.get = async url => {
    assert.equal(url, '/api/premium/play/' + uuid)
    return { data: record }
  }
  assert.equal(premiumUuid(uuid), uuid)
  assert.deepEqual(await getPremiumAudio(uuid), record)
  for (const value of ['', null, ['invalid'], '../admin/list', 'not-a-uuid']) assert.equal(premiumUuid(value), null)
})

test('publishing never submits an existing identity or overwrites a saved UUID', async () => {
  axios.post = async (url, body) => {
    assert.equal(url, '/api/premium/admin/save')
    assert.deepEqual(body, { title: 'New episode', audioUrl: 'https://example.test/song.flac' })
    return { data: '发布成功' }
  }
  await publishPremiumAudio({ id: 5, uuid, title: ' New episode ', audioUrl: ' https://example.test/song.flac ' })
})

test('a failed list request stays an error instead of being reported as an empty library', async () => {
  axios.get = async url => { assert.equal(url, '/api/premium/list'); throw new Error('Backend unavailable') }
  await assert.rejects(getPremiumList, /Backend unavailable/)
})

test('entry access is checked on the server each time instead of trusting a saved browser role', async () => {
  let read = 0
  axios.get = async url => {
    assert.equal(url, '/api/premium/access')
    return { data: { allowed: ++read === 1, admin: false } }
  }
  assert.equal((await getPremiumAccess()).allowed, true)
  assert.equal((await getPremiumAccess()).allowed, false)
})

test('listeners use the dedicated episode list and do not fetch administrator data', async () => {
  axios.get = async url => {
    assert.equal(url, '/api/premium/list')
    return { data: [{ id: 1, uuid, title: 'Episode', createAt: null }] }
  }
  assert.equal((await getPremiumList())[0].title, 'Episode')
})

test('account search and listener changes use administrator-only endpoints without changing account roles', async () => {
  const listener = { userId: 7, username: 'reader', grantedAt: '2026-10-28T12:00:00' }
  axios.get = async (url, config) => {
    if (url === '/api/premium/admin/users') {
      assert.deepEqual(config.params, { query: 'reader' })
      return { data: [{ userId: 7, username: 'reader', authorized: false, admin: false }] }
    }
    assert.equal(url, '/api/premium/admin/listeners')
    return { data: [listener] }
  }
  assert.equal((await searchPremiumUsers(' reader '))[0].username, 'reader')
  assert.deepEqual(await getPremiumListeners(), [listener])
  axios.post = async (url, body) => {
    assert.equal(url, '/api/premium/admin/listeners')
    assert.equal('role' in body, false)
    assert.ok(body.userId === 7 || body.username === 'reader')
    return { data: listener }
  }
  assert.deepEqual(await grantPremiumListener({ userId: 7 }), listener)
  assert.deepEqual(await grantPremiumListener({ username: 'reader' }), listener)
  axios.delete = async url => { assert.equal(url, '/api/premium/admin/listeners/7'); return {} }
  await revokePremiumListener(7)
})

test('uploads reuse the existing media upload endpoint and field names', async () => {
  const file = new File(['test-audio'], 'voice.wav', { type: 'audio/wav' })
  axios.post = async (url, body) => {
    assert.equal(url, '/api/upload/project')
    assert.equal(body.get('type'), 'media')
    assert.equal(body.get('file').name, 'voice.wav')
    return { data: '/uploads/media/voice.wav' }
  }
  assert.equal(await uploadPremiumAudio(file), '/uploads/media/voice.wav')
})

test('audio URLs preserve signed query strings, local uploads and non-MP3 formats', () => {
  for (const value of ['https://example.test/file.wav?sign=A%2FB&expires=999', 'http://localhost:8080/uploads/song.m4a', '/uploads/media/song.flac']) {
    assert.equal(premiumAudioUrl(value), value)
  }
  for (const value of ['', null, 'javascript:alert(1)', 'file:///C:/song.mp3', '//example.test/song.mp3', '/api/premium/admin/list', 'https://user:password@example.test/song.mp3']) {
    assert.equal(premiumAudioUrl(value), null)
  }
})
