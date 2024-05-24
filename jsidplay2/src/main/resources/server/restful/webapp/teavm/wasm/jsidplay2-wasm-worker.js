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
    var { eventType, eventData } = event.data;

    if (eventType === "CLOCK") {
      instance.exports.clock();

      self.postMessage({
        eventType: "CLOCKED",
      });
    } else if (eventType === "IDLE") {
      self.postMessage({
        eventType: "CLOCKED",
      });
    } else if (eventType === "OPEN") {
      instance.exports.open(
        eventData.contents ? allocateTeaVMbyteArray(eventData.contents) : undefined,
        eventData.tuneName ? allocateTeaVMstring(eventData.tuneName) : undefined,
        eventData.startSong,
        eventData.nthFrame,
        eventData.sidWrites,
        eventData.cartContents ? allocateTeaVMbyteArray(eventData.cartContents) : undefined,
        eventData.cartName ? allocateTeaVMstring(eventData.cartName) : undefined
      );

      self.postMessage({
        eventType: "OPENED",
      });
    } else if (eventType === "INSERT_DISK") {
      instance.exports.insertDisk(
        eventData.contents ? allocateTeaVMbyteArray(eventData.contents) : undefined,
        eventData.diskName ? allocateTeaVMstring(eventData.diskName) : undefined
      );

      self.postMessage({
        eventType: "DISK_INSERTED",
      });
    } else if (eventType === "EJECT_DISK") {
      instance.exports.ejectDisk();

      self.postMessage({
        eventType: "DISK_EJECTED",
      });
    } else if (eventType === "INSERT_TAPE") {
      instance.exports.insertTape(
        eventData.contents ? allocateTeaVMbyteArray(eventData.contents) : undefined,
        eventData.tapeName ? allocateTeaVMstring(eventData.tapeName) : undefined
      );

      self.postMessage({
        eventType: "TAPE_INSERTED",
      });
    } else if (eventType === "EJECT_TAPE") {
      instance.exports.ejectTape();

      self.postMessage({
        eventType: "TAPE_EJECTED",
      });
    } else if (eventType === "PRESS_PLAY_ON_TAPE") {
      instance.exports.pressPlayOnTape();

      self.postMessage({
        eventType: "PRESSED_PLAY_ON_TAPE",
      });
    } else if (eventType === "SET_COMMAND") {
      instance.exports.setCommand(eventData.command ? allocateTeaVMstring(eventData.command) : undefined);

      self.postMessage({
        eventType: "COMMAND_SET",
      });
    } else if (eventType === "TYPE_KEY") {
      instance.exports.typeKey(eventData.key ? allocateTeaVMstring(eventData.key) : undefined);

      self.postMessage({
        eventType: "KEY_TYPED",
      });
    } else if (eventType === "INITIALISE") {
      TeaVM.wasm
        .load("jsidplay2.wasm", {
          installImports(o, controller) {
            o.sidplay2section = {
              getPalEmulation: () => eventData.palEmulation,
            };
            o.audiosection = {
              getBufferSize: () => eventData.bufferSize,
              getAudioBufferSize: () => eventData.audioBufferSize,
              getSamplingRate: () => eventData.samplingRate,
              getSamplingMethodResample: () => eventData.samplingMethodResample,
              getReverbBypass: () => eventData.reverbBypass,
            };
            o.emulationsection = {
              getDefaultClockSpeed: () => eventData.defaultClockSpeed,
              getDefaultEmulationReSid: () => eventData.defaultEmulation,
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
              processSidWrite: (relTime, addr, value) =>
                self.postMessage({
                  eventType: "SID_WRITE",
                  eventData: {
                    relTime: relTime,
                    addr: addr,
                    value: value,
                  },
                }),
            };
            o.c1541section = {
              isJiffyDosInstalled: () => eventData.jiffyDosInstalled,
            };
          },
        })
        .then((teavm) => {
          instance = teavm.instance;

          teavm.main();

          self.postMessage({
            eventType: "INITIALISED",
          });
        });
    }
  },
  false
);
