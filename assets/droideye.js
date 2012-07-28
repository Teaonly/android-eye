var supportedSize = new Array();
var planeWidth = 0;
var planeHeight = 0;
var basicURL = "";

var onPlayClick = function () {
    
};

var onQueryDone = function (ret) {
    $("#debug_msg").html(ret);   
    $("#btn_play").removeClass('ui-disabled');        
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
    
    $("#debug_msg").html(basicURL);
    $.ajax({
        url: "http://192.168.0.102:8080/cgi/query",
        cache: false,
        error: onQueryError,
        success: onQueryDone
    });
});


