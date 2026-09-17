/*
 * MAURIMAX for Windows.
 *
 * The same panel, the same account and the same catalogue as the phone and the
 * television. The differences are the ones the machine forces: a mouse and a
 * keyboard instead of a remote, and a browser engine instead of ExoPlayer, so
 * live streams are played through hls.js and mpegts.js rather than natively.
 */

const $ = (id) => document.getElementById(id)
let PORTAL = 'http://hlaamart.site'
let account = null                 // { username, password }
let tab = 'live'
let categories = []                // [{ id, name }]
let chosenCategory = null
let items = []                     // what is on screen now
let liveLine = []                  // every channel, in order, for ▲ ▼ and dialling
let playing = null
let engine = null                  // the hls.js or mpegts.js instance in use
let typedDigits = ''
let typingTimer = null

// ---------------------------------------------------------------- panel

async function panel (action, params) {
  const r = await window.mx.panel(action, account.username, account.password, params)
  if (!r.ok) throw new Error(r.error)
  return r.data
}

/** The panel answers an empty list as `null` or as an error object. */
const asList = (v) => (Array.isArray(v) ? v : [])

const streamUrl = (kind, id, ext) => {
  const u = encodeURIComponent(account.username)
  const p = encodeURIComponent(account.password)
  if (kind === 'live') return `${PORTAL}/live/${u}/${p}/${id}.m3u8`
  if (kind === 'movie') return `${PORTAL}/movie/${u}/${p}/${id}.${ext || 'mp4'}`
  return `${PORTAL}/series/${u}/${p}/${id}.${ext || 'mp4'}`
}

// ---------------------------------------------------------------- sign in

async function signIn (username, password) {
  const r = await window.mx.panel(null, username, password, {})
  if (!r.ok) throw new Error(r.error)
  const info = r.data && r.data.user_info
  if (!info || String(info.auth) !== '1') throw new Error('بيانات الدخول غير صحيحة')
  if (info.status && String(info.status).toLowerCase() !== 'active') {
    throw new Error(`الاشتراك ${info.status}`)
  }
  return { username, password }
}

$('go').onclick = async () => {
  const username = $('u').value.trim()
  const password = $('p').value.trim()
  if (!username || !password) return
  $('go').disabled = true
  $('go').textContent = 'جارٍ التحقق…'
  $('loginErr').textContent = ''
  try {
    account = await signIn(username, password)
    await window.mx.saveAccount(account)
    enter()
  } catch (error) {
    $('loginErr').textContent = String(error.message || error)
  } finally {
    $('go').disabled = false
    $('go').textContent = 'دخول'
  }
}

$('u').onkeydown = $('p').onkeydown = (e) => { if (e.key === 'Enter') $('go').click() }

$('out').onclick = async () => {
  await window.mx.forgetAccount()
  stop()
  account = null
  $('app').classList.add('hide')
  $('login').classList.remove('hide')
  $('p').value = ''
}

function enter () {
  $('login').classList.add('hide')
  $('app').classList.remove('hide')
  $('who').textContent = (account.username[0] || 'M').toUpperCase()
  openTab('live')
}

// ---------------------------------------------------------------- catalogue

document.querySelectorAll('.tab').forEach((button) => {
  button.onclick = () => openTab(button.dataset.tab)
})

async function openTab (which) {
  tab = which
  chosenCategory = null
  $('q').value = ''
  document.querySelectorAll('.tab').forEach((b) => b.classList.toggle('on', b.dataset.tab === which))
  $('cats').innerHTML = ''
  $('body').innerHTML = '<div class="state"><div class="spin"></div>جارٍ التحميل…</div>'
  $('count').textContent = ''

  const action = which === 'live' ? 'get_live_categories'
    : which === 'movies' ? 'get_vod_categories' : 'get_series_categories'
  try {
    categories = asList(await panel(action)).map((c) => ({
      id: String(c.category_id),
      name: String(c.category_name || '').trim() || 'بدون اسم',
    }))
  } catch (error) {
    return fail(error)
  }
  if (!categories.length) return empty()

  // Everything at once, then filed by category here — one request instead of
  // one per rail, which is the difference between seconds and minutes.
  await loadEverything()
}

