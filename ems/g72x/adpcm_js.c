#include <stdio.h>
#include "g72x.h"

static g726_state g726State;
static int initFlag = 0;

int adpcmDecode(unsigned char *inBuffer, int length, unsigned char *pcmData) {
    if ( initFlag == 0) {
        initFlag = 1;
        g726_init_state(&g726State); 
    }

    int j = 0;
    for(int i = 0; i < length; i++) {
        unsigned char code;
        unsigned short pcm;

        code = inBuffer[i] & 0x0F;
        pcm = g726_32_decoder(code, AUDIO_ENCODING_LINEAR, &g726State);  
        ((unsigned short *)pcmData)[j] = pcm;
        j++;

        code = inBuffer[i] & 0xF0;
        code = code >> 4;
        pcm = g726_32_decoder(code, AUDIO_ENCODING_LINEAR, &g726State);  
        ((unsigned short *)pcmData)[j] = pcm;
        j++;
    }
    return j;

}

