"use strict";

// Define your client-side logic here.
function getNetwork() {
    const url = "/api/network";
    log(`ℹ️ ️GET ${url}`);
    fetch(url)
        .then(response => response.json())
        .then(data => log(`ℹ️ ${JSON.stringify(data)}`));
}

function getMyInfo() {
    const url = "/api/my-info";
    log(`ℹ️ GET ${url}`);
    fetch('/api/my-info')
        .then(response => response.json())
        .then(data => log(`ℹ️ ${JSON.stringify(data)}`));
}

let nextFlowId = 1

function runFlow() {
    const url = "/api/flow"
    const flowId = nextFlowId++;
    log(`➡️ ️Flow ${flowId}: Invoking ${url}`);
    fetch(url, {
        method: "POST",
        headers: {
            clientRequestId: flowId,
            recipient: "O=PartyB, L=New York, C=US",
            message: `Flow invocation ${flowId} at ${new Date().toISOString()}`
        }
    })
        .then(response => response.text())
        .then(data => {
            log(`➡️ Flow ${flowId}
    received: ${data}`)
        })
        .catch(err => {
            log(`➡️ Flow ${flowId} failed with: ${err}`)
        })
}

class TrackingQuery {
    static url = `ws://${document.location.host}/api/query`;
    ws = null;

    toggle(e) {
        if (e.checked) {
            this._connect();
        } else {
            this._disconnect();
        }
    }

    _connect() {
        const checkBox = document.getElementById("queryCheck")
        checkBox.disabled = true

        log(`Opening query websocket on ${TrackingQuery.url} ...`);
        this.ws = new WebSocket(TrackingQuery.url);
        this.ws.onopen = () => {
            checkBox.disabled = false;
            checkBox.checked = true;
            log("Query websocket open ✅");
        }
        this.ws.onclose = () => {
            checkBox.disabled = false;
            checkBox.checked = false;
            log("Query websocket closed.")
        }
        this.ws.onmessage = event => {
            log(`✨ ${event.data}`);
        }
    }

    _disconnect() {
        log("Closing query websocket ...")
        if (this.ws != null) {
            // noinspection JSUnresolvedFunction
            this.ws.close();
            this.ws = null;
        }
    }
}

const trackingQuery = new TrackingQuery();

function log(msg) {
    const output = document.getElementById("output")
    output.innerText += `\n${new Date().toISOString()} - ${msg}\n`;
    output.scroll({top: output.scrollHeight, behavior: 'smooth'});
}


function clearOutput() {
    const output = document.getElementById("output");
    output.innerText = "";
}

function setTitle() {
    const url = "/api/name";
    fetch(url)
        .then(response => response.text())
        .then(name => {
            document.title = name;
            console.log(name);
        })
}

setTitle()