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

    <title>C64 Jukebox (${teaVMFormatName})</title>
  </head>
  <body>
    <div id="app">
      <form enctype="multipart/form-data">
        <div class="locale-changer">
          <h1 class="c64jukebox" style="width: 100%">C64 Jukebox (${teaVMFormatName})</h1>
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

        <nav class="navbar navbar-expand navbar-dark bg-primary p-0">
          <div class="container-fluid">
            <div class="collapse navbar-collapse" id="main_nav">
              <ul class="navbar-nav">
                <li class="nav-item dropdown" id="myDropdown" style="margin-right: 16px">
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
                <li class="nav-item dropdown" id="myDropdown2" style="margin-right: 16px">
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
                <li class="nav-item dropdown" id="myDropdown3" style="margin-right: 16px">
                  <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">{{ $t("devices") }}</a>
                  <ul class="dropdown-menu" style="width: 160px !important">
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showFloppy = !showFloppy;
                          showTape = false;
                          showCart = false;
                        "
                        >{{ $t("floppy") }}&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showFloppy
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
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
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showTape = !showTape;
                          showFloppy = false;
                          showCart = false;
                        "
                        >{{ $t("tape") }}&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showTape
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
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
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showCart = !showCart;
                          showTape = false;
                          showFloppy = false;
                        "
                        >{{ $t("cart") }}&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showCart
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
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

        <nav class="navbar navbar-expand navbar-dark bg-primary p-0">
          <div class="container-fluid">
            <div class="collapse navbar-collapse" id="main_nav">
              <ul class="navbar-nav">
                <li class="nav-item dropdown" id="myDropdown4" style="margin-right: 16px">
                  <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">{{ $t("exampleMusic") }}</a>
                  <ul class="dropdown-menu" style="width: 480px !important">
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          downloadAndStartTune(
                            'Turrican Rise Of the Mashine',
                            '/jsidplay2service/JSIDPlay2REST/download/turrican_rotm.sid?itemId=189430&categoryId=4'
                          )
                        "
                        >Chris Huelsbeck &amp; Jason Page - Turrican Rise Of the Mashine</a
                      >
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          downloadAndStartTune(
                            'Only 299.99',
                            '/jsidplay2service/JSIDPlay2REST/download/Only_299_99.sid?itemId=3470375608&categoryId=18'
                          )
                        "
                        >Mutetus - Only 299.99</a
                      >
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          downloadAndStartTune(
                            'Banaanin Alle',
                            '/jsidplay2service/JSIDPlay2REST/download/mutetus_banaaninalle.sid?itemId=209406&categoryId=4'
                          )
                        "
                      >
                        Mutetus - Banaanin Alle
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          downloadAndStartTune(
                            'Rocco_Siffredi_Invades_1541_II.sid',
                            '/jsidplay2service/JSIDPlay2REST/download/Rocco_Siffredi_Invades_1541_II.sid?itemId=73719&categoryId=4'
                          )
                        "
                      >
                        Jammer - Rocco Siffredi Invades 1541II
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          downloadAndStartTune(
                            'Hi Fi Sky',
                            '/jsidplay2service/JSIDPlay2REST/download/Hi_Fi_Sky.sid?itemId=4064310083&categoryId=18'
                          )
                        "
                      >
                        LMan - Hi Fi Sky
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          downloadAndStartTune(
                            'blindsided',
                            '/jsidplay2service/JSIDPlay2REST/download/blindsided.sid?itemId=239345&categoryId=4'
                          )
                        "
                      >
                        Stinsen - blindsided
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          downloadAndStartTune(
                            'running_up_that_hill.sid',
                            '/jsidplay2service/JSIDPlay2REST/download/running_up_that_hill.sid?itemId=238798&categoryId=4'
                          )
                        "
                      >
                        Slaxx, Nordischsound - Running Up That Hill
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          downloadAndStartTune(
                            'stinsen_last_night_of_89.sid',
                            '/jsidplay2service/JSIDPlay2REST/download/stinsen_last_night_of_89.sid?itemId=201399&categoryId=4'
                          )
                        "
                      >
                        Bonzai - Stinsens Last Night of 89
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          downloadAndStartTune(
                            'generations.sid',
                            '/jsidplay2service/JSIDPlay2REST/download/generations.sid?itemId=242010&categoryId=4'
                          )
                        "
                      >
                        Flotsam - Generations
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          downloadAndStartTune(
                            'Cauldron_II_Sinus_Milieu_Studie.sid',
                            '/jsidplay2service/JSIDPlay2REST/download/Cauldron_II_Sinus_Milieu_Studie.sid?itemId=61763&categoryId=4'
                          )
                        "
                      >
                        Viruz - Cauldron II Sinus Milieu Studie
                      </a>
                    </li>
                  </ul>
                </li>
                <li class="nav-item dropdown" id="myDropdow5" style="margin-right: 16px">
                  <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">{{ $t("exampleOneFiler") }}</a>
                  <ul class="dropdown-menu" style="width: 480px !important">
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          stopTune();
                          downloadAndStartProgram(
                            'fppscroller.prg',
                            '/jsidplay2service/JSIDPlay2REST/download/fppscroller.prg?itemId=230558&categoryId=1'
                          );
                        "
                      >
                        Booze Design - Party Elk 2
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          stopTune();
                          downloadAndStartProgram(
                            'copperbooze.prg',
                            '/jsidplay2service/JSIDPlay2REST/download/copperbooze.prg?itemId=197429&categoryId=1'
                          );
                        "
                      >
                        Booze Design - Copper Booze
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          stopTune();
                          downloadAndStartProgram(
                            'foryourspritesonly.prg',
                            '/jsidplay2service/JSIDPlay2REST/download/foryourspritesonly.prg?itemId=198971&categoryId=1'
                          );
                        "
                      >
                        Booze Design - For Your Sprites Only
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          stopTune();
                          downloadAndStartProgram(
                            'layers.prg',
                            '/jsidplay2service/JSIDPlay2REST/download/layers.prg?itemId=242834&categoryId=1'
                          );
                        "
                      >
                        Finnish Gold - Layers
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          stopTune();
                          downloadAndStartProgram(
                            'atl-lovecats.prg',
                            '/jsidplay2service/JSIDPlay2REST/download/atl-lovecats.prg?itemId=198558&categoryId=1'
                          );
                        "
                      >
                        Atlantis - Lovecats
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          stopTune();
                          downloadAndStartProgram(
                            'smile-to-the-sky.prg',
                            '/jsidplay2service/JSIDPlay2REST/download/smile-to-the-sky.prg?itemId=172574&categoryId=1'
                          );
                        "
                      >
                        Offence - Smile to the Sky
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          stopTune();
                          downloadAndStartProgram(
                            'Comajob.t64',
                            '/jsidplay2service/JSIDPlay2REST/download/Comajob.t64?itemId=11653&categoryId=1'
                          );
                        "
                      >
                        Crest,Oxyron - Coma Job
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          stopTune();
                          downloadAndStartProgram(
                            'X12Demo.t64',
                            '/jsidplay2service/JSIDPlay2REST/download/X12Demo.t64?itemId=112416&categoryId=1'
                          );
                        "
                      >
                        Abnormal - X2012
                      </a>
                    </li>
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        @click="
                          stopTune();
                          downloadAndStartProgram(
                            'whitelines2bh.prg',
                            '/jsidplay2service/JSIDPlay2REST/download/whitelines2bh.prg?itemId=232984&categoryId=1'
                          );
                        "
                      >
                        Plush - White Lines
                      </a>
                    </li>
                  </ul>
                </li>
                <li class="nav-item dropdown" id="myDropdown6" style="margin-right: 16px">
                  <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">{{ $t("exampleDemos") }}</a>

                  <ul class="dropdown-menu" style="width: 340px !important">
                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showDemo1 = !showDemo1;
                          showDemo2 =
                            showDemo3 =
                            showDemo4 =
                            showDemo5 =
                            showDemo6 =
                            showDemo7 =
                            showDemo8 =
                            showDemo9 =
                            showDemo10 =
                              false;
                        "
                        >1337&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showDemo1
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              stopTune();
                              downloadAndInsertDisk(
                                '1337-a',
                                '/jsidplay2service/JSIDPlay2REST/download/fairlight-1337-58679b69-a.d64?itemId=242855&categoryId=1'
                              );
                            "
                          >
                            Autostart
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                '1337-a',
                                '/jsidplay2service/JSIDPlay2REST/download/fairlight-1337-58679b69-a.d64?itemId=242855&categoryId=1'
                              )
                            "
                          >
                            Disk 1
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                '1337-b',
                                '/jsidplay2service/JSIDPlay2REST/download/fairlight-1337-58679b69-b.d64?itemId=242855&categoryId=1'
                              )
                            "
                          >
                            Disk 2
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                '1337-c',
                                '/jsidplay2service/JSIDPlay2REST/download/fairlight-1337-58679b69-c.d64?itemId=242855&categoryId=1'
                              )
                            "
                          >
                            Disk 3
                          </a>
                        </li>
                      </ul>
                    </li>

                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showDemo2 = !showDemo2;
                          showDemo1 =
                            showDemo3 =
                            showDemo4 =
                            showDemo5 =
                            showDemo6 =
                            showDemo7 =
                            showDemo8 =
                            showDemo9 =
                            showDemo10 =
                              false;
                        "
                        >Next Level&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showDemo2
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              stopTune();
                              downloadAndInsertDisk(
                                'NextLevelImage1.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/image1.d64?itemId=232976&categoryId=1'
                              );
                            "
                          >
                            Autostart
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'NextLevelImage1.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/image1.d64?itemId=232976&categoryId=1'
                              )
                            "
                          >
                            Disk 1
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'NextLevelImage2.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/image2.d64?itemId=232976&categoryId=1'
                              )
                            "
                          >
                            Disk 2
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'NextLevelImage3.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/image3.d64?itemId=232976&categoryId=1'
                              )
                            "
                          >
                            Disk 3
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'NextLevelImage4.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/image4.d64?itemId=232976&categoryId=1'
                              )
                            "
                          >
                            Disk 4
                          </a>
                        </li>
                      </ul>
                    </li>

                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showDemo3 = !showDemo3;
                          showDemo1 =
                            showDemo2 =
                            showDemo4 =
                            showDemo5 =
                            showDemo6 =
                            showDemo7 =
                            showDemo8 =
                            showDemo9 =
                            showDemo10 =
                              false;
                        "
                        >Mojo&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showDemo3
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              stopTune();
                              downloadAndInsertDisk(
                                'Mojo_Side1.D64',
                                '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side1.D64?itemId=232966&categoryId=1'
                              );
                            "
                          >
                            Autostart
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'Mojo_Side1.D64',
                                '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side1.D64?itemId=232966&categoryId=1'
                              )
                            "
                          >
                            Disk 1
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'Mojo_Side2.D64',
                                '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side2.D64?itemId=232966&categoryId=1'
                              )
                            "
                          >
                            Disk 2
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'Mojo_Side3.D64',
                                '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side3.D64?itemId=232966&categoryId=1'
                              )
                            "
                          >
                            Disk 3
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'Mojo_Side4.D64',
                                '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side4.D64?itemId=232966&categoryId=1'
                              )
                            "
                          >
                            Disk 4
                          </a>
                        </li>
                      </ul>
                    </li>

                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showDemo4 = !showDemo4;
                          showDemo1 =
                            showDemo2 =
                            showDemo3 =
                            showDemo5 =
                            showDemo6 =
                            showDemo7 =
                            showDemo8 =
                            showDemo9 =
                            showDemo10 =
                              false;
                        "
                        >Coma Light 13&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showDemo4
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              stopTune();
                              downloadAndInsertDisk(
                                'ComaLight13Side1',
                                '/jsidplay2service/JSIDPlay2REST/download/coma-light-13-by-oxyron/side1.d64?itemId=112378&categoryId=1'
                              );
                            "
                          >
                            Autostart
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'ComaLight13Side1',
                                '/jsidplay2service/JSIDPlay2REST/download/coma-light-13-by-oxyron/side1.d64?itemId=112378&categoryId=1'
                              )
                            "
                          >
                            Disk 1
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'ComaLight13Side2',
                                '/jsidplay2service/JSIDPlay2REST/download/coma-light-13-by-oxyron/side2.d64?itemId=112378&categoryId=1'
                              )
                            "
                          >
                            Disk 2
                          </a>
                        </li>
                      </ul>
                    </li>

                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showDemo5 = !showDemo5;
                          showDemo1 =
                            showDemo2 =
                            showDemo3 =
                            showDemo4 =
                            showDemo6 =
                            showDemo7 =
                            showDemo8 =
                            showDemo9 =
                            showDemo10 =
                              false;
                        "
                        >Andropolis&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showDemo5
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              stopTune();
                              downloadAndInsertDisk(
                                'Andropolis.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/Instinct%20BoozeDesign%20-%20Andropolis.d64?itemId=81157&categoryId=1'
                              );
                            "
                          >
                            Autostart
                          </a>
                        </li>
                      </ul>
                    </li>

                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showDemo6 = !showDemo6;
                          showDemo1 =
                            showDemo2 =
                            showDemo3 =
                            showDemo4 =
                            showDemo5 =
                            showDemo7 =
                            showDemo8 =
                            showDemo9 =
                            showDemo10 =
                              false;
                        "
                        >Comaland&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showDemo6
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              stopTune();
                              downloadAndInsertDisk(
                                'ComalandImage1.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/image1.d64?itemId=139278&categoryId=1'
                              );
                            "
                          >
                            Autostart
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'ComalandImage1.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/image1.d64?itemId=139278&categoryId=1'
                              )
                            "
                          >
                            Disk 1
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'Mojo_Side2.D64',
                                '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side2.D64?itemId=232966&categoryId=1'
                              )
                            "
                          >
                            Disk 2
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'Mojo_Side3.D64',
                                '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side3.D64?itemId=232966&categoryId=1'
                              )
                            "
                          >
                            Disk 3
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'Mojo_Side4.D64',
                                '/jsidplay2service/JSIDPlay2REST/download/Mojo_Side4.D64?itemId=232966&categoryId=1'
                              )
                            "
                          >
                            Disk 4
                          </a>
                        </li>
                      </ul>
                    </li>

                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showDemo7 = !showDemo7;
                          showDemo1 =
                            showDemo2 =
                            showDemo3 =
                            showDemo4 =
                            showDemo5 =
                            showDemo6 =
                            showDemo8 =
                            showDemo9 =
                            showDemo10 =
                              false;
                        "
                        >Edge Of Disgrace&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showDemo7
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              stopTune();
                              downloadAndInsertDisk(
                                'EdgeOfDisgrace_0.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/EdgeOfDisgrace_0.d64?itemId=72550&categoryId=1'
                              );
                            "
                          >
                            Autostart
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'EdgeOfDisgrace_0.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/EdgeOfDisgrace_0.d64?itemId=72550&categoryId=1'
                              )
                            "
                          >
                            Disk 0
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'EdgeOfDisgrace_1a.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/EdgeOfDisgrace_1a.d64?itemId=72550&categoryId=1'
                              )
                            "
                          >
                            Disk 1a
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'EdgeOfDisgrace_1b.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/EdgeOfDisgrace_1b.d64?itemId=72550&categoryId=1'
                              )
                            "
                          >
                            Disk 1b
                          </a>
                        </li>
                      </ul>
                    </li>

                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showDemo8 = !showDemo8;
                          showDemo1 =
                            showDemo2 =
                            showDemo3 =
                            showDemo4 =
                            showDemo5 =
                            showDemo6 =
                            showDemo7 =
                            showDemo9 =
                            showDemo10 =
                              false;
                        "
                        >E2IRA&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showDemo8
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              stopTune();
                              downloadAndInsertDisk(
                                'e2ira_101_A.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/e2ira_101_A.d64?itemId=218343&categoryId=1'
                              );
                            "
                          >
                            Autostart
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'e2ira_101_A.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/e2ira_101_A.d64?itemId=218343&categoryId=1'
                              )
                            "
                          >
                            Disk 1
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'e2ira_101_B.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/e2ira_101_B.d64?itemId=218343&categoryId=1'
                              )
                            "
                          >
                            Disk 2
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'e2ira_101_C.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/e2ira_101_C.d64?itemId=218343&categoryId=1'
                              )
                            "
                          >
                            Disk 3
                          </a>
                        </li>
                      </ul>
                    </li>

                    <li>
                      <a
                        class="dropdown-item"
                        href="#"
                        v-on:click.stop="
                          showDemo9 = !showDemo9;
                          showDemo1 =
                            showDemo2 =
                            showDemo3 =
                            showDemo4 =
                            showDemo5 =
                            showDemo6 =
                            showDemo7 =
                            showDemo8 =
                            showDemo10 =
                              false;
                        "
                        >Partypopper&raquo;
                      </a>
                      <ul
                        class="submenu dropdown-menu"
                        :style="
                          showDemo9
                            ? 'display: block !important; left: auto; right: 100% !important;'
                            : 'left: auto; right: 100% !important;'
                        "
                      >
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              stopTune();
                              downloadAndInsertDisk(
                                'Partypopper-Disk1.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/Partypopper-Disk1.d64?itemId=216277&categoryId=1'
                              );
                            "
                          >
                            Autostart
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'Partypopper-Disk1.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/Partypopper-Disk1.d64?itemId=216277&categoryId=1'
                              )
                            "
                          >
                            Disk 1
                          </a>
                        </li>
                        <li>
                          <a
                            class="dropdown-item"
                            href="#"
                            @click="
                              downloadAndInsertDisk(
                                'Partypopper-Disk2.d64',
                                '/jsidplay2service/JSIDPlay2REST/download/Partypopper-Disk2.d64?itemId=216277&categoryId=1'
                              )
                            "
                          >
                            Disk 2
                          </a>
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
                class="btn btn-secondary btn-sm"
                v-on:click="typeInCommand('LOAD&quot;*&quot;,8,1\rRUN\r')"
                :disabled="!$refs.formDiskFileSm || !$refs.formDiskFileSm.files[0] || !playing || !screen"
              >
                {{ $t("loadDisk") }}
              </button>
              <button
                type="button"
                class="btn btn-secondary btn-sm"
                v-on:click="typeInCommand('LOAD\rRUN\r')"
                :disabled="!$refs.formTapeFileSm || !$refs.formTapeFileSm.files[0] || !playing || !screen"
              >
                {{ $t("loadTape") }}
              </button>
              <button
                type="button"
                class="btn btn-secondary btn-sm"
                v-on:click="typeKey('SPACE')"
                :disabled="!playing || !screen"
              >
                {{ $t("space") }}
              </button>
              <input
                type="button"
                :class="wakeLockEnable ? 'btn btn-primary btn-sm' : 'btn btn-secondary btn-sm'"
                id="toggle"
                value="Wake Lock Off"
              />
              <span class="p-1 fs-6 fst-italic"
                >{{ msg }} <span v-show="playing"> / {{ $t("fiq") }} {{ framesCounter }}</span></span
              >
            </div>
          </div>

          <div class="row">
            <div v-show="screen" class="col screen-parent p-0">
              <div style="width: 100%; margin: 0px auto">
                <canvas
                  id="c64Screen"
                  style="border: 2px solid black; background-color: black; max-width: 100vw"
                  width="384"
                  height="285"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="container">
          <div class="row">
            <div class="col">
              <h2>${teaVMFormatName} Version powered by <a href="https://teavm.org/" target="_blank">TeaVM</a></h2>
              <ol>
                <li>
                  Run JSIDPlay2 in a browser in <a href="/static/teavm/c64jukebox.vue?teavmFormat=JS">JavaScript</a> or
                  <a href="/static/teavm/c64jukebox.vue?teavmFormat=WASM">Web Assembly</a> (THIS IS NOT JAVA)
                </li>
                <li>Runs out-of-the-box in all browsers (Chrome is faster than Firefox)</li>
                <li>Only ${teaVMFormatApproximateSize} in size, loads very quick</li>
                <li>Compatible with all SIDs (mono, stereo and 3-SID)</li>
                <li>Plays mono SIDs and ONEfilers on a middle class mobile phone and multi-disk demos on PC</li>
                <li>Runs near to native speed, performance only depends on your max. single core speed</li>
                <li>Runs completely on the client side in a web worker (once in browser's cache)</li>
                <li>Full emulation quality, no compromises, C64, Floppy and more</li>
                <li>
                  Developed single source in JSIDPlay2 project, enhancements are automatically available in all versions
                </li>
                <li>For the first time, embed music or demos in YOUR web-site</li>
              </ol>
              <p style="text-align: center; font-size: smaller; padding: 16px">
                C64 Jukebox of JSIDPlay2 - Music Player &amp; C64 SID Chip Emulator<br />
                JSIDPlay2 is copyrighted to:<br />
                2007-${year} Ken H&#228;ndel,<br />
                Antti S. Lankila and Wilfred Bos<br /><br />
                Distortion Simulation and 6581/8580 emulation:<br />
                Copyright &#169; 2005-2011 Antti S. Lankila<br />
                ReSID engine and 6581/8580 emulation:<br />
                Copyright &#169; 1999-2011 Dag Lem<br />
                Source code of JSIDPlay2 and the built-in AppServer can be found at:<br />
                <a href="https://sourceforge.net/projects/jsidplay2" target="_blank"
                  >https://sourceforge.net/projects/jsidplay2</a
                ><br /><br />
                The search function of this web-site is powered by Assembly64 by Fredrik &Aring;berg.<br />
                <a href="https://hackerswithstyle.se/assembly/" target="_blank"
                  >http://hackerswithstyle.ddns.net/assembly/</a
                ><br />
                Thank you mate!<br /><br />
                This program is free software; you can redistribute it and/or modify<br />
                it under the terms of the GNU General Public License as published by<br />
                the Free Software Foundation; either version 2 of the License, or<br />
                (at your option) any later version.
              </p>
            </div>
            <div v-show="!playing" class="col">
              <div class="form-check">
                <div class="settings-box">
                  <span class="setting">
                    <h2 style="text-align: end; margin-right: 1rem">{{ $t("settings") }}</h2>
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
              if (!app.paused && size * app.nthFrame < 60) {
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
            exampleMusic: "Music",
            exampleOneFiler: "OneFiler",
            exampleDemos: "Demos",
            settings: "Settings",
            fiq: "FIQ",
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
            exampleMusic: "Musik",
            exampleOneFiler: "Programme",
            exampleDemos: "Demos",
            settings: "Einstellungen",
            fiq: "FIQ",
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
            nthFrame: 2,
            nthFrames: [1, 2, 4, 10, 25, 30, 50, 60],
            defaultEmulation: "RESIDFP",
            defaultSidModel: true,
            jiffyDosInstalled: false,
            sampling: false,
            reverbBypass: true,
            sidWrites: false,
            bufferSize: 3 * 48000,
            audioBufferSize: 48000,
            framesCounter: 0,
            showFloppy: false,
            showDemo1: false,
            showDemo2: false,
            showDemo3: false,
            showDemo4: false,
            showDemo5: false,
            showDemo6: false,
            showDemo7: false,
            showDemo8: false,
            showDemo9: false,
            showDemo10: false,
            showTape: false,
            showCart: false,
            wakeLockEnable: false,
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
          startTune(screen) {
            app.screen = screen ? screen : false;
            app.stopTune();
            if (app.$refs.formFileSm.files[0]) {
              var reader = new FileReader();
              reader.onload = function () {
                wasmWorker(new Uint8Array(this.result), app.$refs.formFileSm.files[0].name);
              };
              reader.readAsArrayBuffer(app.$refs.formFileSm.files[0]);
            }
          },
          downloadAndStartTune(name, url, screen) {
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
                app.startTune(screen);
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
          downloadAndStartProgram(name, url) {
            app.ejectTape();
            app.ejectDisk();
            app.downloadAndStartTune(name, url, true);
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
            toggleEl.value = "Wake Lock On";
            document.body.style.backgroundColor = "lightblue";
            app.wakeLockEnable = true;
          } else {
            noSleep.disable(); // let the screen turn off.
            wakeLockEnabled = false;
            toggleEl.value = "Wake Lock Off";
            document.body.style.backgroundColor = "";
            app.wakeLockEnable = false;
          }
        },
        false
      );
    </script>
  </body>
</html>
