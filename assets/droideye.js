var planeWidth = 0;
var planeHeight = 0;
var basicURL = "";

function CameraSize () {
    this.width = 0;
    this.height = 0;
}
var supportedSize = new Array();

// =========================================================
var onPlayClick = function () {
        
};

var onQueryDone = function (ret) {
    $("#btn_play").removeClass('ui-disabled');
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
var onQueryError = function () {
    $("#debug_msg").html("连接视频错误，请刷新重试！");   
}

$("#page_main").live("pageinit", function() {
    basicURL = $(location).attr('href');
        
    var screenHeight = $(window).height();
    var screenWidth = $(window).width();
    planeHeight = Math.round( screenHeight * 0.5);
    planeWidth = Math.round( screenWidth * 0.80);

    $("#video_plane").width(planeWidth);
    $("#video_plane").height(planeHeight);

    $("#btn_play").addClass('ui-disabled');        
    $("#btn_play").click(onPlayClick);
   
    $.ajax({
        url: basicURL + "cgi/query",
        cache: false,
        error: onQueryError,
        success: onQueryDone
    });
});


