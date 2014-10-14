function TeaMedia(blob, parseDone) {
    // vars
    this.pcmBlocks = null;
    this.nalBlocks = null;

    // call backs
    this.onParseDone = parseDone;

    // public interfaces
    this.getPicturePayload = function() {

    }.bind(this);

    this.getAudioPayload = function() {

    }.bind(this);

    // internal functions
    this._findNext = function(payload, i) {
        var info = {'type': 0, 'length': 0};

        if ( payload[i] === 0x19
             && payload[i+1] === 0x79
             && payload[i+2] === 0x10
             && payload[i+3] === 0x10) {

            info.type = 1;
            info.length = (payload[i+7] << 24) + (payload[i+6] << 16) + (payload[i+5] << 8) + payload[i+4];

        } else if ( payload[i] === 0x19
                    && payload[i+1] === 0x82
                    && payload[i+2] === 0x08
                    && payload[i+3] === 0x25) {
            info.type = 2;
            info.length = (payload[i+7] << 24) + (payload[i+6] << 16) + (payload[i+5] << 8) + payload[i+4];
        }

        return info;

    }.bind(this);

    this._decodeBuffer = function(arrayBuffer) {
        var payload = new Uint8Array(arrayBuffer);

        var i = 0;
        while(1) {
            if ( payload.length - i <= 8) {
                // drop left data, because it is not a packet.
                break;
            }

            info = this._findNext(payload, i);
            if ( info.type === 1) {
                this.nalBlocks.push ( payload.subarray(i+8, i+8+info.length) );
                i = i + 8 + info.length;
            } else if ( info.type === 2) {
                this.pcmBlocks.push ( payload.subarray(i+8, i+8+info.length) );
                i = i + 8 + info.length;
            } else {
                break;
            }
        }

        this.onParseDone();

    }.bind(this);

    this._constructor = function() {
        this.pcmBlocks = [];
        this.nalBlocks = [];

        var fileReader = new FileReader();
        var that = this;
        fileReader.onload = function() {
            that._decodeBuffer( this.result);
        };
        fileReader.readAsArrayBuffer(blob);

    }.bind(this);




    // init
    this._constructor();
};
