var ws;

function appendLog(text) {
    var log = document.getElementById("log");
    var ts = new Date().toLocaleTimeString();
    log.value += "[" + ts + "] " + text + "\n";
    log.scrollTop = log.scrollHeight;
}

function setStatus(state, message) {
    var dot = document.getElementById("status-dot");
    var text = document.getElementById("status-text");
    if (!dot || !text) return;
    dot.classList.remove("online", "error");
    if (state === "online") dot.classList.add("online");
    if (state === "error") dot.classList.add("error");
    text.textContent = message;
}

function connect() {
    var username = document.getElementById("username").value.trim();
    var wsserver = document.getElementById("wsserver").value.trim();
    if (!wsserver || !username) {
        appendLog("Please provide both server URL and username.");
        setStatus("error", "Connect");
        return;
    }
    var url = wsserver + username;
    ws = new WebSocket(url);

    ws.onmessage = function (event) { // Called when client receives a message from the server
        console.log(event.data);
        appendLog("message from server: " + event.data);
    };

    ws.onopen = function (event) { // called when connection is opened
        appendLog("Connected to " + event.currentTarget.url);
        setStatus("online", "Connected");
    };

    ws.onclose = function () {
        appendLog("Connection closed.");
        setStatus("error", "Connect");
    };

    ws.onerror = function (err) {
        appendLog("WebSocket error.");
        setStatus("error", "Connect");
    };
}

function send() {  // this is how to send messages
    var content = document.getElementById("msg").value;
    if (!content) return;
    if (!ws || ws.readyState !== 1) {
        appendLog("Not connected. Please connect first.");
        return;
    }
    ws.send(content);
    document.getElementById("msg").value = "";
}

// UX nicety: Enter to send
(function () {
    var msg = document.getElementById("msg");
    if (!msg) return;
    msg.addEventListener("keydown", function (e) {
        if (e.key === "Enter") {
            e.preventDefault();
            send();
        }
    });
})();
