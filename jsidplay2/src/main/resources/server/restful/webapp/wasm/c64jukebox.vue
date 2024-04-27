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
              <input
                ref="formFileSm"
                id="file"
                type="file"
                @input="chosenFile = $refs.formFileSm.files[0]"
                :disabled="playing"
              />

              <button type="button" v-on:click="startTune()" :disabled="!chosenFile || playing">
                {{ $t("play") }}
              </button>
              <button type="button" v-on:click="stopTune()" :disabled="!playing">{{ $t("stop") }}</button>
              <button type="button" v-on:click="reset()">{{ $t("reset") }}</button>

              <div class="form-check" v-show="!playing">
                <div class="settings-box">
                  <span class="setting">
                    <label class="form-check-label" for="screen">
                      <input
                        class="form-check-input"
                        type="checkbox"
                        id="screen"
                        style="float: right; margin-left: 8px"
                        v-model="screen"
                      />
                      {{ $t("screen") }}
                    </label>
                  </span>
                </div>
              </div>

              <div>
                <p>{{ msg }}</p>
              </div>
            </div>
          </div>

          <div class="row">
            <div class="col">
              <div v-show="screen">
                <canvas id="c64Screen" width="384" :height="canvasHeight" />
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
                    <label for="nthFrame" v-show="screen">
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
            <li>Complete client side code, no server required</li>
            <li>&lt;2MB in size</li>
            <li>Developed as single Source code in JSIDPlay2 project</li>
            <li>Runs with nearly native speed (approximately 1.5x)</li>
            <li>Runs in all browsers</li>
            <li>Full emulation quality, no compromises</li>
            <li>Compatible with all SIDs (mono, stereo and 3-SID)</li>
            <li>Performance only depends on your PC setup</li>
            <li>Could even run on a mobile phone, if it's fast enough :-)</li>
          </ol>
        </div>
      </form>
    </div>
    <script>
      const maxWidth = 384;
      const maxHeight = 312;
      const MAX_QUEUE_SIZE = 60;
      const DROP_NTH_FRAME = 10;
      const DROP_FRAMES_SIZE = DROP_NTH_FRAME << 1;

      function Queue() {
        var head, tail, size;
        return Object.freeze({
          enqueue(value) {
            // prevent overflow, remove first frame
            if (size === MAX_QUEUE_SIZE) {
              head = head.next;
              size--;
            }
            const link = { value, next: undefined };
            tail = head ? (tail.next = link) : (head = link);
            size++;
          },
          dequeue() {
            // prevent overflow by dropping in-between frames
            if (size > DROP_FRAMES_SIZE) {
              var prev = head,
                scan = head;
              var count = size / DROP_NTH_FRAME;
              while (count-- > 0) {
                // skip frames
                for (i = 0; i < DROP_NTH_FRAME && scan.next; i++) {
                  prev = scan;
                  scan = scan.next;
                }
                if (!scan.next) {
                  // end of list? We remove the last frame
                  tail = prev;
                }
                // remove in-between frame
                prev.next = scan.next;
                size--;
              }
            }
            if (head) {
              const value = head.value;
              head = head.next;
              return value;
            }
            size--;
          },
          peek() {
            return head?.value;
          },
          clear() {
            tail = head = undefined;
            size = 0;
          },
          isNotEmpty() {
            return head;
          },
        });
      }

      var worker;

      var AudioContext = window.AudioContext || window.webkitAudioContext;
      var audioContext;
      var chunkNumber;

      var canvasContext;
      var imageData, data;
      var imageQueue = new Queue();

      function wasmWorker(contents, tuneName) {
        if (worker) {
          worker.terminate();
          worker = undefined;
        }

        worker = new Worker("jsidplay2-wasm-worker.js");

        return new Promise((resolve, reject) => {
          worker.postMessage({
            eventType: "INITIALISE",
            eventData: {
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
              worker.postMessage({ eventType: "CLOCK" });
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
              app.clearScreen();
              app.msg = app.$t("playing");
              if (app.screen) {
                setTimeout(() => app.showFrame(), app.startTime * app.audioBufferSize * 1000 / audioContext.sampleRate);
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
            screen: "Screen",
            defaultClockSpeed: "Default clock speed",
            defaultSidModel: "Default SID model",
            sampling: "Sampling Method",
            reverbBypass: "Bypass Schroeder reverb",
            sidWrites: "Print SID writes to console",
            bufferSize: "Emulation buffer size",
            audioBufferSize: "Audio buffer size",
            startSong: "Start song",
            nthFrame: "Show every nth frame",
            startTime: "Initial delay for warm-up phase",
            play: "Play",
            reset: "Reset",
            stop: "Stop",
            loading: "Loading tune, please wait...",
            playing: "Playing...",
          },
          de: {
            screen: "Bildschirm",
            defaultClockSpeed: "Default Clock Speed",
            defaultSidModel: "Default SID Model",
            sampling: "Sampling Methode",
            reverbBypass: "Schroeder Reverb überbrücken",
            sidWrites: "SID writes in Konsole schreiben",
            bufferSize: "Emulationspuffer Größe",
            audioBufferSize: "Audio Puffer Größe",
            startSong: "Start Song",
            nthFrame: "Zeige jedes Nte Bild",
            startTime: "Initiale Verzögerung für die Aufwärmphase",
            play: "Spiele",
            reset: "Reset",
            stop: "Stop",
            loading: "Lade den tune, bitte warten...",
            playing: "Abspielen...",
          },
        },
      });

      let app = Vue.createApp({
        data: function () {
          return {
            langs: ["de", "en"],
            msg: "",
            chosenFile: undefined,
            playing: false,
            screen: false,
            defaultClockSpeed: 50,
            startSong: 0,
            nthFrame: 1,
            nthFrames: [1, 10, 25, 30, 50, 60],
            startTime: 2,
            defaultSidModel: false,
            sampling: false,
            reverbBypass: true,
            sidWrites: false,
            bufferSize: 8 * 48000,
            audioBufferSize: 48000,
          };
        },
        computed: {
          canvasHeight: {
            get: function () {
              return this.defaultClockSpeed == 50 ? 285 : 236;
            },
          },
        },
        methods: {
          updateLanguage() {
            localStorage.locale = this.$i18n.locale;
          },
          reset() {
            audioContext = new AudioContext();
            wasmWorker(undefined, undefined);
            app.msg = app.$t("loading");
          },
          startTune() {
            audioContext = new AudioContext();
            var reader = new FileReader();
            reader.onload = function () {
              wasmWorker(new Uint8Array(this.result), app.chosenFile.name);
            };
            reader.readAsArrayBuffer(app.chosenFile);
            app.msg = app.$t("loading");
          },
          stopTune() {
            worker.terminate();
            worker = undefined;
            setTimeout(() => {
              imageQueue.clear();
              app.msg = "";
              app.playing = false;
              audioContext.close();
            });
          },
          clearScreen: function () {
            data.set(new Uint8Array((maxWidth * maxHeight) << 2));
            canvasContext.putImageData(imageData, 0, 0);
          },
          showFrame: function () {
            if (imageQueue.isNotEmpty()) {
              data.set(imageQueue.dequeue().image);
              canvasContext.putImageData(imageData, 0, 0);
            }
            if (app.playing) setTimeout(() => app.showFrame(), (1000 / app.defaultClockSpeed) * app.nthFrame);
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
