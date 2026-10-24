import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import axios from 'axios'
import { friendLinkHost, friendLinkUrl } from '../src/utils/about.ts'
import { deleteFriendLink, getAboutContent, saveAboutProfile, saveFriendLink } from '../src/services/about.ts'

const original = { get: axios.get, put: axios.put, post: axios.post, delete: axios.delete }
afterEach(() => { Object.assign(axios, original) })

test('friend navigation allows ordinary web links and rejects executable or credential-bearing links', () => {
  assert.equal(friendLinkUrl('https://example.com/blog?q=1#about'), 'https://example.com/blog?q=1#about')
  assert.equal(friendLinkUrl('http://example.com'), 'http://example.com/')
  assert.equal(friendLinkUrl('https://例え.jp/友人'), 'https://xn--r8jz45g.jp/%E5%8F%8B%E4%BA%BA')
  for (const value of ['', null, 'javascript:alert(1)', 'data:text/html,<script>', 'file:///tmp/a', '//example.com', 'https://user:pass@example.com', 'ftp://example.com', 'https://example.com:99999']) {
    assert.equal(friendLinkUrl(value), null)
  }
})

test('displayed domains use the actual destination rather than query or fragment text', () => {
  assert.equal(friendLinkHost('https://www.example.com/path?site=elsewhere.test#other.test'), 'example.com')
  assert.equal(friendLinkHost('javascript:alert(1)'), '')
})

test('each about read uses shared server content rather than browser storage or an in-memory cache', async () => {
  let reads = 0
  axios.get = async url => {
    assert.equal(url, '/api/about')
    return { data: { translations: {}, links: [{ id: 1, name: 'Update ' + ++reads }] } }
  }
  assert.equal((await getAboutContent()).links[0].name, 'Update 1')
  assert.equal((await getAboutContent()).links[0].name, 'Update 2')
})

test('profile saving submits all three distinct languages and preserves literal text and blank paragraphs', async () => {
  const translations = {
    zh: { title: '关于我', purpose1: '第一段\n换行', purpose2: '' },
    en: { title: 'About me', purpose1: '<b>literal text</b>', purpose2: 'Second paragraph' },
    ja: { title: '自己紹介', purpose1: '日本語の紹介', purpose2: '' },
  }
  axios.put = async (url, body) => {
    assert.equal(url, '/api/about/admin/profile')
    assert.deepEqual(body, { translations })
    return { data: { translations, links: [] } }
  }
  assert.equal((await saveAboutProfile(translations)).translations.ja.title, '自己紹介')
})

test('creating and editing links are separate operations; editing keeps the identity and deletion uses its own route', async () => {
  const draft = { name: 'A friend', url: 'https://example.com/', description: 'A short introduction' }
  axios.post = async (url, body) => {
    assert.equal(url, '/api/about/admin/links')
    assert.deepEqual(body, draft)
    return { data: { id: 42, ...body } }
  }
  const saved = await saveFriendLink(draft)
  axios.put = async (url, body) => {
    assert.equal(url, '/api/about/admin/links/42')
    assert.equal('id' in body, false)
    assert.equal(body.description, '')
    return { data: { id: 42, ...body } }
  }
  assert.equal((await saveFriendLink({ ...saved, description: '' })).id, 42)
  axios.delete = async url => { assert.equal(url, '/api/about/admin/links/42') }
  await deleteFriendLink(42)
  axios.put = async () => { throw new Error('forbidden') }
  await assert.rejects(saveAboutProfile({}), /forbidden/)
})
