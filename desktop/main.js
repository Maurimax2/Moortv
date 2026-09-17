// MAURIMAX for Windows — the shell.
//
// Every request to the panel is made here rather than in the page. The page is
// a browser: it would be bound by CORS, the panel sends no CORS headers at all,
// and the customer's credentials would sit in a document that loads remote
// artwork. Here they stay in the process that already has them.

const { app, BrowserWindow, ipcMain, shell, Menu } = require('electron')
const path = require('path')
const fs = require('fs')
const http = require('http')
const https = require('https')

// Baked in, exactly as it is in the phone and television builds: the customer
// enters a username and a password and nothing else.
const PORTAL = 'http://hlaamart.site'

let window = null

function createWindow () {
  window = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 960,
    minHeight: 600,
    backgroundColor: '#08080A',
    show: false,
    autoHideMenuBar: true,
    title: 'MAURIMAX',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      // Artwork comes off the panel over plain HTTP, like everything else it
      // serves, so the page has to be allowed to load it.
      webSecurity: false,
    },
  })

  Menu.setApplicationMenu(null)
  window.loadFile(path.join(__dirname, 'renderer', 'index.html'))
  window.once('ready-to-show', () => window.show())

  // A stream is not a page. Anything asking for a new window opens in the
  // customer's browser instead of a second copy of the app.
  window.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })
}

app.whenReady().then(createWindow)
app.on('window-all-closed', () => app.quit())
app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow() })

// ---- the panel ------------------------------------------------------------

/**
 * One call to player_api.php.
 *
 * Follows redirects by hand and gives up after a while, because a panel that
 * has stopped answering should show an error rather than a spinner forever.
 */
function ask (url, redirects = 0) {
  return new Promise((resolve, reject) => {
    if (redirects > 4) return reject(new Error('too many redirects'))
    const client = url.startsWith('https') ? https : http
    const request = client.get(url, {
      timeout: 45000,
      headers: { 'User-Agent': 'MAURIMAX/1.0 (Windows)', 'Accept': 'application/json,*/*' },
    }, (response) => {
      const status = response.statusCode || 0
      if (status >= 300 && status < 400 && response.headers.location) {
        response.resume()
        return resolve(ask(new URL(response.headers.location, url).toString(), redirects + 1))
      }
      if (status !== 200) {
        response.resume()
        return reject(new Error('HTTP ' + status))
      }
      let body = ''
      response.setEncoding('utf8')
      response.on('data', (chunk) => { body += chunk })
      response.on('end', () => {
        try {
          resolve(JSON.parse(body))
        } catch (error) {
          // A panel behind a proxy answers with an HTML page rather than JSON,
          // and "unexpected token <" tells a customer nothing.
          reject(new Error('the server did not answer with a catalogue'))
        }
      })
    })
    request.on('timeout', () => { request.destroy(new Error('the server took too long')) })
    request.on('error', reject)
  })
}

ipcMain.handle('panel', async (_event, { action, username, password, params }) => {
  const query = new URLSearchParams({ username, password, ...(params || {}) })
  if (action) query.set('action', action)
  try {
    return { ok: true, data: await ask(`${PORTAL}/player_api.php?${query.toString()}`) }
  } catch (error) {
    return { ok: false, error: String(error.message || error) }
  }
})

ipcMain.handle('portal', () => PORTAL)

// ---- what this machine remembers -----------------------------------------

const storeFile = () => path.join(app.getPath('userData'), 'account.json')

ipcMain.handle('load-account', () => {
  try {
    return JSON.parse(fs.readFileSync(storeFile(), 'utf8'))
  } catch {
    return null
  }
})

ipcMain.handle('save-account', (_event, account) => {
  try {
    fs.writeFileSync(storeFile(), JSON.stringify(account))
    return true
  } catch {
    return false
  }
})

ipcMain.handle('forget-account', () => {
  try { fs.unlinkSync(storeFile()) } catch {}
  return true
})

ipcMain.handle('fullscreen', (_event, on) => {
  if (window) window.setFullScreen(Boolean(on))
  return true
})
