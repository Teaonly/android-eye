var yuv2ImageData = function(yuv, imageData) {
    var width = imageData.width;
    var height = imageData.height;
    var outputData = imageData.data;

    yOffset = 0;
    uOffset = width * height;
    vOffset = width * height + (width*height)/4;
    for (var h=0; h<height; h++) {
        for (var w=0; w<width; w++) {
            ypos = w + h * width + yOffset;

            upos = (w>>1) + (h>>1) * width/2 + uOffset;
            vpos = (w>>1) + (h>>1) * width/2 + vOffset;

            Y = yuv[ypos];
            U = yuv[upos] - 128;
            V = yuv[vpos] - 128;

            R =  (Y + 1.371*V);
            G =  (Y - 0.698*V - 0.336*U);
            B =  (Y + 1.732*U);

            outputData_pos = w*4 + width*h*4;
            outputData[0+outputData_pos] = R;
            outputData[1+outputData_pos] = G;
            outputData[2+outputData_pos] = B;
            outputData[3+outputData_pos] = 255;
        }
    }
};
