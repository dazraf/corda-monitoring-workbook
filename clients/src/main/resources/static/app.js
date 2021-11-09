"use strict";

// Define your client-side logic here.
function getNetwork() {
  const url = "/api/network"
  log(`GET ${url}`)
  fetch(url)
    .then(response => response.json())
    .then(data => log(JSON.stringify(data)))
}

function getMyInfo() {
  const url = "/api/my-info"
  log(`GET ${url}`)
  fetch('/api/my-info')
    .then(response => response.json())
    .then(data => log(JSON.stringify(data)))
}

function runFlow() {
  const url = "/api/flow"
  log(`POST ${url}`)
  fetch(url, {
    method: "POST",
    headers: {
      recipient: "O=PartyB, L=New York, C=US",
      message: `message: ${new Date().toISOString()}`
    }
  }).then(response => response.text())
    .then(data => {
      log(data)
    })
}

let queryWebSocket = null;

function toggleQuery(e) {
  const url = `ws://${document.location.host}/api/query`
  if (e.checked) {
    log(`Connecting to websocket on ${url} ...`)
    queryWebSocket = new WebSocket(url)
    queryWebSocket.onmessage = event => {
      log(`⭐ ${event.data}`)
    }
  } else if (queryWebSocket) {
    log("Closing websocket ...")
    queryWebSocket.close()
    queryWebSocket = null;
  }
  log("✅")
}

function log(msg) {
  const output = document.getElementById("output")
  output.innerText += `\n${new Date().toISOString()} - ${msg}\n`
  output.scroll({ top: output.scrollHeight, behavior: 'smooth' });
}


function clearOutput() {
  const output = document.getElementById("output")
  output.innerText = ""
}