async function loadEverything () {
  const action = tab === 'live' ? 'get_live_streams'
    : tab === 'movies' ? 'get_vod_streams' : 'get_series'
  let all = []
  try {
    all = asList(await panel(action))
  } catch (error) {
    return fail(error)
  }

  const byCategory = new Map()
  for (const raw of all) {
    const item = shape(raw)
    if (!item) continue
    const key = String(raw.category_id)
    if (!byCategory.has(key)) byCategory.set(key, [])
    byCategory.get(key).push(item)
  }

  categories = categories
    .map((c) => ({ ...c, items: byCategory.get(c.id) || [] }))
    .filter((c) => c.items.length)

  if (!categories.length) return empty()

  if (tab === 'live') liveLine = categories.flatMap((c) => c.items)
  const total = categories.reduce((sum, c) => sum + c.items.length, 0)
  $('count').textContent = `${total.toLocaleString('en')} عنوان · ${categories.length} قسم`

  drawCategories()
  choose(categories[0].id)
}

function shape (raw) {
  const name = String(raw.name || '').trim()
  if (!name) return null
  if (tab === 'live') {
    return {
      kind: 'live',
      id: String(raw.stream_id),
      number: Number(raw.num) || 0,
      title: name,
      art: raw.stream_icon || '',
      url: streamUrl('live', raw.stream_id),
    }
  }
  if (tab === 'movies') {
    return {
      kind: 'movie',
      id: String(raw.stream_id),
      title: name,
      art: raw.stream_icon || '',
      rating: raw.rating || '',
      url: streamUrl('movie', raw.stream_id, raw.container_extension),
    }
  }
  return {
    kind: 'series',
    id: String(raw.series_id),
    title: name,
    art: raw.cover || '',
    rating: raw.rating || '',
    plot: raw.plot || '',
  }
}

function drawCategories () {
  $('cats').innerHTML = categories.map((c) => `
    <div class="cat" data-id="${c.id}">
      <span class="t">${escape(c.name)}</span><span class="n">${c.items.length}</span>
    </div>`).join('')
  $('cats').querySelectorAll('.cat').forEach((el) => {
    el.onclick = () => choose(el.dataset.id)
  })
}

function choose (id) {
  chosenCategory = id
  $('cats').querySelectorAll('.cat').forEach((el) => el.classList.toggle('on', el.dataset.id === id))
  const category = categories.find((c) => c.id === id)
  items = category ? category.items : []
  draw(items)
}

// ---------------------------------------------------------------- drawing

function draw (list) {
  if (!list.length) return empty()
  $('body').scrollTop = 0
  if (tab === 'live') {
    $('body').innerHTML = list.map((c) => `
      <div class="chan" data-id="${c.id}">
        <span class="num">${c.number || ''}</span>
        <span class="lg">${c.art ? `<img src="${escape(c.art)}" onerror="this.remove()">` : ''}</span>
        <span class="nm">${escape(c.title)}</span>
      </div>`).join('')
    $('body').querySelectorAll('.chan').forEach((el) => {
      el.onclick = () => play(list.find((c) => c.id === el.dataset.id))
    })
  } else {
    $('body').innerHTML = `<div class="grid">${list.map((m) => `
      <div class="tile" data-id="${m.id}">
        <div class="art">${m.art
          ? `<img src="${escape(m.art)}" onerror="this.parentNode.textContent='${escape(m.title).slice(0, 40)}'">`
          : `<span>${escape(m.title)}</span>`}</div>
        <div class="t">${escape(m.title)}</div>
      </div>`).join('')}</div>`
    $('body').querySelectorAll('.tile').forEach((el) => {
      const item = list.find((m) => m.id === el.dataset.id)
      el.onclick = () => (item.kind === 'series' ? openSeries(item) : openMovie(item))
    })
  }
}

const empty = () => { $('body').innerHTML = '<div class="state"><b>لا يوجد شيء هنا</b>جرّب قسماً آخر.</div>' }

const fail = (error) => {
  $('body').innerHTML = `<div class="state"><b>تعذّر الوصول إلى الخادم</b>${escape(String(error.message || error))}</div>`
}

