#!/usr/bin/python

import os, sys, re, json, shutil
from subprocess import Popen, PIPE, STDOUT

exec(open(os.path.expanduser('~/.emscripten'), 'r').read())

sys.path.append(EMSCRIPTEN_ROOT)
import tools.shared as emscripten

emcc_args = [
  '-m32',
  '-O2',
  '--llvm-opts', '2',
  '-s', 'CORRECT_SIGNS=1',
  '-s', 'CORRECT_OVERFLOWS=1',
  '-s', 'TOTAL_MEMORY=' + str(64*1024*1024),
  '-s', 'FAST_MEMORY=' + str(16*1024*1024),
  '-s', 'INVOKE_RUN=0',
  '-s', 'RELOOP=1',
  '-s', '''EXPORTED_FUNCTIONS=["HEAP8", "HEAP16", "HEAP32", "_get_h264bsdClip", "_main", "_broadwayGetMajorVersion", "_broadwayGetMinorVersion", "_broadwayInit", "_broadwayExit", "_broadwayCreateStream", "_broadwaySetStreamLength", "_broadwayPlayStream", "_broadwayOnHeadersDecoded", "_broadwayOnPictureDecoded", "_initRaptor", "_releaseRaptor","_pushSymbol", "_doDecode", "_getSymbol", "_buildPayload", "_adpcmDecode"]''',
  '--closure', '1',
  '--js-library', 'library.js',
  # '--js-transform', 'python appender.py'
  '--memory-init-file', '0'
]
  
JS_DIR = "js"
if not os.path.exists(JS_DIR):
  os.makedirs(JS_DIR)
  
OBJ_DIR = "obj"
if not os.path.exists(OBJ_DIR):
  os.makedirs(OBJ_DIR)
  os.makedirs(OBJ_DIR + "/avc")
  os.makedirs(OBJ_DIR + "/raptor")
  os.makedirs(OBJ_DIR + "/g72x")

print 'build'

source_files = [
  'avc/h264bsd_transform.c',
  'avc/h264bsd_util.c',
  'avc/h264bsd_byte_stream.c',
  'avc/h264bsd_seq_param_set.c',
  'avc/h264bsd_pic_param_set.c',
  'avc/h264bsd_slice_header.c',
  'avc/h264bsd_slice_data.c',
  'avc/h264bsd_macroblock_layer.c',
  'avc/h264bsd_stream.c',
  'avc/h264bsd_vlc.c',
  'avc/h264bsd_cavlc.c',
  'avc/h264bsd_nal_unit.c',
  'avc/h264bsd_neighbour.c',
  'avc/h264bsd_storage.c',
  'avc/h264bsd_slice_group_map.c',
  'avc/h264bsd_intra_prediction.c',
  'avc/h264bsd_inter_prediction.c',
  'avc/h264bsd_reconstruct.c',
  'avc/h264bsd_dpb.c',
  'avc/h264bsd_image.c',
  'avc/h264bsd_deblocking.c',
  'avc/h264bsd_conceal.c',
  'avc/h264bsd_vui.c',
  'avc/h264bsd_pic_order_cnt.c',
  'avc/h264bsd_decoder.c',
  'avc/H264SwDecApi.c',
  'avc/Decoder.c',
  'g72x/adpcm_js.c',
  'g72x/g711.c',
  'g72x/g726_32.c',
  'g72x/g72x.c']

object_files = []  
for file in source_files:
  
  target = file.replace('.c', '.o')
  if file.endswith('.cpp') :
    target = file.replace('.cpp', '.o')
  
  target = os.path.join('obj', target)
  print 'emcc %s -> %s' % (file, target)
  emscripten.Building.emcc(file, emcc_args + ['-Iavc-inc'], target)
  object_files = object_files + [ target]
  
print 'link -> %s' % 'avc.bc'
emscripten.Building.link(object_files, 'avc.bc')

print 'emcc %s -> %s' % ('avc.bc', os.path.join(JS_DIR, 'module.js'))
emscripten.Building.emcc('avc.bc', emcc_args, os.path.join(JS_DIR, 'module.js'))
