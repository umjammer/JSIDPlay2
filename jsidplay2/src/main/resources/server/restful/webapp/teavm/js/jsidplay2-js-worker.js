importScripts("jsidplay2.js");

function xxx(args) {
  main(args);
}

function getInt64Bytes(x) {
  let y = Math.floor(x / 2 ** 32);
  return [y, y << 8, y << 16, y << 24, x, x << 8, x << 16, x << 24].map((z) => z >>> 24);
}

function processSamples(resultL, resultR, length) {
  self.postMessage({
    eventType: "SAMPLES",
    eventData: {
      left: resultL,
      right: resultR,
      length: length,
    },
  });
}
function processPixels(pixels, length) {
  // Create a new Uint8Array to store the resulting byte array
  const byteArray = new Uint8Array(length); // Each pixel has 4 bytes (ABGR)

  for (let i = 0; i < length / 4; i++) {
    const pixel = pixels[i];

    // Extract individual color components (in big-endian order)
    const alpha = (pixel >> 24) & 0xff;
    const blue = (pixel >> 16) & 0xff;
    const green = (pixel >> 8) & 0xff;
    const red = pixel & 0xff;

    // Populate the byte array with the desired color component
    byteArray[i * 4] = red; // Red
    byteArray[i * 4 + 1] = green; // Green
    byteArray[i * 4 + 2] = blue; // Blue
    byteArray[i * 4 + 3] = alpha; // Alpha
  }

  self.postMessage({
    eventType: "FRAME",
    eventData: {
      image: byteArray,
    },
  });
}
function processSidWrite(relTime, addr, value) {
  self.postMessage({
    eventType: "SID_WRITE",
    eventData: {
      relTime: relTime,
      addr: addr,
      value: value,
    },
  });
}

// Handle incoming messages
self.addEventListener(
  "message",
  function (event) {
    var { eventType, eventData, eventId } = event.data;

    if (eventType === "CLOCK") {
      main.api.clock();

      self.postMessage({
        eventType: "CLOCKED",
      });
    } else if (eventType === "IDLE") {
      self.postMessage({
        eventType: "CLOCKED",
      });
    } else if (eventType === "OPEN") {
      main.api.open(
        eventData.contents ? eventData.contents : null,
        eventData.tuneName ? eventData.tuneName : null,
        eventData.startSong,
        eventData.nthFrame,
        eventData.sidWrites,
        eventData.cartContents ? eventData.cartContents : null,
        eventData.cartName ? eventData.cartName : null
      );

      self.postMessage({
        eventType: "OPENED",
      });
    } else if (eventType === "INSERT_DISK") {
      main.api.insertDisk(
        eventData.contents ? eventData.contents : null,
        eventData.diskName ? eventData.diskName : null
      );

      self.postMessage({
        eventType: "DISK_INSERTED",
      });
    } else if (eventType === "EJECT_DISK") {
      main.api.ejectDisk();

      self.postMessage({
        eventType: "DISK_EJECTED",
      });
    } else if (eventType === "INSERT_TAPE") {
      main.api.insertTape(
        eventData.contents ? eventData.contents : null,
        eventData.tapeName ? eventData.tapeName : null
      );

      self.postMessage({
        eventType: "TAPE_INSERTED",
      });
    } else if (eventType === "EJECT_TAPE") {
      main.api.ejectTape();

      self.postMessage({
        eventType: "TAPE_EJECTED",
      });
    } else if (eventType === "PRESS_PLAY_ON_TAPE") {
      main.api.pressPlayOnTape();

      self.postMessage({
        eventType: "PRESSED_PLAY_ON_TAPE",
      });
    } else if (eventType === "SET_COMMAND") {
      main.api.typeInCommand(eventData.command ? eventData.command : null);

      self.postMessage({
        eventType: "COMMAND_SET",
      });
    } else if (eventType === "TYPE_KEY") {
      main.api.typeKey(eventData.key ? eventData.key : null);

      self.postMessage({
        eventType: "KEY_TYPED",
      });
    } else if (eventType === "INITIALISE") {
      xxx(
        new Array(
          "" + eventData.palEmulation,
          "" + eventData.bufferSize,
          "" + eventData.audioBufferSize,
          "" + eventData.samplingRate,
          "" + eventData.samplingMethodResample,
          "" + eventData.reverbBypass,
          "" + eventData.defaultClockSpeed,
          "" + eventData.defaultSidModel,
          "" + eventData.jiffyDosInstalled
        )
      );

      self.postMessage({
        eventType: "INITIALISED",
      });
    }
  },
  false
);
