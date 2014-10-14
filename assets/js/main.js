var config = {};
config.streamingPort = 8088;

var data = {};
data.mediaSocket = null;

var streamer = {};
streamer.onOpen = function() {

};

streamer.onMessage = function(evt) {
    var blob = evt.data;
    if ( blob.slice !== undefined) {
        media new TeaMedia(blob, function() {
            console.log(media);
        }.bind(this) );
    }
};

streamer.onClose = function() {

};

// like main function in C
$(document).ready(function() {
    var myHost = window.location.hostname;
    var wsURL = "ws://" + window.location.hostname + ":" + config.streamingPort;
    data.mediaSocket = new WebSocket(wsURL);

    data.mediaSocket.onopen = streamer.onOpen;
    data.mediaSocket.onmessage = streamer.onMessage;
    data.mediaSocket.onclose = streamer.onClose;
});
