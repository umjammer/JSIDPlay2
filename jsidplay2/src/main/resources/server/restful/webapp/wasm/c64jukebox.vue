<!DOCTYPE html>
<html>
  <head>
    <style lang="scss" scoped>
      @import "/static/c64jukebox.scss";
    </style>

    <!-- favicon.ico -->
    <link rel="shortcut icon" href="/static/favicon.ico" type="image/x-icon" />
    <link id="favicon" rel="icon" href="/static/favicon.ico" type="image/x-icon" />
    <link id="favicon-16x16" rel="icon" href="/static/favicon-16x16.png" type="image/png" sizes="16x16" />

    <!-- Load required Bootstrap, Icons CSS -->
    <link rel="stylesheet" href="/webjars/bootstrap/5.3.3/dist/css/bootstrap${min}.css" />
    <link rel="stylesheet" href="/webjars/bootstrap-icons/1.11.3/font/bootstrap-icons${min}.css" />

    <!-- Load Vue followed by Bootstrap -->
    <script src="/webjars/vue/3.4.21/dist/vue.global${prod}.js"></script>
    <script src="/webjars/bootstrap/5.3.3/dist/js/bootstrap${min}.js"></script>

    <!-- helpers -->
    <script src="/webjars/vue-i18n/9.10.1/dist/vue-i18n.global${prod}.js"></script>

    <!-- disable pull reload -->
    <style>
      html,
      body {
        overscroll-behavior: none;
      }
    </style>

    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />

    <title>C64 Jukebox (Web Assembly Version)</title>
  </head>
  <body>
    <div id="app">
      <form enctype="multipart/form-data">
        <div class="locale-changer">
          <h1 class="c64jukebox" style="width: 100%">C64 Jukebox (Web Assembly Version)</h1>
          <select
            id="localeselector"
            class="form-select form-select-sm"
            @change="updateLanguage"
            v-model="$i18n.locale"
            style="width: auto; margin: 1px"
          >
            <option v-for="(lang, i) in langs" :key="`Lang${i}`" :value="lang">{{ lang }}</option>
          </select>
        </div>

        <div class="container">
          <div class="row">
            <div class="col">
              <label for="file" class="form-label">{{ $t("chooseTune") }}</label>
              <input
                ref="formFileSm"
                id="file"
                type="file"
                @input="
                  $refs.formDiskFileSm.value = null;
                  chosenDiskFile = undefined;
                  chosenTapeFile = undefined;
                  chosenFile = $refs.formFileSm.files[0];
                "
                :disabled="playing"
              />
            </div>
          </div>
          <div class="row">
            <div class="col">
              <label for="diskFile" class="form-label">{{ $t("chooseDisk") }}</label>
              <input
                ref="formDiskFileSm"
                id="diskFile"
                type="file"
                :disabled="!playing"
                @input="
                  chosenDiskFile = $refs.formDiskFileSm.files[0];
                  insertDisk();
                "
              />
            </div>
          </div>
          <div class="row">
            <div class="col">
              <label for="tapeFile" class="form-label">{{ $t("chooseTape") }}</label>
              <input
                ref="formTapeFileSm"
                id="tapeFile"
                type="file"
                :disabled="!playing"
                @input="
                  chosenTapeFile = $refs.formTapeFileSm.files[0];
                  insertTape();
                "
              />
            </div>
          </div>
          <div class="row">
            <div class="col">
              <button
                type="button"
                v-on:click="
                  $refs.formDiskFileSm.value = null;
                  chosenDiskFile = undefined;
                  chosenTapeFile = undefined;
                  screen = false;
                  startTune();
                "
                :disabled="!chosenFile || playing"
              >
                {{ $t("play") }}
              </button>
              <button
                type="button"
                :class="paused ? 'btn btn-primary btn-sm' : ''"
                :aria-pressed="paused"
                data-bs-toggle="button"
                v-on:click="pauseTune()"
                :disabled="!playing"
              >
                {{ $t("pause") }}
              </button>
              <button type="button" v-on:click="stopTune()" :disabled="!playing">{{ $t("stop") }}</button>
              <button
                type="button"
                v-on:click="
                  $refs.formFileSm.value = null;
                  chosenFile = undefined;
                  $refs.formDiskFileSm.value = null;
                  $refs.formTapeFileSm.value = null;
                  chosenDiskFile = undefined;
                  chosenTapeFile = undefined;
                  stopTune();
                  screen = true;
                  reset();
                "
              >
                {{ $t("reset") }}
              </button>
            </div>
          </div>
          <div class="row">
            <div class="col">
              <button
                type="button"
                v-on:click="setCommand('LOAD&quot;*&quot;,8,1\rRUN\r')"
                :disabled="!chosenDiskFile || !playing"
              >
                {{ $t("loadDisk") }}
              </button>
              <button type="button" v-on:click="setCommand('LOAD\rRUN\r')" :disabled="!chosenTapeFile || !playing">
                {{ $t("loadTape") }}
              </button>
              <button type="button" v-on:click="pressPlayOnTape()" :disabled="!chosenTapeFile || !playing">
                {{ $t("pressPlayOnTape") }}
              </button>
              <button type="button" v-on:click="setCommand(' ')" :disabled="!playing">
                {{ $t("space") }}
              </button>
            </div>
          </div>

          <div class="row">
            <div class="col">
              <div>
                <p>{{ msg }}</p>
              </div>
              <div v-show="screen">
                <canvas id="c64Screen" style="scale: 2; margin: 150px" width="384" height="285" />
              </div>
            </div>
            <div class="col">
              <div class="form-check" v-show="!playing">
                <div class="settings-box">
                  <span class="setting">
                    <label for="startSong"
                      >{{ $t("startSong") }}
                      <input class="right" type="number" id="startSong" class="form-control" v-model.number="startSong"
                    /></label>
                  </span>
                </div>
                <div class="settings-box">
                  <span class="setting">
                    <div class="form-check">
                      <label class="form-check-label" for="palEmulation">
                        {{ $t("palEmulation") }}
                        <input
                          class="form-check-input"
                          type="checkbox"
                          id="palEmulation"
                          style="float: right; margin-left: 8px"
                          v-model="palEmulation"
                        />
                      </label>
                    </div>
                  </span>
                </div>
                <div class="settings-box">
                  <span class="setting">
                    <label for="nthFrame">
                      <select class="form-select form-select-sm right" id="nthFrame" v-model="nthFrame">
                        <option v-for="n in nthFrames" :value="n">{{ n }}</option>
                      </select>
                      {{ $t("nthFrame") }}
                    </label>
                  </span>
                </div>
                <div class="settings-box">
                  <span class="setting">
                    <label for="startTime"
                      >{{ $t("startTime") }}
                      <input class="right" type="number" id="startTime" class="form-control" v-model.number="startTime"
                    /></label>
                  </span>
                </div>
                <div class="settings-box">
                  <span class="setting">
                    <label for="defaultClockSpeed">
                      <select
                        class="form-select form-select-sm right"
                        id="defaultClockSpeed"
                        v-model="defaultClockSpeed"
                      >
                        <option value="50">PAL</option>
                        <option value="60">NTSC</option>
                      </select>
                      <span>{{ $t("defaultClockSpeed") }}</span>
                    </label>
                  </span>
                </div>

                <div class="settings-box">
                  <span class="setting">
                    <label for="defaultSidModel">
                      <select class="form-select form-select-sm right" id="defaultSidModel" v-model="defaultSidModel">
                        <option value="false">MOS6581</option>
                        <option value="true">MOS8580</option>
                      </select>
                      <span>{{ $t("defaultSidModel") }}</span>
                    </label>
                  </span>
                </div>
                <div class="settings-box">
                  <span class="setting">
                    <label for="sampling">
                      <select class="form-select form-select-sm right" id="sampling" v-model="sampling">
                        <option value="false">DECIMATE</option>
                        <option value="true">RESAMPLE</option>
                      </select>
                      <span>{{ $t("sampling") }}</span>
                    </label>
                  </span>
                </div>

                <div class="settings-box">
                  <span class="setting">
                    <div class="form-check">
                      <label class="form-check-label" for="reverbBypass">
                        {{ $t("reverbBypass") }}
                        <input
                          class="form-check-input"
                          type="checkbox"
                          id="reverbBypass"
                          style="float: right; margin-left: 8px"
                          v-model="reverbBypass"
                        />
                      </label>
                    </div>
                  </span>
                </div>
                <div class="settings-box">
                  <span class="setting">
                    <label class="form-check-label" for="sidWrites">
                      <input
                        class="form-check-input"
                        type="checkbox"
                        id="sidWrites"
                        style="float: right; margin-left: 8px"
                        v-model="sidWrites"
                      />
                      {{ $t("sidWrites") }}
                    </label>
                  </span>
                </div>

                <div class="settings-box">
                  <span class="setting">
                    <label for="bufferSize"
                      >{{ $t("bufferSize") }}
                      <input
                        class="right"
                        type="number"
                        id="bufferSize"
                        class="form-control"
                        v-model.number="bufferSize"
                    /></label>
                  </span>
                </div>
                <div class="settings-box">
                  <span class="setting">
                    <label for="audioBufferSize"
                      >{{ $t("audioBufferSize") }}
                      <input
                        class="right"
                        type="number"
                        id="audioBufferSize"
                        class="form-control"
                        v-model.number="audioBufferSize"
                    /></label>
                  </span>
                </div>
              </div>
            </div>
          </div>
          <p>Web Assembly Version (WASM) powered by <a href="https://teavm.org/" target="_blank">TeaVM</a></p>
          <ol>
            <li>NOT JAVA, just Web Assembly (W3C standard) and Javascript</li>
            <li>Runs in all major browsers (tested in Google Chrome, Firefox, MsEdge and Opera)</li>
            <li>Complete client side code, no server required (once loaded into the browsers cache)</li>
            <li>Runs in a web worker using a separate native thread</li>
            <li>Runs near to native speed</li>
            <li>~2MB in size, +2MB used for PAL emulation color data</li>
            <li>Developed as single source code in JSIDPlay2 project</li>
            <li>Full emulation quality, no compromises, C64, Floppy and more</li>
            <li>Compatible with all SIDs (mono, stereo and 3-SID), can run multi-disk demos</li>
            <li>Performance only depends on your PC setup</li>
            <li>Would even run on a mobile phone, if it will get as fast as a PC</li>
          </ol>
        </div>
      </form>
    </div>
    <script>
      function Queue() {
        var head, tail;
        return Object.freeze({
          enqueue(value) {
            const link = { value, next: undefined };
            tail = head ? (tail.next = link) : (head = link);
          },
          dequeue() {
            if (head) {
              var value = head.value;
              head = head.next;
              return value;
            }
            return undefined;
          },
          peek() {
            return head?.value;
          },
          clear() {
            tail = head = undefined;
          },
          isNotEmpty() {
            return head;
          },
        });
      }

      const maxWidth = 384;
      const maxHeight = 312;

      var worker;

      var AudioContext = window.AudioContext || window.webkitAudioContext;
      var audioContext;
      var chunkNumber;

      var canvasContext;
      var imageData, data;
      var imageQueue = new Queue();
      var start, time;

      function wasmWorker(contents, tuneName, reset) {
        audioContext = new AudioContext();

        if (worker) {
          worker.terminate();
          worker = undefined;
        }
        worker = new Worker("jsidplay2-wasm-worker.js");

        return new Promise((resolve, reject) => {
          worker.postMessage({
            eventType: "INITIALISE",
            eventData: {
              palEmulation: app.palEmulation,
              bufferSize: app.bufferSize,
              audioBufferSize: app.audioBufferSize,
              samplingRate: audioContext.sampleRate,
              samplingMethodResample: app.sampling === "true",
              reverbBypass: app.reverbBypass,
              defaultClockSpeed: app.defaultClockSpeed,
              defaultSidModel: app.defaultSidModel === "true",
            },
          });

          worker.addEventListener("message", function (event) {
            var { eventType, eventData, eventId } = event.data;

            if (eventType === "SAMPLES") {
              if (chunkNumber === 0) {
                audioContext.close();
                audioContext = new AudioContext();
              }
              var buffer = audioContext.createBuffer(2, eventData.left.length, audioContext.sampleRate);
              buffer.getChannelData(0).set(eventData.left);
              buffer.getChannelData(1).set(eventData.right);

              var sourceNode = audioContext.createBufferSource();
              sourceNode.buffer = buffer;
              sourceNode.connect(audioContext.destination);
              sourceNode.start((eventData.left.length / audioContext.sampleRate) * chunkNumber++);
            } else if (eventType === "FRAME") {
              imageQueue.enqueue({
                image: eventData.image,
              });
            } else if (eventType === "SID_WRITE") {
              console.log(
                "time=" +
                  eventData.time +
                  ", relTime=" +
                  eventData.relTime +
                  ", addr=" +
                  eventData.addr +
                  ", value=" +
                  eventData.value
              );
            } else if (eventType === "OPENED" || eventType === "CLOCKED") {
              if (!app.paused) {
                worker.postMessage({ eventType: "CLOCK" });
              }
            } else if (eventType === "INITIALISED") {
              worker.postMessage({
                eventType: "OPEN",
                eventData: {
                  contents: contents,
                  tuneName: tuneName,
                  startSong: app.startSong,
                  nthFrame: app.screen ? app.nthFrame : 0,
                  sidWrites: app.sidWrites,
                },
              });

              chunkNumber = app.startTime;
              imageQueue.clear();
              app.playing = true;
              app.paused = false;
              app.clearScreen();
              if (app.screen) {
                (start = new Date().getTime()), (time = 0);
                setTimeout(() => app.showFrame(), 0);
              }
            }
          });

          worker.addEventListener("error", function (error) {
            reject(error);
          });
        });
      }

      const { createApp, ref } = Vue;

      const { createI18n } = VueI18n;

      let i18n = createI18n({
        legacy: false,
        locale: "en",
        messages: {
          en: {
            palEmulation: "PAL emulation",
            defaultClockSpeed: "Default clock speed",
            defaultSidModel: "Default SID model",
            sampling: "Sampling Method",
            reverbBypass: "Bypass Schroeder reverb",
            sidWrites: "Print SID writes to console",
            bufferSize: "Emulation buffer size",
            audioBufferSize: "Audio buffer size",
            startSong: "Start song",
            nthFrame: "Show every nth frame",
            startTime: "Initial delay for warm-up phase [in audio buffers]",
            play: "Play",
            pause: "Pause",
            reset: "Reset",
            stop: "Stop",
            chooseTune: "SID",
            chooseDisk: "Disk",
            chooseTape: "Tape",
            diskInserted: "Disk inserted",
            tapeInserted: "Tape inserted",
            loadDisk: "Load *,8,1",
            loadTape: "Load",
            pressPlayOnTape: "Press Play on Tape",
            space: "Space Key",
          },
          de: {
            palEmulation: "PAL Emulation",
            defaultClockSpeed: "Default Clock Speed",
            defaultSidModel: "Default SID Model",
            sampling: "Sampling Methode",
            reverbBypass: "Schroeder Reverb überbrücken",
            sidWrites: "SID writes in Konsole schreiben",
            bufferSize: "Emulationspuffer Größe",
            audioBufferSize: "Audio Puffer Größe",
            startSong: "Start Song",
            nthFrame: "Zeige jedes Nte Bild",
            startTime: "Initiale Verzögerung für die Aufwärmphase [in Audio Puffern]",
            play: "Spiele",
            pause: "Pause",
            reset: "Reset",
            stop: "Stop",
            chooseTune: "SID",
            chooseDisk: "Diskette",
            chooseTape: "Kasette",
            diskInserted: "Diskette eingelegt",
            tapeInserted: "Kasette eingelegt",
            loadDisk: "Load *,8,1",
            loadTape: "Load",
            pressPlayOnTape: "Drücke Play auf der Datasette",
            space: "Leertaste",
          },
        },
      });

      let app = Vue.createApp({
        data: function () {
          return {
            langs: ["de", "en"],
            msg: "",
            chosenFile: undefined,
            chosenDiskFile: undefined,
            chosenTapeFile: undefined,
            playing: false,
            paused: false,
            screen: true,
            palEmulation: false,
            defaultClockSpeed: 50,
            startSong: 0,
            nthFrame: 4,
            nthFrames: [1, 2, 4, 10, 25, 30, 50, 60],
            startTime: 0,
            defaultSidModel: false,
            sampling: false,
            reverbBypass: true,
            sidWrites: false,
            bufferSize: 8 * 48000,
            audioBufferSize: 48000,
          };
        },
        computed: {},
        methods: {
          updateLanguage() {
            localStorage.locale = this.$i18n.locale;
          },
          reset() {
            wasmWorker();
          },
          startTune() {
            var reader = new FileReader();
            reader.onload = function () {
              wasmWorker(new Uint8Array(this.result), app.chosenFile.name);
            };
            reader.readAsArrayBuffer(app.chosenFile);
          },
          pauseTune() {
            if (app.paused) {
              audioContext.resume();
              worker.postMessage({ eventType: "CLOCK" });
            } else {
              audioContext.suspend();
            }
            app.paused = !app.paused;
          },
          stopTune() {
            if (worker) {
              worker.terminate();
              worker = undefined;
            }
            if (audioContext) {
              audioContext.close();
              audioContext = undefined;
            }
            imageQueue.clear();
            app.msg = "";
            app.playing = false;
            app.paused = false;
          },
          clearScreen: function () {
            data.set(new Uint8Array((maxWidth * maxHeight) << 2));
            canvasContext.putImageData(imageData, 0, 0);
          },
          showFrame: function () {
            var timeSpan = (1000 * app.nthFrame) / app.defaultClockSpeed;
            time += timeSpan;
            var elem = imageQueue.dequeue();
            if (elem) {
              data.set(elem.image);
              canvasContext.putImageData(imageData, 0, 0);
            }
            var diff = new Date().getTime() - start - time;
            if (app.playing) setTimeout(() => app.showFrame(), timeSpan - diff);
          },
          insertDisk() {
            var reader = new FileReader();
            reader.onload = function () {
              worker.postMessage({
                eventType: "INSERT_DISK",
                eventData: {
                  contents: new Uint8Array(this.result),
                  diskName: app.chosenDiskFile.name,
                },
              });
              app.msg = app.$t("diskInserted") + ": " + app.chosenDiskFile.name;
            };
            reader.readAsArrayBuffer(app.chosenDiskFile);
          },
          setCommand(command) {
            if (worker) {
              worker.postMessage({
                eventType: "SET_COMMAND",
                eventData: {
                  command: command,
                },
              });
            }
          },
          insertTape() {
            var reader = new FileReader();
            reader.onload = function () {
              worker.postMessage({
                eventType: "INSERT_TAPE",
                eventData: {
                  contents: new Uint8Array(this.result),
                  tapeName: app.chosenTapeFile.name,
                },
              });
              app.msg = app.$t("tapeInserted") + ": " + app.chosenTapeFile.name;
            };
            reader.readAsArrayBuffer(app.chosenTapeFile);
          },
          pressPlayOnTape() {
            if (worker) {
              worker.postMessage({
                eventType: "PRESS_PLAY_ON_TAPE",
                eventData: {},
              });
            }
          },
        },
        mounted: function () {
          if (localStorage.locale) {
            this.$i18n.locale = localStorage.locale;
          }
          var canvas = document.getElementById("c64Screen");
          canvasContext = canvas.getContext("2d");
          imageData = canvasContext.getImageData(0, 0, maxWidth, maxHeight);
          data = imageData.data;
        },
        watch: {},
      })
        .use(i18n)
        .mount("#app");
    </script>
  </body>
</html>