function escape (s) {
  return String(s).replace(/[&<>"']/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
}

// ---------------------------------------------------------------- search

$('q').oninput = () => {
  const q = $('q').value.trim().toLowerCase()
  if (!q) return choose(chosenCategory)
  const hits = categories
    .flatMap((c) => c.items)
    .filter((i) => i.title.toLowerCase().includes(q))
    .slice(0, 300)
  draw(hits)
}

// ---------------------------------------------------------------- films and series

function openMovie (movie) {
  $('detail').classList.remove('hide')
  $('detail').innerHTML = `
    <div class="top">
      <div class="poster">${movie.art ? `<img src="${escape(movie.art)}">` : ''}</div>
      <div>
        <h2>${escape(movie.title)}</h2>
        <div class="meta">${movie.rating ? '★ ' + escape(movie.rating) + ' · ' : ''}فيلم</div>
        <div class="acts">
          <button class="pbtn a" id="dplay">▶ تشغيل</button>
          <button class="pbtn" id="dback">رجوع</button>
        </div>
      </div>
    </div>`
  $('dplay').onclick = () => { $('detail').classList.add('hide'); play(movie) }
  $('dback').onclick = () => $('detail').classList.add('hide')
}

async function openSeries (series) {
  $('detail').classList.remove('hide')
  $('detail').innerHTML = '<div class="state"><div class="spin"></div>جارٍ تحميل الحلقات…</div>'

  let seasons = []
  try {
    const info = await panel('get_series_info', { series_id: series.id })
    const raw = info && info.episodes ? info.episodes : {}
    const flat = []
    for (const key of Object.keys(raw)) {
      const fromKey = parseInt(key, 10) || 0
      for (const e of asList(raw[key])) {
        const own = parseInt(e.season, 10) || 0
        flat.push({
          season: own > 0 ? own : fromKey,
          number: parseInt(e.episode_num, 10) || 0,
          title: String(e.title || '').trim(),
          minutes: Math.round((parseInt((e.info || {}).duration_secs, 10) || 0) / 60),
          url: parseInt(e.id, 10) > 0
            ? streamUrl('series', parseInt(e.id, 10), e.container_extension)
            : '',
        })
      }
    }
    const grouped = new Map()
    for (const e of flat) {
      if (!grouped.has(e.season)) grouped.set(e.season, [])
      grouped.get(e.season).push(e)
    }
    seasons = [...grouped.entries()]
      .sort((a, b) => a[0] - b[0])
      .map(([n, eps]) => ({ number: n, episodes: eps.sort((a, b) => a.number - b.number) }))
  } catch (error) {
    $('detail').innerHTML = `<div class="state"><b>تعذّر تحميل الحلقات</b>${escape(String(error.message || error))}</div>`
    return
  }

  let chosenSeason = seasons.length ? seasons[0].number : 0
  const render = () => {
    const season = seasons.find((s) => s.number === chosenSeason) || { episodes: [] }
    $('detail').innerHTML = `
      <div class="top">
        <div class="poster">${series.art ? `<img src="${escape(series.art)}">` : ''}</div>
        <div>
          <h2>${escape(series.title)}</h2>
          <div class="meta">${series.rating ? '★ ' + escape(series.rating) + ' · ' : ''}مسلسل · ${seasons.length} موسم</div>
          ${series.plot ? `<div class="plot">${escape(series.plot)}</div>` : ''}
          <div class="acts"><button class="pbtn" id="dback">رجوع</button></div>
        </div>
      </div>
      <div class="seasons">${seasons.map((s) =>
        `<button class="spill${s.number === chosenSeason ? ' on' : ''}" data-s="${s.number}">الموسم ${s.number}</button>`).join('')}</div>
      ${season.episodes.map((e, i) => `
        <div class="ep" data-i="${i}">
          <span class="n">${e.season}×${String(e.number).padStart(2, '0')}</span>
          <span class="t">${escape(e.title || 'الحلقة ' + e.number)}</span>
          <span class="d">${e.minutes ? e.minutes + ' د' : ''}</span>
        </div>`).join('') || '<div class="state">لا توجد حلقات في هذا الموسم.</div>'}`

    $('dback').onclick = () => $('detail').classList.add('hide')
    $('detail').querySelectorAll('.spill').forEach((b) => {
      b.onclick = () => { chosenSeason = Number(b.dataset.s); render() }
    })
    $('detail').querySelectorAll('.ep').forEach((el) => {
      el.onclick = () => {
        const episode = season.episodes[Number(el.dataset.i)]
        if (!episode.url) return
        $('detail').classList.add('hide')
        play({ kind: 'episode', title: `${series.title} — ${episode.title || 'الحلقة ' + episode.number}`, url: episode.url })
      }
    })
  }
  render()
}

// ---------------------------------------------------------------- playback

function stop () {
  const video = $('v')
  if (engine) {
    try { engine.destroy() } catch {}
    engine = null
  }
  video.removeAttribute('src')
  try { video.load() } catch {}
}

function play (item) {
  if (!item || !item.url) return
  playing = item
  stop()

  $('player').classList.remove('hide')
  $('pmsg').classList.add('hide')
  $('ptitle').textContent = item.title
  $('pnum').textContent = item.number ? String(item.number) : ''
  $('liveTag').classList.toggle('hide', item.kind !== 'live')
  $('prev').classList.toggle('hide', item.kind !== 'live')
  $('next').classList.toggle('hide', item.kind !== 'live')

  const video = $('v')
  const url = item.url

  const show = (text) => {
    $('pmsg').textContent = text
    $('pmsg').classList.remove('hide')
  }

  if (url.includes('.m3u8') && window.Hls && Hls.isSupported()) {
    // Tuned for live rather than for a file: a small buffer starts the picture
    // sooner, and a stalled segment is retried instead of ending the stream.
    engine = new Hls({
      lowLatencyMode: false,
      backBufferLength: 30,
      maxBufferLength: 20,
      manifestLoadingMaxRetry: 4,
      levelLoadingMaxRetry: 4,
      fragLoadingMaxRetry: 6,
    })
    engine.on(Hls.Events.ERROR, (_e, data) => {
      if (!data.fatal) return
      if (data.type === Hls.ErrorTypes.NETWORK_ERROR) engine.startLoad()
      else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) engine.recoverMediaError()
      else show('تعذّر تشغيل هذه القناة. جرّب قناة أخرى.')
    })
    engine.loadSource(url)
    engine.attachMedia(video)
    video.play().catch(() => {})
    return
  }

  if (url.includes('.ts') && window.mpegts && mpegts.isSupported()) {
    engine = mpegts.createPlayer({ type: 'mpegts', isLive: true, url })
    engine.attachMediaElement(video)
    engine.load()
    engine.play().catch(() => {})
    return
  }

  // A film or an episode: the container decides. Chromium plays MP4 and will
  // not play MKV, so say which one it is rather than showing a black screen.
  video.src = url
  video.play().catch(() => {})
  video.onerror = () => {
    show(/\.mkv$/i.test(url)
      ? 'هذا الملف بصيغة MKV ولا يدعمها ويندوز داخل التطبيق.\nجرّب نسخة أخرى من نفس العنوان.'
      : 'تعذّر تشغيل هذا العنوان.')
  }
}

function step (by) {
  if (!playing || playing.kind !== 'live' || !liveLine.length) return
  const at = liveLine.findIndex((c) => c.id === playing.id)
  if (at < 0) return
  play(liveLine[(at + by + liveLine.length) % liveLine.length])
}

$('prev').onclick = () => step(-1)
$('next').onclick = () => step(1)
$('close').onclick = () => { stop(); $('player').classList.add('hide'); window.mx.fullscreen(false) }
$('fs').onclick = () => {
  const on = !document.fullscreenElement
  window.mx.fullscreen(on)
  if (on) document.documentElement.requestFullscreen().catch(() => {})
  else document.exitFullscreen().catch(() => {})
}
$('v').ondblclick = () => $('fs').click()

// ---- the keyboard, doing what a remote does -------------------------------

document.addEventListener('keydown', (e) => {
  const inField = ['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName)
  const watching = !$('player').classList.contains('hide')

  if (e.key === 'Escape') {
    if (watching) return $('close').click()
    if (!$('detail').classList.contains('hide')) return $('detail').classList.add('hide')
  }
  if (!watching || inField) return

  if (e.key >= '0' && e.key <= '9') {
    typedDigits = (typedDigits + e.key).slice(-4)
    $('typed').textContent = typedDigits
    $('typed').classList.remove('hide')
    clearTimeout(typingTimer)
    typingTimer = setTimeout(() => {
      const wanted = parseInt(typedDigits, 10)
      const match = liveLine.find((c) => c.number === wanted)
      typedDigits = ''
      $('typed').classList.add('hide')
      if (match) play(match)
    }, 1200)
    return
  }
  if (e.key === 'ArrowUp') { e.preventDefault(); step(-1) }
  if (e.key === 'ArrowDown') { e.preventDefault(); step(1) }
  if (e.key === ' ') { e.preventDefault(); $('v').paused ? $('v').play() : $('v').pause() }
  if (e.key.toLowerCase() === 'f') $('fs').click()
})

// ---------------------------------------------------------------- start

;(async () => {
  PORTAL = await window.mx.portal()
  const saved = await window.mx.loadAccount()
  if (saved && saved.username && saved.password) {
    account = saved
    $('u').value = saved.username
    $('p').value = saved.password
    enter()
  } else {
    $('u').focus()
  }
})()
