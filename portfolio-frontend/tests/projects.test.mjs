import { test } from 'node:test'
import assert from 'node:assert/strict'
import { attachmentTarget, emptyProject, projectChanged, projectDraft, projectPayload, sortedProjects, uploadedMediaType } from '../src/utils/projects.ts'

test('both project orders work for search responses without changing server data', () => {
  const projects = [
    { id: 1, createdAt: '2026-01-01', likeCount: 10 },
    { id: 2, createdAt: '2026-03-01', likeCount: 2 },
    { id: 3, createdAt: '2026-02-01', likeCount: 10 },
    { id: 4, createdAt: null, likeCount: null },
  ]
  assert.deepEqual(sortedProjects(projects, 'latest').map(item => item.id), [2, 3, 1, 4])
  assert.deepEqual(sortedProjects(projects, 'likes').map(item => item.id), [3, 1, 2, 4])
  assert.deepEqual(projects.map(item => item.id), [1, 2, 3, 4])
})

test('an existing project opens clean; actual edits and reversions control the leave prompt', () => {
  const baseline = projectDraft({ id: 7, title: '作品', category: '原有自定义分类', description: '说明', coverUrl: 'https://cdn.example/cover.jpg', mediaUrl: 'https://cdn.example/song.mp3', mediaType: 'AUDIO', attachmentUrl: '/uploads/files/source.zip' })
  assert.equal(projectChanged({ ...baseline }, baseline), false)
  for (const field of ['category', 'mediaType', 'attachmentUrl', 'title', 'description', 'coverUrl', 'mediaUrl']) {
    const edited = { ...baseline, [field]: baseline[field] + ' changed' }
    assert.equal(projectChanged(edited, baseline), true)
    edited[field] = baseline[field]
    assert.equal(projectChanged(edited, baseline), false)
  }
})

test('publishing preserves external links, local links, media choice and an explicitly removed attachment', () => {
  const form = { ...emptyProject(), title: '  音乐作品  ', description: '第一行\n第二行', coverUrl: ' https://cdn.example/cover.jpg ', mediaUrl: ' /uploads/media/audio.mp3 ', mediaType: 'AUDIO', attachmentUrl: '' }
  const payload = projectPayload(form)
  assert.equal(payload.title, '音乐作品')
  assert.equal(payload.description, form.description)
  assert.equal(payload.coverUrl, 'https://cdn.example/cover.jpg')
  assert.equal(payload.mediaUrl, '/uploads/media/audio.mp3')
  assert.equal(payload.mediaType, 'AUDIO')
  assert.equal(payload.attachmentUrl, '')
  assert.equal(form.title, '  音乐作品  ')
  assert.equal('id' in payload, false)
  assert.equal('likeCount' in payload, false)
})

test('older optional fields do not become dirty merely from opening the editor', () => {
  const value = projectDraft({ id: 1, title: '旧作品' })
  assert.equal(value.category, '视频剪辑')
  assert.equal(value.attachmentUrl, '')
  assert.equal(value.mediaType, 'VIDEO')
  assert.equal(projectChanged({ ...value }, value), false)
})

test('local and external attachment downloads keep their existing destinations', () => {
  assert.deepEqual(attachmentTarget('https://cdn.example/a.zip?key=abc'), { external: true, url: 'https://cdn.example/a.zip?key=abc' })
  assert.deepEqual(attachmentTarget('http://cdn.example/old.pdf'), { external: true, url: 'http://cdn.example/old.pdf' })
  const local = '/uploads/files/作品 #1.zip'
  assert.deepEqual(attachmentTarget(local), { external: false, url: '/api/download?fileUrl=' + encodeURIComponent(local) })
})

test('local uploads choose audio for audio files even when the browser omits their MIME type', () => {
  assert.equal(uploadedMediaType({ type: 'audio/mpeg', name: 'song' }), 'AUDIO')
  assert.equal(uploadedMediaType({ type: '', name: 'song.FLAC' }), 'AUDIO')
  assert.equal(uploadedMediaType({ type: 'video/mp4', name: 'video.mp4' }), 'VIDEO')
})
