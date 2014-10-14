var config = {};
config.streamingPort = 8088;

var data = {};
data.mediaSocket = null;

var streamer = {};
streamer.onOpen = function() {

};

streamer.onMessage = function(evt) {
    var msg = evt.data;
};

streamer.onClose = function() {

}

// like main function in C
$(document).ready(function() {
    var myHost = window.location.hostname;
    var wsURL = "ws://" + window.location.hostname + ":" + config.streamingPort;
    data.mediaSocket = new WebSocket(wsURL);

    data.mediaSocket.onopen = streamer.onOpen;
    data.mediaSocket.onmessage = streamer.onMessage;
    data.mediaSocket.onclose = streamer.onClose;
});
