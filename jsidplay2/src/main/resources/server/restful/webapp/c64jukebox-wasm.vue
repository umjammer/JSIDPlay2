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
    <link rel="stylesheet" href="/webjars/bootstrap/5.3.3/dist/css/bootstrap$min.css" />
    <link rel="stylesheet" href="/webjars/bootstrap-icons/1.11.3/font/bootstrap-icons$min.css" />

    <!-- Load Vue followed by Bootstrap -->
    <script src="/webjars/vue/3.4.21/dist/vue.global$prod.js"></script>
    <script src="/webjars/bootstrap/5.3.3/dist/js/bootstrap$min.js"></script>

    <!-- helpers -->
    <script src="/webjars/vue-i18n/9.10.1/dist/vue-i18n.global$prod.js"></script>
    <script src="/webjars/axios/1.5.1/dist/axios$min.js"></script>

    <!-- WASM -->
    <script src="/static/wasm/jsidplay2.wasm-runtime.js"></script>

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

        <input
          ref="formFileSm"
          id="file"
          type="file"
          @input="chosenFile = $refs.formFileSm.files[0]"
          :disabled="chosenFile && playing"
        />

        <button type="button" v-on:click="startTune()" :disabled="!chosenFile || playing">{{ $t("play") }}</button>
        <button type="button" v-on:click="stopTune()" :disabled="!playing">{{ $t("stop") }}</button>

        <div class="form-check" v-show="chosenFile && !playing">
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
        <div v-show="screen">
          <canvas id="myCanvas" width="384" height="312" />
        </div>

        <div class="form-check" v-show="chosenFile && !playing">
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
              <label for="defaultClockSpeed">
                <select class="form-select form-select-sm right" id="defaultClockSpeed" v-model="defaultClockSpeed">
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
              <label for="bufferSize"
                >{{ $t("bufferSize") }}
                <input class="right" type="number" id="bufferSize" class="form-control" v-model.number="bufferSize"
              /></label>
            </span>
          </div>
          <div class="settings-box">
            <span class="setting">
              <label for="audioBufferSize">
                <select class="form-select form-select-sm right" id="audioBufferSize" v-model="audioBufferSize">
                  <option value="1024">1024</option>
                  <option value="2048">2048</option>
                  <option value="4096">4096</option>
                  <option value="8192">8192</option>
                  <option value="16384">16384</option>
                </select>
                <span>{{ $t("audioBufferSize") }}</span>
              </label>
            </span>
          </div>
        </div>
      </form>
    </div>
    <script>
      var AudioContext = window.AudioContext || window.webkitAudioContext;
      var audioContext;
      var chunkNumber;
      var canvas;
      var ctx;
      var imageData;
      const maxWidth = 384;
      const maxHeight = 312;
      const screenByteLength = (maxWidth * maxHeight) << 2;

      function allocateTeaVMbyteArray(array) {
        let byteArrayPtr = window.instance.exports.teavm_allocateByteArray(array.length);
        let byteArrayData = window.instance.exports.teavm_byteArrayData(byteArrayPtr);
        new Uint8Array(window.instance.exports.memory.buffer, byteArrayData, array.length).set(array);
        return byteArrayPtr;
      }

      function allocateTeaVMstring(str) {
        let stringPtr = window.instance.exports.teavm_allocateString(str.length);
        let objectArrayData = window.instance.exports.teavm_objectArrayData(
          instance.exports.teavm_stringData(stringPtr)
        );
        let arrayView = new Uint16Array(window.instance.exports.memory.buffer, objectArrayData, str.length);
        for (let i = 0; i < arrayView.length; ++i) {
          arrayView[i] = str.charCodeAt(i);
        }
        return stringPtr;
      }

      function processSamples(leftChannelPtr, rightChannelPtr, length) {
        var leftChannelAddress = instance.exports.teavm_floatArrayData(leftChannelPtr);
        var rightChannelAddress = instance.exports.teavm_floatArrayData(rightChannelPtr);

        var buffer = audioContext.createBuffer(2, length, audioContext.sampleRate);
        buffer.getChannelData(0).set(new Float32Array(instance.exports.memory.buffer, leftChannelAddress, length));
        buffer.getChannelData(1).set(new Float32Array(instance.exports.memory.buffer, rightChannelAddress, length));

        var sourceNode = audioContext.createBufferSource();
        sourceNode.buffer = buffer;
        sourceNode.connect(audioContext.destination);
        sourceNode.start((length / audioContext.sampleRate) * chunkNumber++);
      }

      function processPixels(pixelsPtr) {
        var pixelsAddress = instance.exports.teavm_byteArrayData(pixelsPtr);

        imageData.data.set(new Uint8Array(instance.exports.memory.buffer, pixelsAddress, screenByteLength));
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
            bufferSize: "Emulation buffer size",
            audioBufferSize: "Audio buffer size",
            nthFrame: "Show every nth frame",
            play: "Play",
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
            bufferSize: "Emulationspuffer Größe",
            audioBufferSize: "Audio Puffer Größe",
            nthFrame: "Zeige jedes Nte Bild",
            play: "Spiele",
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
            defaultClockSpeed: "50",
            nthFrame: 10,
            nthFrames: [10, 25, 30, 50, 60],
            defaultSidModel: false,
            sampling: false,
            reverbBypass: true,
            bufferSize: 16 * 65536,
            audioBufferSize: 16384,
          };
        },
        computed: {},
        methods: {
          updateLanguage() {
            localStorage.locale = this.$i18n.locale;
          },
          startTune() {
            audioContext = new AudioContext();
            TeaVM.wasm
              .load("/static/wasm/jsidplay2.wasm", {
                installImports(o, controller) {
                  o.audiosection = {
                    getBufferSize: () => app.bufferSize,
                    getAudioBufferSize: () => app.audioBufferSize,
                    getSamplingRate: () => audioContext.sampleRate,
                    getSamplingMethodResample: () => app.sampling === "true",
                    getReverbBypass: () => app.reverbBypass,
                  };
                  o.emulationsection = {
                    getDefaultClockSpeed: () => app.defaultClockSpeed,
                    getDefaultSidModel8580: () => app.defaultSidModel === "true",
                  };
                  o.audiodriver = {
                    processSamples: processSamples,
                    processPixels: processPixels,
                  };
                },
              })
              .then((teavm) => {
                window.instance = teavm.instance;

                var reader = new FileReader();
                reader.onload = function () {
                  let sidContentsPtr = allocateTeaVMbyteArray(new Uint8Array(this.result));
                  let tuneNamePtr = allocateTeaVMstring(app.chosenFile.name);

                  window.instance.exports.open(sidContentsPtr, tuneNamePtr, app.screen ? app.nthFrame : 0);

                  app.play();
                };
                reader.readAsArrayBuffer(app.chosenFile);
                app.msg = app.$t("loading");
              })
              .catch((error) => {
                console.log(error);
              });
          },
          stopTune() {
            ctx.clearRect(0, 0, maxWidth, maxHeight);
            window.instance.exports.close();
            setTimeout(() => {
              app.msg = "";
              app.playing = false;
              audioContext.close();
            });
          },
          play: function () {
            chunkNumber = 6; // small delay for warm-up phase
            ctx.clearRect(0, 0, maxWidth, maxHeight);
            app.playing = true;
            app.msg = app.$t("playing");

            app.clock();
            if (app.screen) {
              app.show();
            }
          },
          clock: function () {
            if (window.instance.exports.clock() > 0) setTimeout(() => app.clock());
          },
          show: function () {
            if (imageData) ctx.putImageData(imageData, 0, 0);
            if (app.playing) setTimeout(() => app.show(), (1000 / app.defaultClockSpeed) * app.nthFrame);
          },
        },
        mounted: function () {
          if (localStorage.locale) {
            this.$i18n.locale = localStorage.locale;
          }
          canvas = document.getElementById("myCanvas");
          ctx = canvas.getContext("2d", { willReadFrequently: true, alpha: false });
          imageData = ctx.createImageData(maxWidth, maxHeight);
        },
        watch: {},
      })
        .use(i18n)
        .mount("#app");
    </script>
  </body>
</html>
