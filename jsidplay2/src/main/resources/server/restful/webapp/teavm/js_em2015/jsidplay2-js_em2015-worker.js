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
  joystick,
  defaultEmulation,
  defaultChipModel,
  filterName,
  mute,
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
    } else if (eventType === "PRESS_JOYSTICK") {
      joystick(eventData.number, eventData.value);

      postMessage({
        eventType: "JOYSTICK_PRESSED",
      });
    } else if (eventType === "SET_DEFAULT_EMULATION") {
      defaultEmulation(eventData.emulation);

      postMessage({
        eventType: "DEFAULT_EMULATION_SET",
      });
    } else if (eventType === "SET_DEFAULT_CHIP_MODEL") {
      defaultChipModel(eventData.chipModel);

      postMessage({
        eventType: "DEFAULT_CHIP_MODEL_SET",
      });
    } else if (eventType === "SET_FILTER_NAME") {
      filterName(eventData.emulation, eventData.chipModel, eventData.sidNum, eventData.filterName);

      postMessage({
        eventType: "FILTER_NAME_SET",
      });
    } else if (eventType === "SET_MUTE") {
      mute(eventData.sidNum, eventData.voice, eventData.value);

      postMessage({
        eventType: "MUTE_SET",
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