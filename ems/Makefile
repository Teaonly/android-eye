INTERFACES = "['_initRaptor', \
               '_releaseRaptor', \
               '_pushSymbol', \
               '_doDecode', \
               '_getSymbol',\
               '_buildPayload',\
               '_adpcmDecode']"

RAPTOR_SOURCES = raptor_js.cpp \
          raptor.cpp \
          numberdb.cpp \
          rprandom.cpp 

ADPCM_SOURCES = adpcm_js.c\
                g711.c \
                g726_32.c \
                g72x.c

js:
	emcc -O2 --memory-init-file 0 -o native_module.js $(RAPTOR_SOURCES) $(ADPCM_SOURCES) -s EXPORTED_FUNCTIONS=$(INTERFACES)

.PHONY: clean all
all: js
clean:
	rm -f raptor_module.js*
