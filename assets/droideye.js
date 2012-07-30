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


//////////////////////////////////////////////
// Global function define
//////////////////////////////////////////////
var onStausDone = function (ret) {
    if (ret == "idle") {
        $.ajax({
            url: basicURL + "cgi/query",
            cache: false,
            error: onHttpError,
            success: onQueryDone
        });
    } else {
        $("#debug_msg").html("其他人正在使用，请刷新重试！" + ret);   
    }
}
var onQueryDone = function (ret) {
    $("#btn_play").removeClass('ui-disabled');
    $("#btn_play").bind("click", playClick);
    
    $("#resolution-choice").empty();
    var resList = ret.split("|");
    for(var i = 0; i < resList.length; i++) {
        var res = resList[i].split("x");
        var newRes = new CameraSize();
        newRes.width = res[0];
        newRes.height = res[1];    
        supportedSize.push(newRes);
        var newOption = "<option value='" + i + "'>" + resList[i] + "</option>";
        $("#resolution-choice").append(newOption);
    }
    $("#resolution-choice").selectmenu('refresh');

    $("#debug_msg").html("连接成功");
}

var onPlayDone = function (ret) {
    if ( ret == "BUSY") {
        $("#debug_msg").html("连接视频错误，请刷新重试！");   
        $("#btn_play").addClass('ui-disabled');               
    } else {
        $("#debug_msg").html("正在播放...");   
    }
}

var onHttpError = function () {
    $("#debug_msg").html("连接视频错误，请刷新重试！");   
    $("#btn_play").addClass('ui-disabled');        
}

var playClick = function () {
    var resIndex = $("#resolution-choice").val();
    var str = "wid=" + supportedSize[resIndex].width;
    str = str + "&hei=" + supportedSize[resIndex].height;
    $.ajax({    
    url: basicURL + "cgi/start",
        data: str, 
        cache: false,
        error: onHttpError,
        success: onPlayDone
    });
};

$("#page_main").live("pageinit", function() {
    basicURL = $(location).attr('href');
        
    var screenHeight = $(window).height();
    var screenWidth = $(window).width();
    planeHeight = Math.round( screenHeight * 0.5);
    planeWidth = Math.round( screenWidth * 0.80);

    $("#video_plane").width(planeWidth);
    $("#video_plane").height(planeHeight);

    $("#btn_play").addClass('ui-disabled');
    
    $.ajax({
        url: basicURL + "cgi/status",
        cache: false,
        error: onHttpError,
        success: onStausDone
    });

});


