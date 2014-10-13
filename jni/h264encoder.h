#ifndef _H264ENCODER_H_
#define _H264ENCODER_H_

#include <stdint.h>
#include <inttypes.h>

extern "C" {
#include <x264/x264.h>
}

class H264Encoder {
public:    
    H264Encoder(const int wid, const int hei);
    ~H264Encoder();

    int doEncode(const unsigned char* yuv, unsigned char* outBuffer, const int flag);

private:
    void init_(const int wid, const int hei);
    
    x264_param_t x264_opt_;;
    x264_t *x264_hdl_;
    x264_picture_t x264_picin_;
    x264_picture_t x264_picout_;
};

#endif
