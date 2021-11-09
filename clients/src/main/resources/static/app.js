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
    method: "POST"
  }).then(response => {
    console.log(response);
    return response.text()
  })
    .then(data => {
      console.log(data)
      log(data)
    })
}

function log(msg) {
  const output = document.getElementById("output")
  output.innerText += `\n${new Date().toISOString()} - ${msg}\n`
  output.scroll({ top: output.scrollHeight, behavior: 'smooth' });
}
