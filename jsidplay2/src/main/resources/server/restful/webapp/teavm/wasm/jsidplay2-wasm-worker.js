importScripts("jsidplay2.wasm-runtime.js");

function allocateTeaVMbyteArray(array) {
  if (array === null) {
    return undefined;
  }
  let byteArrayPtr = instance.exports.teavm_allocateByteArray(array.length);
  let byteArrayData = instance.exports.teavm_byteArrayData(byteArrayPtr);
  new Uint8Array(instance.exports.memory.buffer, byteArrayData, array.length).set(array);
  return byteArrayPtr;
}

function allocateTeaVMstring(str) {
  if (str === null) {
    return undefined;
  }
  let stringPtr = instance.exports.teavm_allocateString(str.length);
  let objectArrayData = instance.exports.teavm_objectArrayData(instance.exports.teavm_stringData(stringPtr));
  let arrayView = new Uint16Array(instance.exports.memory.buffer, objectArrayData, str.length);
  for (let i = 0; i < arrayView.length; ++i) {
    arrayView[i] = str.charCodeAt(i);
  }
  return stringPtr;
}

// Handle incoming messages
addEventListener(
  "message",
  function (event) {
    var { eventType, eventData } = event.data;

    if (eventType === "CLOCK") {
      instance.exports.clock();

      postMessage({
        eventType: "CLOCKED",
      });
    } else if (eventType === "IDLE") {
      postMessage({
        eventType: "CLOCKED",
      });
    } else if (eventType === "OPEN") {
      instance.exports.open(
        allocateTeaVMbyteArray(eventData.contents ?? null),
        allocateTeaVMstring(eventData.tuneName ?? null),
        eventData.startSong,
        eventData.nthFrame,
        eventData.sidWrites,
        allocateTeaVMbyteArray(eventData.cartContents ?? null),
        allocateTeaVMstring(eventData.cartName ?? null),
        allocateTeaVMstring(eventData.command ?? null)
      );

      postMessage({
        eventType: "OPENED",
      });
    } else if (eventType === "INSERT_DISK") {
      instance.exports.insertDisk(
        allocateTeaVMbyteArray(eventData.contents ?? null),
        allocateTeaVMstring(eventData.diskName ?? null)
      );

      postMessage({
        eventType: "DISK_INSERTED",
      });
    } else if (eventType === "EJECT_DISK") {
      instance.exports.ejectDisk();

      postMessage({
        eventType: "DISK_EJECTED",
      });
    } else if (eventType === "INSERT_TAPE") {
      instance.exports.insertTape(
        allocateTeaVMbyteArray(eventData.contents ?? null),
        allocateTeaVMstring(eventData.tapeName ?? null)
      );

      postMessage({
        eventType: "TAPE_INSERTED",
      });
    } else if (eventType === "EJECT_TAPE") {
      instance.exports.ejectTape();

      postMessage({
        eventType: "TAPE_EJECTED",
      });
    } else if (eventType === "PRESS_PLAY_ON_TAPE") {
      instance.exports.pressPlayOnTape();

      postMessage({
        eventType: "PRESSED_PLAY_ON_TAPE",
      });
    } else if (eventType === "SET_COMMAND") {
      instance.exports.typeInCommand(allocateTeaVMstring(eventData.command ?? null));

      postMessage({
        eventType: "COMMAND_SET",
      });
    } else if (eventType === "TYPE_KEY") {
      instance.exports.typeKey(allocateTeaVMstring(eventData.key ?? null));

      postMessage({
        eventType: "KEY_TYPED",
      });
    } else if (eventType === "PRESS_KEY") {
      instance.exports.pressKey(allocateTeaVMstring(eventData.key ?? null));

      postMessage({
        eventType: "KEY_PRESSED",
      });
    } else if (eventType === "RELEASE_KEY") {
      instance.exports.releaseKey(allocateTeaVMstring(eventData.key ?? null));

      postMessage({
        eventType: "KEY_RELEASED",
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
                postMessage({
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
                    length: length,
                  },
                }),
              processPixels: (pixelsPtr, length) =>
                postMessage({
                  eventType: "FRAME",
                  eventData: {
                    image: new Uint8Array(
                      instance.exports.memory.buffer,
                      instance.exports.teavm_intArrayData(pixelsPtr),
                      length
                    ),
                  },
                }),
              processSidWrite: (relTime, addr, value) =>
                postMessage({
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

          postMessage({
            eventType: "INITIALISED",
          });
        });
    }
  },
  false
);
