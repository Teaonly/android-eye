var config = {};
config.streamingPort = 8088;

var mediaSocket = null;

var player = null;

var streamer = {};
streamer.onOpen = function() {

};

streamer.onMessage = function(evt) {
    var blob = evt.data;
    if ( blob.slice !== undefined) {
        var media = new TeaMedia(blob, function() {
            player.playMedia(media);
        }.bind(this) );
    }
};

streamer.onClose = function() {

};

// like main function in C
$(document).ready(function() {

    var myHost = window.location.hostname;
    var wsURL = "ws://" + window.location.hostname + ":" + config.streamingPort;
    mediaSocket = new WebSocket(wsURL);
    player = new Player(document.getElementById("videoPlayer"), 8000);

    mediaSocket.onopen = streamer.onOpen;
    mediaSocket.onmessage = streamer.onMessage;
    mediaSocket.onclose = streamer.onClose;
});
