importScripts("jsidplay2.js");

// Handle incoming messages
self.addEventListener(
  "message",
  function (event) {
    var { eventType, eventData } = event.data;

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
      self.main(
        [
          eventData.palEmulation,
          eventData.bufferSize,
          eventData.audioBufferSize,
          eventData.samplingRate,
          eventData.samplingMethodResample,
          eventData.reverbBypass,
          eventData.defaultClockSpeed,
          eventData.defaultEmulation,
          eventData.defaultSidModel,
          eventData.jiffyDosInstalled,
        ].map((item) => "" + item)
      );

      self.postMessage({
        eventType: "INITIALISED",
      });
    }
  },
  false
);
