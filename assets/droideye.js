//////////////////////////////////////////////
// Global variable define
//////////////////////////////////////////////
var planeWidth = 0;
var planeHeight = 0;
var basicURL = "";
var inStreaming = false;

function CameraSize () {
    this.width = 0;
    this.height = 0;
}
var supportedSize = new Array();
var currentSize = new CameraSize();

//////////////////////////////////////////////
// Global function define
//////////////////////////////////////////////
var onImageLoadOK = function() {
    var wid = 0;
    var hei = 0;
    if ( planeHeight * currentSize.width / currentSize.height > planeWidth) {
        wid = planeWidth;
        hei = math.round(planeWidth * currentSize.height / currentSize.width); 
    } else {
        hei = planeHeight;
        wid = planeHeight * currentSize.width / currentSize.height;  
    }
    $("#live_image").width(wid);
    $("#live_image").height(hei);
    if ( inStreaming == true)
        setTimeout(refreshLive, 50);    
};
var onImageLoadError = function() {
    if ( inStreaming == true)
        setTimeout(refreshLive, 50);    
};

var onQueryDone = function (ret) {
    $("#btn_play").button('enable');
    
    $("#resolution-choice").empty();
    var resList = ret.split("|");
    currentSize.width = resList[0].split("x")[0];
    currentSize.height = resList[0].split("x")[1];
    var currentSelect = -1;
    for(var i = 1; i < resList.length; i++) {
        var res = resList[i].split("x");
        var newRes = new CameraSize();
        newRes.width = res[0];
        newRes.height = res[1];    
        supportedSize.push(newRes);
        if ( newRes.width == currentSize.width  && newRes.height == currentSize.height) {
            currentSelect = i;
            var newOption = "<option value='" + (i-1) + "'>" + resList[i] + "</option>";
            $("#resolution-choice").append(newOption);
        }
    }
    for(var i = 1; i < resList.length; i++) {
        if ( currentSelect != i) {
            var newOption = "<option value='" + (i-1) + "'>" + resList[i] + "</option>";
            $("#resolution-choice").append(newOption);
        }
    }
    $("#resolution-choice").selectmenu('refresh');
    $("#resolution-choice").bind("change", doChangeRes);  

    $("#debug_msg").html("连接成功");
};

var onHttpError = function () {
    $("#debug_msg").html("连接视频错误，请刷新重试！");   
    $("#btn_play").button('disable'); 
};

var refreshLive = function() {
    $("#live_image").load(onImageLoadOK).error(onImageLoadError).attr("src", basicURL + "stream/live.jpg"); 
};

var playClick = function () {
    if  ( inStreaming == false) {
        inStreaming = true;
        $("#btn_play").val("停止播放").button("refresh");
        refreshLive();
    } else {
        $("#btn_play").val("开始播放");
    }
};

var doChangeRes = function () {
};

$("#page_main").live("pageinit", function() {
    basicURL = $(location).attr('href');
        
    var screenHeight = $(window).height();
    var screenWidth = $(window).width();
    planeHeight = Math.round( screenHeight * 0.5);
    planeWidth = Math.round( screenWidth * 0.80);

    $("#video_plane").height(planeHeight);
    $("#video_plane").width(planeWidth);

    $("#btn_play").button('disable');
    $("#btn_play").bind("click", playClick);

    $.ajax({
        url: basicURL + "cgi/query",
        cache: false,
        error: onHttpError,
        success: onQueryDone
    });

});

