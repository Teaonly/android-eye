function AdpcmDecoder() {
    this._inMemory = Module._malloc(1024*128);
    this._outMemory = Module._malloc(1024*128);

    this.doDecode = function(inBuffer) {
        Module.HEAPU8.set(inBuffer, this._inMemory);
        var ret = Module._adpcmDecode(this._inMemory, inBuffer.length, this._outMemory);
        return Module.HEAPU8.subarray(this._outMemory, this._outMemory + ret * 2);
    }.bind(this);

    this.release = function() {
        Module._free(this._inMemory);
        Module._free(this._outMemory);
    }.bind(this);
};
