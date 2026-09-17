// The only door between the page and the machine. Nothing else is exposed:
// the page cannot read a file, open a socket, or see the customer's disk.

const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('mx', {
  panel: (action, username, password, params) =>
    ipcRenderer.invoke('panel', { action, username, password, params }),
  portal: () => ipcRenderer.invoke('portal'),
  loadAccount: () => ipcRenderer.invoke('load-account'),
  saveAccount: (account) => ipcRenderer.invoke('save-account', account),
  forgetAccount: () => ipcRenderer.invoke('forget-account'),
  fullscreen: (on) => ipcRenderer.invoke('fullscreen', on),
})
