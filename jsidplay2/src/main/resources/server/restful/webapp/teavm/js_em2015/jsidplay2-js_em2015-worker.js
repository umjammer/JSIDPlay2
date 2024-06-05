import {
  main,
  open,
  typeInCommand,
  clock,
  insertDisk,
  ejectDisk,
  insertTape,
  ejectTape,
  pressPlayOnTape,
  typeKey,
  pressKey,
  releaseKey,
} from "./jsidplay2.js";

// Handle incoming messages
addEventListener(
  "message",
  (event) => {
    var { eventType, eventData } = event.data;

    if (eventType === "CLOCK") {
      clock();

      postMessage({
        eventType: "CLOCKED",
      });
    } else if (eventType === "IDLE") {
      postMessage({
        eventType: "CLOCKED",
      });
    } else if (eventType === "OPEN") {
      open(
        eventData.contents ?? null,
        eventData.tuneName ?? null,
        eventData.startSong,
        eventData.nthFrame,
        eventData.sidWrites,
        eventData.cartContents ?? null,
        eventData.cartName ?? null,
        eventData.command ?? null
      );

      postMessage({
        eventType: "OPENED",
      });
    } else if (eventType === "INSERT_DISK") {
      insertDisk(eventData.contents ?? null, eventData.diskName ?? null);

      postMessage({
        eventType: "DISK_INSERTED",
      });
    } else if (eventType === "EJECT_DISK") {
      ejectDisk();

      postMessage({
        eventType: "DISK_EJECTED",
      });
    } else if (eventType === "INSERT_TAPE") {
      insertTape(eventData.contents ?? null, eventData.tapeName ?? null);

      postMessage({
        eventType: "TAPE_INSERTED",
      });
    } else if (eventType === "EJECT_TAPE") {
      ejectTape();

      postMessage({
        eventType: "TAPE_EJECTED",
      });
    } else if (eventType === "PRESS_PLAY_ON_TAPE") {
      pressPlayOnTape();

      postMessage({
        eventType: "PRESSED_PLAY_ON_TAPE",
      });
    } else if (eventType === "SET_COMMAND") {
      typeInCommand(eventData.command ?? null);

      postMessage({
        eventType: "COMMAND_SET",
      });
    } else if (eventType === "TYPE_KEY") {
      typeKey(eventData.key ?? null);

      postMessage({
        eventType: "KEY_TYPED",
      });
    } else if (eventType === "PRESS_KEY") {
      pressKey(eventData.key ?? null);

      postMessage({
        eventType: "KEY_PRESSED",
      });
    } else if (eventType === "RELEASE_KEY") {
      releaseKey(eventData.key ?? null);

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
          eventData.filter6581,
          eventData.filter8580,
          eventData.stereoFilter6581,
          eventData.stereoFilter8580,
          eventData.thirdSIDFilter6581,
          eventData.thirdSIDFilter8580,
          eventData.reSIDfpFilter6581,
          eventData.reSIDfpFilter8580,
          eventData.reSIDfpStereoFilter6581,
          eventData.reSIDfpStereoFilter8580,
          eventData.reSIDfpThirdFilter6581,
          eventData.reSIDfpThirdFilter8580,
        ].map((item) => "" + item)
      );

      postMessage({
        eventType: "INITIALISED",
      });
    }
  },
  false
);
