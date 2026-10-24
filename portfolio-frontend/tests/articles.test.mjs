import { test } from 'node:test'
import assert from 'node:assert/strict'
import { articleChanged, articlePayload, emptyArticle, parseArticleDraft, plainExcerpt, sortedArticles } from '../src/utils/articles.ts'

const articles = [
  { id: 1, title: 'First', createdAt: '2026-01-01T10:00:00', likeCount: 3 },
  { id: 2, title: 'Second', createdAt: '2026-02-01T10:00:00', likeCount: 8 },
  { id: 3, title: 'Third', createdAt: '2026-03-01T10:00:00', likeCount: 5 },
]

test('publication date sorting supports both directions without mutating the server response', () => {
  assert.deepEqual(sortedArticles(articles, 'time', 'desc').map(item => item.id), [3, 2, 1])
  assert.deepEqual(sortedArticles(articles, 'time', 'asc').map(item => item.id), [1, 2, 3])
  assert.deepEqual(articles.map(item => item.id), [1, 2, 3])
})

test('like sorting supports most and least liked first', () => {
  assert.deepEqual(sortedArticles(articles, 'likes', 'desc').map(item => item.id), [2, 3, 1])
  assert.deepEqual(sortedArticles(articles, 'likes', 'asc').map(item => item.id), [1, 3, 2])
})

test('tied like counts keep a deterministic newest-first order and missing counts act as zero', () => {
  const tied = articles.map(item => ({ ...item, likeCount: 0 }))
  tied[0].likeCount = null
  assert.deepEqual(sortedArticles(tied, 'likes', 'desc').map(item => item.id), [3, 2, 1])
  assert.deepEqual(sortedArticles(tied, 'likes', 'asc').map(item => item.id), [3, 2, 1])
})

test('excerpts remove images, Markdown decoration, HTML and link destinations', () => {
  const content = '![](https://example.test/photo.jpg)\n## My **story**\nA [good day](https://example.test) with <b>friends</b>.\n```js\nsecretCode()\n```'
  assert.equal(plainExcerpt(content), 'My story A good day with friends.')
  assert.equal(plainExcerpt(null), '')
})

test('excerpt limits preserve complete Unicode characters', () => {
  assert.equal(plainExcerpt('🐱🐱🐱🐱', 3), '🐱🐱🐱…')
  assert.equal(plainExcerpt('short', 10), 'short')
})

test('opening an existing article is clean and reverting edits clears the unsaved flag', () => {
  const baseline = { title: 'My title', content: 'Existing text', category: '生活' }
  assert.equal(articleChanged({ ...baseline }, baseline), false)
  const edited = { ...baseline, content: 'Changed text' }
  assert.equal(articleChanged(edited, baseline), true)
  edited.content = baseline.content
  assert.equal(articleChanged(edited, baseline), false)
})

test('changing only the category is still an unsaved modification', () => {
  assert.equal(articleChanged({ ...emptyArticle(), category: '生活' }, emptyArticle()), true)
})

test('legacy drafts restore content and receive the original default category', () => {
  assert.deepEqual(parseArticleDraft(JSON.stringify({ title: 'Draft', content: 'Text', time: 123 })), { title: 'Draft', content: 'Text', category: '技术' })
})

test('new drafts retain category, including category-only edits', () => {
  const draft = { title: 'Draft', content: 'Text', category: '杂谈' }
  assert.deepEqual(parseArticleDraft(JSON.stringify(draft)), draft)
  assert.deepEqual(parseArticleDraft(JSON.stringify({ ...emptyArticle(), category: '生活' })), { ...emptyArticle(), category: '生活' })
})

test('empty, invalid and untouched starter drafts do not cause restore prompts', () => {
  for (const value of [null, '{bad json', 'null', '{}', '{"title":42,"content":"text"}', JSON.stringify(emptyArticle()), JSON.stringify({ title: '', content: '# 开始你的写作...' })]) {
    assert.equal(parseArticleDraft(value), null)
  }
})

test('publishing keeps Markdown intact while generating a plain summary and first-image cover', () => {
  const draft = { title: ' My title ', content: '# Hello\n**World**\n![cover](https://example.test/cover.jpg)', category: '生活' }
  const payload = articlePayload(draft)
  assert.equal(payload.title, 'My title')
  assert.equal(payload.content, draft.content)
  assert.equal(payload.coverUrl, 'https://example.test/cover.jpg')
  assert.equal(payload.summary, 'Hello World')
  assert.equal(payload.category, '生活')
})

test('articles without images publish with an empty cover and preserve their category', () => {
  const payload = articlePayload({ title: 'No image', content: 'Just text', category: '其他' })
  assert.equal(payload.coverUrl, '')
  assert.equal(payload.summary, 'Just text')
})
