var config = {};
config.streamingPort = 8088;

var mediaSocket = null;
var player = null;
var myState = 0;    // -1: error; 0: idle; 1: wainting; 2: palying
   
var streamer = {};
streamer.onOpen = function() {

};
streamer.onMessage = function(evt) {
    if ( myState != 2) {
        myState = 2;
        $("#spanInfo").html("Playing...");
    }
    var blob = evt.data;
    if ( blob.slice !== undefined) {
        var media = new TeaMedia(blob, function() {
            player.playMedia(media);
        }.bind(this) );
    }
};

streamer.onClose = function() {
    alert("Mobile is disconnected!");
    $("#btnPlay").prop('disabled', true);
    $("#spanInfo").html("Please relaod...");
};

var connect = function() {
    var myHost = window.location.hostname;
    var wsURL = "ws://" + window.location.hostname + ":" + config.streamingPort;
    mediaSocket = new WebSocket(wsURL);
    player = new Player(document.getElementById("videoPlayer"), 8000);
    
    mediaSocket.onopen = streamer.onOpen;
    mediaSocket.onmessage = streamer.onMessage;
    mediaSocket.onclose = streamer.onClose;

    $("#spanInfo").html("Connected, waiting for media..");
};

// like main function in C
$(document).ready(function() {
    //$("#btnPlay").prop('disabled', false);

    $("#btnPlay").click( function() {
        if( myState == 0) {
            myState = 1;
            $("#btnPlay").html("Stop")
            $("#spanInfo").html("Connecting...");

            $.ajax({
              url: "/cgi/query",
              type: "get",
              cache: false,
              success: function(ret) {
                console.log(ret);
                var result = JSON.parse(ret);
                if ( result.state === "ok") {
                    document.getElementById("videoPlayer").width = result.width;
                    document.getElementById("videoPlayer").height = result.height;

                    connect();
                } else {
                    alert("Mobile is busy!");
                    location.reload();
                }
              },
              error: function() {
                  alert("Mobile is error");
                  location.reload();
                },
            });
        } else if (myState != -1) {
            location.reload();
        }
    });

});
