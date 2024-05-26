<!DOCTYPE html>
<html>
  <head>
    <style lang="scss" scoped>
      @import "/static/teavm/c64jukebox.scss";
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
    <script src="/webjars/bootstrap/5.3.3/dist/js/bootstrap.bundle${min}.js"></script>

    <!-- helpers -->
    <script src="/webjars/vue-i18n/9.10.1/dist/vue-i18n.global${prod}.js"></script>
    <script src="/webjars/nosleep.js/0.12.0/dist/NoSleep${min}.js"></script>

    <!-- disable pull reload -->
    <style>
      html,
      body {
        overscroll-behavior: none;
      }
    </style>

    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />

    <title>C64 Jukebox (${teaVMFormatName} beta Version)</title>
  </head>
  <body>
    <div id="app">
      <form enctype="multipart/form-data">
        <div class="locale-changer">
          <h1 class="c64jukebox" style="width: 100%">C64 Jukebox (${teaVMFormatName} beta Version)</h1>
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

        <nav class="navbar navbar-expand navbar-dark bg-primary">
          <div class="container-fluid">
            <div class="collapse navbar-collapse" id="main_nav">
              <ul class="navbar-nav">
                <li class="nav-item dropdown" id="myDropdown">
                  <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">{{ $t("file") }}</a>
                  <ul class="dropdown-menu" style="width: 200px !important">
                    <li>
                      <a class="dropdown-item" href="#" @click="$refs.formFileSm.click()"> {{ $t("play") }} </a>
                      <input ref="formFileSm" id="file" type="file" @input="startTune()" style="display: none" />
                    </li>
                    <li>
                      <a class="dropdown-item" href="#" @click="reset()"> {{ $t("reset") }} </a>
                    </li>
                  </ul>
                </li>
                <li class="nav-item dropdown" id="myDropdown">
                  <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">{{ $t("player") }}</a>
                  <ul class="dropdown-menu" style="width: 200px !important">
                    <li>
                      <div class="dropdown-item form-check">
                        <label class="form-check-label" for="pause" style="cursor: pointer">
                          <input
                            class="form-check-input"
                            type="checkbox"
                            id="pause"
                            style="float: right; margin-left: 8px"
                            v-model="paused"
                            @click="pauseTune()"
                          />
                          {{ $t("pauseContinue") }}
                        </label>
                      </div>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          stopTune();
                          screen = true;
                        "
                        >{{ $t("stop") }}</a
                      >
                    </li>
                  </ul>
                </li>
                <li class="nav-item dropdown" id="myDropdown">
                  <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">{{ $t("devices") }}</a>
                  <ul class="dropdown-menu" style="width: 200px !important">
                    <li>
                      <a class="dropdown-item" href="#" v-on:click.stop="showFloppy = !showFloppy"
                        >{{ $t("floppy") }}&raquo;
                      </a>
                      <ul class="submenu dropdown-menu" :style="showFloppy ? 'display: block !important' : ''">
                        <li>
                          <a class="dropdown-item" href="#" @click="$refs.formDiskFileSm.click()">{{
                            $t("insertDisk")
                          }}</a>
                          <input
                            ref="formDiskFileSm"
                            id="diskFile"
                            type="file"
                            @input="insertDisk()"
                            style="display: none"
                          />
                        </li>

                        <li>
                          <div class="dropdown-item form-check">
                            <label class="form-check-label" for="jiffyDosInstalled" style="cursor: pointer">
                              <input
                                class="form-check-input"
                                type="checkbox"
                                id="jiffyDosInstalled"
                                style="float: right; margin-left: 8px"
                                v-model="jiffyDosInstalled"
                                @change="reset()"
                              />
                              {{ $t("jiffyDosInstalled") }}
                            </label>
                          </div>
                        </li>

                        <li>
                          <a class="dropdown-item" href="#" @click="ejectDisk()">{{ $t("ejectDisk") }}</a>
                        </li>
                      </ul>
                    </li>
                    <li>
                      <a class="dropdown-item" href="#" v-on:click.stop="showTape = !showTape"
                        >{{ $t("tape") }}&raquo;
                      </a>
                      <ul class="submenu dropdown-menu" :style="showTape ? 'display: block !important' : ''">
                        <li>
                          <a class="dropdown-item" href="#" @click="$refs.formTapeFileSm.click()">{{
                            $t("insertTape")
                          }}</a>
                          <input
                            ref="formTapeFileSm"
                            id="tapeFile"
                            type="file"
                            @input="insertTape()"
                            style="display: none"
                          />
                        </li>
                        <li>
                          <a class="dropdown-item" href="#" @click="pressPlayOnTape()">{{ $t("pressPlayOnTape") }}</a>
                        </li>
                        <li>
                          <a class="dropdown-item" href="#" @click="ejectTape()">{{ $t("ejectTape") }}</a>
                        </li>
                      </ul>
                    </li>
                    <li>
                      <a class="dropdown-item" href="#" v-on:click.stop="showCart = !showCart"
                        >{{ $t("cart") }}&raquo;
                      </a>
                      <ul class="submenu dropdown-menu" :style="showCart ? 'display: block !important' : ''">
                        <li>
                          <a class="dropdown-item" href="#" @click="$refs.formCartFileSm.click()">{{
                            $t("insertCart")
                          }}</a>
                          <input
                            ref="formCartFileSm"
                            id="cartFile"
                            type="file"
                            @input="insertCart()"
                            style="display: none"
                          />
                        </li>
                        <li>
                          <a class="dropdown-item" href="#" @click="ejectCart()">{{ $t("ejectCart") }}</a>
                        </li>
                      </ul>
                    </li>
                  </ul>
                </li>
              </ul>
            </div>
            <!-- navbar-collapse.// -->
          </div>
          <!-- container-fluid.// -->
        </nav>

        <div class="container">
          <div class="row">
            <div class="col">
              <button
                type="button"
                v-on:click="typeInCommand('LOAD&quot;*&quot;,8,1\rRUN\r')"
                :disabled="!$refs.formDiskFileSm || !$refs.formDiskFileSm.files[0] || !playing || !screen"
              >
                {{ $t("loadDisk") }}
              </button>
              <button
                type="button"
                v-on:click="typeInCommand('LOAD\rRUN\r')"
                :disabled="!$refs.formTapeFileSm || !$refs.formTapeFileSm.files[0] || !playing || !screen"
              >
                {{ $t("loadTape") }}
              </button>
              <button type="button" v-on:click="typeKey('SPACE')" :disabled="!playing || !screen">
                {{ $t("space") }}
              </button>
              <input type="button" id="toggle" value="Wake Lock is disabled" />
            </div>
          </div>

          <div class="row">
            <div class="col">
              <div>
                <p>{{ msg }}</p>
              </div>
              <div v-show="screen" style="width: 100%; margin: 0px auto">
                <span v-show="playing">Frames in der Queue: {{ framesCounter }}</span>
                <canvas
                  id="c64Screen"
                  style="border: 2px solid black; background-color: black"
                  width="384"
                  height="285"
                />
              </div>
            </div>
            <div class="col" style="margin-left: 100px">
              <h2>Example Music</h2>
              <ol>
                <li>
                  Mutetus - Only 299.99
                  <button
                    type="button"
                    v-on:click="
                      downloadAndStartTune(
                        'Only 299.99',
                        '/jsidplay2service/JSIDPlay2REST/download/Only_299_99.sid?itemId=3470375608&categoryId=18'
                      )
                    "
                  >
                    <i class="bi bi-download"></i>
                  </button>
                </li>
                <li>
                  LMan - Hi fi Sky
                  <button
                    type="button"
                    v-on:click="
                      downloadAndStartTune(
                        'Hi Fi Sky',
                        '/jsidplay2service/JSIDPlay2REST/download/Hi_Fi_Sky.sid?itemId=4064310083&categoryId=18'
                      )
                    "
                  >
                    <i class="bi bi-download"></i>
                  </button>
                </li>
                <li>
                  DJ Space - Monty is a Maniac
                  <button
                    type="button"
                    v-on:click="
                      downloadAndStartTune(
                        'Monty is a Maniac',
                        '/jsidplay2service/JSIDPlay2REST/download/djspace_monty_is_a_maniac.sid?itemId=239515&categoryId=4'
                      )
                    "
                  >
                    <i class="bi bi-download"></i>
                  </button>
                </li>
              </ol>

              <h2>Example Demos</h2>
              <ol>
                <li>
                  Fairlight - 1337
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        '1337-a',
                        '/jsidplay2service/JSIDPlay2REST/download/fairlight-1337-58679b69-a.d64?itemId=242855&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #1
                  </button>
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        '1337-b',
                        '/jsidplay2service/JSIDPlay2REST/download/fairlight-1337-58679b69-b.d64?itemId=242855&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #2
                  </button>
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        '1337-c',
                        '/jsidplay2service/JSIDPlay2REST/download/fairlight-1337-58679b69-c.d64?itemId=242855&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #3
                  </button>
                </li>
                <li>
                  Performers - Next Level
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        'image1',
                        '/jsidplay2service/JSIDPlay2REST/download/image1.d64?itemId=232976&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #1
                  </button>
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        'image2',
                        '/jsidplay2service/JSIDPlay2REST/download/image2.d64?itemId=232976&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #2
                  </button>
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        'image3',
                        '/jsidplay2service/JSIDPlay2REST/download/image3.d64?itemId=232976&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #3
                  </button>
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        'image4',
                        '/jsidplay2service/JSIDPlay2REST/download/image4.d64?itemId=232976&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #4
                  </button>
                </li>
                <li>
                  Bonzai,Pretzel Logic - Mojo
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        'Side1',
                        '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side1.D64?itemId=232966&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #1
                  </button>
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        'Side2',
                        '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side2.D64?itemId=232966&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #2
                  </button>
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        'Side3',
                        '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side3.D64?itemId=232966&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #3
                  </button>
                  <button
                    type="button"
                    v-on:click="
                      downloadAndInsertDisk(
                        'Side4',
                        '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side4.D64?itemId=232966&categoryId=1'
                      )
                    "
                  >
                    <i class="bi bi-floppy"></i> #4
                  </button>
                </li>
              </ol>
            </div>
          </div>
        </div>
        <div class="container">
          <div class="row">
            <div class="col">
              <p>${teaVMFormatName} beta Version powered by <a href="https://teavm.org/" target="_blank">TeaVM</a></p>
              <ol>
                <li>Run JSIDPlay2 in a browser in ${teaVMFormatName} (THIS IS NOT JAVA)</li>
                <li>Runs out-of-the-box in all browsers (Chrome is faster than Firefox)</li>
                <li>Only 2MB in size, loads very quick</li>
                <li>Compatible with all SIDs (mono, stereo and 3-SID), can even run multi-disk demos on PC</li>
                <li>Runs near to native speed, performance only depends on your max. single core speed</li>
                <li>Plays at least mono SIDs on a mobile phone</li>
                <li>Runs completely on the client side in a web worker (once in browser's cache)</li>
                <li>Full emulation quality, no compromises, C64, Floppy and more</li>
                <li>
                  Developed single source in JSIDPlay2 project, enhancements are automatically available in all versions
                </li>
                <li>For the first time, embed music or demos in YOUR web-site</li>
              </ol>
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
                    <label for="defaultEmulation">
                      <select class="form-select form-select-sm right" id="defaultEmulation" v-model="defaultEmulation">
                        <option value="RESID">Dag Lem's resid 1.0 beta</option>
                        <option value="RESIDFP">Antti S. Lankila's resid-fp</option>
                      </select>
                      <span>{{ $t("defaultEmulation") }}</span>
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
        </div>
      </form>
    </div>
    <script>
      var size = 0;
      function Queue() {
        var head, tail;
        return Object.freeze({
          enqueue(value) {
            const link = { value, next: undefined };
            tail = head ? (tail.next = link) : (head = link);
            size++;
          },
          dequeue() {
            if (head) {
              var value = head.value;
              head = head.next;
              size--;
              return value;
            }
            return undefined;
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

      const maxWidth = 384;
      const maxHeight = 312;

      var worker;

      var AudioContext = window.AudioContext || window.webkitAudioContext;
      var audioContext;
      var nextTime, fix;

      var canvasContext;
      var imageData, data;
      var imageQueue = new Queue();
      var start, time;

      function wasmWorker(contents, tuneName, cartContents, cartName, command) {
        audioContext = new AudioContext({
          latencyHint: "interactive",
          sampleRate: 48000,
        });

        if (worker) {
          worker.terminate();
          worker = undefined;
        }
        worker = new Worker("${teaVMFormat}/jsidplay2-${teaVMFormat}-worker.js");

        return new Promise((resolve, reject) => {
          worker.postMessage({
            eventType: "INITIALISE",
            eventData: {
              palEmulation: app.palEmulation,
              bufferSize: app.bufferSize,
              audioBufferSize: app.audioBufferSize,
              samplingRate: audioContext.sampleRate,
              samplingMethodResample: "" + app.sampling === "true",
              reverbBypass: app.reverbBypass,
              defaultClockSpeed: app.defaultClockSpeed,
              defaultSidModel: "" + app.defaultSidModel === "true",
              jiffyDosInstalled: "" + app.jiffyDosInstalled === "true",
              defaultEmulation: app.defaultEmulation === "RESID",
            },
          });

          worker.addEventListener("message", function (event) {
            var { eventType, eventData } = event.data;

            if (eventType === "SAMPLES") {
              var buffer = audioContext.createBuffer(2, eventData.left.length, audioContext.sampleRate);
              buffer.getChannelData(0).set(eventData.left);
              buffer.getChannelData(1).set(eventData.right);

              var sourceNode = audioContext.createBufferSource();
              sourceNode.buffer = buffer;
              sourceNode.connect(audioContext.destination);

              if (nextTime == 0) {
                fix = app.screen ? 0.005 : 0;
                nextTime = audioContext.currentTime + 0.05; // add 50ms latency to work well across systems
              } else if (nextTime < audioContext.currentTime) {
                nextTime = audioContext.currentTime + 0.005; // if samples are not produced fast enough
              }
              sourceNode.start(nextTime);
              nextTime += eventData.left.length / audioContext.sampleRate + fix;
            } else if (eventType === "FRAME") {
              imageQueue.enqueue({
                image: eventData.image,
              });
            } else if (eventType === "SID_WRITE") {
              console.log("relTime=" + eventData.relTime + ", addr=" + eventData.addr + ", value=" + eventData.value);
            } else if (eventType === "OPENED" || eventType === "CLOCKED") {
              if (eventType === "OPENED") {
                if (app.screen) {
                  app.insertDisk();
                }
                if (app.screen) {
                  app.insertTape();
                }
              }
              if (!app.paused && size * app.nthFrame < 120) {
                worker.postMessage({ eventType: "CLOCK" });
              } else {
                worker.postMessage({ eventType: "IDLE" });
              }
              app.framesCounter = size;
            } else if (eventType === "INITIALISED") {
              worker.postMessage({
                eventType: "OPEN",
                eventData: {
                  contents: contents,
                  tuneName: tuneName,
                  startSong: app.startSong,
                  nthFrame: app.screen ? app.nthFrame : 0,
                  sidWrites: app.sidWrites,
                  cartContents: cartContents,
                  cartName: cartName,
                  command: command,
                },
              });

              nextTime = 0;
              imageQueue.clear();
              app.playing = true;
              app.paused = false;
              app.clearScreen();
              if (app.screen) {
                (start = new Date().getTime()), (time = 0);
                app.showFrame();
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
            FileMenu: "File",
            palEmulation: "PAL emulation",
            defaultClockSpeed: "Default clock speed",
            defaultEmulation: "Default Emulation",
            defaultSidModel: "Default SID model",
            jiffyDosInstalled: "JiffyDOS",
            sampling: "Sampling Method",
            reverbBypass: "Bypass Schroeder reverb",
            sidWrites: "Print SID writes to console",
            bufferSize: "Emulation buffer size",
            audioBufferSize: "Audio buffer size",
            startSong: "Start song",
            nthFrame: "Show every nth frame",
            file: "File",
            play: "Load SID/PRG/P00/T64",
            player: "Player",
            pauseContinue: "Pause/Continue",
            reset: "Reset C64",
            stop: "Stop",
            devices: "Devices",
            floppy: "Floppy",
            insertDisk: "Insert Disk",
            ejectDisk: "Eject Disk",
            tape: "Tape",
            insertTape: "Insert Tape",
            pressPlayOnTape: "Press Play on Tape",
            ejectTape: "Eject Tape",
            cart: "Cart",
            insertCart: "Insert Cartridge",
            ejectCart: "Eject Cartridge",
            diskInserted: "Disk inserted",
            diskEjected: "Disk ejected",
            tapeInserted: "Tape inserted",
            tapeEjected: "Tape ejected",
            cartInserted: "Cartridge inserted",
            cartEjected: "Cartridge ejected",
            loadDisk: "Load *,8,1",
            loadTape: "Load",
            space: "Space Key",
          },
          de: {
            FileMenu: "Datei",
            palEmulation: "PAL Emulation",
            defaultClockSpeed: "Default Clock Speed",
            defaultEmulation: "Default Emulation",
            defaultSidModel: "Default SID Model",
            jiffyDosInstalled: "JiffyDOS",
            sampling: "Sampling Methode",
            reverbBypass: "Schroeder Reverb überbrücken",
            sidWrites: "SID writes in Konsole schreiben",
            bufferSize: "Emulationspuffer Größe",
            audioBufferSize: "Audio Puffer Größe",
            startSong: "Start Song",
            nthFrame: "Zeige jedes Nte Bild",
            file: "Datei",
            play: "Lade SID/PRG/P00/T64",
            player: "Player",
            pauseContinue: "Pause/Continue",
            reset: "Reset C64",
            stop: "Stop",
            devices: "Geräte",
            floppy: "Floppy",
            insertDisk: "Diskette einlegen",
            ejectDisk: "Diskette auswerfen",
            tape: "Datasette",
            insertTape: "Kasette einlegen",
            pressPlayOnTape: "Drücke Play auf der Datasette",
            ejectTape: "Kasette auswerfen",
            cart: "Modul",
            insertCart: "Modul einlegen",
            ejectCart: "Modul auswerfen",
            diskInserted: "Diskette eingelegt",
            diskEjected: "Diskette ausgeworfen",
            tapeInserted: "Kasette eingelegt",
            tapeEjected: "Kasette ausgeworfen",
            cartInserted: "Modul eingesteckt",
            cartEjected: "Modul ausgeworfen",
            loadDisk: "Load *,8,1",
            loadTape: "Load",
            space: "Leertaste",
          },
        },
      });

      let app = Vue.createApp({
        data: function () {
          return {
            langs: ["de", "en"],
            msg: "",
            playing: false,
            paused: false,
            screen: true,
            palEmulation: true,
            defaultClockSpeed: 50,
            startSong: 0,
            nthFrame: 4,
            nthFrames: [1, 2, 4, 10, 25, 30, 50, 60],
            defaultEmulation: "RESID",
            defaultSidModel: true,
            jiffyDosInstalled: false,
            sampling: false,
            reverbBypass: true,
            sidWrites: false,
            bufferSize: 3 * 48000,
            audioBufferSize: 48000,
            framesCounter: 0,
            showFloppy: false,
            showTape: false,
            showCart: false,
          };
        },
        computed: {},
        methods: {
          updateLanguage() {
            localStorage.locale = this.$i18n.locale;
          },
          reset(command) {
            app.screen = true;
            app.stopTune();
            if (app.$refs.formCartFileSm.files[0]) {
              var reader = new FileReader();
              reader.onload = function () {
                wasmWorker(
                  undefined,
                  undefined,
                  new Uint8Array(this.result),
                  app.$refs.formCartFileSm.files[0].name,
                  command
                );
              };
              reader.readAsArrayBuffer(app.$refs.formCartFileSm.files[0]);
            } else {
              wasmWorker(undefined, undefined, undefined, undefined, command);
            }
          },
          startTune() {
            app.screen = false;
            app.stopTune();
            if (app.$refs.formFileSm.files[0]) {
              var reader = new FileReader();
              reader.onload = function () {
                wasmWorker(new Uint8Array(this.result), app.$refs.formFileSm.files[0].name);
              };
              reader.readAsArrayBuffer(app.$refs.formFileSm.files[0]);
            }
          },
          downloadAndStartTune(name, url) {
            let headers = new Headers();
            headers.set("Authorization", "Basic " + window.btoa("jsidplay2:jsidplay2!"));
            fetch(url, { method: "GET", headers: headers })
              .then((response) => response.blob())
              .then((blob) => {
                let file = new File([blob], name, {
                  type: "application/octet-stream",
                });
                const dataTransfer = new DataTransfer();
                dataTransfer.items.add(file);
                app.$refs.formFileSm.files = dataTransfer.files;
                app.startTune();
              });
          },
          pauseTune() {
            if (app.playing) {
              if (app.paused) {
                audioContext.resume();
                worker.postMessage({ eventType: "CLOCK" });
              } else {
                audioContext.suspend();
              }
              app.paused = !app.paused;
            } else if (app.$refs.formFileSm.files[0]) {
              app.startTune();
            } else {
              app.reset();
            }
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
            app.playing = false;
            app.paused = false;
          },
          clearScreen: function () {
            data.set(new Uint8Array((maxWidth * maxHeight) << 2));
            canvasContext.putImageData(imageData, 0, 0);
          },
          showFrame: function () {
            var timeSpan = (1000 * app.nthFrame) / (app.defaultClockSpeed - 1);
            time += timeSpan;
            if (!app.paused) {
              var elem = imageQueue.dequeue();
              if (elem) {
                data.set(elem.image);
                canvasContext.putImageData(imageData, 0, 0);
              }
            }
            var diff = new Date().getTime() - start - time;
            if (app.playing) setTimeout(() => app.showFrame(), timeSpan - diff);
          },
          insertDisk() {
            var reader = new FileReader();
            reader.onload = function () {
              if (worker) {
                worker.postMessage({
                  eventType: "INSERT_DISK",
                  eventData: {
                    contents: new Uint8Array(this.result),
                    diskName: app.$refs.formDiskFileSm.files[0].name,
                  },
                });
              }
              app.msg = app.$t("diskInserted") + ": " + app.$refs.formDiskFileSm.files[0].name;
            };
            if (app.$refs.formDiskFileSm && app.$refs.formDiskFileSm.files[0]) {
              reader.readAsArrayBuffer(app.$refs.formDiskFileSm.files[0]);
            }
          },
          downloadAndInsertDisk(name, url) {
            let headers = new Headers();
            headers.set("Authorization", "Basic " + window.btoa("jsidplay2:jsidplay2!"));
            fetch(url, { method: "GET", headers: headers })
              .then((response) => response.blob())
              .then((blob) => {
                let file = new File([blob], name, {
                  type: "application/octet-stream",
                });
                const dataTransfer = new DataTransfer();
                dataTransfer.items.add(file);
                app.$refs.formDiskFileSm.files = dataTransfer.files;
                if (app.playing && app.screen) {
                  app.insertDisk();
                } else {
                  app.reset('LOAD"*",8,1\rRUN\r');
                }
              });
          },
          ejectDisk() {
            if (worker) {
              worker.postMessage({
                eventType: "EJECT_DISK",
              });
            }
            app.$refs.formDiskFileSm.value = "";
            app.msg = app.$t("diskEjected");
          },
          insertTape() {
            var reader = new FileReader();
            reader.onload = function () {
              if (worker) {
                worker.postMessage({
                  eventType: "INSERT_TAPE",
                  eventData: {
                    contents: new Uint8Array(this.result),
                    tapeName: app.$refs.formTapeFileSm.files[0].name,
                  },
                });
              }
              app.msg = app.$t("tapeInserted") + ": " + app.$refs.formTapeFileSm.files[0].name;
            };
            if (app.$refs.formTapeFileSm && app.$refs.formTapeFileSm.files[0]) {
              reader.readAsArrayBuffer(app.$refs.formTapeFileSm.files[0]);
            }
          },
          ejectTape() {
            if (worker) {
              worker.postMessage({
                eventType: "EJECT_TAPE",
              });
            }
            app.$refs.formTapeFileSm.value = "";
            app.msg = app.$t("tapeEjected");
          },
          pressPlayOnTape() {
            if (worker) {
              worker.postMessage({
                eventType: "PRESS_PLAY_ON_TAPE",
                eventData: {},
              });
            }
          },
          typeInCommand(command) {
            if (worker) {
              worker.postMessage({
                eventType: "SET_COMMAND",
                eventData: {
                  command: command,
                },
              });
            }
          },
          typeKey(key) {
            if (worker) {
              worker.postMessage({
                eventType: "TYPE_KEY",
                eventData: {
                  key: key,
                },
              });
            }
          },
          insertCart() {
            app.msg = app.$t("cartInserted") + ": " + app.$refs.formCartFileSm.files[0].name;
            app.reset();
          },
          ejectCart() {
            app.$refs.formCartFileSm.value = "";
            app.msg = app.$t("cartEjected");
            app.reset();
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

      var noSleep = new NoSleep();

      var wakeLockEnabled = false;
      var toggleEl = document.querySelector("#toggle");
      toggleEl.addEventListener(
        "click",
        function () {
          if (!wakeLockEnabled) {
            noSleep.enable(); // keep the screen on!
            wakeLockEnabled = true;
            toggleEl.value = "Wake Lock is enabled";
            document.body.style.backgroundColor = "lightblue";
          } else {
            noSleep.disable(); // let the screen turn off.
            wakeLockEnabled = false;
            toggleEl.value = "Wake Lock is disabled";
            document.body.style.backgroundColor = "";
          }
        },
        false
      );
    </script>
  </body>
</html>
