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
    <script src="/static/wasm/bytecoder/bytecoderruntime.js"></script>

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
          type="file"
          name="file"
          v-on:change="fileChange($event.target.files)"
          :disabled="chosenFile && playing"
        />
        <button type="button" v-on:click="startTune()" :disabled="!chosenFile || playing">{{ $t("play") }}</button>
        <button type="button" v-on:click="stopTune()" :disabled="!playing">{{ $t("stop") }}</button>
      </form>
      <div>
        <p>{{ msg }}</p>
      </div>
    </div>
    <script>
      const { createApp, ref } = Vue;

      const { createI18n } = VueI18n;

      var i18n = createI18n({
        legacy: false,
        locale: "en",
        messages: {
          en: {
            play: "Play",
            stop: "Stop",
            loading: "Loading tune, please wait...",
            playing: "Playing...",
          },
          de: {
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
          };
        },
        computed: {},
        methods: {
          updateLanguage() {
            localStorage.locale = this.$i18n.locale;
          },
          fileChange(fileList) {
            this.chosenFile = fileList[0];
            app.msg = "";
          },
          startTune() {
            bytecoder.instantiate("/static/wasm/bytecoder/bytecoderwasmclasses.wasm").then(function () {
              console.log("Bootstrapped");
              bytecoder.instance.exports.main(null, bytecoder.instance.exports.newObjectArray(null, 0));
              console.log("Ready for action!");
            });
          },
          stopTune() {
            setTimeout(() => {
              chunkNumber = 0;
              app.msg = "";
              app.playing = false;
              audioContext.close();
            });
          },
        },
        mounted: function () {
          if (localStorage.locale) {
            this.$i18n.locale = localStorage.locale;
          }
        },
        watch: {},
      })
        .use(i18n)
        .mount("#app");
    </script>
  </body>
</html>
