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
    this._renderInterval = 5;

    this._playedTime = -1;
    this._lastTime = -1;
    this._videoTime = -1;
    this._lastVideoTime = -1;

    this._avc = null;
    this._videoBufferList = null;

    // call backs
    this.onStreamStarvation = null;

    // public functions
    this._constructor = function() {
        this._canvas = canvas;
        this._canvasContext = this._canvas.getContext("2d");
        this._canvasContext.font = "12px sans-serif";
        this._canvasContext.textAlign = 'right';
        this._canvasContext.fillStyle = 'rgba(0,255,0, 0.9)';
        this._rgba = this._canvasContext.getImageData(0, 0, this._canvas.width, this._canvas.height);
        this._videoBufferList = new Array();

        this._avc = new Avc();

        setInterval(this._playVideo, this._renderInterval);
        //requestAnimFrame(this._playVideo);
    }.bind(this);

    this.playMedia = function(media) {
        if ( this._videoBufferList.length > 150) {
            return;
        }   
        
        var picture = null;
        this._avc.onPictureDecoded = function(buffer, wid, hei) {
            var yuv = new Uint8Array(buffer.length);
            yuv.set(buffer, 0, buffer.length);
            picture = {'yuv':yuv, 'wid':wid, 'hei':hei};
        }.bind(this);

        /*
        var doDecode = function(first) {
            if ( media.nalBlocks.length > 0) {
                picture = null;
                this._avc.decode(media.nalBlocks[0].payload);
                if( picture != null) {
                    picture.timeStamp = media.nalBlocks[0].timeStamp;
                    picture.flag = first;
                    this._videoBufferList.push(picture);
                }
                media.nalBlocks.shift();
                setTimeout(doDecode(false), 2);
            } else {
                delete media;

            }
        }.bind(this);
        doDecode(true);
        */

        for (i = 0; i < media.nalBlocks.length; i++) {
            picture = null;
            this._avc.decode(media.nalBlocks[i].payload);
            if( picture != null) {
                picture.timeStamp = media.nalBlocks[i].timeStamp;
                if ( i === 0) {
                    console.log(">>>>> " +  picture.timeStamp);
                    picture.flag = true;
                } else {
                    picture.flag = false;
                }

                this._videoBufferList.push(picture);
            }
        }

        delete media;

    }.bind(this);

    // private functions
    this._updateInfo = function(info) {
        this._infoText = info;
        this._canvasContext.fillText(this._infoText, this._canvas.width - 5, 20);
    }.bind(this);

    this._showPicture = function(picture) {
        yuv2ImageData(picture.yuv, this._rgba);
        this._canvasContext.putImageData(this._rgba, 0, 0);
    }.bind(this);

    this._playVideo = function() {
        //requestAnimFrame(this._playVideo);
        if ( this._videoBufferList.length > 0) {
            if ( this._videoBufferList[0].flag === true) {
                this._playedTime = this._videoBufferList[0].timeStamp;
                this._lastVideoTime = this._videoBufferList[0].timeStamp;
                this._videoTime = 0;
            } else {
                this._playedTime += (new Date()).getTime() - this._lastTime;
            }

            //console.log(this._playedTime + " vs " + this._videoTime + this._videoBufferList[0].timeStamp );

            if ( this._playedTime >= this._videoTime + this._videoBufferList[0].timeStamp ) {
                // updated video time
                if (  this._videoBufferList[0].timeStamp < this._lastVideoTime ) {
                    this._videoTime += 65535;
                }
                this._lastVideoTime = this._videoBufferList[0].timeStamp;

                //console.log(">>>>> " +  this._videoBufferList[0].timeStamp)
                this._showPicture(this._videoBufferList[0] );
                delete this._videoBufferList[0];
                this._videoBufferList.shift();
            }
        }
        this._lastTime = (new Date()).getTime();
    }.bind(this);

    this._pushVideoBuffer = function(picture) {
        this._videoBufferList.push(picture);

    }.bind(this);

    // init
    this._constructor();
}
