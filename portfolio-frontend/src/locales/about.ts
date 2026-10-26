export const aboutUi = {
  zh: {
    editProfile: '编辑介绍', friends: '友情链接', addLink: '添加友情链接', editLink: '编辑友情链接', edit: '编辑', remove: '删除', empty: '这里还没有友情链接。', loading: '正在加载…', loadFailed: '内容暂时无法加载，请重试。', retry: '重新加载',
    title: '介绍标题', purpose1: '第一段介绍', purpose2: '第二段介绍', profileHint: '分别维护中、英、日三套介绍。切换语言会保留已输入内容，保存时一起更新。段落留空则隐藏。', name: '网站名称', url: '网站链接', description: '简短介绍（选填）', linkHint: '请填写完整的 HTTP 或 HTTPS 地址。链接将在新窗口打开，名称与介绍在三种语言中共用。',
    save: '保存', saving: '保存中…', cancel: '取消', close: '关闭', saveFailed: '保存失败，输入内容仍然保留，请重试。', forbidden: '登录已过期或没有管理员权限，请重新登录。', invalidLink: '请填写网站名称和有效的 HTTP 或 HTTPS 地址。', invalidProfile: '请为中、英、日三种语言分别填写标题。',
    discardTitle: '还有未保存的修改', discardHint: '离开后，本次尚未保存的修改将会丢失。', discard: '放弃修改并离开', keepEditing: '继续编辑', deleteTitle: '删除这条友情链接？', deleteHint: '删除后无法恢复，请确认是否继续。', confirmDelete: '确认删除', deleting: '删除中…', deleteFailed: '删除失败，请重试。', newWindow: '在新窗口打开',
  },
  en: {
    editProfile: 'Edit introduction', friends: 'Friends on the web', addLink: 'Add a link', editLink: 'Edit link', edit: 'Edit', remove: 'Delete', empty: 'No friend links yet.', loading: 'Loading…', loadFailed: 'Content could not be loaded. Please try again.', retry: 'Reload',
    title: 'Introduction title', purpose1: 'First paragraph', purpose2: 'Second paragraph', profileHint: 'Maintain Chinese, English and Japanese separately. Switching languages preserves your input; saving updates all three. Blank paragraphs are hidden.', name: 'Website name', url: 'Website link', description: 'Short description (optional)', linkHint: 'Enter a complete HTTP or HTTPS address. Links open in a new window. Names and descriptions are shared across languages.',
    save: 'Save', saving: 'Saving…', cancel: 'Cancel', close: 'Close', saveFailed: 'Save failed. Your input is preserved. Please try again.', forbidden: 'Your session expired or you do not have administrator access. Please sign in again.', invalidLink: 'Enter a website name and a valid HTTP or HTTPS address.', invalidProfile: 'Enter a title for each of the three languages.',
    discardTitle: 'You have unsaved changes', discardHint: 'Leaving now will discard your unsaved changes.', discard: 'Discard and leave', keepEditing: 'Keep editing', deleteTitle: 'Delete this friend link?', deleteHint: 'This cannot be undone. Please confirm before continuing.', confirmDelete: 'Delete link', deleting: 'Deleting…', deleteFailed: 'Could not delete. Please try again.', newWindow: 'Opens in a new window',
  },
  ja: {
    editProfile: '紹介を編集', friends: 'リンク集', addLink: 'リンクを追加', editLink: 'リンクを編集', edit: '編集', remove: '削除', empty: 'まだリンクがありません。', loading: '読み込み中…', loadFailed: '内容を読み込めませんでした。再試行してください。', retry: '再読み込み',
    title: '紹介のタイトル', purpose1: '最初の段落', purpose2: '二つ目の段落', profileHint: '中国語・英語・日本語を個別に編集できます。言語を切り替えても入力内容は保持され、保存時に三言語をまとめて更新します。空欄の段落は表示されません。', name: 'サイト名', url: 'サイトのリンク', description: '短い紹介（任意）', linkHint: 'HTTP または HTTPS の完全なアドレスを入力してください。リンクは新しいウィンドウで開き、サイト名と紹介は三言語で共通です。',
    save: '保存', saving: '保存中…', cancel: 'キャンセル', close: '閉じる', saveFailed: '保存できませんでした。入力内容は保持されています。再試行してください。', forbidden: 'セッションの期限切れ、または管理者権限がありません。再度ログインしてください。', invalidLink: 'サイト名と有効な HTTP または HTTPS アドレスを入力してください。', invalidProfile: '三言語すべてにタイトルを入力してください。',
    discardTitle: '未保存の変更があります', discardHint: 'このページを離れると、未保存の変更は失われます。', discard: '変更を破棄して離れる', keepEditing: '編集を続ける', deleteTitle: 'このリンクを削除しますか？', deleteHint: '削除後は元に戻せません。続行する前にご確認ください。', confirmDelete: '削除する', deleting: '削除中…', deleteFailed: '削除できませんでした。再試行してください。', newWindow: '新しいウィンドウで開きます',
  },
} as const
