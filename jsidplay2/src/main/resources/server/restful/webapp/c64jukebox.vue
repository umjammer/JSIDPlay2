<!doctype html>
<html>
  <head>
    <style lang="scss" scoped>
      @import "/static/c64jukebox.scss";
    </style>

    <!-- favicon.ico -->
    <link rel="shortcut icon" href="/static/favicon.ico" type="image/x-icon" />
    <link id="favicon" rel="icon" href="/static/favicon.ico" type="image/x-icon" />
    <link id="favicon-16x16" rel="icon" href="/static/favicon-16x16.png" type="image/png" sizes="16x16" />

    <!-- Load required Bootstrap, BootstrapVue and Icons CSS -->
    <link type="text/css" rel="stylesheet" href="/static/bootstrap@4.5.3/dist/css/bootstrap.min.css" />
    <link type="text/css" rel="stylesheet" href="/static/bootstrap-vue@2.22.0/dist/bootstrap-vue.min.css" />
    <link type="text/css" rel="stylesheet" href="/static/bootstrap-vue@2.22.0/dist/bootstrap-vue-icons.min.css" />

    <!-- Load Vue followed by BootstrapVue and Icons -->
    <script src="/static/vue@2.6.14/dist/vue.min.js"></script>
    <script src="/static/vue-router@3.5.4/dist/vue-router.min.js"></script>
    <script src="/static/bootstrap-vue@2.22.0/dist/bootstrap-vue.min.js"></script>
    <script src="/static/bootstrap-vue@2.22.0/dist/bootstrap-vue-icons.min.js"></script>

    <!-- helpers -->
    <script src="/static/vue-i18n@8.27.2/dist/vue-i18n.min.js"></script>
    <script src="/static/axios@0.27.2/dist/axios.min.js"></script>

    <!-- USB -->
    <script src="/static/usb/hardsid.js"></script>
    <script src="/static/usb/libftdi.js"></script>
    <script src="/static/usb/exsid.js"></script>
    <script src="/static/usb/sidblaster.js"></script>

    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />

    <title>C64 Jukebox</title>
  </head>
  <body>
    <div id="app">
      <b-form>
        <div class="locale-changer">
          <select @change="updateLanguage" v-model="$i18n.locale" style="float: right">
            <option v-for="(lang, i) in langs" :key="`Lang${i}`" :value="lang">
              {{ lang }}
            </option>
          </select>
          <h1 class="c64jukebox">C64 Jukebox</h1>
        </div>
        <div class="audio" style="position: relative; text-align: center; background: white">
          <audio ref="audioElm" v-show="showAudio" v-on:ended="setNextPlaylistEntry" type="audio/mpeg" controls>
            I'm sorry. Your browser doesn't support HTML5 audio
          </audio>
          <div v-show="deviceCount > 0">
            <b-button
              size="sm"
              variant="success"
              v-on:click="
                play(
                  '',
                  playlist[playlistIndex].filename,
                  playlist[playlistIndex].itemId,
                  playlist[playlistIndex].categoryId
                )
              "
            >
              <b-icon-play-fill> </b-icon-play-fill>
              <span>Play using Hardware</span>
            </b-button>
            <b-button variant="success" size="sm" v-on:click="stop()">
              <b-icon-stop-fill> </b-icon-stop-fill>
              <span>Stop Hardware Player</span>
            </b-button>
            <b-button size="sm" variant="danger" v-on:click="end()">
              <span>Quit Hardware Player</span>
            </b-button>
          </div>
          <div style="position: absolute; bottom: 5px; left: 0px; line-height: 0.7">
            <span class="current-sid"
              ><span v-if="currentSid">{{ $t("currentlyPlaying") }}</span> <span>{{ currentSid }}</span></span
            >
          </div>
        </div>
        <div>
          <b-card no-body>
            <b-tabs v-model="tabIndex" active-nav-item-class="font-weight-bold text-italic" pills card>
              <b-tab>
                <template #title>{{ $t("CON") }}</template>

                <b-card-text>
                  <div class="settings-box">
                    <div class="button-box">
                      <b-button v-b-modal.modal-set-default-user size="sm" variant="outline-success">
                        <span>{{ $t("setDefaultUser") }}</span></b-button
                      >
                      <b-modal id="modal-set-default-user" :title="$t('confirmationTitle')" @ok="setDefaultUser">
                        <p>{{ $t("setDefaultUserReally") }}</p>
                      </b-modal>
                    </div>
                  </div>
                  <div class="settings-box">
                    <span class="setting">
                      <label for="username"
                        >{{ $t("username") }}
                        <b-form-input
                          type="text"
                          id="username"
                          class="right"
                          v-model="username"
                          autocomplete="off"
                          autocorrect="off"
                          autocapitalize="off"
                          spellcheck="false"
                      /></label>
                    </span>
                  </div>
                  <div class="settings-box">
                    <span class="setting">
                      <label for="password"
                        >{{ $t("password") }}
                        <b-form-input
                          type="password"
                          id="password"
                          class="right"
                          v-model="password"
                          autocomplete="off"
                          autocorrect="off"
                          autocapitalize="off"
                          spellcheck="false"
                          v-on:blur="fetchDirectory(rootDir)"
                      /></label>
                    </span>
                  </div>
                  <p style="text-align: center; font-size: smaller; padding: 16px">
                    C64 Jukebox of JSIDPlay2 - Music Player &amp; C64 SID Chip Emulator<br />
                    JSIDPlay2 is copyrighted to:<br />
                    2007-
                    <script type="text/javascript">
                      document.write(new Date().getFullYear());
                    </script>
                    Ken H&#228;ndel,<br />
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
                </b-card-text>
              </b-tab>
              <b-tab active style="position: relative">
                <template #title>
                  {{ $t("SIDS") }}
                  <b-spinner
                    type="border"
                    :variant="tabIndex == 1 ? 'light' : 'primary'"
                    small
                    v-if="
                      rootDir.loading ||
                      top200Dir.loading ||
                      oneFilerTop200Dir.loading ||
                      toolsTop200Dir.loading ||
                      musicTop200Dir.loading ||
                      graphicsTop200Dir.loading ||
                      gamesTop200Dir.loading
                    "
                  ></b-spinner>
                </template>

                <b-button size="sm" variant="success" v-on:click="fetchDirectory(rootDir)">
                  <b-icon-house-door-fill> </b-icon-house-door-fill>
                </b-button>

                <span
                  style="
                    font-style: italic;
                    padding: 2px 4px;
                    position: absolute;
                    top: 0px;
                    right: 304px;
                    z-index: 9999;
                  "
                  >{{ $t("filter") }}</span
                >

                <b-button
                  size="sm"
                  style="
                    font-size: smaller;
                    padding: 2px 4px;
                    position: absolute;
                    top: 0px;
                    right: 224px;
                    z-index: 9999;
                  "
                  variant="secondary"
                  v-on:click="fetchDirectory(top200Dir)"
                >
                  <b-icon-filter-circle-fill></b-icon-filter-circle-fill>
                  <span>{{ $t("top200") }}</span>
                </b-button>

                <b-button
                  size="sm"
                  style="
                    font-size: smaller;
                    padding: 2px 4px;
                    position: absolute;
                    top: 0px;
                    right: 140px;
                    z-index: 9999;
                  "
                  variant="secondary"
                  v-on:click="fetchDirectory(oneFilerTop200Dir)"
                >
                  <b-icon-filter-circle-fill> </b-icon-filter-circle-fill>
                  <span>{{ $t("onefilerTop200") }}</span>
                </b-button>

                <b-button
                  size="sm"
                  style="font-size: smaller; padding: 2px 4px; position: absolute; top: 0px; right: 70px; z-index: 9999"
                  variant="secondary"
                  v-on:click="fetchDirectory(toolsTop200Dir)"
                >
                  <b-icon-filter-circle-fill> </b-icon-filter-circle-fill>
                  <span>{{ $t("toolsTop100") }}</span>
                </b-button>

                <b-button
                  size="sm"
                  style="
                    font-size: smaller;
                    padding: 2px 4px;
                    position: absolute;
                    top: 30px;
                    right: 240px;
                    z-index: 9999;
                  "
                  variant="secondary"
                  v-on:click="fetchDirectory(musicTop200Dir)"
                >
                  <b-icon-filter-circle-fill></b-icon-filter-circle-fill>
                  <span>{{ $t("musicTop200") }}</span>
                </b-button>

                <b-button
                  size="sm"
                  style="
                    font-size: smaller;
                    padding: 2px 4px;
                    position: absolute;
                    top: 30px;
                    right: 150px;
                    z-index: 9999;
                  "
                  variant="secondary"
                  v-on:click="fetchDirectory(graphicsTop200Dir)"
                >
                  <b-icon-filter-circle-fill> </b-icon-filter-circle-fill>
                  <span>{{ $t("graphicsTop200") }}</span>
                </b-button>

                <b-button
                  size="sm"
                  style="
                    font-size: smaller;
                    padding: 2px 4px;
                    position: absolute;
                    top: 30px;
                    right: 70px;
                    z-index: 9999;
                  "
                  variant="secondary"
                  v-on:click="fetchDirectory(gamesTop200Dir)"
                >
                  <b-icon-filter-circle-fill> </b-icon-filter-circle-fill>
                  <span>{{ $t("gamesTop200") }}</span>
                </b-button>

                <b-button
                  size="sm"
                  style="
                    font-size: smaller;
                    padding: 2px 4px;
                    position: absolute;
                    top: 30px;
                    right: 16px;
                    z-index: 9999;
                  "
                  variant="primary"
                  v-show="directory.filter((entry) => isMusic(entry)).length > 0"
                  v-on:click="
                    directory
                      .filter((entry) => isMusic(entry))
                      .forEach((entry) =>
                        playlist.push({
                          filename: entry.filename,
                        })
                      );
                    tabIndex = 5;
                    showAudio = true;
                  "
                >
                  <b-icon-plus> </b-icon-plus>
                  <span>{{ $t("addAllToPlaylist") }}</span>
                </b-button>

                <div style="height: 4px"></div>

                <b-card-text>
                  <b-list-group>
                    <div v-for="entry in directory" :key="entry.filename">
                      <template v-if="isParentDirectory(entry)">
                        <b-list-group-item
                          button
                          :variant="getVariant(entry)"
                          style="white-space: pre-line"
                          v-on:click="fetchDirectory(entry)"
                        >
                          <div class="directory parent">
                            <b-spinner type="border" variant="primary" small v-if="entry.loading"></b-spinner>
                            <b-icon-arrow-up v-if="!entry.loading"> </b-icon-arrow-up> <span>{{ entry.filename }}</span>
                            <span class="parent-directory-hint">&larr; {{ $t("parentDirectoryHint") }}</span>
                          </div>
                        </b-list-group-item>

                        <b-carousel
                          v-show="directory.filter((entry) => isPicture(entry)).length > 0"
                          id="picture-carousel"
                          v-model="slide"
                          :interval="5000"
                          controls
                          indicators
                          fade
                          no-hover-pause
                          background="#ababab"
                          @sliding-start="onSlideStart"
                          @sliding-end="onSlideEnd"
                        >
                          <b-carousel-slide v-for="entry in directory.filter((entry) => isPicture(entry))">
                            <template #img>
                              <b-img-lazy
                                :src="createDownloadUrl(entry.filename)"
                                :alt="entry.filename"
                                block
                                center
                                fluid
                                :style="{
                                  height: carouselImageHeight + 'px',
                                  width: 'auto',
                                }"
                              />
                            </template>
                            <template #default>
                              <b-icon-download> </b-icon-download>
                              <b-link
                                style="
                                  white-space: pre-line;
                                  text-shadow:
                                    -1px 0 black,
                                    0 1px black,
                                    1px 0 black,
                                    0 -1px black;
                                  font-family: sans;
                                  color: #007bff;
                                  background-color: white;
                                  padding: 2px;
                                  opacity: 0.75;
                                "
                                v-on:click="openDownloadUrl(entry.filename)"
                              >
                                <span>{{ shortEntry(entry.filename) }}</span>
                              </b-link>
                            </template>
                          </b-carousel-slide>
                        </b-carousel>
                      </template>
                      <template v-else-if="isDirectory(entry)">
                        <b-list-group-item
                          button
                          :variant="getVariant(entry)"
                          style="white-space: pre-line"
                          v-on:click="fetchDirectory(entry)"
                        >
                          <div :class="directory">
                            <b-spinner type="border" variant="primary" small v-if="entry.loading"></b-spinner>
                            <b-icon-folder-fill v-if="!entry.loading"> </b-icon-folder-fill>
                            <span>{{ shortEntry(entry.filename) }}</span>
                          </div>
                        </b-list-group-item>
                      </template>
                      <template v-else-if="isMusic(entry)">
                        <b-list-group-item
                          button
                          :variant="getVariant(entry)"
                          style="white-space: pre-line; display: flex; justify-content: space-between"
                          v-on:click="
                            currentSid = shortEntry(entry.filename);
                            updateSid(entry.filename);
                            showAudio = true;
                            Vue.nextTick(function () {
                              play('', entry.filename);
                            });
                          "
                        >
                          <div style="flex-grow: 4; word-break: break-all">
                            <b-spinner type="border" variant="primary" small v-if="entry.loading"></b-spinner>
                            <b-icon-music-note v-if="!entry.loading"> </b-icon-music-note>
                            <span class="sid-file">{{ shortEntry(entry.filename) }}</span>
                          </div>
                          <b-button
                            size="sm"
                            style="font-size: smaller; padding: 2px 4px"
                            v-on:click.stop="openDownloadMP3Url(entry.filename)"
                            v-show="!isMP3(entry)"
                          >
                            <b-icon-download> </b-icon-download>
                            <span>{{ $t("downloadMP3") }}</span></b-button
                          >
                          <b-button
                            size="sm"
                            style="font-size: smaller; padding: 2px 4px"
                            v-on:click.stop="openDownloadSIDUrl(entry.filename)"
                          >
                            <b-icon-download> </b-icon-download>
                          </b-button>
                          <div>
                            <b-button
                              size="sm"
                              style="font-size: smaller; padding: 2px 4px"
                              variant="primary"
                              v-on:click.stop="
                                playlist.push({
                                  filename: entry.filename,
                                });
                                tabIndex = 5;
                                showAudio = true;
                              "
                            >
                              <b-icon-plus> </b-icon-plus>
                            </b-button>
                          </div>
                        </b-list-group-item>
                      </template>
                      <template v-else-if="isVideo(entry)">
                        <template v-if="canFastload(entry)">
                          <b-list-group-item
                            :button="!isVideo(entry)"
                            :variant="getVariant(entry)"
                            v-bind:href="createConvertUrl('', entry.filename)"
                            v-on:click.self="pause"
                            target="c64"
                          >
                            <div style="white-space: pre-line; display: flex; justify-content: space-between">
                              <div style="flex-grow: 4; word-break: break-all">
                                <b-spinner
                                  type="border"
                                  variant="primary"
                                  small
                                  v-if="entry.loading || entry.loadingDisk"
                                ></b-spinner>
                                <b-icon-camera-video-fill v-if="!(entry.loading || entry.loadingDisk)">
                                </b-icon-camera-video-fill>
                                <span>{{ shortEntry(entry.filename) }}</span>
                              </div>
                              <b-button
                                size="sm"
                                style="font-size: smaller; padding: 2px 4px"
                                variant="primary"
                                v-on:click.prevent="fetchDiskDirectory(entry)"
                                :disabled="entry.loadingDisk"
                              >
                                <span> {{ $t("showDirectory") }} </span>
                              </b-button>
                              <b-button
                                size="sm"
                                style="font-size: smaller; padding: 2px 4px"
                                v-on:click.prevent="openDownloadSIDUrl(entry.filename)"
                              >
                                <b-icon-download> </b-icon-download>
                              </b-button>
                            </div>
                            <div>
                              <div v-show="entry.directoryMode > 0" class="disk-directory">
                                <div>
                                  <span class="c64-font">{{ entry.diskDirectoryHeader }}</span>
                                </div>
                                <div v-for="(program, index) in entry.diskDirectory" :key="index">
                                  <a
                                    v-bind:href="createConvertUrl(program.directoryLine, entry.filename)"
                                    v-on:click="pause"
                                    target="c64"
                                  >
                                    <span class="c64-font">{{ program.formatted }}</span>
                                  </a>
                                </div>
                              </div>
                            </div>
                          </b-list-group-item>
                        </template>
                        <template v-else>
                          <b-list-group-item
                            :button="!isVideo(entry)"
                            :variant="getVariant(entry)"
                            v-bind:href="createConvertUrl('', entry.filename)"
                            v-on:click.self="pause"
                            target="c64"
                          >
                            <div style="white-space: pre-line; display: flex; justify-content: space-between">
                              <div style="flex-grow: 4; word-break: break-all">
                                <b-spinner type="border" variant="primary" small v-if="entry.loading"></b-spinner>
                                <b-icon-camera-video-fill v-if="!entry.loading"> </b-icon-camera-video-fill>
                                <span>{{ shortEntry(entry.filename) }}</span>
                              </div>
                              <b-button
                                size="sm"
                                style="font-size: smaller; padding: 2px 4px"
                                v-on:click.prevent="openDownloadSIDUrl(entry.filename)"
                              >
                                <b-icon-download> </b-icon-download>
                              </b-button>
                            </div>
                          </b-list-group-item>
                        </template>
                      </template>
                      <template v-else>
                        <b-list-group-item
                          :button="!isVideo(entry)"
                          :variant="getVariant(entry)"
                          style="white-space: pre-line"
                          v-on:click="openDownloadUrl(entry.filename)"
                        >
                          <b-spinner type="border" variant="primary" small v-if="entry.loading"></b-spinner>
                          <b-icon-download v-if="!entry.loading"> </b-icon-download>
                          <span>{{ shortEntry(entry.filename) }}</span>
                        </b-list-group-item>
                      </template>
                    </div>
                  </b-list-group>
                </b-card-text>
              </b-tab>
              <b-tab style="position: relative">
                <template #title>
                  {{ $t("ASSEMBLY64") }}
                  <b-spinner
                    type="border"
                    :variant="tabIndex == 2 ? 'light' : 'primary'"
                    small
                    v-if="loadingAssembly64"
                  ></b-spinner>
                </template>

                <b-button
                  size="sm"
                  style="
                    font-size: smaller;
                    padding: 2px 4px;
                    position: absolute;
                    top: 0px;
                    right: 252px;
                    z-index: 9999;
                  "
                  variant="secondary"
                  @click="(event) => requestSearchResults(event, 'Hubbard_Rob')"
                >
                  <b-icon-filter-circle-fill> </b-icon-filter-circle-fill>
                  <span>Rob Hubbard</span>
                </b-button>

                <b-button
                  size="sm"
                  style="
                    font-size: smaller;
                    padding: 2px 4px;
                    position: absolute;
                    top: 0px;
                    right: 128px;
                    z-index: 9999;
                  "
                  variant="secondary"
                  @click="(event) => requestSearchResults(event, 'Galway_Martin')"
                >
                  <b-icon-filter-circle-fill> </b-icon-filter-circle-fill>
                  <span>Martin Galway</span>
                </b-button>

                <b-button
                  size="sm"
                  style="font-size: smaller; padding: 2px 4px; position: absolute; top: 0px; right: 0px; z-index: 9999"
                  variant="secondary"
                  @click="(event) => requestSearchResults(event, 'Huelsbeck_Chris')"
                >
                  <b-icon-filter-circle-fill> </b-icon-filter-circle-fill>
                  <span>Chris H&uuml;lsbeck</span>
                </b-button>

                <b-button
                  size="sm"
                  style="
                    font-size: smaller;
                    padding: 2px 4px;
                    position: absolute;
                    top: 30px;
                    right: 211px;
                    z-index: 9999;
                  "
                  variant="secondary"
                  @click="(event) => requestSearchResults(event, 'Ouwehand_Reyn')"
                >
                  <b-icon-filter-circle-fill> </b-icon-filter-circle-fill>
                  <span>Reyn Ouwehand</span>
                </b-button>

                <b-button
                  size="sm"
                  style="
                    font-size: smaller;
                    padding: 2px 4px;
                    position: absolute;
                    top: 30px;
                    right: 109px;
                    z-index: 9999;
                  "
                  variant="secondary"
                  @click="(event) => requestSearchResults(event, 'Tel_Jeroen')"
                >
                  <b-icon-filter-circle-fill> </b-icon-filter-circle-fill>
                  <span>Jeroen Tel</span>
                </b-button>

                <b-button
                  size="sm"
                  style="font-size: smaller; padding: 2px 4px; position: absolute; top: 30px; right: 0px; z-index: 9999"
                  variant="secondary"
                  @click="(event) => requestSearchResults(event, 'Daglish_Ben')"
                >
                  <b-icon-filter-circle-fill> </b-icon-filter-circle-fill>
                  <span>Ben Daglish</span>
                </b-button>

                <div style="height: 40px"></div>
                <b-card-text>
                  <b-table
                    striped
                    bordered
                    :items="searchResults"
                    :fields="searchFields"
                    small
                    fixed
                    responsive
                    :sort-by="sortBy"
                    :sort-desc="sortDesc"
                    @sort-changed="sortChanged"
                  >
                    <template #table-colgroup="scope">
                      <col
                        v-for="field in scope.fields"
                        :key="field.key"
                        :style="{
                          width: field.key === 'actions' ? '32px' : '',
                        }"
                      />
                    </template>
                    <template #cell(name)="row">
                      <div>
                        <span>{{ row.item.name }}</span>
                      </div>
                      <div>
                        <span style="font-style: italic; font-size: small">{{ row.item.group }}</span>
                      </div>
                    </template>
                    <template #head(actions)="row">
                      <b-button
                        size="xsm"
                        style="padding: 0px"
                        variant="secondary"
                        @click="
                          (event) => {
                            resetSearchResults(event);
                            requestSearchResults(event);
                          }
                        "
                      >
                        <b-icon-eraser-fill> </b-icon-eraser-fill>
                      </b-button>
                    </template>
                    <template #cell(actions)="row">
                      <b-button size="sm" @click="requestContentEntries(row.item)" class="mr-1" style="padding: 0">
                        <b-icon-caret-up-fill v-if="row.detailsShowing"> </b-icon-caret-up-fill>
                        <b-icon-caret-down-fill v-if="!row.detailsShowing"> </b-icon-caret-down-fill>
                      </b-button>
                    </template>
                    <template #row-details="row">
                      <b-card>
                        <b-table
                          striped
                          bordered
                          :items="row.item.contentEntries"
                          :fields="contentEntryFields"
                          small
                          fixed
                          responsive
                        >
                          <template #table-colgroup="scope">
                            <col
                              v-for="field in scope.fields"
                              :key="field.key"
                              :style="{
                                width: field.key === 'actions' ? '54px' : '',
                              }"
                            />
                          </template>

                          <template #cell(filename)="innerRow">
                            <template v-if="isMusic(innerRow.item)">
                              <div style="white-space: pre-line; display: flex; justify-content: space-between">
                                <div style="flex-grow: 4; word-break: break-all">
                                  <b-icon-music-note> </b-icon-music-note>
                                  <b-link
                                    style="white-space: pre-line"
                                    v-on:click="
                                      currentSid = shortEntry(innerRow.item.filename);
                                      updateSid(innerRow.item.filename, row.item.id, row.item.categoryId);
                                      showAudio = true;
                                      Vue.nextTick(function () {
                                        play('', innerRow.item.filename, row.item.id, row.item.categoryId);
                                      });
                                    "
                                  >
                                    <span class="sid-file">{{ shortEntry(innerRow.item.filename) }}</span>
                                  </b-link>
                                </div>

                                <b-button
                                  size="sm"
                                  style="font-size: smaller; padding: 2px 4px"
                                  v-on:click="
                                    openDownloadMP3Url(innerRow.item.filename, row.item.id, row.item.categoryId)
                                  "
                                >
                                  <b-icon-download> </b-icon-download>
                                  <span>{{ $t("downloadMP3") }}</span>
                                </b-button>
                                <b-button
                                  size="sm"
                                  style="font-size: smaller; padding: 2px 4px"
                                  v-on:click="
                                    openDownloadSIDUrl(innerRow.item.filename, row.item.id, row.item.categoryId)
                                  "
                                >
                                  <b-icon-download> </b-icon-download>
                                </b-button>
                                <div>
                                  <b-button
                                    size="sm"
                                    style="font-size: smaller; padding: 2px 4px"
                                    variant="primary"
                                    v-on:click="
                                      playlist.push({
                                        filename: innerRow.item.filename,
                                        itemId: row.item.id,
                                        categoryId: row.item.categoryId,
                                      });
                                      tabIndex = 5;
                                      playlistIndex = 0;
                                    "
                                  >
                                    <b-icon-plus> </b-icon-plus>
                                  </b-button>
                                </div>
                              </div>
                            </template>
                            <template v-else-if="isVideo(innerRow.item)">
                              <span>
                                <template v-if="canFastload(innerRow.item)">
                                  <div style="white-space: pre-line; display: flex; justify-content: space-between">
                                    <div style="flex-grow: 4; word-break: break-all">
                                      <b-spinner
                                        type="border"
                                        variant="primary"
                                        small
                                        v-if="innerRow.item.loadingDisk"
                                      ></b-spinner>
                                      <a
                                        v-bind:href="
                                          createConvertUrl('', innerRow.item.filename, row.item.id, row.item.categoryId)
                                        "
                                        v-on:click="pause"
                                        target="c64"
                                      >
                                        <b-icon-camera-video-fill> </b-icon-camera-video-fill>
                                        <span style="word-break: break-all">{{
                                          shortEntry(innerRow.item.filename)
                                        }}</span>
                                      </a>
                                    </div>
                                    <b-button
                                      size="sm"
                                      style="font-size: smaller; padding: 2px 4px"
                                      variant="primary"
                                      v-on:click="fetchDiskDirectory(innerRow.item, row.item.id, row.item.categoryId)"
                                      :disabled="innerRow.item.loadingDisk"
                                    >
                                      <span> {{ $t("showDirectory") }} </span>
                                    </b-button>
                                    <b-button
                                      size="sm"
                                      style="font-size: smaller; padding: 2px 4px"
                                      v-on:click="
                                        openDownloadSIDUrl(innerRow.item.filename, row.item.id, row.item.categoryId)
                                      "
                                    >
                                      <b-icon-download> </b-icon-download>
                                    </b-button>
                                  </div>
                                  <div>
                                    <div v-show="innerRow.item.directoryMode > 0">
                                      <div class="disk-directory">
                                        <div>
                                          <span class="c64-font">{{ innerRow.item.diskDirectoryHeader }}</span>
                                        </div>
                                        <div v-for="(program, index) in innerRow.item.diskDirectory" :key="index">
                                          <a
                                            v-bind:href="
                                              createConvertUrl(
                                                program.directoryLine,
                                                innerRow.item.filename,
                                                row.item.id,
                                                row.item.categoryId
                                              )
                                            "
                                            v-on:click="pause"
                                            target="c64"
                                          >
                                            <span class="c64-font">{{ program.formatted }}</span>
                                          </a>
                                        </div>
                                      </div>
                                    </div>
                                  </div>
                                </template>
                                <template v-else>
                                  <div style="white-space: pre-line; display: flex; justify-content: space-between">
                                    <div style="flex-grow: 4; word-break: break-all">
                                      <a
                                        v-bind:href="
                                          createConvertUrl('', innerRow.item.filename, row.item.id, row.item.categoryId)
                                        "
                                        v-on:click="pause"
                                        target="c64"
                                      >
                                        <b-icon-camera-video-fill> </b-icon-camera-video-fill>
                                        <span style="word-break: break-all">{{
                                          shortEntry(innerRow.item.filename)
                                        }}</span>
                                      </a>
                                    </div>
                                    <b-button
                                      size="sm"
                                      style="font-size: smaller; padding: 2px 4px"
                                      v-on:click="
                                        openDownloadSIDUrl(innerRow.item.filename, row.item.id, row.item.categoryId)
                                      "
                                    >
                                      <b-icon-download> </b-icon-download>
                                    </b-button>
                                  </div>
                                </template>
                              </span>
                            </template>
                            <template v-else>
                              <div>
                                <b-icon-download> </b-icon-download>
                                <b-link
                                  style="white-space: pre-line"
                                  v-on:click="openDownloadUrl(innerRow.item.filename, row.item.id, row.item.categoryId)"
                                >
                                  <span>{{ shortEntry(innerRow.item.filename) }}</span>
                                </b-link>
                              </div>
                            </template>
                          </template>
                        </b-table>
                      </b-card>
                    </template>

                    <template #head(category)="data">
                      <label for="category" style="font-size: smaller; margin-left: 0px">{{ data.label }}</label>
                      <b-form-select
                        id="category"
                        v-model="category"
                        @change="requestSearchResults"
                        value-field="id"
                        text-field="description"
                        :options="categories"
                        size="sm"
                        class="mt-1"
                        style="margin: 4px; padding: 0.175em 0em"
                        :select-size="1"
                        style="margin-left: 0 !important; margin-right: 0 !important; max-width: 100%"
                      >
                        <template #first>
                          <b-form-select-option value="">{{ $t("firstCategory") }}</b-form-select-option>
                        </template>
                      </b-form-select>
                    </template>
                    <template #head(name)="data">
                      <label for="name" style="font-size: smaller; margin-left: 0px">{{ data.label }}</label>
                      <b-form-input
                        type="text"
                        id="name"
                        v-model="name"
                        @change="requestSearchResults"
                        style="max-width: 100%; padding: 0.175em 0em"
                        autocomplete="off"
                        autocorrect="off"
                        autocapitalize="off"
                        spellcheck="false"
                      />
                    </template>
                    <template #head(group)="data">
                      <label for="group" style="font-size: smaller; margin-left: 0px">{{ data.label }}</label>
                      <b-form-input
                        type="text"
                        id="group"
                        v-model="group"
                        @change="requestSearchResults"
                        style="max-width: 100%; padding: 0.175em 0em"
                        autocomplete="off"
                        autocorrect="off"
                        autocapitalize="off"
                        spellcheck="false"
                      />
                    </template>
                    <template #head(event)="data">
                      <label for="event" style="font-size: smaller; margin-left: 0px">{{ data.label }}</label>
                      <b-form-input
                        type="text"
                        id="event"
                        v-model="event"
                        @change="requestSearchResults"
                        style="max-width: 100%; padding: 0.175em 0em"
                        autocomplete="off"
                        autocorrect="off"
                        autocapitalize="off"
                        spellcheck="false"
                      />
                    </template>
                    <template #head(released)="data" style="padding-right: calc(0.3rem + 0.1em)">
                      <label for="released" style="font-size: smaller; margin-left: 0px">{{ data.label }}</label>
                      <b-form-input
                        type="text"
                        id="released"
                        v-model="released"
                        @change="requestSearchResults"
                        style="max-width: 100%; padding: 0.175em 0em"
                        autocomplete="off"
                        autocorrect="off"
                        autocapitalize="off"
                        spellcheck="false"
                      />
                    </template>
                    <template #head(handle)="data">
                      <label for="handle" style="font-size: smaller; margin-left: 0px">{{ data.label }}</label>
                      <b-form-input
                        type="text"
                        id="handle"
                        v-model="handle"
                        @change="requestSearchResults"
                        style="max-width: 100%; padding: 0.175em 0em"
                        autocomplete="off"
                        autocorrect="off"
                        autocapitalize="off"
                        spellcheck="false"
                      />
                    </template>
                    <template #head(rating)="data">
                      <label for="rating" style="font-size: smaller; margin-left: 0px">{{ data.label }}</label>
                      <b-form-input
                        type="number"
                        id="rating"
                        v-model.number="rating"
                        @change="requestSearchResults"
                        style="max-width: 100%; padding: 0.175em 0em"
                        autocomplete="off"
                        autocorrect="off"
                        autocapitalize="off"
                        spellcheck="false"
                      />
                    </template>
                  </b-table>
                </b-card-text>
              </b-tab>
              <b-tab>
                <template #title>
                  {{ $t("SID") }}
                  <b-spinner
                    type="border"
                    :variant="tabIndex == 3 ? 'light' : 'primary'"
                    small
                    v-if="loadingSid"
                  ></b-spinner>
                </template>

                <b-card-text>
                  <div class="sid">
                    <b-table striped bordered :items="translatedInfos" :fields="translatedFields">
                      <template #cell(Value)="row">
                        <span
                          :style="row.item.opacity ? 'opacity: 0.25; line-break: anywhere;' : 'line-break: anywhere;'"
                          >{{ row.item.Value }}</span
                        >
                      </template>
                    </b-table>
                    <div class="picture-container">
                      <b-img :src="picture" id="img" class="picture" rounded="circle" fluid> </b-img>
                    </div>
                  </div>
                </b-card-text>
              </b-tab>
              <b-tab :disabled="!hasStil">
                <template #title>
                  {{ $t("STIL") }}
                  <b-spinner
                    type="border"
                    :variant="tabIndex == 4 ? 'light' : 'primary'"
                    small
                    v-if="loadingStil"
                  ></b-spinner>
                </template>

                <b-card-text>
                  <div class="stil">
                    <div class="stil-comment">
                      <span>{{ stil.comment }}</span>
                    </div>
                    <ul>
                      <li v-for="info in stil.infos" v-show="isValidStil(info)">
                        <div class="stil-name" v-show="info.name">
                          <span class="stil-name-header">{{ $t("STILINFO.NAME") }}:</span>
                          <span>{{ info.name }}</span>
                        </div>
                        <div class="stil-author" v-show="info.author">
                          <span class="stil-author-header">{{ $t("STILINFO.AUTHOR") }}:</span>
                          <span>{{ info.author }}</span>
                        </div>
                        <div class="stil-title" v-show="info.title">
                          <span class="stil-title-header">{{ $t("STILINFO.TITLE") }}:</span>
                          <span>{{ info.title }}</span>
                        </div>
                        <div class="stil-artist" v-show="info.artist">
                          <span class="stil-artist-header">{{ $t("STILINFO.ARTIST") }}:</span>
                          <span>{{ info.artist }}</span>
                        </div>
                        <div class="stil-comment" v-show="info.comment">
                          <span>{{ info.comment }}</span>
                        </div>
                      </li>
                    </ul>
                    <ul>
                      <li v-for="subtune in stil.subtunes">
                        <div class="stil-subtune" v-show="subtune.tuneNo">
                          <span class="stil-subtune-header">{{ $t("STILINFO.SUBTUNE") }}:</span>
                          <span>{{ subtune.tuneNo }}</span>
                        </div>
                        <ul>
                          <li v-for="info in subtune.infos" v-show="isValidStil(info)">
                            <div class="stil-name" v-show="info.name">
                              <span class="stil-name-header">{{ $t("STILINFO.NAME") }}:</span>
                              <span>{{ info.name }}</span>
                            </div>
                            <div class="stil-author" v-show="info.author">
                              <span class="stil-author-header">{{ $t("STILINFO.AUTHOR") }}:</span>
                              <span>{{ info.author }}</span>
                            </div>
                            <div class="stil-title" v-show="info.title">
                              <span class="stil-title-header">{{ $t("STILINFO.TITLE") }}:</span>
                              <span>{{ info.title }}</span>
                            </div>
                            <div class="stil-artist" v-show="info.artist">
                              <span class="stil-artist-header">{{ $t("STILINFO.ARTIST") }}:</span>
                              <span>{{ info.artist }}</span>
                            </div>
                            <div class="stil-comment" v-show="info.comment">
                              <span>{{ info.comment }}</span>
                            </div>
                          </li>
                        </ul>
                      </li>
                    </ul>
                  </div>
                </b-card-text>
              </b-tab>
              <b-tab>
                <template #title>
                  {{ $t("PL") }}
                  <b-spinner
                    type="border"
                    :variant="tabIndex == 5 ? 'light' : 'primary'"
                    small
                    v-if="loadingPl"
                  ></b-spinner>
                </template>

                <b-card-text>
                  <div class="button-box">
                    <div>
                      <b-form-file
                        v-model="importFile"
                        accept=".js2,.json"
                        :state="Boolean(importFile)"
                        ref="file-input"
                        class="mb-2"
                        label-cols-sm="2"
                        label-size="sm"
                        :placeholder="$t('importPlaylistPlaceholder')"
                        :drop-placeholder="$t('importPlaylistDropPlaceholder')"
                      >
                      </b-form-file>
                      <b-button v-if="importFile != null" @click="importFile = null">
                        <b-icon-trash-fill> </b-icon-trash-fill>
                        <span>{{ $t("reset") }}</span>
                      </b-button>
                      <b-button
                        variant="success"
                        v-b-modal.modal-import-playlist
                        v-if="importFile != null"
                        class="mr-2"
                      >
                        <b-icon-file-arrow-up-fill> </b-icon-file-arrow-up-fill>
                        <span>{{ $t("startImport") }}</span>
                      </b-button>
                      <b-modal id="modal-import-playlist" :title="$t('confirmationTitle')" @ok="importPlaylist">
                        <p>{{ $t("removePlaylistReally") }}</p>
                      </b-modal>
                    </div>
                    <b-button v-b-modal.modal-fetch-favorites size="sm">
                      <b-icon-download> </b-icon-download>
                      <span>{{ $t("fetchFavorites") }}</span></b-button
                    >
                    <b-modal id="modal-fetch-favorites" :title="$t('confirmationTitle')" @ok="fetchFavorites">
                      <p>{{ $t("removePlaylistReally") }}</p>
                    </b-modal>
                    <b-button size="sm" @click="exportPlaylist" v-if="playlist.length > 0">
                      <b-icon-file-arrow-down-fill> </b-icon-file-arrow-down-fill>
                      <span>{{ $t("exportPlaylist") }}</span></b-button
                    >
                    <b-button variant="success" v-on:click="setNextPlaylistEntry" v-if="playlist.length > 0">
                      <b-icon-play-fill> </b-icon-play-fill>
                      <span>{{ $t("next") }}</span></b-button
                    >
                  </div>
                  <div class="button-box" v-if="playlist.length > 0">
                    <b-button v-b-modal.modal-remove-playlist variant="danger" size="sm">
                      <b-icon-trash-fill> </b-icon-trash-fill>
                      <span>{{ $t("removePlaylist") }}</span></b-button
                    >
                    <b-modal id="modal-remove-playlist" :title="$t('confirmationTitle')" @ok="removePlaylist">
                      <p>{{ $t("removePlaylistReally") }}</p>
                    </b-modal>
                  </div>

                  <ol class="striped-list">
                    <li
                      v-for="(entry, index) in playlist"
                      :key="index"
                      :class="index == playlistIndex ? 'highlighted' : ''"
                      v-on:click="
                        playlistIndex = index;
                        Vue.nextTick(function () {
                          play(
                            '',
                            playlist[playlistIndex].filename,
                            playlist[playlistIndex].itemId,
                            playlist[playlistIndex].categoryId
                          );
                        });
                        currentSid = playlistIndex + 1 + ': ' + shortEntry(playlist[playlistIndex].filename);
                        updateSid(
                          playlist[playlistIndex].filename,
                          playlist[playlistIndex].itemId,
                          playlist[playlistIndex].categoryId
                        );
                      "
                    >
                      <span style="display: flex; justify-content: space-between">
                        <div>
                          <div class="playlist-item">
                            <span>{{ shortEntry(entry.filename) }}</span>
                          </div>
                          <div v-show="pathEntry(entry.filename).length > 1">
                            <span style="font-size: smaller; line-break: anywhere">{{
                              pathEntry(entry.filename)
                            }}</span>
                          </div>
                        </div>
                        <b-button
                          v-b-modal:[`modal-remove-${index}`]
                          v-on:click.stop=""
                          pill
                          size="sm"
                          style="height: fit-content"
                        >
                          <b-icon-trash-fill style="margin: 2px"> </b-icon-trash-fill>
                        </b-button>
                        <b-modal :id="`modal-remove-${index}`" :title="$t('confirmationTitle')" @ok="remove(index)">
                          <p>{{ $t("removeReally") }}</p>
                        </b-modal>
                      </span>
                    </li>
                  </ol>
                </b-card-text>
              </b-tab>
              <b-tab :disabled="!hasHardware">
                <template #title>
                  {{ $t("HARDWARE") }}
                </template>

                <b-card-text>
                  <b-container fluid>
                    <b-row>
                      <b-col style="border-right: 1px dotted grey">
                        <b
                          ><div style="margin-bottom: 8px; text-align: center">{{ $t("HARDSID") }}</div></b
                        >
                      </b-col>
                      <b-col>
                        <b
                          ><div style="margin-bottom: 8px; text-align: center">{{ $t("EXSID") }}</div></b
                        >
                      </b-col>
                      <!--b-col>
                        <b><div style="margin-bottom: 8px; text-align: center;">{{ $t("SIDBLASTER") }}</div></b>
                      </b-col-->
                    </b-row>
                    <b-row>
                      <b-col style="border-right: 1px dotted grey">
                        <div>
                          <b-container>
                            <b-img-lazy
                              src="/static/images/hardsid4u.jpeg"
                              alt="HardSID4U"
                              p-4
                              bg-dark
                              thumbnail
                              block
                              center
                              fluid
                            />
                          </b-container>
                          <div class="settings-box">
                            <span class="setting">
                              <label size="sm" for="hardsid6581">
                                <b-form-select
                                  id="hardsid6581"
                                  class="right"
                                  v-model="convertOptions.config.emulationSection.hardsid6581"
                                  size="sm"
                                  class="mt-3"
                                  :select-size="1"
                                >
                                  <option :value="0">1</option>
                                  <option :value="1">2</option>
                                  <option :value="2">3</option>
                                  <option :value="3">4</option>
                                </b-form-select>
                                <span>{{ $t("convertMessages.config.emulationSection.hardsid6581") }}</span>
                              </label></span
                            >
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <label size="sm" for="hardsid8580">
                                <b-form-select
                                  id="hardsid8580"
                                  class="right"
                                  v-model="convertOptions.config.emulationSection.hardsid8580"
                                  size="sm"
                                  class="mt-3"
                                  :select-size="1"
                                >
                                  <option :value="0">1</option>
                                  <option :value="1">2</option>
                                  <option :value="2">3</option>
                                  <option :value="3">4</option>
                                </b-form-select>
                                <span>{{ $t("convertMessages.config.emulationSection.hardsid8580") }}</span>
                              </label></span
                            >
                          </div>
                        </div>
                      </b-col>
                      <b-col>
                        <div>
                          <b-container>
                            <b-img-lazy
                              src="/static/images/exsid.jpg"
                              width="300px"
                              height="200px"
                              alt="ExSID"
                              thumbnail
                              block
                              center
                              fluid
                            />
                          </b-container>
                        </div>
                      </b-col>

                      <!--b-col style="border-left: 1px dotted grey;">
                        <div>
                          <b-container>
							<b-img-lazy
								src="/static/images/sidblaster.jpeg"
								alt="SIDBlaster"
								block
								center
								fluid
							/>
                          </b-container>
						</div>
                      </b-col-->
                    </b-row>
                    <b-row>
                      <b-col style="border-right: 1px dotted grey">
                        <div class="settings-box">
                          <span class="setting">
                            <b-button
                              size="sm"
                              variant="success"
                              v-on:click="
                                HardwareFunctions.init = init_hardsid;
                                HardwareFunctions.reset = reset_hardsid;
                                HardwareFunctions.write = write_hardsid;
                                HardwareFunctions.next = next_hardsid;
                                HardwareFunctions.quit = quit_hardsid;
                                HardwareFunctions.mapping = 'hardsid-mapping';
                                init();
                              "
                            >
                              <span>{{ $t("CONNECT") }}</span>
                            </b-button>
                          </span>
                        </div>
                      </b-col>

                      <b-col>
                        <div class="settings-box">
                          <span class="setting">
                            <b-button
                              size="sm"
                              variant="success"
                              v-on:click="
                                HardwareFunctions.init = init_exsid;
                                HardwareFunctions.reset = reset_exsid;
                                HardwareFunctions.write = write_exsid;
                                HardwareFunctions.next = next_exsid;
                                HardwareFunctions.quit = quit_exsid;
                                HardwareFunctions.mapping = 'exsid-mapping';
                                init();
                              "
                            >
                              <span>{{ $t("CONNECT") }}</span>
                            </b-button></span
                          >
                        </div>
                      </b-col>

                      <!--b-col style="border-left: 1px dotted grey;">
						<div class="settings-box">
                    	  <span class="setting">
							<b-button
								size="sm"
								variant="success"
								v-on:click="
									HardwareFunctions.init = init_sidblaster;
									HardwareFunctions.reset = reset_sidblaster;
									HardwareFunctions.write = write_sidblaster;
									HardwareFunctions.next = next_sidblaster;
									HardwareFunctions.quit = quit_sidblaster;
									HardwareFunctions.mapping = 'sidblaster-mapping';
									init();
								"
							>
                              <span>{{ $t("CONNECT") }}</span>
							</b-button></span
						  >
						</div>
                      </b-col-->
                    </b-row>
                  </b-container>

                  <p style="margin-top: 16px">
                    {{ $t("USE_REAL_HARDWARE") }}
                  </p>
                  <p>
                    {{ $t("HARDWARE_PREPARATION_1") }}
                    <a href="https://zadig.akeo.ie/" target="_blank">
                      {{ $t("HERE") }}
                    </a>
                    {{ $t("HARDWARE_PREPARATION_2") }}
                  </p>
                  <p>
                    <span>{{ $t("USE_MOBILE_DEVICES_1") }}</span>

                    <a
                      href="https://www.amazon.de/gp/product/B09H2TJCQG/ref=ppx_yo_dt_b_search_asin_image?ie=UTF8&psc=1"
                      target="_blank"
                    >
                      <span>{{ $t("USE_MOBILE_DEVICES_2") }}</span></a
                    >.
                    <b-img-lazy src="/static/images/usbc.jpg" alt="HardSID4U" block center fluid />
                  </p>
                  <p>
                    <b> {{ $t("STREAMING_NOTES") }} </b>
                  </p>
                </b-card-text>
              </b-tab>
              <b-tab>
                <template #title>
                  {{ $t("CFG") }}
                  <b-spinner
                    type="border"
                    :variant="tabIndex == 6 ? 'light' : 'primary'"
                    small
                    v-if="loadingCfg"
                  ></b-spinner>
                </template>

                <b-card-text>
                  <div class="settings-box">
                    <div class="button-box">
                      <b-button v-b-modal.modal-set-default size="sm" variant="outline-success">
                        <span>{{ $t("setDefault") }}</span></b-button
                      >
                      <b-modal id="modal-set-default" :title="$t('confirmationTitle')" @ok="setDefault">
                        <p>{{ $t("setDefaultReally") }}</p>
                      </b-modal>
                    </div>
                  </div>

                  <b-tabs
                    v-model="tabConfigIndex"
                    active-nav-item-class="font-weight-bold text-italic"
                    pills
                    card
                    small
                    align="right"
                  >
                    <b-tab>
                      <template #title>{{ $t("streamingCfgHeader") }}</template>

                      <b-card-text>
                        <div class="settings-box">
                          <div class="button-box">
                            <b-button size="sm" variant="outline-success" v-on:click="mobileProfile">
                              <b-icon-phone-fill> </b-icon-phone-fill>
                              <span>{{ $t("mobileProfile") }}</span></b-button
                            >
                            <b-button size="sm" variant="outline-success" v-on:click="wifiProfile">
                              <b-icon-wifi> </b-icon-wifi>
                              <span>{{ $t("wifiProfile") }}</span></b-button
                            >
                          </div>
                        </div>

                        <b-tabs
                          v-model="streamingTabConfigIndex"
                          active-nav-item-class="font-weight-bold text-italic"
                          pills
                          card
                          small
                          align="right"
                        >
                          <b-tab>
                            <template #title>{{ $t("audioStreamingCfgHeader") }}</template>

                            <b-card-text>
                              <div class="settings-box">
                                <span class="setting"
                                  ><label for="cbr">
                                    {{ $t("convertMessages.config.audioSection.cbr") }}
                                    <select id="cbr" class="right" v-model="convertOptions.config.audioSection.cbr">
                                      <option v-for="cbr in cbrs">{{ cbr }}</option>
                                    </select></label
                                  ></span
                                >
                              </div>
                              <div class="settings-box">
                                <span class="setting"
                                  ><label for="vbr">
                                    {{ $t("convertMessages.config.audioSection.vbr") }}
                                    <b-form-checkbox
                                      id="vbr"
                                      class="right"
                                      v-model="convertOptions.config.audioSection.vbr"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting"
                                  ><label for="vbrQuality">
                                    {{ $t("convertMessages.config.audioSection.vbrQuality") }}
                                    <select
                                      id="vbrQuality"
                                      class="right"
                                      v-model="convertOptions.config.audioSection.vbrQuality"
                                    >
                                      <option v-for="vbrQuality in vbrQualities">
                                        {{ vbrQuality }}
                                      </option>
                                    </select></label
                                  ></span
                                >
                              </div>
                            </b-card-text>
                          </b-tab>

                          <b-tab>
                            <template #title>{{ $t("videoStreamingCfgHeader") }}</template>

                            <b-card-text>
                              <div class="settings-box">
                                <span class="setting"
                                  ><label for="useHls">
                                    {{ $t("convertMessages.useHls") }}
                                    <b-form-checkbox id="useHls" class="right" v-model="convertOptions.useHls">
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting"
                                  ><label for="hlsType"
                                    >{{ $t("convertMessages.hlsType") }}
                                    <b-form-group class="right">
                                      <b-form-radio-group
                                        id="hlsType"
                                        v-model="convertOptions.hlsType"
                                        style="display: flex"
                                      >
                                        <b-form-radio value="VIDEO_JS">video-js</b-form-radio>
                                        <b-form-radio value="HLS_JS">hls-js</b-form-radio>
                                      </b-form-radio-group>
                                    </b-form-group>
                                  </label></span
                                >
                              </div>
                              <div class="settings-box">
                                <span class="setting"
                                  ><label for="videoCoderAudioDelay"
                                    >{{ $t("convertMessages.config.audioSection.videoCoderAudioDelay") }}
                                    <b-form-input
                                      id="videoCoderAudioDelay"
                                      class="right"
                                      v-model.number="convertOptions.config.audioSection.videoCoderAudioDelay"
                                      type="number" /></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting"
                                  ><label for="audioCoderBitRate"
                                    >{{ $t("convertMessages.config.audioSection.audioCoderBitRate") }}
                                    <b-form-input
                                      id="audioCoderBitRate"
                                      class="right"
                                      type="number"
                                      min="0"
                                      oninput="validity.valid||(value='');"
                                      v-model.number="convertOptions.config.audioSection.audioCoderBitRate" /></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting"
                                  ><label for="videoCoderBitRate"
                                    >{{ $t("convertMessages.config.audioSection.videoCoderBitRate") }}
                                    <b-form-input
                                      id="videoCoderBitRate"
                                      class="right"
                                      type="number"
                                      min="0"
                                      oninput="validity.valid||(value='');"
                                      v-model.number="convertOptions.config.audioSection.videoCoderBitRate" /></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting"
                                  ><label for="showStatus">
                                    {{ $t("convertMessages.showStatus") }}
                                    <b-form-checkbox id="showStatus" class="right" v-model="convertOptions.showStatus">
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting"
                                  ><label for="pressSpaceInterval"
                                    >{{ $t("convertMessages.pressSpaceInterval") }}
                                    <b-form-input
                                      id="pressSpaceInterval"
                                      class="right"
                                      v-model.number="convertOptions.pressSpaceInterval"
                                      type="number" /></label
                                ></span>
                              </div>
                            </b-card-text>
                          </b-tab>
                        </b-tabs>
                      </b-card-text>
                    </b-tab>

                    <b-tab>
                      <template #title>{{ $t("playbackCfgHeader") }}</template>

                      <b-card-text>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="videoTuneAsAudio">
                              {{ $t("convertMessages.videoTuneAsAudio") }}
                              <b-form-checkbox
                                id="videoTuneAsAudio"
                                class="right"
                                v-model="convertOptions.videoTuneAsAudio"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="random">
                              {{ $t("random") }}
                              <b-form-checkbox id="random" class="right" v-model="random"> </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="enableDatabase">
                              {{ $t("convertMessages.config.sidplay2Section.enableDatabase") }}
                              <b-form-checkbox
                                id="enableDatabase"
                                class="right"
                                v-model="convertOptions.config.sidplay2Section.enableDatabase"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="single">
                              {{ $t("convertMessages.config.sidplay2Section.single") }}
                              <b-form-checkbox
                                id="single"
                                class="right"
                                v-model="convertOptions.config.sidplay2Section.single"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="loop">
                              {{ $t("convertMessages.config.sidplay2Section.loop") }}
                              <b-form-checkbox
                                id="loop"
                                class="right"
                                v-model="convertOptions.config.sidplay2Section.loop"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                      </b-card-text>
                    </b-tab>

                    <b-tab>
                      <template #title>{{ $t("audioCfgHeader") }}</template>

                      <b-card-text>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="delayBypass">
                              {{ $t("convertMessages.config.audioSection.delayBypass") }}
                              <b-form-checkbox
                                id="delayBypass"
                                class="right"
                                v-model="convertOptions.config.audioSection.delayBypass"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="reverbBypass">
                              {{ $t("convertMessages.config.audioSection.reverbBypass") }}
                              <b-form-checkbox
                                id="reverbBypass"
                                class="right"
                                v-model="convertOptions.config.audioSection.reverbBypass"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="startTime">
                              {{ $t("convertMessages.config.sidplay2Section.startTime") }}
                            </label>
                            <b-form-timepicker
                              id="startTime"
                              reset-button
                              class="right"
                              v-model="convertOptions.config.sidplay2Section.startTime"
                            />
                          </span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="defaultPlayLength"
                              >{{ $t("convertMessages.config.sidplay2Section.defaultPlayLength") }}
                            </label>
                            <b-form-timepicker
                              id="defaultPlayLength"
                              reset-button
                              class="right"
                              v-model="convertOptions.config.sidplay2Section.defaultPlayLength"
                            />
                          </span>
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="fadeInTime"
                              >{{ $t("convertMessages.config.sidplay2Section.fadeInTime") }}
                            </label>
                            <b-form-timepicker
                              id="fadeInTime"
                              reset-button
                              class="right"
                              v-model="convertOptions.config.sidplay2Section.fadeInTime"
                          /></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="fadeOutTime"
                              >{{ $t("convertMessages.config.sidplay2Section.fadeOutTime") }}
                            </label>
                            <b-form-timepicker
                              id="fadeOutTime"
                              reset-button
                              class="right"
                              v-model="convertOptions.config.sidplay2Section.fadeOutTime"
                          /></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="mainVolume">
                              {{ $t("convertMessages.config.audioSection.mainVolume") }}
                              <b-form-spinbutton
                                id="mainVolume"
                                class="right"
                                v-model="convertOptions.config.audioSection.mainVolume"
                                :locale="i18n.locale"
                                :formatter-fn="volumeFormatter"
                                min="-6"
                                max="6"
                                step="1"
                              ></b-form-spinbutton> </label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="secondVolume"
                              >{{ $t("convertMessages.config.audioSection.secondVolume") }}
                              <b-form-spinbutton
                                id="secondVolume"
                                class="right"
                                v-model="convertOptions.config.audioSection.secondVolume"
                                :locale="i18n.locale"
                                :formatter-fn="volumeFormatter"
                                min="-6"
                                max="6"
                                step="1"
                              ></b-form-spinbutton> </label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="thirdVolume">
                              {{ $t("convertMessages.config.audioSection.thirdVolume") }}
                              <b-form-spinbutton
                                id="thirdVolume"
                                class="right"
                                v-model="convertOptions.config.audioSection.thirdVolume"
                                :locale="i18n.locale"
                                :formatter-fn="volumeFormatter"
                                min="-6"
                                max="6"
                                step="1"
                              ></b-form-spinbutton> </label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="mainBalance">
                              {{ $t("convertMessages.config.audioSection.mainBalance") }}
                              <b-form-spinbutton
                                id="mainBalance"
                                class="right"
                                v-model="convertOptions.config.audioSection.mainBalance"
                                :locale="i18n.locale"
                                :formatter-fn="balanceFormatter"
                                min="0"
                                max="1"
                                step="0.1"
                              ></b-form-spinbutton> </label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="secondBalance"
                              >{{ $t("convertMessages.config.audioSection.secondBalance") }}

                              <b-form-spinbutton
                                id="secondBalance"
                                class="right"
                                v-model="convertOptions.config.audioSection.secondBalance"
                                :locale="i18n.locale"
                                :formatter-fn="balanceFormatter"
                                min="0"
                                max="1"
                                step="0.1"
                              ></b-form-spinbutton> </label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="thirdBalance"
                              >{{ $t("convertMessages.config.audioSection.thirdBalance") }}
                              <b-form-spinbutton
                                id="thirdBalance"
                                class="right"
                                v-model="convertOptions.config.audioSection.thirdBalance"
                                :locale="i18n.locale"
                                :formatter-fn="balanceFormatter"
                                min="0"
                                max="1"
                                step="0.1"
                              ></b-form-spinbutton> </label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="mainDelay">
                              {{ $t("convertMessages.config.audioSection.mainDelay") }}
                              <b-form-spinbutton
                                id="mainDelay"
                                class="right"
                                v-model="convertOptions.config.audioSection.mainDelay"
                                :locale="i18n.locale"
                                :formatter-fn="delayFormatter"
                                min="0"
                                max="100"
                                step="10"
                              ></b-form-spinbutton> </label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="secondDelay">
                              {{ $t("convertMessages.config.audioSection.secondDelay") }}
                              <b-form-spinbutton
                                id="secondDelay"
                                class="right"
                                v-model="convertOptions.config.audioSection.secondDelay"
                                :locale="i18n.locale"
                                :formatter-fn="delayFormatter"
                                min="0"
                                max="100"
                                step="10"
                              ></b-form-spinbutton> </label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="thirdDelay">
                              {{ $t("convertMessages.config.audioSection.thirdDelay") }}
                              <b-form-spinbutton
                                id="thirdDelay"
                                class="right"
                                v-model="convertOptions.config.audioSection.thirdDelay"
                                :locale="i18n.locale"
                                :formatter-fn="delayFormatter"
                                min="0"
                                max="100"
                                step="10"
                              ></b-form-spinbutton> </label
                          ></span>
                        </div>
                      </b-card-text>
                    </b-tab>

                    <b-tab>
                      <template #title>{{ $t("emulationCfgHeader") }}</template>

                      <b-card-text>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="stereoMode">
                              {{ $t("stereoMode") }}
                              <b-form-group class="right">
                                <b-form-radio-group id="stereoMode" v-model="stereoMode" style="display: flex">
                                  <b-form-radio value="AUTO">Auto</b-form-radio>
                                  <b-form-radio value="FORCE_2SID">2-SID</b-form-radio>
                                  <b-form-radio value="FORCE_3SID">3-SID</b-form-radio>
                                </b-form-radio-group>
                              </b-form-group>
                            </label></span
                          >
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="dualSidBase"
                              >{{ $t("convertMessages.config.emulationSection.dualSidBase") }}
                              <b-form-select
                                id="dualSidBase"
                                class="right"
                                v-model="convertOptions.config.emulationSection.dualSidBase"
                                size="sm"
                                class="mt-3"
                                :select-size="1"
                              >
                                <option :value="54304">0xd420</option>
                                <option :value="54336">0xd440</option>
                                <option :value="54528">0xd500</option>
                                <option :value="56832">0xde00</option>
                                <option :value="57088">0xdf00</option>
                              </b-form-select></label
                            ></span
                          >
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="thirdSIDBase"
                              >{{ $t("convertMessages.config.emulationSection.thirdSIDBase") }}
                              <b-form-select
                                id="thirdSIDBase"
                                class="right"
                                v-model="convertOptions.config.emulationSection.thirdSIDBase"
                                size="sm"
                                class="mt-3"
                                :select-size="1"
                              >
                                <option :value="54304">0xd420</option>
                                <option :value="54336">0xd440</option>
                                <option :value="54528">0xd500</option>
                                <option :value="56832">0xde00</option>
                                <option :value="57088">0xdf00</option>
                              </b-form-select></label
                            ></span
                          >
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="defaultClockSpeed"
                              >{{ $t("convertMessages.config.emulationSection.defaultClockSpeed") }}
                              <b-form-group>
                                <b-form-radio-group
                                  id="defaultClockSpeed"
                                  class="right"
                                  v-model="convertOptions.config.emulationSection.defaultClockSpeed"
                                  style="display: flex"
                                >
                                  <b-form-radio value="PAL">PAL</b-form-radio>
                                  <b-form-radio value="NTSC">NTSC</b-form-radio>
                                </b-form-radio-group>
                              </b-form-group>
                            </label></span
                          >
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="defaultEmulation"
                              >{{ $t("convertMessages.config.emulationSection.defaultEmulation") }}
                              <b-form-group>
                                <b-form-radio-group
                                  id="defaultEmulation"
                                  class="right"
                                  v-model="convertOptions.config.emulationSection.defaultEmulation"
                                  style="display: flex"
                                >
                                  <b-form-radio value="RESIDFP">RESIDFP</b-form-radio>
                                  <b-form-radio value="RESID">RESID</b-form-radio>
                                </b-form-radio-group>
                              </b-form-group>
                            </label></span
                          >
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="defaultSidModel"
                              >{{ $t("convertMessages.config.emulationSection.defaultSidModel") }}
                              <b-form-group>
                                <b-form-radio-group
                                  id="defaultSidModel"
                                  class="right"
                                  v-model="convertOptions.config.emulationSection.defaultSidModel"
                                  style="display: flex"
                                >
                                  <b-form-radio value="MOS6581">MOS6581</b-form-radio>
                                  <b-form-radio value="MOS8580">MOS8580</b-form-radio>
                                </b-form-radio-group>
                              </b-form-group>
                            </label></span
                          >
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="sampling">
                              {{ $t("convertMessages.config.audioSection.sampling") }}
                              <b-form-group>
                                <b-form-radio-group
                                  id="sampling"
                                  class="right"
                                  v-model="convertOptions.config.audioSection.sampling"
                                  style="display: flex"
                                >
                                  <b-form-radio value="DECIMATE">DECIMATE</b-form-radio>
                                  <b-form-radio value="RESAMPLE">RESAMPLE</b-form-radio>
                                </b-form-radio-group>
                              </b-form-group>
                            </label></span
                          >
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="samplingRate">
                              {{ $t("convertMessages.config.audioSection.samplingRate") }}
                              <b-form-group>
                                <b-form-radio-group
                                  id="samplingRate"
                                  class="right"
                                  v-model="convertOptions.config.audioSection.samplingRate"
                                  style="display: flex"
                                >
                                  <b-form-radio value="LOW">LOW</b-form-radio>
                                  <b-form-radio value="MEDIUM">MEDIUM</b-form-radio>
                                  <b-form-radio value="HIGH">HIGH</b-form-radio>
                                </b-form-radio-group>
                              </b-form-group>
                            </label></span
                          >
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="digiBoosted8580">
                              {{ $t("convertMessages.config.emulationSection.digiBoosted8580") }}
                              <b-form-checkbox
                                id="digiBoosted8580"
                                class="right"
                                v-model="convertOptions.config.emulationSection.digiBoosted8580"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="detectPSID64ChipModel">
                              {{ $t("convertMessages.config.emulationSection.detectPSID64ChipModel") }}
                              <b-form-checkbox
                                id="detectPSID64ChipModel"
                                class="right"
                                v-model="convertOptions.config.emulationSection.detectPSID64ChipModel"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="fakeStereo">
                              {{ $t("convertMessages.config.emulationSection.fakeStereo") }}
                              <b-form-checkbox
                                id="fakeStereo"
                                class="right"
                                v-model="convertOptions.config.emulationSection.fakeStereo"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="sidToRead"
                              >{{ $t("convertMessages.config.emulationSection.sidToRead") }}

                              <b-form-group class="right">
                                <b-form-radio-group
                                  id="sidToRead"
                                  v-model="convertOptions.config.emulationSection.sidToRead"
                                  style="display: flex"
                                >
                                  <b-form-radio value="FIRST_SID">{{ $t("firstSid") }}</b-form-radio>
                                  <b-form-radio value="SECOND_SID">{{ $t("secondSid") }}</b-form-radio>
                                  <b-form-radio value="THIRD_SID">{{ $t("thirdSid") }}</b-form-radio>
                                </b-form-radio-group>
                              </b-form-group>
                            </label></span
                          >
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="bufferSize">
                              {{ $t("convertMessages.config.audioSection.bufferSize") }}
                              <b-form-input
                                type="number"
                                min="0"
                                oninput="validity.valid||(value='');"
                                id="bufferSize"
                                class="right"
                                v-model.number="convertOptions.config.audioSection.bufferSize"
                              /> </label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting"
                            ><label for="audioBufferSize"
                              >{{ $t("convertMessages.config.audioSection.audioBufferSize") }}
                              <b-form-select
                                id="audioBufferSize"
                                class="right"
                                v-model="convertOptions.config.audioSection.audioBufferSize"
                                size="sm"
                                class="mt-3"
                              >
                                <option :value="1024">1024</option>
                                <option :value="2048">2048</option>
                                <option :value="4096">4096</option>
                                <option :value="8192">8192</option>
                                <option :value="16384">16384</option>
                              </b-form-select>
                            </label></span
                          >
                        </div>
                      </b-card-text>
                    </b-tab>

                    <b-tab>
                      <template #title>{{ $t("filterCfgHeader") }}</template>

                      <b-card-text>
                        <b-tabs
                          v-model="filterTabConfigIndex"
                          active-nav-item-class="font-weight-bold text-italic"
                          pills
                          card
                          small
                          align="right"
                        >
                          <b-tab>
                            <template #title>{{ $t("residFpFilterCfgHeader") }}</template>

                            <b-card-text>
                              <b-tabs
                                v-model="residFpFilterModelTabConfigIndex"
                                active-nav-item-class="font-weight-bold text-italic"
                                pills
                                card
                                small
                                align="right"
                              >
                                <b-tab>
                                  <template #title>{{ $t("residFpFilter6581CfgHeader") }}</template>

                                  <b-card-text>
                                    <b-tabs
                                      v-model="reSIDfpFilter6581SidTabConfigIndex"
                                      active-nav-item-class="font-weight-bold text-italic"
                                      pills
                                      card
                                      small
                                      align="right"
                                    >
                                      <b-tab>
                                        <template #title>{{ $t("reSIDfpFilter6581Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting"
                                              ><label for="reSIDfpFilter6581"
                                                >{{ $t("convertMessages.config.emulationSection.reSIDfpFilter6581") }}
                                                <b-form-select
                                                  id="reSIDfpFilter6581"
                                                  class="right"
                                                  v-model="convertOptions.config.emulationSection.reSIDfpFilter6581"
                                                  :options="reSIDfpFilters6581"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>

                                      <b-tab>
                                        <template #title>{{ $t("reSIDfpStereoFilter6581Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting"
                                              ><label for="reSIDfpStereoFilter6581"
                                                >{{
                                                  $t("convertMessages.config.emulationSection.reSIDfpStereoFilter6581")
                                                }}
                                                <b-form-select
                                                  id="reSIDfpStereoFilter6581"
                                                  class="right"
                                                  v-model="
                                                    convertOptions.config.emulationSection.reSIDfpStereoFilter6581
                                                  "
                                                  :options="reSIDfpFilters6581"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>
                                      <b-tab>
                                        <template #title>{{ $t("reSIDfpThirdSIDFilter6581Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting"
                                              ><label for="reSIDfpThirdSIDFilter6581"
                                                >{{
                                                  $t(
                                                    "convertMessages.config.emulationSection.reSIDfpThirdSIDFilter6581"
                                                  )
                                                }}
                                                <b-form-select
                                                  id="reSIDfpThirdSIDFilter6581"
                                                  class="right"
                                                  v-model="
                                                    convertOptions.config.emulationSection.reSIDfpThirdSIDFilter6581
                                                  "
                                                  :options="reSIDfpFilters6581"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>
                                    </b-tabs>
                                  </b-card-text>
                                </b-tab>

                                <b-tab>
                                  <template #title>{{ $t("residFpFilter8580CfgHeader") }}</template>

                                  <b-card-text>
                                    <b-tabs
                                      v-model="reSIDfpFilter8580SidTabConfigIndex"
                                      active-nav-item-class="font-weight-bold text-italic"
                                      pills
                                      card
                                      small
                                      align="right"
                                    >
                                      <b-tab>
                                        <template #title>{{ $t("reSIDfpFilter8580Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting"
                                              ><label for="reSIDfpFilter8580"
                                                >{{ $t("convertMessages.config.emulationSection.reSIDfpFilter8580") }}
                                                <b-form-select
                                                  id="reSIDfpFilter8580"
                                                  class="right"
                                                  v-model="convertOptions.config.emulationSection.reSIDfpFilter8580"
                                                  :options="reSIDfpFilters8580"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>
                                      <b-tab>
                                        <template #title>{{ $t("reSIDfpStereoFilter8580Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting"
                                              ><label for="reSIDfpStereoFilter8580"
                                                >{{
                                                  $t("convertMessages.config.emulationSection.reSIDfpStereoFilter8580")
                                                }}
                                                <b-form-select
                                                  id="reSIDfpStereoFilter8580"
                                                  class="right"
                                                  v-model="
                                                    convertOptions.config.emulationSection.reSIDfpStereoFilter8580
                                                  "
                                                  :options="reSIDfpFilters8580"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>

                                      <b-tab>
                                        <template #title>{{ $t("reSIDfpThirdSIDFilter8580Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting"
                                              ><label for="reSIDfpThirdSIDFilter8580"
                                                >{{
                                                  $t(
                                                    "convertMessages.config.emulationSection.reSIDfpThirdSIDFilter8580"
                                                  )
                                                }}
                                                <b-form-select
                                                  id="reSIDfpThirdSIDFilte8580"
                                                  class="right"
                                                  v-model="
                                                    convertOptions.config.emulationSection.reSIDfpThirdSIDFilter8580
                                                  "
                                                  :options="reSIDfpFilters8580"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>
                                    </b-tabs>
                                  </b-card-text>
                                </b-tab>
                              </b-tabs>
                            </b-card-text>
                          </b-tab>

                          <b-tab>
                            <template #title>{{ $t("residFilterCfgHeader") }}</template>

                            <b-card-text>
                              <b-tabs
                                v-model="residFilterModelTabConfigIndex"
                                active-nav-item-class="font-weight-bold text-italic"
                                pills
                                card
                                small
                                align="right"
                              >
                                <b-tab>
                                  <template #title>{{ $t("residFilter6581CfgHeader") }}</template>

                                  <b-card-text>
                                    <b-tabs
                                      v-model="filter6581SidTabConfigIndex"
                                      active-nav-item-class="font-weight-bold text-italic"
                                      pills
                                      card
                                      small
                                      align="right"
                                    >
                                      <b-tab>
                                        <template #title>{{ $t("filter6581Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting">
                                              <label for="filter6581"
                                                >{{ $t("convertMessages.config.emulationSection.filter6581") }}
                                                <b-form-select
                                                  id="filter6581"
                                                  class="right"
                                                  v-model="convertOptions.config.emulationSection.filter6581"
                                                  :options="reSIDfilters6581"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>

                                      <b-tab>
                                        <template #title>{{ $t("stereoFilter6581Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting"
                                              ><label for="stereoFilter6581"
                                                >{{ $t("convertMessages.config.emulationSection.stereoFilter6581") }}
                                                <b-form-select
                                                  id="stereoFilter6581"
                                                  class="right"
                                                  v-model="convertOptions.config.emulationSection.stereoFilter6581"
                                                  :options="reSIDfilters6581"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>
                                      <b-tab>
                                        <template #title>{{ $t("thirdSIDFilter6581Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting"
                                              ><label for="thirdSIDFilter6581"
                                                >{{ $t("convertMessages.config.emulationSection.thirdSIDFilter6581") }}
                                                <b-form-select
                                                  id="thirdSIDFilter6581"
                                                  class="right"
                                                  v-model="convertOptions.config.emulationSection.thirdSIDFilter6581"
                                                  :options="reSIDfilters6581"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>
                                    </b-tabs>
                                  </b-card-text>
                                </b-tab>

                                <b-tab>
                                  <template #title>{{ $t("residFilter8580CfgHeader") }}</template>

                                  <b-card-text>
                                    <b-tabs
                                      v-model="filter8580SidTabConfigIndex"
                                      active-nav-item-class="font-weight-bold text-italic"
                                      pills
                                      card
                                      small
                                      align="right"
                                    >
                                      <b-tab>
                                        <template #title>{{ $t("filter8580Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting">
                                              <label for="filter8580"
                                                >{{ $t("convertMessages.config.emulationSection.filter8580") }}
                                                <b-form-select
                                                  id="filter8580"
                                                  class="right"
                                                  v-model="convertOptions.config.emulationSection.filter8580"
                                                  :options="reSIDfilters8580"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>

                                      <b-tab>
                                        <template #title>{{ $t("stereoFilter8580Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting"
                                              ><label for="stereoFilter8580"
                                                >{{ $t("convertMessages.config.emulationSection.stereoFilter8580") }}
                                                <b-form-select
                                                  id="stereoFilter8580"
                                                  class="right"
                                                  v-model="convertOptions.config.emulationSection.stereoFilter8580"
                                                  :options="reSIDfilters8580"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>
                                      <b-tab>
                                        <template #title>{{ $t("thirdSIDFilter8580Header") }}</template>

                                        <b-card-text>
                                          <div class="settings-box">
                                            <span class="setting"
                                              ><label for="thirdSIDFilter8580"
                                                >{{ $t("convertMessages.config.emulationSection.thirdSIDFilter8580") }}
                                                <b-form-select
                                                  id="thirdSIDFilter8580"
                                                  class="right"
                                                  v-model="convertOptions.config.emulationSection.thirdSIDFilter8580"
                                                  :options="reSIDfilters8580"
                                                  size="sm"
                                                  class="mt-3"
                                                  :select-size="3"
                                                ></b-form-select> </label
                                            ></span>
                                          </div>
                                        </b-card-text>
                                      </b-tab>
                                    </b-tabs>
                                  </b-card-text>
                                </b-tab>
                              </b-tabs>
                            </b-card-text>
                          </b-tab>
                        </b-tabs>
                      </b-card-text>
                    </b-tab>
                    <b-tab>
                      <template #title>{{ $t("mutingCfgHeader") }}</template>

                      <b-card-text>
                        <b-tabs
                          v-model="muteSidTabConfigIndex"
                          active-nav-item-class="font-weight-bold text-italic"
                          pills
                          card
                          small
                          align="right"
                        >
                          <b-tab>
                            <template #title>{{ $t("muteSidHeader") }}</template>

                            <b-card-text>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteVoice1">
                                    {{ $t("convertMessages.config.emulationSection.muteVoice1") }}
                                    <b-form-checkbox
                                      id="muteVoice1"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteVoice1"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteVoice2">
                                    {{ $t("convertMessages.config.emulationSection.muteVoice2") }}
                                    <b-form-checkbox
                                      id="muteVoice2"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteVoice2"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteVoice3">
                                    {{ $t("convertMessages.config.emulationSection.muteVoice3") }}
                                    <b-form-checkbox
                                      id="muteVoice3"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteVoice3"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteVoice4">
                                    {{ $t("convertMessages.config.emulationSection.muteVoice4") }}
                                    <b-form-checkbox
                                      id="muteVoice4"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteVoice4"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                            </b-card-text>
                          </b-tab>

                          <b-tab>
                            <template #title>{{ $t("muteStereoSidHeader") }}</template>

                            <b-card-text>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteStereoVoice1">
                                    {{ $t("convertMessages.config.emulationSection.muteStereoVoice1") }}
                                    <b-form-checkbox
                                      id="muteStereoVoice1"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteStereoVoice1"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteStereoVoice2">
                                    {{ $t("convertMessages.config.emulationSection.muteStereoVoice2") }}
                                    <b-form-checkbox
                                      id="muteStereoVoice2"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteStereoVoice2"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteStereoVoice3">
                                    {{ $t("convertMessages.config.emulationSection.muteStereoVoice3") }}
                                    <b-form-checkbox
                                      id="muteStereoVoice3"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteStereoVoice3"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteStereoVoice4">
                                    {{ $t("convertMessages.config.emulationSection.muteStereoVoice4") }}
                                    <b-form-checkbox
                                      id="muteStereoVoice4"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteStereoVoice4"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                            </b-card-text>
                          </b-tab>

                          <b-tab>
                            <template #title>{{ $t("muteThirdSidHeader") }}</template>

                            <b-card-text>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteThirdSIDVoice1">
                                    {{ $t("convertMessages.config.emulationSection.muteThirdSIDVoice1") }}
                                    <b-form-checkbox
                                      id="muteThirdSIDVoice1"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteThirdSIDVoice1"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteThirdSIDVoice2">
                                    {{ $t("convertMessages.config.emulationSection.muteThirdSIDVoice2") }}
                                    <b-form-checkbox
                                      id="muteThirdSIDVoice2"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteThirdSIDVoice2"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteThirdSIDVoice3">
                                    {{ $t("convertMessages.config.emulationSection.muteThirdSIDVoice3") }}
                                    <b-form-checkbox
                                      id="muteThirdSIDVoice3"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteThirdSIDVoice3"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                              <div class="settings-box">
                                <span class="setting">
                                  <label for="muteThirdSIDVoice4">
                                    {{ $t("convertMessages.config.emulationSection.muteThirdSIDVoice4") }}
                                    <b-form-checkbox
                                      id="muteThirdSIDVoice4"
                                      class="right"
                                      v-model="convertOptions.config.emulationSection.muteThirdSIDVoice4"
                                    >
                                    </b-form-checkbox></label
                                ></span>
                              </div>
                            </b-card-text>
                          </b-tab>
                        </b-tabs>
                      </b-card-text>
                    </b-tab>

                    <b-tab>
                      <template #title>{{ $t("floppyCartCfgHeader") }}</template>

                      <b-card-text>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="jiffyDosInstalled">
                              {{ $t("convertMessages.config.c1541Section.jiffyDosInstalled") }}
                              <b-form-checkbox
                                id="jiffyDosInstalled"
                                class="right"
                                v-model="convertOptions.config.c1541Section.jiffyDosInstalled"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <b class="right"> {{ $t("CART_NOTES") }} </b>
                          </span>
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="reuSize">
                              {{ $t("convertMessages.reuSize") }}
                              <b-form-group class="right">
                                <b-form-radio-group v-model="convertOptions.reuSize">
                                  <b-form-radio value="null">Auto</b-form-radio>
                                  <b-form-radio value="128">REU 1700 (128KB)</b-form-radio>
                                  <b-form-radio value="512">REU 1750 (512KB)</b-form-radio>
                                  <b-form-radio value="256">REU 1764 (256KB)</b-form-radio>
                                  <b-form-radio value="2048">REU 1750 XL (2MB)</b-form-radio>
                                  <b-form-radio value="16384">REU (16MB)</b-form-radio>
                                </b-form-radio-group>
                              </b-form-group></label
                            ></span
                          >
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="sfxSoundExpander">
                              {{ $t("convertMessages.sfxSoundExpander") }}
                              <b-form-checkbox
                                id="sfxSoundExpander"
                                class="right"
                                v-model="convertOptions.sfxSoundExpander"
                              >
                              </b-form-checkbox></label
                          ></span>
                        </div>
                        <div class="settings-box">
                          <span class="setting">
                            <label for="sfxSoundExpanderType">
                              {{ $t("convertMessages.sfxSoundExpanderType") }}
                              <b-form-group>
                                <b-form-radio-group
                                  id="sfxSoundExpanderType"
                                  class="right"
                                  v-model="convertOptions.sfxSoundExpanderType"
                                >
                                  <b-form-radio value="0">OPL1 (YM3526)</b-form-radio>
                                  <b-form-radio value="1">OPL2 (YM3812)</b-form-radio>
                                </b-form-radio-group>
                              </b-form-group></label
                            ></span
                          >
                        </div>
                      </b-card-text>
                    </b-tab>
                  </b-tabs>
                </b-card-text>
              </b-tab>
            </b-tabs>
          </b-card>
        </div>
      </b-form>
    </div>

    <script>
      async function init_hardsid() {
        await hardsid_usb_init(true, SysMode.SIDPLAY);
        deviceCount = hardsid_usb_getdevcount();
        console.log("Device count: " + deviceCount);
        if (deviceCount > 0) {
          chipCount = hardsid_usb_getsidcount(0);
          console.log("Chip count: " + chipCount);
          return 0;
        }
        return -1;
      }
      async function init_exsid() {
        var ok = await exSID_init();
        if (ok != -1) {
          deviceCount = 1;
          return 0;
        }
        return -1;
      }
      async function init_sidblaster() {
        var ok = await sidblaster_init();
        if (ok != -1) {
          deviceCount = 1;
          return 0;
        }
        return -1;
      }
      async function reset_hardsid() {
        await hardsid_usb_abortplay(0);
        for (let chipNum = 0; chipNum < chipCount; chipNum++) {
          await hardsid_usb_reset(0, chipNum, 0x00);
        }
      }
      async function reset_exsid() {
        if (mapping) {
          const chipModel = mapping[0];
          const stereo = mapping[-1] === "true";
          const fakeStereo = mapping[-2] === "true";
          const cpuClock = mapping[-3];

          if (fakeStereo) {
            lastChipModel = chipModel;
            exSID_chipselect(ChipSelect.XS_CS_BOTH);
          }
          exSID_audio_op(AudioOp.XS_AU_MUTE);
          exSID_clockselect(cpuClock === "PAL" ? ClockSelect.XS_CL_PAL : ClockSelect.XS_CL_NTSC);
          if (stereo) {
            exSID_audio_op(chipModel === "MOS6581" ? AudioOp.XS_AU_6581_8580 : AudioOp.XS_AU_8580_6581);
          } else {
            exSID_audio_op(chipModel === "MOS6581" ? AudioOp.XS_AU_6581_6581 : AudioOp.XS_AU_8580_8580);
          }
          exSID_audio_op(AudioOp.XS_AU_UNMUTE);
        }
        exSID_reset(0);
      }
      async function reset_sidblaster() {
        sidblaster_reset(0);
      }
      async function write_hardsid(write) {
        while ((await hardsid_usb_delay(0, write.cycles)) == WState.BUSY) {}
        while ((await hardsid_usb_write(0, (write.chip << 5) | write.reg, write.value)) == WState.BUSY) {}
      }
      async function write_exsid(write) {
        if (write.reg <= 0x18) {
          // "Ragga Run.sid" denies to work!

          const chipModel = mapping[write.chip];
          if (lastChipModel !== chipModel) {
            exSID_chipselect(chipModel === "MOS8580" ? ChipSelect.XS_CS_CHIP1 : ChipSelect.XS_CS_CHIP0);
            lastChipModel = chipModel;
          }
          exSID_clkdwrite(write.cycles, write.reg, write.value);
        }
      }
      async function write_sidblaster(write) {
        sidblaster_write(write.cycles, write.reg, write.value);
      }
      async function quit_hardsid() {}
      async function quit_exsid() {
        await exSID_exit();
      }
      async function quit_sidblaster() {
        await sidblaster_exit();
      }
      async function next_hardsid() {
        await hardsid_usb_sync(0);
        while ((await hardsid_usb_flush(0)) == WState.BUSY) {}
        return 0;
      }
      async function next_exsid() {
        if (exSID_is_playing()) {
          return -1;
        } else {
          exSID_reset(0);
          return 0;
        }
      }
      async function next_sidblaster() {
        if (sidblaster_is_playing()) {
          return -1;
        } else {
          sidblaster_reset();
          return 0;
        }
      }
      const HardwareFunctions = {
        init: undefined,
        write: undefined,
        next: undefined,
        reset: undefined,
        quit: undefined,
        mapping: undefined,
      };
      const Chip = {
        NEXT: -1,
        RESET: -2,
        QUIT: -3,
      };
      var deviceCount = 0;
      var chipCount = 0;
      var ajaxRequest;
      var timer;
      var write;

      function Queue() {
        var head, tail;
        return Object.freeze({
          enqueue(value) {
            const link = { value, next: undefined };
            tail = head ? (tail.next = link) : (head = link);
          },
          dequeue() {
            if (head) {
              const value = head.value;
              head = head.next;
              return value;
            }
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
      var sidWriteQueue = new Queue();
      var mapping, lastChipModel;

      function uriEncode(entry) {
        // escape is deprecated and cannot handle utf8
        // encodeURI() will not encode: ~!@#$&*()=:/,;?+'
        // untested characters: !*=,;?
        // tested characters: /~@#$&():+''
        return encodeURI(entry).replace(/\+/g, "%2B").replace(/#/g, "%23").replace(/&/g, "%26");
      }
      function petsciiToFont(str, fontSet) {
        var original = str;
        var result = "";
        for (var i = 0; i < original.length; i++) {
          let c = original.charCodeAt(i);
          if ((c & 0x60) == 0) {
            result = result + String.fromCharCode(c | 0x40 | (fontSet ^ 0x0200));
          } else {
            result = result + String.fromCharCode(c | fontSet);
          }
        }
        return result;
      }
      /**
       * Returns a random integer between min (inclusive) and max (inclusive).
       * The value is no lower than min (or the next integer greater than min
       * if min isn't an integer) and no greater than max (or the next integer
       * lower than max if max isn't an integer).
       * Using Math.round() will give you a non-uniform distribution!
       */
      function getRandomInt(min, max) {
        min = Math.ceil(min);
        max = Math.floor(max);
        return Math.floor(Math.random() * (max - min + 1)) + min;
      }
      // Month in JavaScript is 0-indexed (January is 0, February is 1, etc),
      // but by using 0 as the day it will give us the last day of the prior
      // month. So passing in 1 as the month number will return the last day
      // of January, not February
      function daysInMonth(month, year) {
        return new Date(year, month, 0).getDate();
      }
      function timeConverter(time) {
        if (("" + time).includes(":")) {
          // HH:MM:SS -> MM:SS
          return time.split(":").slice(0, 2).join(":");
        } else {
          // SS -> MM:SS
          return new Date(time * 1000).toISOString().slice(14, 19);
        }
      }
      function download(filename, contentType, text) {
        var pom = document.createElement("a");
        pom.setAttribute("href", "data:" + contentType + "," + encodeURIComponent(text));
        pom.setAttribute("download", filename);

        if (document.createEvent) {
          var event = document.createEvent("MouseEvents");
          event.initEvent("click", true, true);
          pom.dispatchEvent(event);
        } else {
          pom.click();
        }
      }
      const messages = {
        en: {
          CON: "Login",
          SIDS: "Directories",
          ASSEMBLY64: "Search",
          SID: "SID",
          STIL: "STIL",
          STILINFO: {
            SUBTUNE: "Song",
            NAME: "Name",
            AUTHOR: "Author",
            TITLE: "Title",
            ARTIST: "Artist",
          },
          PL: "Playlist",
          CFG: "Configuration",
          HARDWARE: "Hardware",
          USE_REAL_HARDWARE:
            "You can use real Hardware connected with USB directly inside your Browser whether on a PC or on a mobile device.",
          HARDWARE_PREPARATION_1: "Only on a Windows-PC WinUSB is required, download from",
          HARDWARE_PREPARATION_2: ", but on Linux and MacOSX it works out-of-the-box.",
          HERE: "here",
          HARDSID: "HardSID 4U, HardSID UPlay and HardSID Uno",
          EXSID: "ExSID, ExSID+",
          SIDBLASTER: "SIDBlaster",
          CONNECT: "Connect...",
          USE_MOBILE_DEVICES_1: "To use mobile devices, please use an",
          USE_MOBILE_DEVICES_2: "USBC to USB adapter",
          STREAMING_NOTES:
            "This function requires intensive streaming of SID register writes from the server to the browser! Please make sure you are connected to a free WLAN. I will not take responsibility for any costs, that arise from streaming from the internet!",
          CART_NOTES: "Important: Only one cartridge can be plugged-in at the same time!",
          currentlyPlaying: "Playing: ",
          parentDirectoryHint: "Go up one Level",
          sidInfoKey: "Name",
          sidInfoValue: "Value",
          HVSCEntry: {
            path: "Full Path",
            name: "File Name",
            title: "Title",
            author: "Author",
            released: "Released",
            format: "Format",
            playerId: "Player ID",
            noOfSongs: "No. of Songs",
            startSong: "Start Song",
            clockFreq: "Clock Freq.",
            speed: "Speed",
            sidModel1: "SID Model 1",
            sidModel2: "SID Model 2",
            sidModel3: "SID Model 3",
            compatibility: "Compatibility",
            tuneLength: "Tune Length (s)",
            audio: "Audio",
            sidChipBase1: "SID Chip Base 1",
            sidChipBase2: "SID Chip Base 2",
            sidChipBase3: "SID Chip Base 3",
            driverAddress: "Driver Address",
            loadAddress: "Load Address",
            loadLength: "Load Length",
            initAddress: "Init Address",
            playerAddress: "Player Address",
            fileDate: "File Date",
            fileSizeKb: "File Size (kb)",
            tuneSizeB: "Tune Size (b)",
            relocStartPage: "Reloc. Start Page",
            relocNoPages: "Reloc. no. Pages",
            stilGlbComment: "Tune Size (b)",
          },
          username: "Username",
          password: "Password",
          filter: "Top:",
          onefilerTop200: "Onefiler",
          toolsTop100: "Tools",
          gamesTop200: "Games",
          top200: "Demos",
          musicTop200: "Music",
          graphicsTop200: "Graphics",
          addAllToPlaylist: "All",
          downloadMP3: "MP3",
          remove: "Remove last tune",
          removeReally: "Do you really want to remove the playlist tune?",
          next: "Next",
          reset: "Reset",
          startImport: "Import",
          fetchFavorites: "Download",
          removePlaylist: "Remove All",
          confirmationTitle: "Confirmation Dialogue",
          removePlaylistReally: "Do you really want to remove ALL playlist entries?",
          exportPlaylist: "Export",
          importPlaylistPlaceholder: "Import favorites or drop it here...",
          importPlaylistDropPlaceholder: "Drop favorites here...",
          random: "Random Playback",
          mobileProfile: "Mobile profile",
          wifiProfile: "WiFi profile",
          stereoMode: "Stereo Mode",
          streamingCfgHeader: "Streaming",
          audioStreamingCfgHeader: "Audio",
          videoStreamingCfgHeader: "Video",
          playbackCfgHeader: "Playback",
          emulationCfgHeader: "Emulation",
          audioCfgHeader: "Audio",
          filterCfgHeader: "Filter",
          residFpFilterCfgHeader: "RESIDFP",
          residFilterCfgHeader: "RESID",
          residFpFilter6581CfgHeader: "MOS6581",
          residFpFilter8580CfgHeader: "MOS8580",
          residFilter6581CfgHeader: "MOS6581",
          residFilter8580CfgHeader: "MOS8580",
          reSIDfpFilter6581Header: "SID",
          reSIDfpStereoFilter6581Header: "Stereo SID",
          reSIDfpThirdSIDFilter6581Header: "3rd SID",
          reSIDfpFilter8580Header: "SID",
          reSIDfpStereoFilter8580Header: "Stereo SID",
          reSIDfpThirdSIDFilter8580Header: "3rd SID",
          filter6581Header: "SID",
          stereoFilter6581Header: "Stereo SID",
          thirdSIDFilter6581Header: "3rd SID",
          filter8580Header: "SID",
          stereoFilter8580Header: "Stereo SID",
          thirdSIDFilter8580Header: "3rd SID",
          mutingCfgHeader: "Muting",
          muteSidHeader: "SID",
          muteStereoSidHeader: "Stereo SID",
          muteThirdSidHeader: "3rd SID",
          floppyCartCfgHeader: "Floppy/Cart",
          showDirectory: "DIR",
          firstSid: "Main SID",
          secondSid: "Stereo SID",
          thirdSid: "3-SID",
          setDefault: "Restore Defaults",
          setDefaultUser: "Restore Default User",
          setDefaultReally: "Do you really want to restore defaults?",
          setDefaultUserReally: "Do you really want to restore default user?",
          firstCategory: "",

          convertMessages: $convertMessagesEn,
        },
        de: {
          CON: "Anmeldung",
          SIDS: "Verzeichnisse",
          ASSEMBLY64: "Suche",
          SID: "SID",
          STIL: "STIL",
          STILINFO: {
            SUBTUNE: "Song",
            NAME: "Name",
            AUTHOR: "Autor",
            TITLE: "Titel",
            ARTIST: "K\u00fcnstler",
          },
          PL: "Favoriten",
          CFG: "Konfiguration",
          HARDWARE: "Hardware",
          USE_REAL_HARDWARE:
            "Sie k\u00f6nnen echte Hardware, die per USB angeschlossen ist, direkt in ihrem Browser verwenden und zwar entweder am PC oder an ihrem Handy.",
          HARDWARE_PREPARATION_1:
            "Auf Linux und MacOSX funktioniert es auf Anhieb, nur auf einem Windows-PC ist WinUSB erforderlich, bitte",
          HARDWARE_PREPARATION_2: " herunterladen.",
          HERE: "hier",
          HARDSID: "HardSID 4U, HardSID UPlay and HardSID Uno",
          EXSID: "ExSID, ExSID+",
          SIDBLASTER: "SIDBlaster",
          CONNECT: "Verbinden...",
          USE_MOBILE_DEVICES_1: "Um Mobilger\u00e4te zu verwenden, verwenden Sie bitte einen",
          USE_MOBILE_DEVICES_2: "USBC nach USB adapter",
          STREAMING_NOTES:
            "Diese Funktion macht von intensivem Streaming der SID-Register Schreibbefehle vom Server zum Browser gebrauch! Bitte stellen Sie sicher, dass sie mit einem freien WLAN verbunden sind. Ich \u00fcbernehme keine Verantwortung f\u00fcr jegliche Kosten, die f\u00fcr das Streaming \u00fcber das Internet entstehen k\u00f6nnten!",
          CART_NOTES: "Wichtig: Es es kann nur eine Cartridge zur selben Zeit eingesteckt sein!",
          currentlyPlaying: "Es spielt: ",
          parentDirectoryHint: "Gehe eine Ebene h\u00f6her",
          sidInfoKey: "Name",
          sidInfoValue: "Wert",
          HVSCEntry: {
            path: "Dateipfad",
            name: "Dateiname",
            title: "Titel",
            author: "Autor",
            released: "Publiziert",
            format: "Format",
            playerId: "Player ID",
            noOfSongs: "Song Anzahl",
            startSong: "Start Song",
            clockFreq: "Takt Frequenz",
            speed: "Geschwindigkeit",
            sidModel1: "SID Model 1",
            sidModel2: "SID Model 2",
            sidModel3: "SID Model 3",
            compatibility: "Kompatibilit\u00e4t",
            tuneLength: "Tune L\u00e4nge (s)",
            audio: "Ton",
            sidChipBase1: "SID Chip Basisadresse 1",
            sidChipBase2: "SID Chip Basisadresse 2",
            sidChipBase3: "SID Chip Basisadresse 3",
            driverAddress: "Treiberaddresse",
            loadAddress: "Lade-Addresse",
            loadLength: "Ladel\u00e4nge",
            initAddress: "Init-Addresse",
            playerAddress: "Player-Addresse",
            fileDate: "File Datum",
            fileSizeKb: "File Gr\u00f6sse (kb)",
            tuneSizeB: "Tune Gr\u00f6sse (b)",
            relocStartPage: "Reloc. Start Seite",
            relocNoPages: "Reloc. Seitenanzahl",
            stilGlbComment: "STIL glb. Kommentar",
          },
          username: "Benutzername",
          password: "Passwort",
          filter: "Top:",
          onefilerTop200: "Onefiler",
          toolsTop100: "Tools",
          gamesTop200: "Games",
          top200: "Demos",
          musicTop200: "Music",
          graphicsTop200: "Graphics",
          addAllToPlaylist: "Alle",
          downloadMP3: "MP3",
          remove: "Letzten Tune l\u00f6schen",
          removeReally: "Wollen sie wirklich den Favoriten l\u00f6schen?",
          next: "N\u00e4chster",
          reset: "Zur\u00fccksetzen",
          startImport: "Importieren",
          fetchFavorites: "Laden",
          removePlaylist: "L\u00f6schen",
          confirmationTitle: "Sicherheitsabfrage",
          removePlaylistReally: "Wollen sie wirklich ALL Favoriten l\u00f6schen?",
          exportPlaylist: "Export",
          importPlaylistPlaceholder: "Importiere Favoriten oder DnD...",
          importPlaylistDropPlaceholder: "DnD Favoriten hier...",
          random: "Zuf\u00e4llige Wiedergabe",
          mobileProfile: "Mobiles Profil",
          wifiProfile: "WiFi Profil",
          stereoMode: "Stereo Mode",
          streamingCfgHeader: "Streaming",
          audioStreamingCfgHeader: "Audio",
          videoStreamingCfgHeader: "Video",
          playbackCfgHeader: "Wiedergabe",
          emulationCfgHeader: "Emulation",
          audioCfgHeader: "Audio",
          filterCfgHeader: "Filter",
          residFpFilterCfgHeader: "RESIDFP",
          residFilterCfgHeader: "RESID",
          residFpFilter6581CfgHeader: "MOS6581",
          residFpFilter8580CfgHeader: "MOS8580",
          residFilter6581CfgHeader: "MOS6581",
          residFilter8580CfgHeader: "MOS8580",
          reSIDfpFilter6581Header: "SID",
          reSIDfpStereoFilter6581Header: "Stereo SID",
          reSIDfpThirdSIDFilter6581Header: "3. SID",
          reSIDfpFilter8580Header: "SID",
          reSIDfpStereoFilter8580Header: "Stereo SID",
          reSIDfpThirdSIDFilter8580Header: "3. SID",
          filter6581Header: "SID",
          stereoFilter6581Header: "Stereo SID",
          thirdSIDFilter6581Header: "3. SID",
          filter8580Header: "SID",
          stereoFilter8580Header: "Stereo SID",
          thirdSIDFilter8580Header: "3. SID",
          mutingCfgHeader: "Stummschalten",
          muteSidHeader: "SID",
          muteStereoSidHeader: "Stereo SID",
          muteThirdSidHeader: "3. SID",
          floppyCartCfgHeader: "Floppy/Cart",
          showDirectory: "DIR",
          firstSid: "Haupt SID",
          secondSid: "Stereo SID",
          thirdSid: "3-SID",
          setDefault: "Standardeinstellungen wiederherstellen",
          setDefaultUser: "Standardbenutzer wiederherstellen",
          setDefaultReally: "Wollen sie wirklich die Standardeinstellungen wiederherstellen?",
          setDefaultUserReally: "Wollen sie wirklich den Standardbenutzer wiederherstellen?",
          firstCategory: "",

          convertMessages: $convertMessagesDe,
        },
      };

      const i18n = new VueI18n({
        locale: "en", // set locale
        messages, // set locale messages
      });

      new Vue({
        el: "#app",
        i18n, //import mutil-lang
        data: {
          carouselImageHeight:
            window.innerHeight > window.innerWidth ? window.innerHeight * 0.3 : window.innerHeight * 0.8,
          slide: 0,
          sliding: null,
          showAudio: false,
          langs: ["de", "en"],
          directoryMode: 0,
          // CON (connection parameters)
          username: "jsidplay2",
          password: "jsidplay2!",
          // SIDS (directories containing SIDS)
          directory: [],
          rootDir: {
            filename: "/",
            loading: false,
          },
          top200Dir: {
            filename: "/Assembly64/Demos/CSDB/Top200",
            loading: false,
          },
          oneFilerTop200Dir: {
            filename: "/Assembly64/Demos/CSDB/Onefile-top200",
            loading: false,
          },
          toolsTop200Dir: {
            filename: "/Assembly64/Tools/CSDB/Top100",
            loading: false,
          },
          musicTop200Dir: {
            filename: "/Assembly64/Music/CSDB/Top200",
            loading: false,
          },
          graphicsTop200Dir: {
            filename: "/Assembly64/Graphics/CSDB/Top200",
            loading: false,
          },
          gamesTop200Dir: {
            filename: "/Assembly64/Games/CSDB/Top200",
            loading: false,
          },
          // SID (info + picture)
          infos: "",
          stil: [],
          hasStil: false,
          hasHardware: false,
          picture: "",
          currentSid: "",
          // ASSEMBLY64
          category: "",
          categories: [],
          searchResults: [],
          searchFields: [
            {
              key: "category",
              label: "Content",
              sortable: true,
              class: "field-category",
            },
            {
              key: "name",
              sortable: true,
              class: "field-name",
            },
            {
              key: "event",
              sortable: true,
              class: "field-event",
            },
            {
              key: "released",
              label: "Release",
              sortable: true,
              class: "field-released",
            },
            {
              key: "handle",
              sortable: true,
              class: "field-handle",
            },
            {
              key: "rating",
              sortable: true,
              class: "field-rating",
            },
            { key: "actions" },
          ],
          contentEntryFields: [
            {
              key: "filename",
              label: "File",
            },
          ],
          sortBy: null,
          sortDesc: null,
          name: "",
          event: "",
          released: "",
          rating: "",
          handle: "",
          // PL (Playlist)
          importFile: null,
          playlist: [],
          playlistIndex: 0,
          random: true,
          // CFG (configuration)
          stereoMode: "AUTO",
          // pre-fetched filter definitions
          reSIDfilters6581: [],
          reSIDfilters8580: [],
          reSIDfpFilters6581: [],
          reSIDfpFilters8580: [],
          cbrs: [-1, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320],
          vbrQualities: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
          // Misc.
          tabIndex: 0,
          tabConfigIndex: 0,
          streamingTabConfigIndex: 0,
          filterTabConfigIndex: 0,
          residFpFilterModelTabConfigIndex: 0,
          residFilterModelTabConfigIndex: 0,
          muteSidTabConfigIndex: 0,
          reSIDfpFilter6581SidTabConfigIndex: 0,
          reSIDfpFilter8580SidTabConfigIndex: 0,
          filter6581SidTabConfigIndex: 0,
          filter8580SidTabConfigIndex: 0,
          loadingSid: false,
          loadingStil: false,
          loadingAssembly64: false,
          loadingPl: false,
          loadingCfg: false,
          convertOptions: $convertOptions,
          defaultConvertOptions: $convertOptions,
        },
        computed: {
          playlistEntryUrl: function () {
            if (this.playlist.length === 0 || this.playlistIndex >= this.playlist.length) {
              return undefined;
            } else {
              return this.createConvertUrl(
                "",
                this.playlist[this.playlistIndex].filename,
                this.playlist[this.playlistIndex].itemId,
                this.playlist[this.playlistIndex].categoryId
              );
            }
          },
          reuParameters: function () {
            return this.convertOptions.reuSize !== null ? "&reuSize=" + this.convertOptions.reuSize : "";
          },
          sfxSoundExpanderParameters: function () {
            return (
              "&sfxSoundExpander=" +
              this.convertOptions.sfxSoundExpander +
              (this.convertOptions.sfxSoundExpanderType !== null
                ? "&sfxSoundExpanderType=" + this.convertOptions.sfxSoundExpanderType
                : "")
            );
          },
          stereoParameters: function () {
            if (this.stereoMode === "FORCE_2SID") {
              return "&dualSID=true&dualSIDBase=" + this.convertOptions.config.emulationSection.dualSidBase;
            } else if (this.stereoMode === "FORCE_3SID") {
              return (
                "&dualSID=true&dualSIDBase=" +
                this.convertOptions.config.emulationSection.dualSidBase +
                "&thirdSID=true&thirdSIDBase=" +
                this.convertOptions.config.emulationSection.thirdSIDBase
              );
            }
            return "";
          },
          translatedFields() {
            return [
              {
                key: "Name",
                label: i18n.t("sidInfoKey"),
              },
              {
                key: "Value",
                label: i18n.t("sidInfoValue"),
              },
            ];
          },
          translatedInfos: function () {
            if (!this.infos) {
              return [];
            }
            return this.infos.map(function (obj) {
              return {
                Name: i18n.t(obj.Name),
                Value: obj.Value,
                opacity: obj.Name == "HVSCEntry.path",
              };
            });
          },
        },
        methods: {
          init: async function () {
            sidWriteQueue.clear();
            if (typeof timer !== "undefined") {
              clearTimeout(timer);
            }
            if ((await HardwareFunctions.init()) == 0) {
              sidWriteQueue.enqueue({
                chip: Chip.RESET,
              });
              // regularly process SID write queue from now on!
              timer = setTimeout(() => this.doPlay());
              this.showAudio = false;
            }
          },
          doPlay: async function () {
            while (sidWriteQueue.isNotEmpty()) {
              write = sidWriteQueue.dequeue();

              if (write.chip == Chip.QUIT) {
                await HardwareFunctions.quit();
                return;
              } else if (write.chip == Chip.RESET) {
                await HardwareFunctions.reset();
                timer = setTimeout(() => this.doPlay(), 250);
                return;
              } else if (write.chip == Chip.NEXT) {
                if ((await HardwareFunctions.next()) == 0) {
                  Vue.nextTick(() => this.setNextPlaylistEntry());
                } else {
                  sidWriteQueue.enqueue({
                    chip: Chip.NEXT,
                  });
                  timer = setTimeout(() => this.doPlay(), 250);
                  return;
                }
              } else {
                await HardwareFunctions.write(write);
              }
            }
            timer = setTimeout(() => this.doPlay());
          },
          play: function (autostart, entry, itemId, categoryId) {
            if (deviceCount > 0) {
              // Hardware PLAY
              this.showAudio = false;
              this.pause();

              axios({
                method: "get",
                url: this.createSIDMappingUrl(entry, itemId, categoryId),
                auth: {
                  username: this.username,
                  password: this.password,
                },
              }).then((response) => {
                mapping = response.data;

                // cancel  previous ajax if exists
                if (ajaxRequest) {
                  ajaxRequest.cancel();
                }
                // creates a new token for upcoming ajax (overwrite the previous one)
                ajaxRequest = axios.CancelToken.source();

                lastChipModel = undefined;
                let start = 1;
                sidWriteQueue.clear();
                sidWriteQueue.enqueue({
                  chip: Chip.RESET,
                });

                axios({
                  method: "get",
                  url:
                    this.createConvertUrl(autostart, entry, itemId, categoryId) +
                    "&audio=SID_REG&sidRegFormat=C64_JUKEBOX",
                  cancelToken: ajaxRequest.token,
                  onDownloadProgress: (progressEvent) => {
                    const dataChunk = progressEvent.currentTarget.response;

                    var i = start;
                    while ((i = dataChunk.indexOf("\n", start)) != -1) {
                      const cells = dataChunk.substring(start, i).split(",");
                      const address = parseInt(cells[1], 16);
                      sidWriteQueue.enqueue({
                        chip: mapping[address & 0xffe0] || mapping[0xd400],
                        cycles: parseInt(cells[0]),
                        reg: address & 0x1f,
                        value: parseInt(cells[2], 16),
                      });
                      start = i + 1;
                    }
                  },
                })
                  .then((response) => {
                    sidWriteQueue.enqueue({
                      chip: Chip.NEXT,
                    });
                  })
                  .catch((err) => {
                    if (axios.isCancel(err)) {
                      sidWriteQueue.clear();
                      sidWriteQueue.enqueue({
                        chip: Chip.RESET,
                      });
                    }
                  });
              });
            } else {
              // Software PLAY
              this.showAudio = true;
              this.$refs.audioElm.src = this.createConvertUrl(autostart, entry, itemId, categoryId);
              this.$refs.audioElm.play();
            }
          },
          stop: function () {
            if (ajaxRequest) {
              ajaxRequest.cancel();
            }
            sidWriteQueue.clear();
            sidWriteQueue.enqueue({
              chip: Chip.RESET,
            });
          },
          end: function () {
            stop();
            sidWriteQueue.enqueue({
              chip: Chip.QUIT,
            });
            deviceCount = 0;
            this.showAudio = true;
          },
          sortChanged(e) {
            localStorage.sortBy = JSON.stringify(e.sortBy);
            localStorage.sortDesc = JSON.stringify(e.sortDesc);
            this.sortBy = e.sortBy;
            this.sortDesc = e.sortDesc;
          },
          onSlideStart(slide) {
            this.sliding = true;
          },
          onSlideEnd(slide) {
            this.sliding = false;
          },
          updateLanguage() {
            localStorage.locale = this.$i18n.locale;
          },
          shortEntry: function (filename) {
            return filename
              .split("/")
              .slice(filename.endsWith("/") ? -2 : -1)
              .join("/");
          },
          volumeFormatter: function (value) {
            return value.toLocaleString(this.$i18n.locale) + " db";
          },
          balanceFormatter: function (value) {
            return value.toLocaleString(this.$i18n.locale);
          },
          delayFormatter: function (value) {
            return value.toLocaleString(this.$i18n.locale) + " ms";
          },
          pathEntry: function (filename) {
            const files = filename.split("/");
            return "/" + files.slice(-files.length + 1, filename.endsWith("/") ? -2 : -1).join("/");
          },
          getVariant: function (entry) {
            if (this.isDirectory(entry)) {
              return "";
            } else if (this.isMusic(entry)) {
              return "primary";
            } else if (this.isVideo(entry)) {
              return "success";
            }
            return "dark";
          },
          pause: function () {
            this.$refs.audioElm.pause();
          },
          isDirectory: function (entry) {
            return entry.filename.endsWith("/");
          },
          isParentDirectory: function (entry) {
            return entry.filename.endsWith("../");
          },
          isMusic: function (entry) {
            let filename = entry.filename.toLowerCase();
            if (this.convertOptions.videoTuneAsAudio) {
              return (
                filename.endsWith(".sid") ||
                filename.endsWith(".dat") ||
                filename.endsWith(".mus") ||
                filename.endsWith(".str") ||
                filename.endsWith(".c64") ||
                filename.endsWith(".prg") ||
                filename.endsWith(".p00") ||
                filename.endsWith(".mp3")
              );
            } else {
              return (
                filename.endsWith(".sid") ||
                filename.endsWith(".dat") ||
                filename.endsWith(".mus") ||
                filename.endsWith(".str") ||
                filename.endsWith(".mp3")
              );
            }
          },
          isPicture: function (entry) {
            let filename = entry.filename.toLowerCase();
            return (
              (filename.endsWith(".apng") ||
                filename.endsWith(".gif") ||
                filename.endsWith(".ico") ||
                filename.endsWith(".cur") ||
                filename.endsWith(".jpg") ||
                filename.endsWith(".jpeg") ||
                filename.endsWith(".jfif") ||
                filename.endsWith(".pjpeg") ||
                filename.endsWith(".pjp") ||
                filename.endsWith(".png") ||
                filename.endsWith(".svg")) &&
              !filename.endsWith("small.jpg")
            );
          },
          isVideo: function (entry) {
            let filename = entry.filename.toLowerCase();
            return (
              filename.endsWith(".prg") ||
              filename.endsWith(".c64") ||
              filename.endsWith(".p00") ||
              filename.endsWith(".d64") ||
              filename.endsWith(".g64") ||
              filename.endsWith(".nib") ||
              filename.endsWith(".tap") ||
              filename.endsWith(".t64") ||
              filename.endsWith(".reu") ||
              filename.endsWith(".ima") ||
              filename.endsWith(".crt") ||
              filename.endsWith(".img")
            );
          },
          isMP3: function (entry) {
            let filename = entry.filename.toLowerCase();
            return filename.endsWith(".mp3");
          },
          canFastload: function (entry) {
            let filename = entry.filename.toLowerCase();
            return filename.endsWith(".d64") || filename.endsWith(".g64") || filename.endsWith(".nib");
          },
          isValidStil: function (entry) {
            return entry.name || entry.author || entry.title || entry.artist || entry.comment;
          },
          remove: function (index) {
            this.playlist.splice(index, 1);
          },
          removePlaylist: function () {
            this.playlist = [];
          },
          importPlaylist: function () {
            const reader = new FileReader();
            reader.onerror = (err) => console.log(err);
            var extension = this.importFile.name.split(".").pop().toLowerCase();

            if (extension === "js2") {
              reader.onload = (res) => {
                var content = res.target.result;
                var lines = content.split("\n");

                this.playlist = [];
                for (var i = 0; i < lines.length; i++) {
                  if (lines[i].length > 0) {
                    if (
                      !(
                        lines[i].startsWith("/C64Music/") ||
                        lines[i].startsWith("/CGSC/") ||
                        lines[i].startsWith("/Assembly64/") ||
                        lines[i].startsWith("/REU/")
                      )
                    ) {
                      lines[i] = "/C64Music" + lines[i];
                    }
                    this.playlist.push({
                      filename: lines[i],
                    });
                  }
                }
                this.playlistIndex = 0;
                this.importFile = null;
                if (this.playlist.length === 0 || this.playlistIndex >= this.playlist.length) {
                  return;
                }
                this.currentSid =
                  this.playlistIndex + 1 + ". " + this.shortEntry(this.playlist[this.playlistIndex].filename);
                this.updateSid(
                  this.playlist[this.playlistIndex].filename,
                  this.playlist[this.playlistIndex].itemId,
                  this.playlist[this.playlistIndex].categoryId
                );
                this.showAudio = true;
              };
              reader.readAsText(this.importFile);
            } else if (extension === "json") {
              reader.onload = (res) => {
                this.playlist = JSON.parse(res.target.result);
                this.playlistIndex = 0;
                this.importFile = null;
                if (this.playlist.length === 0 || this.playlistIndex >= this.playlist.length) {
                  return;
                }
                this.currentSid =
                  this.playlistIndex + 1 + ". " + this.shortEntry(this.playlist[this.playlistIndex].filename);
                this.updateSid(
                  this.playlist[this.playlistIndex].filename,
                  this.playlist[this.playlistIndex].itemId,
                  this.playlist[this.playlistIndex].categoryId
                );
                this.showAudio = true;
              };
              reader.readAsText(this.importFile);
            }
          },
          exportPlaylist: function () {
            download("jsidplay2.json", "application/json; charset=utf-8; ", JSON.stringify(this.playlist));
          },
          setNextPlaylistEntry: function () {
            this.stop();

            if (this.playlist.length === 0) {
              return;
            }
            if (this.random) {
              this.playlistIndex = getRandomInt(0, this.playlist.length - 1);
            } else {
              if (this.playlistIndex === this.playlist.length - 1) {
                this.playlistIndex = 0;
              } else {
                this.playlistIndex++;
              }
            }
            if (this.playlist.length === 0 || this.playlistIndex >= this.playlist.length) {
              return;
            }
            this.play(
              "",
              this.playlist[this.playlistIndex].filename,
              this.playlist[this.playlistIndex].itemId,
              this.playlist[this.playlistIndex].categoryId
            );
            this.currentSid =
              this.playlistIndex + 1 + ". " + this.shortEntry(this.playlist[this.playlistIndex].filename);
            this.updateSid(
              this.playlist[this.playlistIndex].filename,
              this.playlist[this.playlistIndex].itemId,
              this.playlist[this.playlistIndex].categoryId
            );
          },
          setDefault: function () {
            this.convertOptions = JSON.parse(JSON.stringify(this.defaultConvertOptions));
            this.convertOptions.useHls = true;
            this.convertOptions.config.sidplay2Section.single = true;
            this.convertOptions.config.sidplay2Section.defaultPlayLength = 240;
            this.convertOptions.config.audioSection.reverbBypass = false;
            this.convertOptions.config.audioSection.mainBalance = 0.3;
            this.convertOptions.config.audioSection.secondBalance = 0.7;
            this.convertOptions.config.audioSection.thirdBalance = 0.5;
            this.convertOptions.config.audioSection.secondDelay = 10;
            this.convertOptions.config.audioSection.sampling = "RESAMPLE";
            this.convertOptions.config.emulationSection.defaultSidModel = "MOS8580";
            this.convertOptions.config.c1541Section.jiffyDosInstalled = true;
            this.mobileProfile();
          },
          setDefaultUser: function () {
            this.username = "jsidplay2";
            this.password = "jsidplay2!";
          },
          mobileProfile: function () {
            this.convertOptions.config.audioSection.vbr = false;
            this.convertOptions.config.audioSection.cbr = 64;
            this.convertOptions.config.audioSection.audioCoderBitRate = 64000;
            this.convertOptions.config.audioSection.videoCoderBitRate = 480000;
          },
          wifiProfile: function () {
            this.convertOptions.config.audioSection.vbr = true;
            this.convertOptions.config.audioSection.vbrQuality = 0;
            this.convertOptions.config.audioSection.audioCoderBitRate = 320000;
            this.convertOptions.config.audioSection.videoCoderBitRate = 1000000;
          },
          updateFilters: function () {
            this.convertOptions.config.emulationSection.reSIDfpFilter6581 = this.reSIDfpFilters6581[1];
            this.convertOptions.config.emulationSection.reSIDfpFilter8580 = this.reSIDfpFilters8580[1];
            this.convertOptions.config.emulationSection.reSIDfpStereoFilter6581 = this.reSIDfpFilters6581[1];
            this.convertOptions.config.emulationSection.reSIDfpStereoFilter8580 = this.reSIDfpFilters8580[1];
            this.convertOptions.config.emulationSection.reSIDfpThirdSIDFilter6581 = this.reSIDfpFilters6581[1];
            this.convertOptions.config.emulationSection.reSIDfpThirdSIDFilter8580 = this.reSIDfpFilters8580[1];

            this.convertOptions.config.emulationSection.filter6581 = this.reSIDfilters6581[3];
            this.convertOptions.config.emulationSection.filter8580 = this.reSIDfilters8580[1];
            this.convertOptions.config.emulationSection.stereoFilter6581 = this.reSIDfilters6581[3];
            this.convertOptions.config.emulationSection.stereoFilter8580 = this.reSIDfilters8580[1];
            this.convertOptions.config.emulationSection.thirdSIDFilter6581 = this.reSIDfilters6581[3];
            this.convertOptions.config.emulationSection.thirdSIDFilter8580 = this.reSIDfilters8580[1];
          },
          updateSid: function (entry, itemId, categoryId) {
            if (entry) {
              this.fetchInfo(entry, itemId, categoryId);
              this.fetchStil(entry, itemId, categoryId);
              this.fetchPhoto(entry, itemId, categoryId);
            }
          },
          createDownloadUrl: function (entry, itemId, categoryId) {
            var url = uriEncode(
              (typeof itemId === "undefined" && typeof categoryId === "undefined" ? "" : "/") + entry
            );
            return (
              window.location.protocol +
              "//" +
              window.location.host +
              "/jsidplay2service/JSIDPlay2REST/convert" +
              url +
              (typeof itemId === "undefined" && typeof categoryId === "undefined"
                ? ""
                : "?itemId=" + itemId + "&categoryId=" + categoryId)
            );
          },
          createConvertUrl: function (autostart, entry, itemId, categoryId) {
            var url = uriEncode(
              (typeof itemId === "undefined" && typeof categoryId === "undefined" ? "" : "/") + entry
            );
            return (
              window.location.protocol +
              "//" +
              window.location.host +
              "/jsidplay2service/JSIDPlay2REST/convert" +
              url +
              "?enableSidDatabase=" +
              this.convertOptions.config.sidplay2Section.enableDatabase +
              "&startTime=" +
              this.convertOptions.config.sidplay2Section.startTime +
              "&defaultLength=" +
              this.convertOptions.config.sidplay2Section.defaultPlayLength +
              "&fadeIn=" +
              this.convertOptions.config.sidplay2Section.fadeInTime +
              "&fadeOut=" +
              this.convertOptions.config.sidplay2Section.fadeOutTime +
              "&loop=" +
              this.convertOptions.config.sidplay2Section.loop +
              "&single=" +
              this.convertOptions.config.sidplay2Section.single +
              "&frequency=" +
              this.convertOptions.config.audioSection.samplingRate +
              "&sampling=" +
              this.convertOptions.config.audioSection.sampling +
              "&mainVolume=" +
              this.convertOptions.config.audioSection.mainVolume +
              "&secondVolume=" +
              this.convertOptions.config.audioSection.secondVolume +
              "&thirdVolume=" +
              this.convertOptions.config.audioSection.thirdVolume +
              "&mainBalance=" +
              this.convertOptions.config.audioSection.mainBalance +
              "&secondBalance=" +
              this.convertOptions.config.audioSection.secondBalance +
              "&thirdBalance=" +
              this.convertOptions.config.audioSection.thirdBalance +
              "&mainDelay=" +
              this.convertOptions.config.audioSection.mainDelay +
              "&secondDelay=" +
              this.convertOptions.config.audioSection.secondDelay +
              "&thirdDelay=" +
              this.convertOptions.config.audioSection.thirdDelay +
              "&bufferSize=" +
              this.convertOptions.config.audioSection.bufferSize +
              "&audioBufferSize=" +
              this.convertOptions.config.audioSection.audioBufferSize +
              "&cbr=" +
              this.convertOptions.config.audioSection.cbr +
              "&vbrQuality=" +
              this.convertOptions.config.audioSection.vbrQuality +
              "&vbr=" +
              this.convertOptions.config.audioSection.vbr +
              "&acBitRate=" +
              this.convertOptions.config.audioSection.audioCoderBitRate +
              "&vcBitRate=" +
              this.convertOptions.config.audioSection.videoCoderBitRate +
              "&vcAudioDelay=" +
              this.convertOptions.config.audioSection.videoCoderAudioDelay +
              "&delayBypass=" +
              this.convertOptions.config.audioSection.delayBypass +
              "&reverbBypass=" +
              this.convertOptions.config.audioSection.reverbBypass +
              "&defaultEmulation=" +
              this.convertOptions.config.emulationSection.defaultEmulation +
              "&defaultClock=" +
              this.convertOptions.config.emulationSection.defaultClockSpeed +
              "&defaultModel=" +
              this.convertOptions.config.emulationSection.defaultSidModel +
              "&sidToRead=" +
              this.convertOptions.config.emulationSection.sidToRead +
              "&digiBoosted8580=" +
              this.convertOptions.config.emulationSection.digiBoosted8580 +
              "&fakeStereo=" +
              this.convertOptions.config.emulationSection.fakeStereo +
              "&muteVoice1=" +
              this.convertOptions.config.emulationSection.muteVoice1 +
              "&muteVoice2=" +
              this.convertOptions.config.emulationSection.muteVoice2 +
              "&muteVoice3=" +
              this.convertOptions.config.emulationSection.muteVoice3 +
              "&muteVoice4=" +
              this.convertOptions.config.emulationSection.muteVoice4 +
              "&muteStereoVoice1=" +
              this.convertOptions.config.emulationSection.muteStereoVoice1 +
              "&muteStereoVoice2=" +
              this.convertOptions.config.emulationSection.muteStereoVoice2 +
              "&muteStereoVoice3=" +
              this.convertOptions.config.emulationSection.muteStereoVoice3 +
              "&muteStereoVoice4=" +
              this.convertOptions.config.emulationSection.muteStereoVoice4 +
              "&muteThirdSidVoice1=" +
              this.convertOptions.config.emulationSection.muteThirdSIDVoice1 +
              "&muteThirdSidVoice2=" +
              this.convertOptions.config.emulationSection.muteThirdSIDVoice2 +
              "&muteThirdSidVoice3=" +
              this.convertOptions.config.emulationSection.muteThirdSIDVoice3 +
              "&muteThirdSidVoice4=" +
              this.convertOptions.config.emulationSection.muteThirdSIDVoice4 +
              "&filter6581=" +
              this.convertOptions.config.emulationSection.filter6581 +
              "&stereoFilter6581=" +
              this.convertOptions.config.emulationSection.stereoFilter6581 +
              "&thirdFilter6581=" +
              this.convertOptions.config.emulationSection.thirdSIDFilter6581 +
              "&filter8580=" +
              this.convertOptions.config.emulationSection.filter8580 +
              "&stereoFilter8580=" +
              this.convertOptions.config.emulationSection.stereoFilter8580 +
              "&thirdFilter8580=" +
              this.convertOptions.config.emulationSection.thirdSIDFilter8580 +
              "&reSIDfpFilter6581=" +
              this.convertOptions.config.emulationSection.reSIDfpFilter6581 +
              "&reSIDfpStereoFilter6581=" +
              this.convertOptions.config.emulationSection.reSIDfpStereoFilter6581 +
              "&reSIDfpThirdFilter6581=" +
              this.convertOptions.config.emulationSection.reSIDfpThirdSIDFilter6581 +
              "&reSIDfpFilter8580=" +
              this.convertOptions.config.emulationSection.reSIDfpFilter8580 +
              "&reSIDfpStereoFilter8580=" +
              this.convertOptions.config.emulationSection.reSIDfpStereoFilter8580 +
              "&reSIDfpThirdFilter8580=" +
              this.convertOptions.config.emulationSection.reSIDfpThirdSIDFilter8580 +
              "&detectPSID64ChipModel=" +
              this.convertOptions.config.emulationSection.detectPSID64ChipModel +
              "&videoTuneAsAudio=" +
              this.convertOptions.videoTuneAsAudio +
              "&hls=" +
              this.convertOptions.useHls +
              "&hlsType=" +
              this.convertOptions.hlsType +
              "&pressSpaceInterval=" +
              this.convertOptions.pressSpaceInterval +
              "&status=" +
              this.convertOptions.showStatus +
              "&jiffydos=" +
              this.convertOptions.config.c1541Section.jiffyDosInstalled +
              this.sfxSoundExpanderParameters +
              this.reuParameters +
              this.stereoParameters +
              (typeof itemId === "undefined" && typeof categoryId === "undefined"
                ? ""
                : "&itemId=" + itemId + "&categoryId=" + categoryId) +
              (autostart ? "&autostart=" + uriEncode(autostart) : "")
            );
          },
          createSIDMappingUrl: function (entry, itemId, categoryId) {
            var url = uriEncode(
              (typeof itemId === "undefined" && typeof categoryId === "undefined" ? "" : "/") + entry
            );
            return (
              window.location.protocol +
              "//" +
              window.location.host +
              "/jsidplay2service/JSIDPlay2REST/" +
              HardwareFunctions.mapping +
              url +
              "?defaultModel=" +
              this.convertOptions.config.emulationSection.defaultSidModel +
              "&fakeStereo=" +
              this.convertOptions.config.emulationSection.fakeStereo +
              "&hardSid6581=" +
              this.convertOptions.config.emulationSection.hardsid6581 +
              "&hardSid8580=" +
              this.convertOptions.config.emulationSection.hardsid8580 +
              (HardwareFunctions.mapping === "hardsid-mapping" ? "&chipCount=" + chipCount : "") +
              this.stereoParameters +
              (typeof itemId === "undefined" && typeof categoryId === "undefined"
                ? ""
                : "&itemId=" + itemId + "&categoryId=" + categoryId)
            );
          },
          openDownloadMP3Url: function (entry, itemId, categoryId) {
            var url = this.createConvertUrl(
              "",
              (typeof itemId === "undefined" && typeof categoryId === "undefined" ? "" : "/") + entry
            );
            window.open(
              url +
                "&download=true" +
                (typeof itemId === "undefined" && typeof categoryId === "undefined"
                  ? ""
                  : "&itemId=" + itemId + "&categoryId=" + categoryId)
            );
          },
          openDownloadSIDUrl: function (entry, itemId, categoryId) {
            var url = uriEncode(
              (typeof itemId === "undefined" && typeof categoryId === "undefined" ? "" : "/") + entry
            );
            window.open(
              window.location.protocol +
                "//" +
                this.username +
                ":" +
                this.password +
                "@" +
                window.location.host +
                "/jsidplay2service/JSIDPlay2REST/download" +
                url +
                (typeof itemId === "undefined" && typeof categoryId === "undefined"
                  ? ""
                  : "?itemId=" + itemId + "&categoryId=" + categoryId)
            );
          },
          openDownloadUrl: function (entry, itemId, categoryId) {
            var url = uriEncode(
              (typeof itemId === "undefined" && typeof categoryId === "undefined" ? "" : "/") + entry
            );
            window.open(this.createDownloadUrl(entry, itemId, categoryId));
          },
          fetchDirectory: function (entry) {
            entry.loading = true; //the loading begin
            axios({
              method: "get",
              url: "/jsidplay2service/JSIDPlay2REST/directory" + uriEncode(entry.filename) + "?filter=.*",
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                this.slide = 0;
                this.directory = response.data.map((file) => {
                  return {
                    filename: file,
                    diskDirectory: [],
                    directoryMode: 0,
                    loading: false,
                    loadingDisk: false,
                  };
                });
              })
              .catch((error) => {
                console.log(error);
              })
              .finally(() => (entry.loading = false));
          },
          fetchInfo: function (entry, itemId, categoryId) {
            this.loadingSid = true; //the loading begin
            var url =
              uriEncode((typeof itemId === "undefined" && typeof categoryId === "undefined" ? "" : "/") + entry) +
              "?list=true" +
              (typeof itemId === "undefined" && typeof categoryId === "undefined"
                ? ""
                : "&itemId=" + itemId + "&categoryId=" + categoryId);
            axios({
              method: "get",
              url: "/jsidplay2service/JSIDPlay2REST/info" + url,
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                this.infos = response.data;
              })
              .catch((error) => {
                this.infos = [];
                console.log(error);
              })
              .finally(() => (this.loadingSid = false));
          },
          fetchStil: function (entry, itemId, categoryId) {
            this.loadingStil = true; //the loading begin
            var url =
              uriEncode((typeof itemId === "undefined" && typeof categoryId === "undefined" ? "" : "/") + entry) +
              (typeof itemId === "undefined" && typeof categoryId === "undefined"
                ? ""
                : "?itemId=" + itemId + "&categoryId=" + categoryId);
            axios({
              method: "get",
              url: "/jsidplay2service/JSIDPlay2REST/stil" + url,
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                this.stil = response.data;
                if (!this.stil) {
                  this.hasStil = false;
                  this.stil = [];
                } else {
                  this.hasStil = true;
                }
              })
              .catch((error) => {
                this.stil = [];
                console.log(error);
              })
              .finally(() => (this.loadingStil = false));
          },
          fetchPhoto: function (entry, itemId, categoryId) {
            this.loadingSid = true; //the loading begin
            var url =
              uriEncode((typeof itemId === "undefined" && typeof categoryId === "undefined" ? "" : "/") + entry) +
              (typeof itemId === "undefined" && typeof categoryId === "undefined"
                ? ""
                : "?itemId=" + itemId + "&categoryId=" + categoryId);
            axios({
              method: "get",
              url: "/jsidplay2service/JSIDPlay2REST/photo" + url,
              auth: {
                username: this.username,
                password: this.password,
              },
              responseType: "blob",
            })
              .then((response) => {
                var reader = new window.FileReader();
                reader.readAsDataURL(response.data);
                reader.onload = function () {
                  this.picture = reader.result;
                  var imgSrc = this.picture;
                  Vue.nextTick(function () {
                    if (document.getElementById("img")) {
                      document.getElementById("img").setAttribute("src", imgSrc);
                    }
                  });
                };
              })
              .catch((error) => {
                this.picture = "";
                console.log(error);
              })
              .finally(() => (this.loadingSid = false));
          },
          fetchFavorites: function () {
            this.loadingPl = true; //the loading begin
            axios({
              method: "get",
              url: "/jsidplay2service/JSIDPlay2REST/favorites",
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                this.playlist = response.data.map((file) => {
                  return {
                    filename: file,
                  };
                });
                this.playlistIndex = 0;
                if (this.playlist.length === 0 || this.playlistIndex >= this.playlist.length) {
                  return;
                }
                this.currentSid =
                  this.playlistIndex + 1 + ". " + this.shortEntry(this.playlist[this.playlistIndex].filename);
                this.updateSid(
                  this.playlist[this.playlistIndex].filename,
                  this.playlist[this.playlistIndex].itemId,
                  this.playlist[this.playlistIndex].categoryId
                );
                this.$refs.audioElm.src = this.createConvertUrl(
                  "",
                  this.playlist[this.playlistIndex].filename,
                  this.playlist[this.playlistIndex].itemId,
                  this.playlist[this.playlistIndex].categoryId
                );
                this.showAudio = true;
              })
              .catch((error) => {
                this.playlist = [];
                this.playlistIndex = 0;
                console.log(error);
              })
              .finally(() => (this.loadingPl = false));
          },
          fetchFilters: function () {
            this.loadingCfg = true; //the loading begin
            axios({
              method: "get",
              url: "/jsidplay2service/JSIDPlay2REST/filters",
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                const filters = response.data;
                this.reSIDfilters6581 = filters
                  .filter((filter) => filter.startsWith("RESID_MOS6581_"))
                  .map((filter) => filter.substring("RESID_MOS6581_".length));
                this.reSIDfilters8580 = filters
                  .filter((filter) => filter.startsWith("RESID_MOS8580_"))
                  .map((filter) => filter.substring("RESID_MOS8580_".length));
                this.reSIDfpFilters6581 = filters
                  .filter((filter) => filter.startsWith("RESIDFP_MOS6581_"))
                  .map((filter) => filter.substring("RESIDFP_MOS6581_".length));
                this.reSIDfpFilters8580 = filters
                  .filter((filter) => filter.startsWith("RESIDFP_MOS8580_"))
                  .map((filter) => filter.substring("RESIDFP_MOS8580_".length));

                this.updateFilters();
              })
              .catch((error) => {
                this.reSIDfilters6581 = [];
                this.reSIDfilters8580 = [];
                this.reSIDfpFilters6581 = [];
                this.reSIDfpFilters8580 = [];
                console.log(error);
              })
              .finally(() => (this.loadingCfg = false));
          },
          fetchDiskDirectory: function (entry, itemId, categoryId) {
            if (entry.directoryMode) {
              if (entry.directoryMode === 0xe000) {
                entry.directoryMode = 0xe100;
              } else {
                entry.diskDirectoryHeader = null;
                entry.diskDirectory = [];
                entry.directoryMode = 0;
                return;
              }
            } else {
              entry.directoryMode = 0xe000;
            }
            entry.loadingDisk = true; //the loading begin
            var url =
              uriEncode(
                (typeof itemId === "undefined" && typeof categoryId === "undefined" ? "" : "/") + entry.filename
              ) +
              (typeof itemId === "undefined" && typeof categoryId === "undefined"
                ? ""
                : "?itemId=" + itemId + "&categoryId=" + categoryId);
            axios({
              method: "get",
              url: "/jsidplay2service/JSIDPlay2REST/disk-directory" + url,
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                entry.diskDirectoryHeader = petsciiToFont(response.data.title, entry.directoryMode | 0x200);
                entry.diskDirectory = response.data.dirEntries.map((dirEntry) => {
                  return {
                    directoryLine: dirEntry.directoryLine,
                    formatted: petsciiToFont(dirEntry.directoryLine, entry.directoryMode),
                  };
                });
              })
              .catch((error) => {
                entry.diskDirectoryHeader = null;
                entry.diskDirectory = [];
                entry.directoryMode = 0;
                console.log(error);
              })
              .finally(() => (entry.loadingDisk = false));
          },
          assembly64SearchUrl: function (token) {
            var parameterList = [];
            if (typeof token !== "undefined") {
              this.name = "";
              this.category = this.categories.filter(function (item) {
                return item.name === "hvscmusic";
              })[0].id;
              this.event = "";
              this.released = "";
              this.rating = "";
              this.handle = token;
            }
            if (this.name !== "") {
              parameterList.push("name=" + this.name);
            }
            if (this.category !== "") {
              parameterList.push("category=" + this.category);
            }
            if (this.event !== "") {
              parameterList.push("event=" + this.event);
            }
            if (this.released.length === 4) {
              parameterList.push("dateFrom=" + this.released + "0101");
              parameterList.push("dateTo=" + this.released + "1231");
            }
            if (this.released.length === 7) {
              var splitted = this.released.split("-");
              var year = splitted[0];
              var month = splitted[1];
              parameterList.push("dateFrom=" + year + month + "01");
              parameterList.push("dateTo=" + year + month + daysInMonth(month, year));
            }
            if (this.released.length === 10) {
              var splitted = this.released.split("-");
              var year = splitted[0];
              var month = splitted[1];
              var day = splitted[2];
              parameterList.push("dateFrom=" + year + month + day);
              parameterList.push("dateTo=" + year + month + day);
            }
            if (this.rating !== "") {
              parameterList.push("rating=" + this.rating);
            }
            if (this.handle !== "") {
              parameterList.push("handle=" + this.handle);
            }
            return parameterList.length > 0 ? "?" + parameterList.join("&") : "";
          },
          fetchCategories: function () {
            this.loadingAssembly64 = true; //the loading begin
            axios({
              method: "get",
              url: "$assembly64Url/leet/search/v2/categories",
            })
              .then((response) => {
                this.categories = response.data;
                this.categories.sort((a, b) => {
                  return a.description.localeCompare(b.description);
                });
              })
              .catch((error) => {
                this.categories = [];
                console.log(error);
              })
              .finally(() => (this.loadingAssembly64 = false));
          },
          resetSearchResults: function (event) {
            this.name = "";
            this.category = "";
            this.event = "";
            this.released = "";
            this.rating = "";
            this.handle = "";
          },
          requestSearchResults: function (event, token) {
            var url = this.assembly64SearchUrl(token);
            if (url.length === 0) {
              this.searchResults = [];
              return;
            }
            this.loadingAssembly64 = true; //the loading begin
            axios({
              method: "get",
              url: "$assembly64Url/leet/search/v2" + url,
            })
              .then((response) => {
                if (response.status === 200) {
                  this.searchResults = response.data;

                  var data = this;
                  this.searchResults = this.searchResults.map((obj) => {
                    return {
                      id: obj.id,
                      category: data.categories.filter(function (item) {
                        return item.id === obj.category;
                      })[0]?.description,
                      categoryId: obj.category,
                      name: obj.name,
                      group: obj.group,
                      event: obj.event,
                      released: obj.released,
                      handle: obj.handle,
                      rating: obj.rating,
                      _showDetails: false,
                    };
                  });
                } else {
                  this.searchResults = [];
                }
              })
              .catch((error) => {
                this.searchResults = [];
                console.log(error);
              })
              .finally(() => (this.loadingAssembly64 = false));
          },
          requestContentEntries: function (searchResult) {
            if (searchResult._showDetails === true) {
              searchResult._showDetails = false;
              return;
            }
            if (searchResult.contentEntries) {
              searchResult._showDetails = true;
              return;
            }
            this.loadingAssembly64 = true; //the loading begin
            axios({
              method: "get",
              url:
                "$assembly64Url/leet/search/v2/contententries/" + btoa(searchResult.id) + "/" + searchResult.categoryId,
            })
              .then((response) => {
                if (response.status === 200) {
                  searchResult.contentEntries = response.data.contentEntry.map((contentEntry) => {
                    return {
                      filename: contentEntry.id,
                      diskDirectory: [],
                      directoryMode: 0,
                      loadingDisk: false,
                    };
                  });
                  searchResult._showDetails = true;
                }
              })
              .catch((error) => {
                searchResult.contentEntries = [];
                console.log(error);
              })
              .finally(() => (this.loadingAssembly64 = false));
          },
        },
        mounted: function () {
          window.myAccessor = this;
          window.addEventListener("resize", () => {
            this.carouselImageHeight =
              window.innerHeight > window.innerWidth ? window.innerHeight / 2 : window.innerHeight * 0.8;
          });

          this.hasHardware = typeof navigator.usb !== "undefined";

          if (localStorage.locale) {
            this.$i18n.locale = localStorage.locale;
          }
          if (localStorage.username) {
            this.username = JSON.parse(localStorage.username);
          }
          if (localStorage.password) {
            this.password = JSON.parse(localStorage.password);
          }
          if (localStorage.directory) {
            this.directory = JSON.parse(localStorage.directory);
          } else {
            this.fetchDirectory(this.rootDir);
          }
          this.fetchFilters();
          this.fetchCategories();
          if (localStorage.convertOptions) {
            // restore configuration from last run
            this.convertOptions = JSON.parse(localStorage.convertOptions);
          } else {
            // initialize configuration (if they differ from the default settings)
            this.convertOptions.useHls = true;
            this.convertOptions.config.sidplay2Section.single = true;
            this.convertOptions.config.sidplay2Section.defaultPlayLength = 240;
            this.convertOptions.config.audioSection.reverbBypass = false;
            this.convertOptions.config.audioSection.mainBalance = 0.3;
            this.convertOptions.config.audioSection.secondBalance = 0.7;
            this.convertOptions.config.audioSection.thirdBalance = 0.5;
            this.convertOptions.config.audioSection.secondDelay = 10;
            this.convertOptions.config.audioSection.sampling = "RESAMPLE";
            this.convertOptions.config.emulationSection.defaultSidModel = "MOS8580";
            this.convertOptions.config.c1541Section.jiffyDosInstalled = true;
            this.mobileProfile();
          }
          if (this.convertOptions.config.emulationSection.dualSID) {
            this.stereoMode = "FORCE_3SID";
          } else if (this.convertOptions.config.emulationSection.thirdSID) {
            this.stereoMode = "FORCE_2SID";
          } else {
            this.stereoMode = "AUTO";
          }
          if (localStorage.random) {
            this.random = JSON.parse(localStorage.random);
          }
          if (localStorage.playlistV2) {
            this.playlist = JSON.parse(localStorage.playlistV2);
          }
          if (localStorage.playlistIndex) {
            this.playlistIndex = JSON.parse(localStorage.playlistIndex);
            if (this.playlistIndex >= this.playlist.length) {
              this.playlistIndex = 0;
            }
          }
          if (this.playlist.length !== 0) {
            this.currentSid =
              this.playlistIndex + 1 + ". " + this.shortEntry(this.playlist[this.playlistIndex].filename);
            this.updateSid(
              this.playlist[this.playlistIndex].filename,
              this.playlist[this.playlistIndex].itemId,
              this.playlist[this.playlistIndex].categoryId
            );

            this.showAudio = true;
            this.$refs.audioElm.src = this.createConvertUrl(
              "",
              this.playlist[this.playlistIndex].filename,
              this.playlist[this.playlistIndex].itemId,
              this.playlist[this.playlistIndex].categoryId
            );
          }
          if (localStorage.sortBy) {
            this.sortBy = JSON.parse(localStorage.sortBy);
          }
          if (localStorage.sortDesc) {
            this.sortDesc = JSON.parse(localStorage.sortDesc);
          }
          if (localStorage.category) {
            this.category = JSON.parse(localStorage.category);
          }
          if (localStorage.name) {
            this.name = JSON.parse(localStorage.name);
          }
          if (localStorage.event) {
            this.event = JSON.parse(localStorage.event);
          }
          if (localStorage.released) {
            this.released = JSON.parse(localStorage.released);
          }
          if (localStorage.rating) {
            this.rating = JSON.parse(localStorage.rating);
          }
          if (localStorage.handle) {
            this.handle = JSON.parse(localStorage.handle);
          }
          this.requestSearchResults();
        },
        watch: {
          username(newValue, oldValue) {
            localStorage.username = JSON.stringify(this.username);
          },
          password(newValue, oldValue) {
            localStorage.password = JSON.stringify(this.password);
          },
          directory: {
            handler: function (after, before) {
              localStorage.directory = JSON.stringify(this.directory);
            },
            deep: true,
          },
          category(newValue, oldValue) {
            localStorage.category = JSON.stringify(this.category);
          },
          name(newValue, oldValue) {
            localStorage.name = JSON.stringify(this.name);
          },
          event(newValue, oldValue) {
            localStorage.event = JSON.stringify(this.event);
          },
          released(newValue, oldValue) {
            localStorage.released = JSON.stringify(this.released);
          },
          rating(newValue, oldValue) {
            localStorage.rating = JSON.stringify(this.rating);
          },
          handle(newValue, oldValue) {
            localStorage.handle = JSON.stringify(this.handle);
          },
          random(newValue, oldValue) {
            localStorage.random = JSON.stringify(this.random);
          },
          playlistIndex(newValue, oldValue) {
            localStorage.playlistIndex = JSON.stringify(this.playlistIndex);
          },
          playlist(newValue, oldValue) {
            localStorage.playlistV2 = JSON.stringify(this.playlist);
          },
          convertOptions: {
            handler: function (after, before) {
              this.convertOptions.config.sidplay2Section.defaultPlayLength = timeConverter(
                this.convertOptions.config.sidplay2Section.defaultPlayLength
              );
              this.convertOptions.config.sidplay2Section.startTime = timeConverter(
                this.convertOptions.config.sidplay2Section.startTime
              );
              this.convertOptions.config.sidplay2Section.fadeInTime = timeConverter(
                this.convertOptions.config.sidplay2Section.fadeInTime
              );
              this.convertOptions.config.sidplay2Section.fadeOutTime = timeConverter(
                this.convertOptions.config.sidplay2Section.fadeOutTime
              );
              localStorage.convertOptions = JSON.stringify(this.convertOptions);
            },
            deep: true,
          },
          stereoMode(newValue, oldValue) {
            if (this.stereoMode === "FORCE_3SID") {
              this.convertOptions.config.emulationSection.dualSID = true;
              this.convertOptions.config.emulationSection.thirdSID = true;
            } else if (this.stereoMode === "FORCE_2SID") {
              this.convertOptions.config.emulationSection.dualSID = true;
              this.convertOptions.config.emulationSection.thirdSID = false;
            } else {
              this.convertOptions.config.emulationSection.dualSID = false;
              this.convertOptions.config.emulationSection.thirdSID = false;
            }
            localStorage.convertOptions = JSON.stringify(this.convertOptions);
          },
        },
      });

      // prevent back button
      history.pushState(null, null, document.URL);
      window.addEventListener("popstate", function () {
        history.pushState(null, null, document.URL);
      });
    </script>
  </body>
</html>
