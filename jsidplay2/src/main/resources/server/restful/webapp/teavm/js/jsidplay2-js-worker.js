importScripts("jsidplay2.js");

// Handle incoming messages
addEventListener(
  "message",
  function (event) {
    var { eventType, eventData } = event.data;

    if (eventType === "CLOCK") {
      main.api.clock();

      postMessage({
        eventType: "CLOCKED",
      });
    } else if (eventType === "IDLE") {
      postMessage({
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
        eventData.cartName ? eventData.cartName : null,
        eventData.command ? eventData.command : null
      );

      postMessage({
        eventType: "OPENED",
      });
    } else if (eventType === "INSERT_DISK") {
      main.api.insertDisk(
        eventData.contents ? eventData.contents : null,
        eventData.diskName ? eventData.diskName : null
      );

      postMessage({
        eventType: "DISK_INSERTED",
      });
    } else if (eventType === "EJECT_DISK") {
      main.api.ejectDisk();

      postMessage({
        eventType: "DISK_EJECTED",
      });
    } else if (eventType === "INSERT_TAPE") {
      main.api.insertTape(
        eventData.contents ? eventData.contents : null,
        eventData.tapeName ? eventData.tapeName : null
      );

      postMessage({
        eventType: "TAPE_INSERTED",
      });
    } else if (eventType === "EJECT_TAPE") {
      main.api.ejectTape();

      postMessage({
        eventType: "TAPE_EJECTED",
      });
    } else if (eventType === "PRESS_PLAY_ON_TAPE") {
      main.api.pressPlayOnTape();

      postMessage({
        eventType: "PRESSED_PLAY_ON_TAPE",
      });
    } else if (eventType === "SET_COMMAND") {
      main.api.typeInCommand(eventData.command ? eventData.command : null);

      postMessage({
        eventType: "COMMAND_SET",
      });
    } else if (eventType === "TYPE_KEY") {
      main.api.typeKey(eventData.key ? eventData.key : null);

      postMessage({
        eventType: "KEY_TYPED",
      });
    } else if (eventType === "PRESS_KEY") {
      main.api.pressKey(eventData.key ? eventData.key : null);

      postMessage({
        eventType: "KEY_PRESSED",
      });
    } else if (eventType === "RELEASE_KEY") {
      main.api.releaseKey(eventData.key ? eventData.key : null);

      postMessage({
        eventType: "KEY_RELEASED",
      });
    } else if (eventType === "INITIALISE") {
      main(
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

      postMessage({
        eventType: "INITIALISED",
      });
    }
  },
  false
);
