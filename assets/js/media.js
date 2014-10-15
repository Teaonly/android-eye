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
        var block = {'type': 0, 'length': 0, 'timeStamp': -1};

        if ( payload[i] === 0x19 && payload[i+1] === 0x79) {

            block.type = 1;
            block.timeStamp = (payload[i+3] << 8) + payload[i+2];
            block.length = (payload[i+7] << 24) + (payload[i+6] << 16) + (payload[i+5] << 8) + payload[i+4];
        } else if ( payload[i] === 0x19 && payload[i+1] === 0x82) {

            block.type = 2;
            block.timeStamp = (payload[i+3] << 8) + payload[i+2];
            block.length = (payload[i+7] << 24) + (payload[i+6] << 16) + (payload[i+5] << 8) + payload[i+4];
        }

        return block;

    }.bind(this);

    this._decodeBuffer = function(arrayBuffer) {
        var payload = new Uint8Array(arrayBuffer);

        var i = 0;
        while(1) {
            if ( payload.length - i <= 8) {
                // drop left data, because it is not a packet.
                break;
            }

            var block = this._findNext(payload, i);
            if ( block.type === 1 ) {
                block.payload = payload.subarray(i+8, i+8+block.length);
                this.nalBlocks.push(block);
                i = i + 8 + block.length;

            } else if ( block.type === 2) {
                block.payload = payload.subarray(i+8, i+8+block.length);
                this.pcmBlocks.push(block);
                i = i + 8 + block.length;

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
