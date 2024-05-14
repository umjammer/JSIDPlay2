importScripts("jsidplay2.js");

      function processSamples(resultL, resultR, length) {
                self.postMessage({
                  eventType: "SAMPLES",
                  eventData: {
                    resultL: resultL,
                    resultR: resultR,
                    length: length
                  },
                  })
      }
      function processPixels(pixels, length) {
                self.postMessage({
                  eventType: "FRAME",
                  eventData: {
                    image: new Uint8Array(pixels).slice(),
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
        eventData.contents,
        eventData.tuneName,
        eventData.startSong,
        eventData.nthFrame,
        eventData.sidWrites,
        eventData.cartContents,
        eventData.cartName
      );

      self.postMessage({
        eventType: "OPENED",
      });
    } else if (eventType === "INSERT_DISK") {
      main.api.insertDisk(
        eventData.contents,
        eventData.diskName
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
        eventData.contents,
        eventData.tapeName
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
      main.api.setCommand(eventData.command);

      self.postMessage({
        eventType: "COMMAND_SET",
      });
    } else if (eventType === "TYPE_KEY") {
      main.api.typeKey(eventData.key);

      self.postMessage({
        eventType: "KEY_TYPED",
      });
    } else if (eventType === "INITIALISE") {
      main();

          self.postMessage({
            eventType: "INITIALISED",
          });
    }
  },
  false
);
