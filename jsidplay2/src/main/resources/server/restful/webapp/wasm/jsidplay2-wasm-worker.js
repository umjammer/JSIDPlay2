importScripts("jsidplay2.wasm-runtime.js");

function allocateTeaVMbyteArray(array) {
  let byteArrayPtr = instance.exports.teavm_allocateByteArray(array.length);
  let byteArrayData = instance.exports.teavm_byteArrayData(byteArrayPtr);
  new Uint8Array(instance.exports.memory.buffer, byteArrayData, array.length).set(array);
  return byteArrayPtr;
}

function allocateTeaVMstring(str) {
  let stringPtr = instance.exports.teavm_allocateString(str.length);
  let objectArrayData = instance.exports.teavm_objectArrayData(instance.exports.teavm_stringData(stringPtr));
  let arrayView = new Uint16Array(instance.exports.memory.buffer, objectArrayData, str.length);
  for (let i = 0; i < arrayView.length; ++i) {
    arrayView[i] = str.charCodeAt(i);
  }
  return stringPtr;
}

// Handle incoming messages
self.addEventListener(
  "message",
  function (event) {
    var { eventType, eventData, eventId } = event.data;

    if (eventType === "CLOCK") {
      instance.exports.clock();

      self.postMessage({
        eventType: "CLOCKED",
      });
    } else if (eventType === "OPEN") {
      instance.exports.open(
        eventData.contents ? allocateTeaVMbyteArray(eventData.contents) : undefined,
        eventData.tuneName ? allocateTeaVMstring(eventData.tuneName) : undefined,
        eventData.startSong,
        eventData.nthFrame,
        eventData.sidWrites
      );

      self.postMessage({
        eventType: "OPENED",
      });
    } else if (eventType === "INITIALISE") {
      TeaVM.wasm
        .load("/static/wasm/jsidplay2.wasm", {
          installImports(o, controller) {
            o.audiosection = {
              getBufferSize: () => eventData.bufferSize,
              getAudioBufferSize: () => eventData.audioBufferSize,
              getSamplingRate: () => eventData.samplingRate,
              getSamplingMethodResample: () => eventData.samplingMethodResample,
              getReverbBypass: () => eventData.reverbBypass,
            };
            o.emulationsection = {
              getDefaultClockSpeed: () => eventData.defaultClockSpeed,
              getDefaultSidModel8580: () => eventData.defaultSidModel,
            };
            o.audiodriver = {
              processSamples: (leftChannelPtr, rightChannelPtr, length) =>
                self.postMessage({
                  eventType: "SAMPLES",
                  eventData: {
                    left: new Float32Array(
                      instance.exports.memory.buffer,
                      instance.exports.teavm_floatArrayData(leftChannelPtr),
                      length
                    ),
                    right: new Float32Array(
                      instance.exports.memory.buffer,
                      instance.exports.teavm_floatArrayData(rightChannelPtr),
                      length
                    ),
                  },
                }),
              processPixels: (pixelsPtr, length) =>
                self.postMessage({
                  eventType: "FRAME",
                  eventData: {
                    image: new Uint8Array(
                      instance.exports.memory.buffer,
                      instance.exports.teavm_intArrayData(pixelsPtr),
                      length
                    ).slice(),
                  },
                }),
              processSidWrite: (time, relTime, addr, value) =>
                self.postMessage({
                  eventType: "SID_WRITE",
                  eventData: {
                    time: time,
                    relTime: relTime,
                    addr: addr,
                    value: value,
                  },
                }),
            };
          },
        })
        .then((teavm) => {
          instance = teavm.instance;

          self.postMessage({
            eventType: "INITIALISED",
          });
        });
    }
  },
  false
);
