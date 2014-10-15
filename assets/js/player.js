var requestAnimFrame = (function(){
    return window.requestAnimationFrame ||
        window.webkitRequestAnimationFrame ||
        window.mozRequestAnimationFrame ||
        function( callback ){
            window.setTimeout(callback, 1000 / 60);
        };
})();

function Player (canvas, sampleRate) {
    // vars
    this._canvas = null;
    this._canvasContext = null;
    this._rgba = null;
    this._beginTime = -1;
    this._offsetTime = -1;
    this._renderInterval = 5;

    this._avc = null;
    this._videoBufferList = null;

    // call backs
    this.onStreamStarvation = null;

    // public functions
    this._constructor = function() {
        this._canvas = canvas;
        this._canvasContext = this._canvas.getContext("2d");
        this._rgba = this._canvasContext.getImageData(0, 0, this._canvas.width, this._canvas.height);
        this._videoBufferList = new Array();

        this._avc = new Avc();

        setInterval(this._playVideo, this._renderInterval);
        //requestAnimFrame(this._playVideo);
    }.bind(this);

    this.playMedia = function(media) {

        var picture = null;
        this._avc.onPictureDecoded = function(buffer, wid, hei) {
            var yuv = new Uint8Array(buffer.length);
            yuv.set(buffer, 0, buffer.length);
            picture = {'yuv':yuv, 'wid':wid, 'hei':hei};
        }.bind(this);

        var doDecode = function() {
            if ( media.nalBlocks.length > 0) {
                picture = null;
                this._avc.decode(media.nalBlocks[0].payload);
                if( picture != null) {
                    picture.timeStamp = media.nalBlocks[0].timeStamp;
                    this._videoBufferList.push(picture);
                }
                media.nalBlocks.shift();
                setTimeout(doDecode, 2);
            } else {
                delete media;
            }
        }.bind(this);

        doDecode();

    }.bind(this);

    // private functions
    this._showPicture = function(picture) {
        yuv2ImageData(picture.yuv, this._rgba);
        this._canvasContext.putImageData(this._rgba, 0, 0);
    }.bind(this);

    this._playVideo = function() {
        //requestAnimFrame(this._playVideo);
        if ( this._videoBufferList.length > 0) {

            /*
            if ( this._beginTime === -1) {
                this._beginTime = (new Date()).getMilliseconds();
                this._offsetTime = this._videoBufferList[0].timeStamp;
            }

            var current = ((new Date()).getMilliseconds() - this._beginTime + this._offsetTime) % 65535;
            var diff = current - this._videoBufferList[0].timeStamp;
            var diff = function(a, b) {
                if ( a > 60000 & b < 1000)

            };
            */

            if ( this._beginTime === -1) {
                this._beginTime = this._videoBufferList[0].timeStamp;
            }

            var diff = this._beginTime - this._videoBufferList[0].timeStamp;

            if ( Math.abs(diff) <= this._renderInterval ) {
                this._showPicture(this._videoBufferList[0] );
                delete this._videoBufferList[0];
                this._videoBufferList.shift();
            }
        } else {

        }

        if ( this._beginTime > 0) {
            this._beginTime = (this._beginTime + this._renderInterval) % 65535;
        }
        
    }.bind(this);

    this._pushVideoBuffer = function(picture) {
        this._videoBufferList.push(picture);

    }.bind(this);

    // init
    this._constructor();
}
