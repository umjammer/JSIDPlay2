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
    <script src="/webjars/bootstrap/5.3.3/dist/js/bootstrap.bundle${min}.js"></script>

    <!-- helpers -->
    <script src="/webjars/vue-i18n/9.10.1/dist/vue-i18n.global${prod}.js"></script>
    <script src="/webjars/axios/1.5.1/dist/axios${min}.js"></script>
    <script src="/webjars/web-audio-recorder-js/0.0.2/${lib}/WebAudioRecorder${min}.js"></script>

    <!-- USB -->
    <script src="/static/usb/hardsid.js"></script>
    <script src="/static/usb/libftdi.js"></script>
    <script src="/static/usb/exsid.js"></script>
    <script src="/static/usb/sidblaster.js"></script>
    <script src="/static/teavm/wasm/jsidplay2.wasm-runtime.js"></script>

    <!-- disable pull reload -->
    <style>
      html,
      body {
        overscroll-behavior: none;
      }
    </style>

    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />

    <title>C64 Jukebox (Vue 3 with Bootstrap 5)</title>
  </head>
  <body>
    <div id="app">
      <form id="main">
        <div class="locale-changer">
          <h1 class="c64jukebox" style="width: 100%">C64 Jukebox</h1>
          <button
            :class="theaterMode ? 'btn btn-success btn-sm' : 'btn btn-primary btn-sm'"
            :aria-pressed="theaterMode"
            data-bs-toggle="button"
            v-on:click.prevent="
              theaterMode = !theaterMode;
              if (theaterMode) {
                setNextPlaylistEntry();
              } else {
                pause();
              }
            "
          >
            <span style="white-space: nowrap">
              <i :class="theaterMode ? 'bi bi-pause-fill' : 'bi bi-play-fill'"></i>
              {{ $t("theaterMode") }}
            </span>
          </button>
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

        <div class="audio">
          <audio ref="audioElm" v-show="showAudio" v-on:ended="setNextPlaylistEntry" type="audio/mpeg" controls>
            I'm sorry. Your browser doesn't support HTML5 audio
          </audio>
          <div v-show="showHardwarePlayer">
            <button type="button" class="btn btn-success btn-sm" v-on:click="pause()">
              <i class="bi bi-stop-fill"></i>
              <span>Stop Hardware Player</span>
            </button>
            <button type="button" class="btn btn-danger btn-sm" v-on:click="end()">
              <span>Quit Hardware Player</span>
            </button>
          </div>
          <div style="position: absolute; bottom: 5px; line-height: 0.7; width: 100%">
            <span class="current-sid"
              ><span v-if="currentSid">{{ $t("currentlyPlaying") }}</span> <span>{{ currentSid }}</span></span
            >
          </div>
        </div>
        <div class="card">
          <div class="card-header">
            <ul class="nav nav-pills card-header-pills mb-2" role="tablist">
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link active"
                  id="about-tab"
                  data-bs-toggle="pill"
                  data-bs-target="#about"
                  type="button"
                  role="tab"
                  aria-controls="about"
                  aria-selected="true"
                  @click="tabIndex = 0"
                >
                  {{ $t("ABOUT") }}
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link"
                  id="directory-tab"
                  data-bs-toggle="pill"
                  data-bs-target="#directory"
                  type="button"
                  role="tab"
                  aria-controls="directory"
                  aria-selected="false"
                  @click="tabIndex = 1"
                >
                  {{ $t("SIDS") }}
                  <div
                    :class="'spinner-border spinner-border-sm ' + (tabIndex === 1 ? 'text-light' : 'text-primary')"
                    v-if="
                      rootDir.loading ||
                      top200Dir.loading ||
                      oneFilerTop200Dir.loading ||
                      toolsTop200Dir.loading ||
                      musicTop200Dir.loading ||
                      graphicsTop200Dir.loading ||
                      gamesTop200Dir.loading
                    "
                  ></div>
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link"
                  id="search-tab"
                  data-bs-toggle="pill"
                  data-bs-target="#search"
                  type="button"
                  role="tab"
                  aria-controls="search"
                  aria-selected="false"
                  @click="tabIndex = 2"
                >
                  {{ $t("ASSEMBLY64") }}
                  <div
                    :class="'spinner-border spinner-border-sm ' + (tabIndex === 2 ? 'text-light' : 'text-primary')"
                    v-if="loadingAssembly64"
                  ></div>
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link"
                  id="sid-tab"
                  data-bs-toggle="pill"
                  data-bs-target="#sid"
                  type="button"
                  role="tab"
                  aria-controls="sid"
                  aria-selected="false"
                  @click="tabIndex = 3"
                >
                  {{ $t("SID") }}
                  <div
                    :class="'spinner-border spinner-border-sm ' + (tabIndex === 3 ? 'text-light' : 'text-primary')"
                    v-if="loadingSid"
                  ></div>
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link"
                  id="stil-tab"
                  data-bs-toggle="pill"
                  data-bs-target="#stil"
                  type="button"
                  role="tab"
                  aria-controls="stil"
                  aria-selected="false"
                  :disabled="this.stil.toString() === ''"
                  @click="tabIndex = 4"
                >
                  {{ $t("STIL") }}
                  <div
                    :class="'spinner-border spinner-border-sm ' + (tabIndex === 4 ? 'text-light' : 'text-primary')"
                    v-if="loadingStil"
                  ></div>
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link"
                  id="pl-tab"
                  ref="playlistTab"
                  data-bs-toggle="pill"
                  data-bs-target="#pl"
                  type="button"
                  role="tab"
                  aria-controls="pl"
                  aria-selected="false"
                  @click="tabIndex = 5"
                >
                  {{ $t("PL") }}
                  <div
                    :class="'spinner-border spinner-border-sm ' + (tabIndex === 5 ? 'text-light' : 'text-primary')"
                    v-if="loadingPl"
                  ></div>
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link"
                  id="hw-tab"
                  data-bs-toggle="pill"
                  data-bs-target="#hw"
                  type="button"
                  role="tab"
                  aria-controls="hw"
                  aria-selected="false"
                  @click="tabIndex = 6"
                  :disabled="!this.hasHardware"
                >
                  {{ $t("HARDWARE") }}
                </button>
              </li>
              <li class="nav-item" role="presentation">
                <button
                  class="nav-link"
                  id="cfg-tab"
                  data-bs-toggle="pill"
                  data-bs-target="#cfg"
                  type="button"
                  role="tab"
                  aria-controls="cfg"
                  aria-selected="false"
                  @click="tabIndex = 7"
                >
                  {{ $t("CFG") }}
                  <div
                    :class="'spinner-border spinner-border-sm ' + (tabIndex === 7 ? 'text-light' : 'text-primary')"
                    v-if="loadingCfg"
                  ></div>
                </button>
              </li>
              <li class="nav-item" role="presentation" v-show="username !== 'jsidplay2'">
                <button
                  class="nav-link"
                  id="logs-tab"
                  data-bs-toggle="pill"
                  data-bs-target="#logs"
                  type="button"
                  role="tab"
                  aria-controls="logs"
                  aria-selected="false"
                  @click="tabIndex = 8"
                >
                  {{ $t("LOGS") }}
                  <div
                    :class="'spinner-border spinner-border-sm ' + (tabIndex === 8 ? 'text-light' : 'text-primary')"
                    v-if="loadingLogs"
                  ></div>
                </button>
              </li>
            </ul>
          </div>
          <div class="tab-content card-body" style="position: relative">
            <div class="tab-pane fade show active" id="about" role="tabpanel" aria-labelledby="about-tab">
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
              <div class="settings-box">
                <div class="button-box">
                  <button
                    type="button"
                    class="btn btn-outline-success btn-sm"
                    data-bs-toggle="modal"
                    data-bs-target="#setDefaultUserModal"
                  >
                    <span>{{ $t("setDefaultUser") }}</span>
                  </button>
                  <!-- Modal -->
                  <div
                    class="modal fade"
                    id="setDefaultUserModal"
                    tabindex="-1"
                    aria-labelledby="setDefaultUserModalLabel"
                    aria-hidden="true"
                  >
                    <div class="modal-dialog">
                      <div class="modal-content">
                        <div class="modal-header">
                          <h5 class="modal-title" id="setDefaultUserModalLabel">{{ $t("confirmationTitle") }}</h5>
                          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                          <p>{{ $t("setDefaultUserReally") }}</p>
                        </div>
                        <div class="modal-footer">
                          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                          <button type="button" class="btn btn-primary" data-bs-dismiss="modal" @click="setDefaultUser">
                            OK
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="settings-box">
                <span class="setting">
                  <label for="username" class="form-label"
                    >{{ $t("username") }}
                    <input
                      type="text"
                      id="username"
                      class="form-control right"
                      v-model="username"
                      autocomplete="off"
                      autocorrect="off"
                      autocapitalize="off"
                      spellcheck="false"
                      tabindex="-1"
                  /></label>
                </span>
              </div>
              <div class="settings-box">
                <span class="setting">
                  <label for="password" class="form-label"
                    >{{ $t("password") }}
                    <input
                      type="password"
                      id="password"
                      class="form-control right"
                      v-model="password"
                      autocomplete="off"
                      autocorrect="off"
                      autocapitalize="off"
                      spellcheck="false"
                      tabindex="-1"
                      v-on:blur="delayedFetchDirectory(rootDir)"
                  /></label>
                </span>
              </div>
            </div>

            <div class="tab-pane fade" id="directory" role="tabpanel" aria-labelledby="directory-tab">
              <button type="button" class="btn btn-success btn-sm" v-on:click="fetchDirectory(rootDir)">
                <i class="bi bi-house-door-fill"></i>
              </button>
              <span style="font-style: italic; padding: 2px 4px; position: absolute; top: 0px; right: 264px">{{
                $t("filter")
              }}</span>

              <button
                type="button"
                class="btn btn-secondary"
                style="font-size: smaller; padding: 2px 4px; position: absolute; top: 0px; right: 184px"
                v-on:click="fetchDirectory(top200Dir)"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>{{ $t("top200") }}</span>
              </button>

              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="font-size: smaller; padding: 2px 4px; position: absolute; top: 0px; right: 102px"
                v-on:click="fetchDirectory(oneFilerTop200Dir)"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>{{ $t("onefilerTop200") }}</span>
              </button>

              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="font-size: smaller; padding: 2px 4px; position: absolute; top: 0px; right: 26px"
                v-on:click="fetchDirectory(toolsTop200Dir)"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>{{ $t("toolsTop100") }}</span>
              </button>

              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="font-size: smaller; padding: 2px 4px; position: absolute; top: 30px; right: 190px"
                v-on:click="fetchDirectory(musicTop200Dir)"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>{{ $t("musicTop200") }}</span>
              </button>

              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="font-size: smaller; padding: 2px 4px; position: absolute; top: 30px; right: 96px"
                v-on:click="fetchDirectory(graphicsTop200Dir)"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>{{ $t("graphicsTop200") }}</span>
              </button>

              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="font-size: smaller; padding: 2px 4px; position: absolute; top: 30px; right: 16px"
                v-on:click="fetchDirectory(gamesTop200Dir)"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>{{ $t("gamesTop200") }}</span>
              </button>

              <div style="height: 4px"></div>

              <div class="button-box">
                <div class="input-group input-group-sm mb-2">
                  <div class="input-group-text"><i class="bi bi-search"></i></div>
                  <input
                    id="quickfilter"
                    type="search"
                    class="form-control"
                    v-model="filterText2"
                    :placeholder="$t('searchPlaceholder')"
                  />
                </div>
              </div>

              <div class="list-group">
                <div
                  v-for="entry in directory"
                  :key="entry.filename"
                  :style="
                    filterText2 &&
                    !isParentDirectory(entry) &&
                    !entry.filename.toLowerCase().includes(filterText2.toLowerCase())
                      ? 'height: 0; padding: 0px;visibility: hidden;'
                      : ''
                  "
                >
                  <template v-if="isParentDirectory(entry)">
                    <button
                      type="button"
                      class="list-group-item list-group-item-action"
                      style="white-space: pre-line"
                      v-on:click="fetchDirectory(entry)"
                    >
                      <div class="directory parent">
                        <div class="spinner-border text-primary" v-if="entry.loading"></div>
                        <i class="bi bi-arrow-up" v-if="!entry.loading"></i> <span>{{ entry.filename }}</span>
                      </div>
                      <div class="parent-directory-hint">&larr; {{ $t("parentDirectoryHint") }}</div>
                    </button>

                    <div
                      id="myCarousel"
                      class="carousel slide carousel-fade"
                      data-bs-ride="carousel"
                      data-bs-pause="false"
                      data-bs-wrap="true"
                      v-show="directory.filter((entry) => isPicture(entry)).length > 0"
                    >
                      <div class="carousel-inner">
                        <div
                          :class="'carousel-item ' + (index === 0 ? 'active' : '')"
                          v-for="(entry, index) in directory.filter((entry) => isPicture(entry))"
                          v-bind:key="entry.filename"
                        >
                          <img
                            :src="createDownloadUrl(entry.filename)"
                            :alt="entry.filename"
                            class="img-fluid mx-auto d-block"
                            :style="{
                              height: carouselImageHeight + 'px',
                              width: 'auto',
                            }"
                          />
                          <div class="carousel-caption d-block">
                            <a
                              style="
                                text-shadow: -1px 0 black, 0 1px black, 1px 0 black, 0 -1px black;
                                font-family: sans;
                                color: #007bff;
                                background-color: white;
                                padding: 2px;
                                opacity: 0.75;
                              "
                              v-on:click="openDownloadUrl(entry.filename)"
                            >
                              <span>{{ shortEntry(entry.filename) }}</span>
                            </a>
                          </div>
                        </div>
                      </div>
                      <button
                        class="carousel-control-prev"
                        type="button"
                        data-bs-target="#myCarousel"
                        data-bs-slide="prev"
                      >
                        <span class="carousel-control-prev-icon" aria-hidden="true"></span>
                        <span class="visually-hidden">Previous</span>
                      </button>
                      <button
                        class="carousel-control-next"
                        type="button"
                        data-bs-target="#myCarousel"
                        data-bs-slide="next"
                      >
                        <span class="carousel-control-next-icon" aria-hidden="true"></span>
                        <span class="visually-hidden">Next</span>
                      </button>
                    </div>

                    <button
                      type="button"
                      class="btn btn-primary btn-sm"
                      style="padding: 2px 4px; float: right"
                      v-show="directory.filter((entry) => isMusic(entry)).length > 0"
                      v-on:click="
                        directory
                          .filter((entry) => isMusic(entry))
                          .forEach((entry) =>
                            playlist.push({
                              filename: entry.filename,
                            })
                          );
                        $refs.playlistTab.click();
                        showAudio = true;
                      "
                    >
                      <i class="bi bi-plus"></i>
                      <span>{{ $t("addAllToPlaylist") }}</span>
                    </button>
                  </template>
                  <template v-else-if="isDirectory(entry)">
                    <div
                      type="button"
                      :class="'list-group-item   list-group-item-' + getVariant(entry)"
                      style="white-space: pre-line"
                      v-on:click="fetchDirectory(entry)"
                    >
                      <div class="directory">
                        <div class="spinner-border text-primary" v-if="entry.loading"></div>
                        <i class="bi bi-folder-fill" v-if="!entry.loading"> </i>
                        <span>{{ shortEntry(entry.filename) }}</span>
                      </div>
                    </div>
                  </template>
                  <template v-else-if="isMusic(entry)">
                    <div
                      type="button"
                      :class="'list-group-item list-group-item-' + getVariant(entry)"
                      style="white-space: pre-line; display: flex; justify-content: space-between"
                      v-on:click="
                        currentSid = shortEntry(entry.filename);
                        updateSid(entry.filename);
                        showAudio = true;
                        $nextTick(function () {
                          play(undefined, entry.filename);
                        });
                      "
                    >
                      <div style="flex-grow: 4; word-break: break-all">
                        <div class="spinner-border text-primary" v-if="entry.loading"></div>
                        <i class="bi bi-music-note" v-if="!entry.loading"> </i>
                        <span class="sid-file">{{ shortEntry(entry.filename) }}</span>
                      </div>
                      <button
                        type="button"
                        class="btn btn-secondary"
                        style="height: fit-content; padding: 2px 4px; white-space: nowrap"
                        v-on:click.stop="openDownloadMP3Url(entry.filename)"
                        v-show="!isMP3(entry)"
                      >
                        <i class="bi bi-download"> </i>
                        <span style="font-size: x-small">{{ $t("downloadMP3") }}</span>
                      </button>
                      <button
                        type="button"
                        class="btn btn-secondary"
                        style="height: fit-content; padding: 2px 4px"
                        v-on:click.stop="openDownloadSIDUrl(entry.filename)"
                      >
                        <i class="bi bi-download"> </i>
                      </button>
                      <div>
                        <button
                          type="button"
                          class="btn btn-primary"
                          style="height: fit-content; padding: 2px 4px"
                          v-on:click.stop="
                            playlist.push({
                              filename: entry.filename,
                            });
                            $refs.playlistTab.click();
                            showAudio = true;
                          "
                        >
                          <i class="bi bi-plus"> </i>
                        </button>
                      </div>
                    </div>
                  </template>

                  <template v-else-if="isVideo(entry)">
                    <template v-if="canFastload(entry)">
                      <div
                        type="button"
                        :class="'list-group-item list-group-item-' + getVariant(entry)"
                        v-on:click="openiframe(createConvertUrl(undefined, entry.filename))"
                      >
                        <div style="white-space: pre-line; display: flex; justify-content: space-between">
                          <div style="flex-grow: 4; word-break: break-all">
                            <div class="spinner-border text-primary" v-if="entry.loading || entry.loadingDisk"></div>
                            <i class="bi bi-camera-video-fill" v-if="!(entry.loading || entry.loadingDisk)"> </i>
                            <span>{{ shortEntry(entry.filename) }}</span>
                          </div>
                          <button
                            type="button"
                            class="btn btn-primary"
                            style="height: fit-content; padding: 2px 4px"
                            v-on:click.stop="fetchDiskDirectory(entry)"
                            :disabled="entry.loadingDisk"
                          >
                            <i class="bi bi-journal-text"> </i>
                          </button>
                          <button
                            class="btn btn-secondary"
                            style="height: fit-content; padding: 2px 4px"
                            v-on:click.stop="openDownloadSIDUrl(entry.filename)"
                          >
                            <i class="bi bi-download"> </i>
                          </button>
                        </div>
                        <div>
                          <div v-show="entry.directoryMode > 0" class="disk-directory">
                            <div>
                              <span class="c64-font">{{ entry.diskDirectoryHeader }}</span>
                            </div>
                            <div v-for="(program, index) in entry.diskDirectory" :key="index">
                              <a
                                href="#"
                                v-on:click.stop="openiframe(createConvertUrl(program.directoryLine, entry.filename))"
                              >
                                <span class="c64-font">{{ program.formatted }}</span>
                              </a>
                            </div>
                          </div>
                        </div>
                      </div>
                    </template>
                    <template v-else>
                      <div
                        type="button"
                        :class="'list-group-item list-group-item-' + getVariant(entry)"
                        v-on:click="openiframe(createConvertUrl(undefined, entry.filename))"
                      >
                        <div style="white-space: pre-line; display: flex; justify-content: space-between">
                          <div style="flex-grow: 4; word-break: break-all">
                            <div class="spinner-border text-primary" v-if="entry.loading"></div>
                            <i class="bi bi-camera-video-fill" v-if="!entry.loading"> </i>
                            <span>{{ shortEntry(entry.filename) }}</span>
                          </div>
                          <button
                            type="button"
                            class="btn btn-secondary"
                            style="height: fit-content; padding: 2px 4px"
                            v-on:click.stop="openDownloadSIDUrl(entry.filename)"
                          >
                            <i class="bi bi-download"> </i>
                          </button>
                        </div>
                      </div>
                    </template>
                  </template>
                  <template v-else>
                    <div
                      type="button"
                      :class="'list-group-item list-group-item-' + getVariant(entry)"
                      style="white-space: pre-line"
                      v-on:click="openDownloadUrl(entry.filename)"
                    >
                      <div class="spinner-border text-primary" v-if="entry.loading"></div>
                      <i class="bi bi-download" v-if="!entry.loading"> </i>
                      <span>{{ shortEntry(entry.filename) }}</span>
                    </div>
                  </template>
                </div>
              </div>
            </div>
            <div class="tab-pane fade" id="search" role="tabpanel" aria-labelledby="search-tab">
              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="
                  font-size: smaller;
                  padding: 2px 4px;
                  position: absolute;
                  top: 0px;
                  right: 245px;
                  white-space: nowrap;
                "
                @click="(event) => requestSearchResults(event, 'Hubbard_Rob')"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>R. Hubbard</span>
              </button>

              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="
                  font-size: smaller;
                  padding: 2px 4px;
                  position: absolute;
                  top: 0px;
                  right: 130px;
                  white-space: nowrap;
                "
                @click="(event) => requestSearchResults(event, 'Galway_Martin')"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>M. Galway</span>
              </button>

              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="
                  font-size: smaller;
                  padding: 2px 4px;
                  position: absolute;
                  top: 0px;
                  right: 16px;
                  white-space: nowrap;
                "
                @click="(event) => requestSearchResults(event, 'Huelsbeck_Chris')"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>C. H&uuml;lsbeck</span>
              </button>

              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="
                  font-size: smaller;
                  padding: 2px 4px;
                  position: absolute;
                  top: 30px;
                  right: 232px;
                  white-space: nowrap;
                "
                @click="(event) => requestSearchResults(event, 'Ouwehand_Reyn')"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>R. Ouwehand</span>
              </button>

              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="
                  font-size: smaller;
                  padding: 2px 4px;
                  position: absolute;
                  top: 30px;
                  right: 130px;
                  white-space: nowrap;
                "
                @click="(event) => requestSearchResults(event, 'Tel_Jeroen')"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>Jeroen Tel</span>
              </button>

              <button
                type="button"
                class="btn btn-secondary btn-sm"
                style="
                  font-size: smaller;
                  padding: 2px 4px;
                  position: absolute;
                  top: 30px;
                  right: 28px;
                  white-space: nowrap;
                "
                @click="(event) => requestSearchResults(event, 'Daglish_Ben')"
              >
                <i class="bi bi-filter-circle-fill"></i>
                <span>B. Daglish</span>
              </button>

              <div style="height: 40px"></div>

              <table class="table table-sm w-auto">
                <thead>
                  <tr>
                    <th
                      v-for="(entry, index) in searchFields"
                      :key="entry.key"
                      style="word-break: break-all; font-size: small"
                    >
                      <template v-if="entry.key === 'category'">
                        <label for="category" style="margin: 4px"
                          ><span>{{ $t("Search.category") }}</span></label
                        >
                        <select
                          class="form-select form-select-sm mt-1"
                          id="category"
                          v-model="category"
                          @change="requestSearchResults"
                          value-field="aqlKey"
                          text-field="name"
                          style="margin-left: 0 !important; margin-right: 0 !important; max-width: 100%"
                        >
                          <option v-for="(sfEntry, sfIndex) in categories" :key="sfEntry.key" :value="sfEntry.aqlKey">
                            {{ sfEntry.name }}
                          </option>
                        </select>
                      </template>
                      <template v-if="entry.key === 'name'">
                        <label for="name" style="margin: 4px">{{ $t("Search.name") }}</label>
                        <input
                          class="form-control"
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
                      <template v-if="entry.key === 'event'">
                        <label for="event" style="margin: 4px">{{ $t("Search.event") }}</label>
                        <input
                          class="form-control"
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
                      <template v-if="entry.key === 'released'" style="padding-right: calc(0.3rem + 0.1em)">
                        <label for="released" style="margin: 4px">{{ $t("Search.release") }}</label>
                        <input
                          class="form-control"
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
                      <template v-if="entry.key === 'handle'">
                        <label for="handle" style="margin: 4px">{{ $t("Search.handle") }}</label>
                        <input
                          class="form-control"
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
                      <template v-if="entry.key === 'rating'">
                        <label for="rating" style="margin: 4px">{{ $t("Search.rating") }}</label>
                        <input
                          class="form-control"
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
                      <template v-if="entry.key === 'actions'">
                        <button
                          type="button"
                          class="btn btn-secondary"
                          style="padding: 0px"
                          @click="
                            (event) => {
                              resetSearchResults(event);
                              requestSearchResults(event);
                            }
                          "
                        >
                          <i class="bi bi-eraser-fill"> </i>
                        </button>
                      </template>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <template v-for="(row, rowIndex) in searchResults" :key="row.id">
                    <tr>
                      <td
                        v-for="(searchField, searchFieldIndex) in searchFields"
                        :key="searchField.key"
                        :class="'field-' + searchField.key"
                        :style="
                          rowIndex % 2 === 0
                            ? 'word-break:break-all;background-color: rgba(0, 0, 0, .05)'
                            : 'word-break:break-all;'
                        "
                      >
                        <template v-if="searchField.key === 'category'">
                          <span>{{ row.category }}</span>
                        </template>
                        <template v-if="searchField.key === 'name'">
                          <div>
                            <span>{{ row.name }}</span>
                          </div>
                          <div>
                            <span style="font-style: italic; font-size: small">{{ row.group }}</span>
                          </div>
                        </template>
                        <template v-if="searchField.key === 'event'">
                          <div>
                            <span>{{ row.event }}</span>
                          </div>
                        </template>
                        <template v-if="searchField.key === 'released'">
                          <div>
                            <span>{{ row.released }}</span>
                          </div>
                        </template>
                        <template v-if="searchField.key === 'handle'">
                          <div>
                            <span>{{ row.handle }}</span>
                          </div>
                        </template>
                        <template v-if="searchField.key === 'rating'">
                          <div>
                            <span>{{ row.rating }}</span>
                          </div>
                        </template>
                        <template v-if="searchField.key === 'actions'">
                          <button
                            type="button"
                            class="btn btn-secondary mr-1"
                            @click="requestContentEntries(row)"
                            style="padding: 0"
                          >
                            <i class="bi bi-caret-down-fill" v-if="row._showDetails" @click="row._showDetails = true">
                            </i>
                            <i
                              class="bi bi-caret-right-fill"
                              v-if="!row._showDetails"
                              @click="row._showDetails = false"
                            >
                            </i>
                          </button>
                        </template>
                      </td>
                    </tr>
                    <tr v-if="row._showDetails">
                      <td colspan="7">
                        <div class="card">
                          <table class="table table-sm table-striped table-bordered w-auto">
                            <thead>
                              <tr>
                                <th>
                                  <div>
                                    <span> File </span>
                                  </div>
                                </th>
                              </tr>
                            </thead>
                            <tbody>
                              <tr v-for="(innerRow, index) in row.contentEntries" :key="innerRow.filename">
                                <td :style="index % 2 === 0 ? 'background-color: var(--bs-table-bg)' : ''">
                                  <template v-if="isMusic(innerRow)">
                                    <div style="white-space: pre-line; display: flex; justify-content: space-between">
                                      <div style="flex-grow: 4; word-break: break-all">
                                        <i class="bi bi-music-note"> </i>
                                        <a
                                          href="#"
                                          style="white-space: pre-line"
                                          @click.prevent="
                                            currentSid = shortEntry(innerRow.filename);
                                            updateSid(innerRow.filename, row.id, row.categoryId);
                                            showAudio = true;
                                            $nextTick(function () {
                                              play(undefined, innerRow.filename, row.id, row.categoryId);
                                            });
                                          "
                                        >
                                          <span class="sid-file">{{ shortEntry(innerRow.filename) }}</span>
                                        </a>
                                      </div>

                                      <button
                                        type="button"
                                        class="btn btn-secondary"
                                        style="height: fit-content; padding: 2px 4px; white-space: nowrap"
                                        v-on:click="openDownloadMP3Url(innerRow.filename, row.id, row.categoryId)"
                                      >
                                        <i class="bi bi-download"> </i>
                                        <span style="font-size: x-small">{{ $t("downloadMP3") }}</span>
                                      </button>
                                      <button
                                        type="button"
                                        class="btn btn-secondary"
                                        style="height: fit-content; padding: 2px 4px"
                                        v-on:click="openDownloadSIDUrl(innerRow.filename, row.id, row.categoryId)"
                                      >
                                        <i class="bi bi-download"></i>
                                      </button>
                                      <div>
                                        <button
                                          type="button"
                                          class="btn btn-primary"
                                          style="height: fit-content; padding: 2px 4px"
                                          v-on:click="
                                            playlist.push({
                                              filename: innerRow.filename,
                                              itemId: row.id,
                                              categoryId: row.categoryId,
                                            });
                                            $refs.playlistTab.click();
                                            playlistIndex = 0;
                                          "
                                        >
                                          <i class="bi bi-plus"> </i>
                                        </button>
                                      </div>
                                    </div>
                                  </template>

                                  <template v-else-if="isVideo(innerRow)">
                                    <span>
                                      <template v-if="canFastload(innerRow)">
                                        <div
                                          style="white-space: pre-line; display: flex; justify-content: space-between"
                                        >
                                          <div style="flex-grow: 4; word-break: break-all">
                                            <div
                                              class="spinner-border spinner-border-sm text-primary"
                                              v-if="innerRow.loadingDisk"
                                            ></div>

                                            <a
                                              href="#"
                                              @click.prevent="
                                                openiframe(
                                                  createConvertUrl(undefined, innerRow.filename, row.id, row.categoryId)
                                                )
                                              "
                                            >
                                              <i class="bi bi-camera-video-fill"> </i>
                                              <span style="word-break: break-all">{{
                                                shortEntry(innerRow.filename)
                                              }}</span>
                                            </a>
                                          </div>
                                          <button
                                            type="button"
                                            class="btn btn-primary btn-sm"
                                            style="height: fit-content; padding: 2px 4px"
                                            v-on:click="fetchDiskDirectory(innerRow, row.id, row.categoryId)"
                                            :disabled="innerRow.loadingDisk"
                                          >
                                            <i class="bi bi-journal-text"> </i>
                                          </button>
                                          <button
                                            type="button"
                                            class="btn btn-primary btn-sm"
                                            style="height: fit-content; padding: 2px 4px"
                                            v-on:click="openDownloadSIDUrl(innerRow.filename, row.id, row.categoryId)"
                                          >
                                            <i class="bi bi-download"> </i>
                                          </button>
                                        </div>
                                        <div>
                                          <div v-show="innerRow.directoryMode > 0">
                                            <div class="disk-directory">
                                              <div>
                                                <span class="c64-font">{{ innerRow.diskDirectoryHeader }}</span>
                                              </div>
                                              <div v-for="(program, index) in innerRow.diskDirectory" :key="index">
                                                <a
                                                  href="#"
                                                  @click.prevent="
                                                    openiframe(
                                                      createConvertUrl(
                                                        program.directoryLine,
                                                        innerRow.filename,
                                                        row.id,
                                                        row.categoryId
                                                      )
                                                    )
                                                  "
                                                >
                                                  <span class="c64-font">{{ program.formatted }}</span>
                                                </a>
                                              </div>
                                            </div>
                                          </div>
                                        </div>
                                      </template>
                                      <template v-else>
                                        <div
                                          style="white-space: pre-line; display: flex; justify-content: space-between"
                                        >
                                          <div style="flex-grow: 4; word-break: break-all">
                                            <a
                                              href="#"
                                              @click.prevent="
                                                openiframe(
                                                  createConvertUrl(undefined, innerRow.filename, row.id, row.categoryId)
                                                )
                                              "
                                            >
                                              <i class="bi bi-camera-video-fill"> </i>

                                              <span style="word-break: break-all">{{
                                                shortEntry(innerRow.filename)
                                              }}</span>
                                            </a>
                                          </div>
                                          <button
                                            type="button"
                                            class="btn btn-primary btn-sm"
                                            style="height: fit-content; padding: 2px 4px"
                                            v-on:click="openDownloadSIDUrl(innerRow.filename, row.id, row.categoryId)"
                                          >
                                            <i class="bi bi-download"> </i>
                                          </button>
                                        </div>
                                      </template>
                                    </span>
                                  </template>
                                  <template v-else>
                                    <div>
                                      <i class="bi bi-download"> </i>
                                      <a
                                        href="#"
                                        style="white-space: pre-line"
                                        v-on:click="openDownloadUrl(innerRow.filename, row.id, row.categoryId)"
                                      >
                                        <span>{{ shortEntry(innerRow.filename) }}</span>
                                      </a>
                                    </div>
                                  </template>
                                </td>
                              </tr>
                            </tbody>
                          </table>
                        </div>
                      </td>
                    </tr></template
                  >
                </tbody>
              </table>
            </div>
            <div class="tab-pane fade show" id="sid" role="tabpanel" aria-labelledby="sid-tab">
              <div class="sid">
                <table class="table table-striped table-bordered">
                  <thead>
                    <tr>
                      <th>{{ $t("sidInfoKey") }}</th>
                      <th>{{ $t("sidInfoValue") }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="entry in translatedInfos" :key="entry.Name">
                      <td>
                        <span
                          :style="entry.opacity ? 'opacity: 0.5; line-break: anywhere;' : 'line-break: anywhere;'"
                          >{{ entry.Name }}</span
                        >
                      </td>
                      <td style="word-break: break-all">{{ entry.Value }}</td>
                    </tr>
                  </tbody>
                </table>
                <div class="picture-container">
                  <img :src="picture" id="img" class="picture img-fluid rounded-circle" />
                </div>
              </div>
            </div>
            <div class="tab-pane fade show" id="stil" role="tabpanel" aria-labelledby="stil-tab">
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
            </div>
            <div class="tab-pane fade show" id="pl" role="tabpanel" aria-labelledby="pl-tab">
              <button
                type="button"
                class="btn btn-primary btn-sm"
                style="padding: 0px 4px; float: right"
                v-show="!importFile && !importExportVisible"
                v-on:click="importExportVisible = !importExportVisible"
              >
                <i class="bi bi-arrows-expand"> </i>
                <span>{{ $t("importExport") }}</span>
              </button>
              <button
                type="button"
                class="btn btn-primary btn-sm"
                style="padding: 0px 4px; float: right"
                v-show="!importFile && importExportVisible"
                v-on:click="importExportVisible = !importExportVisible"
              >
                <i class="bi bi-arrows-collapse"> </i>
              </button>
              <div v-show="!importFile && importExportVisible" @click="importExportVisible = !importExportVisible">
                <div class="playlist-examples-box">
                  <span class="sm">{{ $t("fetchFavorites") }}</span>
                  <button
                    type="button"
                    class="btn btn-primary btn-sm"
                    v-for="(favoritesName, index) in favoritesNames"
                    :key="index"
                    data-bs-toggle="modal"
                    data-bs-target="#fetchFavoritesModal"
                    @click="modalIndex = index"
                  >
                    <span>{{ favoritesName }}</span>
                  </button>
                </div>
                <div class="button-box">
                  <div class="input-group input-group-sm mb-2">
                    <input
                      ref="formFileSm"
                      class="form-control form-control-sm"
                      id="formFileSm"
                      type="file"
                      @input="importFile = $refs.formFileSm.files[0]"
                    />
                    <button
                      type="button"
                      class="btn btn-secondary"
                      @click="exportPlaylist"
                      v-if="playlist.length > 0"
                      style="margin-top: 8px !important"
                    >
                      <i class="bi bi-file-arrow-down-fill"></i>
                      <span>{{ $t("exportPlaylist") }}</span>
                    </button>
                  </div>
                </div>
              </div>
              <!-- Modal -->
              <div
                class="modal fade"
                id="fetchFavoritesModal"
                tabindex="-1"
                aria-labelledby="fetchFavoritesModalLabel"
                aria-hidden="true"
              >
                <div class="modal-dialog">
                  <div class="modal-content">
                    <div class="modal-header">
                      <h5 class="modal-title" id="fetchFavoritesModalLabel">{{ $t("confirmationTitle") }}</h5>
                      <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                      <p>{{ $t("removePlaylistReally") }}</p>
                      <span> </span>
                    </div>
                    <div class="modal-footer">
                      <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                      <button
                        type="button"
                        class="btn btn-primary"
                        data-bs-dismiss="modal"
                        @click="fetchFavorites(modalIndex)"
                      >
                        OK
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div>
                <div>
                  <button
                    type="button"
                    class="btn btn-secondary btn-sm"
                    v-if="importFile != null"
                    @click="
                      importFile = null;
                      $refs.formFileSm.value = null;
                    "
                  >
                    <i class="bi bi-backspace"></i>
                    <span>{{ $t("reset") }}</span>
                  </button>
                  <button
                    type="button"
                    class="btn btn-success btn-sm"
                    v-if="importFile != null && (importFile.name.endsWith('.js2') || importFile.name.endsWith('.json'))"
                    class="mr-2"
                    data-bs-toggle="modal"
                    data-bs-target="#importPlaylistModal"
                  >
                    <i class="bi bi-file-arrow-up-fill"></i>
                    <span>{{ $t("startImport") }}</span>
                  </button>
                  <!-- Modal -->
                  <div
                    class="modal fade"
                    id="importPlaylistModal"
                    tabindex="-1"
                    aria-labelledby="importPlaylistModalLabel"
                    aria-hidden="true"
                  >
                    <div class="modal-dialog">
                      <div class="modal-content">
                        <div class="modal-header">
                          <h5 class="modal-title" id="importPlaylistModalLabel">{{ $t("confirmationTitle") }}</h5>
                          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                          <p>{{ $t("removePlaylistReally") }}</p>
                        </div>
                        <div class="modal-footer">
                          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                          <button type="button" class="btn btn-primary" data-bs-dismiss="modal" @click="importPlaylist">
                            OK
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="btn btn-success btn-sm"
                    v-on:click="importPlaylist"
                    v-if="
                      importFile != null && !(importFile.name.endsWith('.js2') || importFile.name.endsWith('.json'))
                    "
                    class="mr-2"
                  >
                    <i class="bi bi-file-arrow-up-fill"></i>
                    <span>{{ $t("startImport") }}</span>
                  </button>
                </div>
                <button
                  type="button"
                  class="btn btn-danger btn-sm"
                  v-if="!importFile && playlist.length > 0"
                  variant="danger"
                  data-bs-toggle="modal"
                  data-bs-target="#removeFavoritesModal"
                >
                  <i class="bi bi-trash-fill"></i>
                  <span>{{ $t("removePlaylist") }}</span>
                </button>
                <!-- Modal -->
                <div
                  class="modal fade"
                  id="removeFavoritesModal"
                  tabindex="-1"
                  aria-labelledby="removeFavoritesModalLabel"
                  aria-hidden="true"
                >
                  <div class="modal-dialog">
                    <div class="modal-content">
                      <div class="modal-header">
                        <h5 class="modal-title" id="removeFavoritesModalLabel">{{ $t("confirmationTitle") }}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                      </div>
                      <div class="modal-body">
                        <p>{{ $t("removePlaylistReally") }}</p>
                        <span> </span>
                      </div>
                      <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="button" class="btn btn-primary" data-bs-dismiss="modal" @click="removePlaylist">
                          OK
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
                <button
                  type="button"
                  class="btn btn-success btn-sm"
                  v-if="!importFile"
                  v-on:click="setNextPlaylistEntry"
                  v-if="playlist.length > 0"
                >
                  <i class="bi bi-play-fill"></i>
                  <span>{{ $t("next") }}</span>
                </button>
              </div>
              <div class="button-box">
                <div class="input-group input-group-sm mb-2">
                  <div class="input-group-text"><i class="bi bi-search"></i></div>
                  <input
                    id="quickfilter2"
                    type="search"
                    class="form-control"
                    type="search"
                    v-model="filterText"
                    :placeholder="$t('searchPlaceholder')"
                  />
                </div>
              </div>
              <ol class="striped-list">
                <li
                  v-for="(entry, index) in playlist"
                  :key="index"
                  :style="
                    (filterText && !entry.filename.toLowerCase().includes(filterText.toLowerCase())
                      ? 'height: 0; padding: 0px;visibility: hidden;'
                      : '') +
                    (index == playlistIndex && !theaterMode ? ' background-color: rgb(0 255 58 / 50%) !important' : '')
                  "
                  v-on:click="
                    playlistIndex = index;
                    $nextTick(function () {
                      play(
                        undefined,
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
                        <span style="line-break: anywhere">{{ pathEntry(entry.filename) }}</span>
                      </div>
                    </div>
                    <button
                      type="button"
                      class="btn btn-danger btn-sm"
                      style="height: fit-content; opacity: 0.5"
                      data-bs-toggle="modal"
                      data-bs-target="#removeFavoriteModal"
                      @click.stop="modalFavoriteIndex = index"
                    >
                      <i class="bi bi-trash-fill" style="margin: 2px"></i>
                    </button>
                  </span>
                </li>
              </ol>

              <!-- Modal -->
              <div
                class="modal fade"
                id="removeFavoriteModal"
                tabindex="-1"
                aria-labelledby="removeFavoriteModalLabel"
                aria-hidden="true"
              >
                <div class="modal-dialog">
                  <div class="modal-content">
                    <div class="modal-header">
                      <h5 class="modal-title" id="removeFavoriteModalLabel">{{ $t("confirmationTitle") }}</h5>
                      <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                      <p>{{ $t("removeReally") }}</p>
                      <span> </span>
                    </div>
                    <div class="modal-footer">
                      <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                      <button
                        type="button"
                        class="btn btn-primary"
                        data-bs-dismiss="modal"
                        @click="remove(modalFavoriteIndex)"
                      >
                        OK
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="tab-pane fade show" id="hw" role="tabpanel" aria-labelledby="hw-tab">
              <div class="container container-fluid">
                <div class="row">
                  <div class="col" style="border-right: 1px dotted grey">
                    <b
                      ><div style="margin-bottom: 8px; text-align: center">{{ $t("HARDSID") }}</div></b
                    >
                    <div class="container">
                      <img
                        src="/static/images/hardsid4u.jpeg"
                        alt="HardSID4U"
                        class="img-fluid img-thumbnail mx-auto"
                      />
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="hardsid6581">
                          <select
                            class="form-select form-select-sm right"
                            id="hardsid6581"
                            v-model="convertOptions.config.emulationSection.hardsid6581"
                          >
                            <option value="0">1</option>
                            <option value="1">2</option>
                            <option value="2">3</option>
                            <option value="3">4</option>
                          </select>
                          <span>{{ $t("convertMessages.config.emulationSection.hardsid6581") }}</span>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="hardsid8580">
                          <select
                            class="form-select form-select-sm right"
                            id="hardsid8580"
                            v-model="convertOptions.config.emulationSection.hardsid8580"
                          >
                            <option value="0">1</option>
                            <option value="1">2</option>
                            <option value="2">3</option>
                            <option value="3">4</option>
                          </select>
                          <span>{{ $t("convertMessages.config.emulationSection.hardsid8580") }}</span>
                        </label></span
                      >
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <button type="button" class="btn btn-sm btn-success" v-on:click="hardware_hardsid_init()">
                          <span>{{ $t("CONNECT") }}</span>
                        </button>
                      </span>
                    </div>
                  </div>
                  <div class="col" style="display: flex; justify-content: space-between; flex-direction: column">
                    <b
                      ><div style="margin-bottom: 8px; text-align: center">{{ $t("EXSID") }}</div></b
                    >
                    <div class="container" style="flex: auto">
                      <img src="/static/images/exsid.jpg" alt="ExSID" class="img-fluid img-thumbnail mx-auto" />
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <button type="button" class="btn btn-sm btn-success" v-on:click="hardware_exsid_init()">
                          <span>{{ $t("CONNECT") }}</span>
                        </button></span
                      >
                    </div>
                  </div>
                  <div
                    class="col"
                    style="
                      border-left: 1px dotted grey;
                      display: flex;
                      justify-content: space-between;
                      flex-direction: column;
                    "
                  >
                    <b
                      ><div style="margin-bottom: 8px; text-align: center">{{ $t("SIDBLASTER") }}</div></b
                    >
                    <div class="container" style="flex: auto">
                      <img
                        src="/static/images/sidblaster.jpeg"
                        alt="SIDBlaster"
                        class="img-fluid img-thumbnail mx-auto"
                      />
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <button type="button" class="btn btn-sm btn-success" v-on:click="hardware_sidblaster_init()">
                          <span>{{ $t("CONNECT") }}</span>
                        </button></span
                      >
                    </div>
                  </div>
                </div>
              </div>

              <p style="margin-top: 16px">{{ $t("USE_REAL_HARDWARE") }}</p>
              <ul>
                <li>{{ $t("HARDWARE_PREPARATION_1") }}</li>
                <li>
                  {{ $t("HARDWARE_PREPARATION_2") }}
                  <a href="https://zadig.akeo.ie/" target="_blank"> {{ $t("HERE") }} </a>
                </li>
                <li>{{ $t("HARDWARE_PREPARATION_3") }}</li>
                <li>
                  Ubuntu with {{ $t("HARDSID") }} devices:
                  <pre>$ sudo vi /etc/udev/rules.d/92-hardsid4u.rules</pre>
                  <pre>with contents:</pre>
                  <pre>SUBSYSTEM=="usb",ATTR{idVendor}=="6581",ATTR{idProduct}=="8580",MODE="0660",GROUP="plugdev"</pre>
                  <pre>$ sudo udevadm trigger</pre>
                  <pre>now reboot</pre>
                </li>
                <li>
                  Ubuntu with {{ $t("EXSID") }} devices:
                  <pre>$ sudo vi /etc/udev/rules.d/93-exsid.rules</pre>
                  <pre>with contents:</pre>
                  <pre>
ACTION=="add", ATTRS{idVendor}=="0403", ATTRS{idProduct}=="6001", MODE="0666", RUN+="/bin/sh -c 'rmmod ftdi_sio && rmmod usbserial'"</pre
                  >
                  <pre>For ExSID+ you have to replace idProduct 6001 with 6015!</pre>
                  <pre>$ sudo udevadm control --reload-rules && udevadm trigger</pre>
                  <pre>now reboot</pre>
                  Fedora Linux with {{ $t("EXSID") }} devices:
                  <pre>$ sudo vi /etc/udev/rules.d/93-exsid.rules</pre>
                  <pre>with contents:</pre>
                  <pre>
ACTION=="add", ATTRS{idVendor}=="0403", ATTRS{idProduct}=="6001", MODE="0666", RUN+="/bin/sh -c 'echo -n $id:1.0 > /sys/bus/usb/drivers/ftdi_sio/unbind; echo -n $id:1.1 > /sys/bus/usb/drivers/ftdi_sio/unbind'"</pre
                  >
                  <pre>For ExSID+ you have to replace idProduct 6001 with 6015!</pre>
                  <pre>$ sudo udevadm control --reload-rules && udevadm trigger</pre>
                  <pre>now reboot</pre>
                </li>
              </ul>
              <p>
                <span>{{ $t("USE_MOBILE_DEVICES_1") }}</span>

                <a
                  href="https://www.amazon.de/gp/product/B09H2TJCQG/ref=ppx_yo_dt_b_search_asin_image?ie=UTF8&psc=1"
                  target="_blank"
                >
                  <span>{{ $t("USE_MOBILE_DEVICES_2") }}</span></a
                >.
                <img src="/static/images/usbc.jpg" alt="HardSID4U" class="d-block img-fluid img-thumbnail mx-auto" />
              </p>
              <p>
                <b> {{ $t("STREAMING_NOTES") }} </b>
              </p>
            </div>
            <div class="tab-pane fade show" id="cfg" role="tabpanel" aria-labelledby="cfg-tab">
              <div class="settings-box">
                <div class="button-box">
                  <button
                    type="button"
                    class="btn btn-outline-success btn-sm"
                    data-bs-toggle="modal"
                    data-bs-target="#setDefaultModal"
                  >
                    <span>{{ $t("setDefault") }}</span>
                  </button>
                  <!-- Modal -->
                  <div
                    class="modal fade"
                    id="setDefaultModal"
                    tabindex="-1"
                    aria-labelledby="setDefaultModalLabel"
                    aria-hidden="true"
                  >
                    <div class="modal-dialog">
                      <div class="modal-content">
                        <div class="modal-header">
                          <h5 class="modal-title" id="setDefaultModalLabel">{{ $t("confirmationTitle") }}</h5>
                          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                          <p>{{ $t("setDefaultReally") }}</p>
                        </div>
                        <div class="modal-footer">
                          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                          <button type="button" class="btn btn-primary" data-bs-dismiss="modal" @click="setDefault">
                            OK
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="card">
                <div class="card-header">
                  <ul class="nav nav-pills card-header-pills mb-2 right" role="tablist">
                    <li class="nav-item" role="presentation">
                      <button
                        class="nav-link active"
                        id="streaming-tab"
                        data-bs-toggle="pill"
                        data-bs-target="#streaming"
                        type="button"
                        role="tab"
                        aria-controls="streaming"
                        aria-selected="true"
                      >
                        {{ $t("streamingCfgHeader") }}
                      </button>
                    </li>
                    <li class="nav-item" role="presentation">
                      <button
                        class="nav-link"
                        id="playback-tab"
                        data-bs-toggle="pill"
                        data-bs-target="#playback"
                        type="button"
                        role="tab"
                        aria-controls="playback"
                        aria-selected="false"
                      >
                        {{ $t("playbackCfgHeader") }}
                      </button>
                    </li>
                    <li class="nav-item" role="presentation">
                      <button
                        class="nav-link"
                        id="audio-tab"
                        data-bs-toggle="pill"
                        data-bs-target="#audio"
                        type="button"
                        role="tab"
                        aria-controls="audio"
                        aria-selected="false"
                      >
                        {{ $t("audioCfgHeader") }}
                      </button>
                    </li>
                    <li class="nav-item" role="presentation">
                      <button
                        class="nav-link"
                        id="emulation-tab"
                        data-bs-toggle="pill"
                        data-bs-target="#emulation"
                        type="button"
                        role="tab"
                        aria-controls="emulation"
                        aria-selected="false"
                      >
                        {{ $t("emulationCfgHeader") }}
                      </button>
                    </li>
                    <li class="nav-item" role="presentation">
                      <button
                        class="nav-link"
                        id="filter-tab"
                        data-bs-toggle="pill"
                        data-bs-target="#filter"
                        type="button"
                        role="tab"
                        aria-controls="filter"
                        aria-selected="false"
                      >
                        {{ $t("filterCfgHeader") }}
                      </button>
                    </li>
                    <li class="nav-item" role="presentation">
                      <button
                        class="nav-link"
                        id="mute-tab"
                        data-bs-toggle="pill"
                        data-bs-target="#mute"
                        type="button"
                        role="tab"
                        aria-controls="mute"
                        aria-selected="false"
                      >
                        {{ $t("mutingCfgHeader") }}
                      </button>
                    </li>
                    <li class="nav-item" role="presentation">
                      <button
                        class="nav-link"
                        id="cart-tab"
                        data-bs-toggle="pill"
                        data-bs-target="#cart"
                        type="button"
                        role="tab"
                        aria-controls="cart"
                        aria-selected="false"
                      >
                        {{ $t("floppyCartCfgHeader") }}
                      </button>
                    </li>
                  </ul>
                </div>
                <div class="tab-content card-body" style="position: relative">
                  <div class="tab-pane fade show active" id="streaming" role="tabpanel" aria-labelledby="streaming-tab">
                    <div class="settings-box">
                      <div class="button-box">
                        <button type="button" class="btn btn-outline-success btn-sm" v-on:click="mobileProfile">
                          <i class="bi bi-phone-fill"> </i>
                          <span>{{ $t("mobileProfile") }}</span>
                        </button>
                        <button type="button" class="btn btn-outline-success btn-sm" v-on:click="wifiProfile">
                          <i class="bi bi-wifi"> </i>
                          <span>{{ $t("wifiProfile") }}</span>
                        </button>
                      </div>
                    </div>

                    <div class="card">
                      <div class="card-header">
                        <ul class="nav nav-pills card-header-pills mb-2 right" ole="tablist">
                          <li class="nav-item" role="presentation">
                            <button
                              class="nav-link active"
                              id="audiostreaming-tab"
                              data-bs-toggle="pill"
                              data-bs-target="#audiostreaming"
                              type="button"
                              role="tab"
                              aria-controls="audiostreaming"
                              aria-selected="true"
                            >
                              {{ $t("audioStreamingCfgHeader") }}
                            </button>
                          </li>
                          <li class="nav-item" role="presentation">
                            <button
                              class="nav-link"
                              id="videostreaming-tab"
                              data-bs-toggle="pill"
                              data-bs-target="#videostreaming"
                              type="button"
                              role="tab"
                              aria-controls="videostreaming"
                              aria-selected="false"
                            >
                              {{ $t("videoStreamingCfgHeader") }}
                            </button>
                          </li>
                        </ul>
                      </div>

                      <div class="tab-content card-body" style="position: relative">
                        <div
                          class="tab-pane fade show active"
                          id="audiostreaming"
                          role="tabpanel"
                          aria-labelledby="audiostreaming-tab"
                        >
                          <div class="settings-box">
                            <span class="setting"
                              ><label for="cbr">
                                {{ $t("convertMessages.config.audioSection.cbr") }}
                                <select
                                  class="form-select form-select-sm right"
                                  id="cbr"
                                  v-model="convertOptions.config.audioSection.cbr"
                                >
                                  <option v-for="cbr in cbrs">{{ cbr }}</option>
                                </select></label
                              ></span
                            >
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="vbr">
                                  {{ $t("convertMessages.config.audioSection.vbr") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="vbr"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.audioSection.vbr"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting"
                              ><label for="vbrQuality">
                                {{ $t("convertMessages.config.audioSection.vbrQuality") }}
                                <select
                                  class="form-select form-select-sm right"
                                  id="vbrQuality"
                                  v-model="convertOptions.config.audioSection.vbrQuality"
                                >
                                  <option v-for="vbrQuality in vbrQualities">{{ vbrQuality }}</option>
                                </select></label
                              ></span
                            >
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <label class="form-check-label" for="textToSpeechTypeOff">
                                {{ $t("convertMessages.textToSpeechType") }}
                              </label>
                              <div class="input-group" style="justify-content: flex-end">
                                <div class="form-check">
                                  <input
                                    class="form-check-input"
                                    type="radio"
                                    value="NONE"
                                    id="textToSpeechTypeOff"
                                    v-model="convertOptions.textToSpeechType"
                                  />
                                  <label class="form-check-label" for="textToSpeechTypeOff">
                                    {{ $t("textToSpeechTypeOff") }}
                                  </label>
                                </div>
                                <div class="form-check">
                                  <input
                                    class="form-check-input"
                                    type="radio"
                                    value="PICO2WAVE"
                                    id="textToSpeechTypePico2Wave"
                                    v-model="convertOptions.textToSpeechType"
                                  />
                                  <label class="form-check-label" for="textToSpeechTypePico2Wave"> PICO2WAVE </label>
                                </div>
                                <div class="form-check">
                                  <input
                                    class="form-check-input"
                                    type="radio"
                                    value="ESPEAK"
                                    id="textToSpeechTypeESpeak"
                                    v-model="convertOptions.textToSpeechType"
                                  />
                                  <label class="form-check-label" for="textToSpeechTypeESpeak"> ESPEAK </label>
                                </div>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <label class="form-check-label" for="textToSpeechLocaleAuto">
                                {{ $t("convertMessages.textToSpeechLocale") }}
                              </label>
                              <div class="input-group" style="justify-content: flex-end">
                                <div class="form-check">
                                  <input
                                    class="form-check-input"
                                    type="radio"
                                    value="null"
                                    id="textToSpeechLocaleAuto"
                                    v-model="convertOptions.textToSpeechLocale"
                                  />
                                  <label class="form-check-label" for="textToSpeechLocaleAuto">
                                    {{ $t("textToSpeechLocaleAuto") }}
                                  </label>
                                </div>
                                <div class="form-check">
                                  <input
                                    class="form-check-input"
                                    type="radio"
                                    value="en"
                                    id="en"
                                    v-model="convertOptions.textToSpeechLocale"
                                  />
                                  <label class="form-check-label" for="en"> English </label>
                                </div>
                                <div class="form-check">
                                  <input
                                    class="form-check-input"
                                    type="radio"
                                    value="de"
                                    id="de"
                                    v-model="convertOptions.textToSpeechLocale"
                                  />
                                  <label class="form-check-label" for="de"> Deutsch </label>
                                </div>
                              </div>
                            </span>
                          </div>
                        </div>
                        <div
                          class="tab-pane fade show"
                          id="videostreaming"
                          role="tabpanel"
                          aria-labelledby="videostreaming-tab"
                        >
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="useHls">
                                  {{ $t("convertMessages.useHls") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="useHls"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.useHls"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <label class="form-check-label right" for="hlsTypeVideoJS">
                                {{ $t("convertMessages.hlsType") }}
                              </label>
                              <div class="input-group" style="justify-content: flex-end">
                                <div class="form-check">
                                  <input
                                    class="form-check-input"
                                    type="radio"
                                    value="VIDEO_JS"
                                    id="hlsTypeVideoJS"
                                    v-model="convertOptions.hlsType"
                                  />
                                  <label class="form-check-label" for="hlsTypeVideoJS"> video-js </label>
                                </div>
                                <div class="form-check">
                                  <input
                                    class="form-check-input"
                                    type="radio"
                                    value="HLS_JS"
                                    id="hlsTypeHLS"
                                    v-model="convertOptions.hlsType"
                                  />
                                  <label class="form-check-label" for="hlsTypeHLS"> hls-js </label>
                                </div>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <label for="videoCoderAudioDelay"
                                >{{ $t("convertMessages.config.audioSection.videoCoderAudioDelay") }}
                                <input
                                  class="right"
                                  type="number"
                                  id="videoCoderAudioDelay"
                                  class="form-control"
                                  v-model.number="convertOptions.config.audioSection.videoCoderAudioDelay"
                              /></label>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <label for="audioCoderBitRate"
                                >{{ $t("convertMessages.config.audioSection.audioCoderBitRate") }}
                                <input
                                  class="right"
                                  type="number"
                                  id="audioCoderBitRate"
                                  class="form-control"
                                  min="0"
                                  v-model.number="convertOptions.config.audioSection.audioCoderBitRate"
                              /></label>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <label for="videoCoderBitRate"
                                >{{ $t("convertMessages.config.audioSection.videoCoderBitRate") }}
                                <input
                                  class="right"
                                  type="number"
                                  id="videoCoderBitRate"
                                  class="form-control"
                                  min="0"
                                  v-model.number="convertOptions.config.audioSection.videoCoderBitRate"
                              /></label>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="showStatus">
                                  {{ $t("convertMessages.showStatus") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="showStatus"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.showStatus"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <label for="pressSpaceInterval"
                                >{{ $t("convertMessages.pressSpaceInterval") }}
                                <input
                                  class="right"
                                  type="number"
                                  id="pressSpaceInterval"
                                  class="form-control"
                                  min="0"
                                  v-model.number="convertOptions.pressSpaceInterval"
                              /></label>
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="tab-pane fade show" id="playback" role="tabpanel" aria-labelledby="playback-tab">
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="videoTuneAsAudio">
                            {{ $t("convertMessages.videoTuneAsAudio") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="videoTuneAsAudio"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.videoTuneAsAudio"
                            />
                          </label>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="random">
                            {{ $t("random") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="random"
                              style="float: right; margin-left: 8px"
                              v-model="random"
                            />
                          </label>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="enableDatabase">
                            {{ $t("convertMessages.config.sidplay2Section.enableDatabase") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="enableDatabase"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.config.sidplay2Section.enableDatabase"
                            />
                          </label>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="single">
                            {{ $t("convertMessages.config.sidplay2Section.single") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="single"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.config.sidplay2Section.single"
                            />
                          </label>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="loop">
                            {{ $t("convertMessages.config.sidplay2Section.loop") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="loop"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.config.sidplay2Section.loop"
                            />
                          </label>
                        </div>
                      </span>
                    </div>
                  </div>
                  <div class="tab-pane fade show" id="audio" role="tabpanel" aria-labelledby="audio-tab">
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="delayBypass">
                            {{ $t("convertMessages.config.audioSection.delayBypass") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="delayBypass"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.config.audioSection.delayBypass"
                            />
                          </label>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="reverbBypass">
                            {{ $t("convertMessages.config.audioSection.reverbBypass") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="reverbBypass"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.config.audioSection.reverbBypass"
                            />
                          </label>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="startTime">
                          {{ $t("convertMessages.config.sidplay2Section.startTime") }}

                          <input
                            class="right"
                            type="number"
                            id="startTime"
                            class="form-control"
                            min="0"
                            max="10"
                            v-model.number="convertOptions.config.sidplay2Section.startTime"
                          />
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="defaultPlayLength">
                          {{ $t("convertMessages.config.sidplay2Section.defaultPlayLength") }}
                          <input
                            class="right"
                            type="text"
                            id="defaultPlayLength"
                            class="form-control"
                            min="0"
                            max="10"
                            v-model="convertOptions.config.sidplay2Section.defaultPlayLength"
                          />
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="fadeInTime">
                          {{ $t("convertMessages.config.sidplay2Section.fadeInTime") }}
                          <input
                            class="right"
                            type="number"
                            id="fadeInTime"
                            class="form-control"
                            min="0"
                            max="10"
                            v-model.number="convertOptions.config.sidplay2Section.fadeInTime"
                          />
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="fadeOutTime">
                          {{ $t("convertMessages.config.sidplay2Section.fadeOutTime") }}
                          <input
                            class="right"
                            type="number"
                            id="fadeOutTime"
                            class="form-control"
                            min="0"
                            max="10"
                            v-model.number="convertOptions.config.sidplay2Section.fadeOutTime"
                          />
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="mainVolume"
                          >{{ $t("convertMessages.config.audioSection.mainVolume") }}
                          <div class="input-group input-group-sm mb-2">
                            <input
                              class="form-control right"
                              type="number"
                              id="mainVolume"
                              class="form-control"
                              min="-6"
                              max="6"
                              v-model.number="convertOptions.config.audioSection.mainVolume"
                            />
                            <span class="input-group-text"> db</span>
                          </div>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="secondVolume"
                          >{{ $t("convertMessages.config.audioSection.secondVolume") }}
                          <div class="input-group input-group-sm mb-2">
                            <input
                              class="form-control right"
                              type="number"
                              id="secondVolume"
                              class="form-control"
                              min="-6"
                              max="6"
                              v-model.number="convertOptions.config.audioSection.secondVolume"
                            />
                            <span class="input-group-text"> db</span>
                          </div>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="thirdVolume"
                          >{{ $t("convertMessages.config.audioSection.thirdVolume") }}
                          <div class="input-group input-group-sm mb-2">
                            <input
                              class="form-control right"
                              type="number"
                              id="thirdVolume"
                              class="form-control"
                              min="-6"
                              max="6"
                              v-model.number="convertOptions.config.audioSection.thirdVolume"
                            />
                            <span class="input-group-text"> db</span>
                          </div>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="mainBalance"
                          >{{ $t("convertMessages.config.audioSection.mainBalance") }}
                          <div class="input-group input-group-sm mb-2">
                            <span class="input-group-text">l(0) ... </span>
                            <input
                              class="form-control right"
                              type="number"
                              id="mainBalance"
                              class="form-control"
                              min="0"
                              max="1"
                              step="0.1"
                              v-model.number="convertOptions.config.audioSection.mainBalance"
                            />
                            <span class="input-group-text"> ... r(1)</span>
                          </div>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="secondBalance"
                          >{{ $t("convertMessages.config.audioSection.secondBalance") }}
                          <div class="input-group input-group-sm mb-2">
                            <span class="input-group-text">l(0) ... </span>
                            <input
                              class="form-control right"
                              type="number"
                              id="secondBalance"
                              class="form-control"
                              min="0"
                              max="1"
                              step="0.1"
                              v-model.number="convertOptions.config.audioSection.secondBalance"
                            />
                            <span class="input-group-text"> ... r(1)</span>
                          </div>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="thirdBalance"
                          >{{ $t("convertMessages.config.audioSection.thirdBalance") }}
                          <div class="input-group input-group-sm mb-2">
                            <span class="input-group-text">l(0) ... </span>
                            <input
                              class="form-control right"
                              type="number"
                              id="thirdBalance"
                              class="form-control"
                              min="0"
                              max="1"
                              step="0.1"
                              v-model.number="convertOptions.config.audioSection.thirdBalance"
                            />
                            <span class="input-group-text"> ... r(1)</span>
                          </div>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="mainDelay"
                          >{{ $t("convertMessages.config.audioSection.mainDelay") }}
                          <div class="input-group input-group-sm mb-2">
                            <input
                              class="form-control right"
                              type="number"
                              id="mainDelay"
                              class="form-control"
                              min="0"
                              max="100"
                              step="10"
                              v-model.number="convertOptions.config.audioSection.mainDelay"
                            />
                            <span class="input-group-text">ms</span>
                          </div>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="secondDelay"
                          >{{ $t("convertMessages.config.audioSection.secondDelay") }}
                          <div class="input-group input-group-sm mb-2">
                            <input
                              class="form-control right"
                              type="number"
                              id="secondDelay"
                              class="form-control"
                              min="0"
                              max="100"
                              step="10"
                              v-model.number="convertOptions.config.audioSection.secondDelay"
                            />
                            <span class="input-group-text">ms</span>
                          </div>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="thirdDelay"
                          >{{ $t("convertMessages.config.audioSection.thirdDelay") }}
                          <div class="input-group input-group-sm mb-2">
                            <input
                              class="form-control right"
                              type="number"
                              id="thirdDelay"
                              class="form-control"
                              min="0"
                              max="100"
                              step="10"
                              v-model.number="convertOptions.config.audioSection.thirdDelay"
                            />
                            <span class="input-group-text">ms</span>
                          </div>
                        </label>
                      </span>
                    </div>
                  </div>
                  <div class="tab-pane fade show" id="emulation" role="tabpanel" aria-labelledby="emulation-tab">
                    <div class="settings-box">
                      <span class="setting">
                        <label class="form-check-label" for="stereoModeAuto"> {{ $t("stereoMode") }} </label>
                        <div class="input-group" style="justify-content: flex-end">
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="AUTO"
                              id="stereoModeAuto"
                              v-model="stereoMode"
                            />
                            <label class="form-check-label" for="stereoModeAuto"> Auto </label>
                          </div>
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="FORCE_2SID"
                              id="stereoMode2Sid"
                              v-model="stereoMode"
                            />
                            <label class="form-check-label" for="stereoMode2Sid"> 2-SID </label>
                          </div>
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="FORCE_3SID"
                              id="stereoMode3Sid"
                              v-model="stereoMode"
                            />
                            <label class="form-check-label" for="stereoMode3Sid"> 3-SID </label>
                          </div>
                        </div>
                      </span>
                    </div>

                    <div class="settings-box">
                      <span class="setting">
                        <label for="dualSidBase">
                          <select
                            class="form-select form-select-sm right"
                            id="dualSidBase"
                            v-model="convertOptions.config.emulationSection.dualSidBase"
                          >
                            <option value="54304">0xd420</option>
                            <option value="54336">0xd440</option>
                            <option value="54528">0xd500</option>
                            <option value="56832">0xde00</option>
                            <option value="57088">0xdf00</option>
                          </select>
                          <span>{{ $t("convertMessages.config.emulationSection.dualSidBase") }}</span>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="thirdSIDBase">
                          <select
                            class="form-select form-select-sm right"
                            id="thirdSIDBase"
                            v-model="convertOptions.config.emulationSection.thirdSIDBase"
                          >
                            <option value="54304">0xd420</option>
                            <option value="54336">0xd440</option>
                            <option value="54528">0xd500</option>
                            <option value="56832">0xde00</option>
                            <option value="57088">0xdf00</option>
                          </select>
                          <span>{{ $t("convertMessages.config.emulationSection.thirdSIDBase") }}</span>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="defaultClockSpeed">
                          <select
                            class="form-select form-select-sm right"
                            id="defaultClockSpeed"
                            v-model="convertOptions.config.emulationSection.defaultClockSpeed"
                          >
                            <option value="PAL">PAL</option>
                            <option value="NTSC">NTSC</option>
                          </select>
                          <span>{{ $t("convertMessages.config.emulationSection.defaultClockSpeed") }}</span>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="defaultEmulation">
                          <select
                            class="form-select form-select-sm right"
                            id="defaultEmulation"
                            v-model="convertOptions.config.emulationSection.defaultEmulation"
                          >
                            <option value="RESIDFP">RESIDFP</option>
                            <option value="RESID">RESID</option>
                          </select>
                          <span>{{ $t("convertMessages.config.emulationSection.defaultEmulation") }}</span>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="defaultSidModel">
                          <select
                            class="form-select form-select-sm right"
                            id="defaultSidModel"
                            v-model="convertOptions.config.emulationSection.defaultSidModel"
                          >
                            <option value="MOS6581">MOS6581</option>
                            <option value="MOS8580">MOS8580</option>
                          </select>
                          <span>{{ $t("convertMessages.config.emulationSection.defaultSidModel") }}</span>
                        </label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="sampling">
                          <select
                            class="form-select form-select-sm right"
                            id="sampling"
                            v-model="convertOptions.config.audioSection.sampling"
                          >
                            <option value="DECIMATE">DECIMATE</option>
                            <option value="RESAMPLE">RESAMPLE</option>
                          </select>
                          <span>{{ $t("convertMessages.config.audioSection.sampling") }}</span>
                        </label>
                      </span>
                    </div>

                    <div class="settings-box">
                      <span class="setting">
                        <label class="form-check-label" for="samplingRateLow">
                          {{ $t("convertMessages.config.audioSection.samplingRate") }}
                        </label>
                        <div class="input-group" style="justify-content: flex-end">
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="LOW"
                              id="samplingRateLow"
                              v-model="convertOptions.config.audioSection.samplingRate"
                            />
                            <label class="form-check-label" for="samplingRateLow"> LOW </label>
                          </div>
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="MEDIUM"
                              id="samplingRateMedium"
                              v-model="convertOptions.config.audioSection.samplingRate"
                            />
                            <label class="form-check-label" for="samplingRateMedium"> MEDIUM </label>
                          </div>
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="HIGH"
                              id="samplingRateHigh"
                              v-model="convertOptions.config.audioSection.samplingRate"
                            />
                            <label class="form-check-label" for="samplingRateHigh"> HIGH </label>
                          </div>
                        </div>
                      </span>
                    </div>

                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="digiBoosted8580">
                            {{ $t("convertMessages.config.emulationSection.digiBoosted8580") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="digiBoosted8580"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.config.emulationSection.digiBoosted8580"
                            />
                          </label>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="detectPSID64ChipModel">
                            {{ $t("convertMessages.config.emulationSection.detectPSID64ChipModel") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="detectPSID64ChipModel"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.config.emulationSection.detectPSID64ChipModel"
                            />
                          </label>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="fakeStereo">
                            {{ $t("convertMessages.config.emulationSection.fakeStereo") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="fakeStereo"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.config.emulationSection.fakeStereo"
                            />
                          </label>
                        </div>
                      </span>
                    </div>

                    <div class="settings-box">
                      <span class="setting">
                        <label class="form-check-label" for="firstSid">
                          {{ $t("convertMessages.config.emulationSection.sidToRead") }}
                        </label>
                        <div class="input-group" style="justify-content: flex-end">
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="FIRST_SID"
                              id="firstSid"
                              v-model="convertOptions.config.emulationSection.sidToRead"
                            />
                            <label class="form-check-label" for="firstSid"> {{ $t("firstSid") }} </label>
                          </div>
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="SECOND_SID"
                              id="secondSid"
                              v-model="convertOptions.config.emulationSection.sidToRead"
                            />
                            <label class="form-check-label" for="secondSid"> {{ $t("secondSid") }} </label>
                          </div>
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="THIRD_SID"
                              id="thirdSid"
                              v-model="convertOptions.config.emulationSection.sidToRead"
                            />
                            <label class="form-check-label" for="thirdSid"> {{ $t("thirdSid") }}</label>
                          </div>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="bufferSize"
                          >{{ $t("convertMessages.config.audioSection.bufferSize") }}
                          <input
                            class="right"
                            type="number"
                            id="bufferSize"
                            class="form-control"
                            v-model.number="convertOptions.config.audioSection.bufferSize"
                        /></label>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <label for="audioBufferSize">
                          <select
                            class="form-select form-select-sm right"
                            id="audioBufferSize"
                            v-model="convertOptions.config.audioSection.audioBufferSize"
                          >
                            <option value="1024">1024</option>
                            <option value="2048">2048</option>
                            <option value="4096">4096</option>
                            <option value="8192">8192</option>
                            <option value="16384">16384</option>
                          </select>
                          <span>{{ $t("convertMessages.config.audioSection.audioBufferSize") }}</span>
                        </label>
                      </span>
                    </div>
                  </div>
                  <div class="tab-pane fade show" id="filter" role="tabpanel" aria-labelledby="filter-tab">
                    <div class="card">
                      <div class="card-header">
                        <ul class="nav nav-pills card-header-pills mb-2 right" role="tablist">
                          <li class="nav-item" role="presentation">
                            <button
                              class="nav-link active"
                              id="residfpfilter-tab"
                              data-bs-toggle="pill"
                              data-bs-target="#residfpfilter"
                              type="button"
                              role="tab"
                              aria-controls="residfpfilter"
                              aria-selected="true"
                            >
                              {{ $t("residFpFilterCfgHeader") }}
                            </button>
                          </li>
                          <li class="nav-item" role="presentation">
                            <button
                              class="nav-link"
                              id="residfilter-tab"
                              data-bs-toggle="pill"
                              data-bs-target="#residfilter"
                              type="button"
                              role="tab"
                              aria-controls="residfilter"
                              aria-selected="false"
                            >
                              {{ $t("residFilterCfgHeader") }}
                            </button>
                          </li>
                        </ul>
                      </div>
                      <div class="tab-content card-body" style="position: relative">
                        <div
                          class="tab-pane fade show active"
                          id="residfpfilter"
                          role="tabpanel"
                          aria-labelledby="residfpfilter-tab"
                        >
                          <div class="card">
                            <div class="card-header">
                              <ul class="nav nav-pills card-header-pills mb-2 right" role="tablist">
                                <li class="nav-item" role="presentation">
                                  <button
                                    class="nav-link active"
                                    id="residfpfilter6581-tab"
                                    data-bs-toggle="pill"
                                    data-bs-target="#residfpfilter6581"
                                    type="button"
                                    role="tab"
                                    aria-controls="residfpfilter6581"
                                    aria-selected="true"
                                  >
                                    {{ $t("residFpFilter6581CfgHeader") }}
                                  </button>
                                </li>
                                <li class="nav-item" role="presentation">
                                  <button
                                    class="nav-link"
                                    id="residfilterfp8580-tab"
                                    data-bs-toggle="pill"
                                    data-bs-target="#residfpfilter8580"
                                    type="button"
                                    role="tab"
                                    aria-controls="residfpfilter8580"
                                    aria-selected="false"
                                  >
                                    {{ $t("residFpFilter8580CfgHeader") }}
                                  </button>
                                </li>
                              </ul>
                            </div>
                            <div class="tab-content card-body" style="position: relative">
                              <div
                                class="tab-pane fade show active"
                                id="residfpfilter6581"
                                role="tabpanel"
                                aria-labelledby="residfpfilter6581-tab"
                              >
                                <div class="card">
                                  <div class="card-header">
                                    <ul class="nav nav-pills card-header-pills mb-2 right" role="tablist">
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link active"
                                          id="reSIDfpFiltername6581-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDfpFiltername6581"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDfpFiltername6581"
                                          aria-selected="true"
                                        >
                                          {{ $t("reSIDfpFilter6581Header") }}
                                        </button>
                                      </li>
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link"
                                          id="reSIDfpStereoFiltername6581-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDfpStereoFiltername6581"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDfpStereoFiltername6581"
                                          aria-selected="false"
                                        >
                                          {{ $t("reSIDfpStereoFilter6581Header") }}
                                        </button>
                                      </li>
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link"
                                          id="reSIDfpThirdSIDFiltername6581-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDfpThirdSIDFiltername6581"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDfpThirdSIDFiltername6581"
                                          aria-selected="false"
                                        >
                                          {{ $t("reSIDfpThirdSIDFilter6581Header") }}
                                        </button>
                                      </li>
                                    </ul>
                                  </div>
                                  <div class="tab-content card-body" style="position: relative">
                                    <div
                                      class="tab-pane fade show active"
                                      id="reSIDfpFiltername6581"
                                      role="tabpanel"
                                      aria-labelledby="reSIDfpFiltername6581-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDfpFilter6581">
                                            {{ $t("convertMessages.config.emulationSection.reSIDfpFilter6581") }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDfpFilter6581"
                                              v-model="convertOptions.config.emulationSection.reSIDfpFilter6581"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDfpFilters6581">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                    <div
                                      class="tab-pane fade show"
                                      id="reSIDfpStereoFiltername6581"
                                      role="tabpanel"
                                      aria-labelledby="reSIDfpStereoFiltername6581-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDfpStereoFilter6581">
                                            {{ $t("convertMessages.config.emulationSection.reSIDfpStereoFilter6581") }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDfpStereoFilter6581"
                                              v-model="convertOptions.config.emulationSection.reSIDfpStereoFilter6581"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDfpFilters6581">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                    <div
                                      class="tab-pane fade show"
                                      id="reSIDfpThirdSIDFiltername6581"
                                      role="tabpanel"
                                      aria-labelledby="reSIDfpThirdSIDFiltername6581-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDfpThirdSIDFilter6581">
                                            {{
                                              $t("convertMessages.config.emulationSection.reSIDfpThirdSIDFilter6581")
                                            }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDfpThirdSIDFilter6581"
                                              v-model="convertOptions.config.emulationSection.reSIDfpThirdSIDFilter6581"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDfpFilters6581">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <div
                                class="tab-pane fade show"
                                id="residfpfilter8580"
                                role="tabpanel"
                                aria-labelledby="residfpfilter8580-tab"
                              >
                                <div class="card">
                                  <div class="card-header">
                                    <ul class="nav nav-pills card-header-pills mb-2 right" role="tablist">
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link active"
                                          id="reSIDfpFiltername8580-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDfpFiltername8580"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDfpFiltername8580"
                                          aria-selected="true"
                                        >
                                          {{ $t("reSIDfpFilter8580Header") }}
                                        </button>
                                      </li>
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link"
                                          id="reSIDfpStereoFiltername8580-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDfpStereoFiltername8580"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDfpStereoFiltername8580"
                                          aria-selected="false"
                                        >
                                          {{ $t("reSIDfpStereoFilter8580Header") }}
                                        </button>
                                      </li>
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link"
                                          id="reSIDfpThirdSIDFiltername8580-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDfpThirdSIDFiltername8580"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDfpThirdSIDFiltername8580"
                                          aria-selected="false"
                                        >
                                          {{ $t("reSIDfpThirdSIDFilter8580Header") }}
                                        </button>
                                      </li>
                                    </ul>
                                  </div>
                                  <div class="tab-content card-body" style="position: relative">
                                    <div
                                      class="tab-pane fade show active"
                                      id="reSIDfpFiltername8580"
                                      role="tabpanel"
                                      aria-labelledby="reSIDfpFiltername8580-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDfpFilter8580">
                                            {{ $t("convertMessages.config.emulationSection.reSIDfpFilter8580") }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDfpFilter8580"
                                              v-model="convertOptions.config.emulationSection.reSIDfpFilter8580"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDfpFilters8580">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                    <div
                                      class="tab-pane fade show"
                                      id="reSIDfpStereoFiltername8580"
                                      role="tabpanel"
                                      aria-labelledby="reSIDfpStereoFiltername8580-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDfpStereoFilter8580">
                                            {{ $t("convertMessages.config.emulationSection.reSIDfpStereoFilter8580") }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDfpStereoFilter8580"
                                              v-model="convertOptions.config.emulationSection.reSIDfpStereoFilter8580"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDfpFilters8580">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                    <div
                                      class="tab-pane fade show"
                                      id="reSIDfpThirdSIDFiltername8580"
                                      role="tabpanel"
                                      aria-labelledby="reSIDfpThirdSIDFiltername8580-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDfpThirdSIDFilter8580">
                                            {{
                                              $t("convertMessages.config.emulationSection.reSIDfpThirdSIDFilter8580")
                                            }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDfpThirdSIDFilter8580"
                                              v-model="convertOptions.config.emulationSection.reSIDfpThirdSIDFilter8580"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDfpFilters8580">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                        <div
                          class="tab-pane fade show"
                          id="residfilter"
                          role="tabpanel"
                          aria-labelledby="residfilter-tab"
                        >
                          <div class="card">
                            <div class="card-header">
                              <ul class="nav nav-pills card-header-pills mb-2 right" role="tablist">
                                <li class="nav-item" role="presentation">
                                  <button
                                    class="nav-link active"
                                    id="residfilter6581-tab"
                                    data-bs-toggle="pill"
                                    data-bs-target="#residfilter6581"
                                    type="button"
                                    role="tab"
                                    aria-controls="residfilter6581"
                                    aria-selected="true"
                                  >
                                    {{ $t("residFilter6581CfgHeader") }}
                                  </button>
                                </li>
                                <li class="nav-item" role="presentation">
                                  <button
                                    class="nav-link"
                                    id="residfilter8580-tab"
                                    data-bs-toggle="pill"
                                    data-bs-target="#residfilter8580"
                                    type="button"
                                    role="tab"
                                    aria-controls="residfilter8580"
                                    aria-selected="false"
                                  >
                                    {{ $t("residFilter8580CfgHeader") }}
                                  </button>
                                </li>
                              </ul>
                            </div>
                            <div class="tab-content card-body" style="position: relative">
                              <div
                                class="tab-pane fade show active"
                                id="residfilter6581"
                                role="tabpanel"
                                aria-labelledby="residfilter6581-tab"
                              >
                                <div class="card">
                                  <div class="card-header">
                                    <ul class="nav nav-pills card-header-pills mb-2 right" role="tablist">
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link active"
                                          id="reSIDFiltername6581-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDFiltername6581"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDFiltername6581"
                                          aria-selected="true"
                                        >
                                          {{ $t("reSIDFilter6581Header") }}
                                        </button>
                                      </li>
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link"
                                          id="reSIDStereoFiltername6581-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDStereoFiltername6581"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDStereoFiltername6581"
                                          aria-selected="false"
                                        >
                                          {{ $t("reSIDStereoFilter6581Header") }}
                                        </button>
                                      </li>
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link"
                                          id="reSIDThirdSIDFiltername6581-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDThirdSIDFiltername6581"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDThirdSIDFiltername6581"
                                          aria-selected="false"
                                        >
                                          {{ $t("reSIDThirdSIDFilter6581Header") }}
                                        </button>
                                      </li>
                                    </ul>
                                  </div>
                                  <div class="tab-content card-body" style="position: relative">
                                    <div
                                      class="tab-pane fade show active"
                                      id="reSIDFiltername6581"
                                      role="tabpanel"
                                      aria-labelledby="reSIDFiltername6581-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDFilter6581">
                                            {{ $t("convertMessages.config.emulationSection.filter6581") }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDFilter6581"
                                              v-model="convertOptions.config.emulationSection.filter6581"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDFilters6581">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                    <div
                                      class="tab-pane fade show"
                                      id="reSIDStereoFiltername6581"
                                      role="tabpanel"
                                      aria-labelledby="reSIDStereoFiltername6581-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDStereoFilter6581">
                                            {{ $t("convertMessages.config.emulationSection.stereoFilter6581") }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDStereoFilter6581"
                                              v-model="convertOptions.config.emulationSection.stereoFilter6581"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDFilters6581">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                    <div
                                      class="tab-pane fade show"
                                      id="reSIDThirdSIDFiltername6581"
                                      role="tabpanel"
                                      aria-labelledby="reSIDThirdSIDFiltername6581-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDThirdSIDFilter6581">
                                            {{ $t("convertMessages.config.emulationSection.thirdSIDFilter6581") }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDThirdSIDFilter6581"
                                              v-model="convertOptions.config.emulationSection.thirdSIDFilter6581"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDFilters6581">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <div
                                class="tab-pane fade show"
                                id="residfilter8580"
                                role="tabpanel"
                                aria-labelledby="residfilter8580-tab"
                              >
                                <div class="card">
                                  <div class="card-header">
                                    <ul class="nav nav-pills card-header-pills mb-2 right" role="tablist">
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link active"
                                          id="reSIDFiltername8580-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDFiltername8580"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDFiltername8580"
                                          aria-selected="true"
                                        >
                                          {{ $t("reSIDFilter8580Header") }}
                                        </button>
                                      </li>
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link"
                                          id="reSIDStereoFiltername8580-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDStereoFiltername8580"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDStereoFiltername8580"
                                          aria-selected="false"
                                        >
                                          {{ $t("reSIDStereoFilter8580Header") }}
                                        </button>
                                      </li>
                                      <li class="nav-item" role="presentation">
                                        <button
                                          class="nav-link"
                                          id="reSIDThirdSIDFiltername8580-tab"
                                          data-bs-toggle="pill"
                                          data-bs-target="#reSIDThirdSIDFiltername8580"
                                          type="button"
                                          role="tab"
                                          aria-controls="reSIDThirdSIDFiltername8580"
                                          aria-selected="false"
                                        >
                                          {{ $t("reSIDThirdSIDFilter8580Header") }}
                                        </button>
                                      </li>
                                    </ul>
                                  </div>
                                  <div class="tab-content card-body" style="position: relative">
                                    <div
                                      class="tab-pane fade show active"
                                      id="reSIDFiltername8580"
                                      role="tabpanel"
                                      aria-labelledby="reSIDFiltername8580-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDFilter8580">
                                            {{ $t("convertMessages.config.emulationSection.filter8580") }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDFilter8580"
                                              v-model="convertOptions.config.emulationSection.filter8580"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDFilters8580">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                    <div
                                      class="tab-pane fade show"
                                      id="reSIDStereoFiltername8580"
                                      role="tabpanel"
                                      aria-labelledby="reSIDStereoFiltername8580-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDStereoFilter8580">
                                            {{ $t("convertMessages.config.emulationSection.stereoFilter8580") }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDStereoFilter8580"
                                              v-model="convertOptions.config.emulationSection.stereoFilter8580"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDFilters8580">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                    <div
                                      class="tab-pane fade show"
                                      id="reSIDThirdSIDFiltername8580"
                                      role="tabpanel"
                                      aria-labelledby="reSIDThirdSIDFiltername8580-tab"
                                    >
                                      <div class="settings-box">
                                        <span class="setting"
                                          ><label for="reSIDThirdSIDFilter8580">
                                            {{ $t("convertMessages.config.emulationSection.thirdSIDFilter8580") }}
                                            <select
                                              class="form-select form-select-sm right"
                                              id="reSIDThirdSIDFilter8580"
                                              v-model="convertOptions.config.emulationSection.thirdSIDFilter8580"
                                              size="3"
                                            >
                                              <option v-for="filter in reSIDFilters8580">{{ filter }}</option>
                                            </select></label
                                          ></span
                                        >
                                      </div>
                                    </div>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="tab-pane fade show" id="mute" role="tabpanel" aria-labelledby="mute-tab">
                    <div class="card">
                      <div class="card-header">
                        <ul class="nav nav-pills card-header-pills mb-2 right" role="tablist">
                          <li class="nav-item" role="presentation">
                            <button
                              class="nav-link active"
                              id="muteSid-tab"
                              data-bs-toggle="pill"
                              data-bs-target="#muteSid"
                              type="button"
                              role="tab"
                              aria-controls="muteSid"
                              aria-selected="true"
                            >
                              {{ $t("muteSidHeader") }}
                            </button>
                          </li>
                          <li class="nav-item" role="presentation">
                            <button
                              class="nav-link"
                              id="muteStereoSid-tab"
                              data-bs-toggle="pill"
                              data-bs-target="#muteStereoSid"
                              type="button"
                              role="tab"
                              aria-controls="muteStereoSid"
                              aria-selected="false"
                            >
                              {{ $t("muteStereoSidHeader") }}
                            </button>
                          </li>
                          <li class="nav-item" role="presentation">
                            <button
                              class="nav-link"
                              id="muteThirdSID-tab"
                              data-bs-toggle="pill"
                              data-bs-target="#muteThirdSID"
                              type="button"
                              role="tab"
                              aria-controls="muteThirdSID"
                              aria-selected="false"
                            >
                              {{ $t("muteThirdSidHeader") }}
                            </button>
                          </li>
                        </ul>
                      </div>
                      <div class="tab-content card-body" style="position: relative">
                        <div
                          class="tab-pane fade show active"
                          id="muteSid"
                          role="tabpanel"
                          aria-labelledby="muteSid-tab"
                        >
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteVoice1">
                                  {{ $t("convertMessages.config.emulationSection.muteVoice1") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteVoice1"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteVoice1"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteVoice2">
                                  {{ $t("convertMessages.config.emulationSection.muteVoice2") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteVoice2"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteVoice2"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteVoice3">
                                  {{ $t("convertMessages.config.emulationSection.muteVoice3") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteVoice3"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteVoice3"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteVoice4">
                                  {{ $t("convertMessages.config.emulationSection.muteVoice4") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteVoice4"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteVoice4"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                        </div>
                        <div
                          class="tab-pane fade show"
                          id="muteStereoSid"
                          role="tabpanel"
                          aria-labelledby="muteStereoSid-tab"
                        >
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteStereoVoice1">
                                  {{ $t("convertMessages.config.emulationSection.muteStereoVoice1") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteStereoVoice1"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteStereoVoice1"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteStereoVoice2">
                                  {{ $t("convertMessages.config.emulationSection.muteStereoVoice2") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteStereoVoice2"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteStereoVoice2"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteStereoVoice3">
                                  {{ $t("convertMessages.config.emulationSection.muteStereoVoice3") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteStereoVoice3"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteStereoVoice3"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteStereoVoice4">
                                  {{ $t("convertMessages.config.emulationSection.muteStereoVoice4") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteStereoVoice4"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteStereoVoice4"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                        </div>
                        <div
                          class="tab-pane fade show"
                          id="muteThirdSID"
                          role="tabpanel"
                          aria-labelledby="muteThirdSID-tab"
                        >
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteThirdSIDVoice1">
                                  {{ $t("convertMessages.config.emulationSection.muteThirdSIDVoice1") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteThirdSIDVoice1"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteThirdSIDVoice1"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteThirdSIDVoice2">
                                  {{ $t("convertMessages.config.emulationSection.muteThirdSIDVoice2") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteThirdSIDVoice2"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteThirdSIDVoice2"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteThirdSIDVoice3">
                                  {{ $t("convertMessages.config.emulationSection.muteThirdSIDVoice3") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteThirdSIDVoice3"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteThirdSIDVoice3"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                          <div class="settings-box">
                            <span class="setting">
                              <div class="form-check">
                                <label class="form-check-label" for="muteThirdSIDVoice4">
                                  {{ $t("convertMessages.config.emulationSection.muteThirdSIDVoice4") }}
                                  <input
                                    class="form-check-input"
                                    type="checkbox"
                                    id="muteThirdSIDVoice4"
                                    style="float: right; margin-left: 8px"
                                    v-model="convertOptions.config.emulationSection.muteThirdSIDVoice4"
                                  />
                                </label>
                              </div>
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="tab-pane fade show" id="cart" role="tabpanel" aria-labelledby="cart-tab">
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="jiffyDosInstalled">
                            {{ $t("convertMessages.config.c1541Section.jiffyDosInstalled") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="jiffyDosInstalled"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.config.c1541Section.jiffyDosInstalled"
                            />
                          </label>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <b class="right"> {{ $t("CART_NOTES") }} </b>
                      </span>
                    </div>

                    <div class="settings-box">
                      <span class="setting">
                        <label class="form-check-label right" for="reuSizeAuto">
                          {{ $t("convertMessages.reuSize") }}
                        </label>
                        <div class="input-group" style="justify-content: flex-end">
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="null"
                              id="reuSizeAuto"
                              v-model="convertOptions.reuSize"
                            />
                            <label class="form-check-label" for="reuSizeAuto"> Auto </label>
                          </div>

                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="128"
                              id="reuSize128"
                              v-model="convertOptions.reuSize"
                            />
                            <label class="form-check-label" for="reuSize128"> REU 1700 (128KB) </label>
                          </div>

                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="512"
                              id="reuSize512"
                              v-model="convertOptions.reuSize"
                            />
                            <label class="form-check-label" for="reuSize512"> REU 1750 (512KB) </label>
                          </div>

                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="256"
                              id="reuSize256"
                              v-model="convertOptions.reuSize"
                            />
                            <label class="form-check-label" for="reuSize256"> REU 1764 (256KB) </label>
                          </div>

                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="2048"
                              id="reuSize2048"
                              v-model="convertOptions.reuSize"
                            />
                            <label class="form-check-label" for="reuSize2048"> REU 1750 XL (2MB) </label>
                          </div>

                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="16384"
                              id="reuSize16384"
                              v-model="convertOptions.reuSize"
                            />
                            <label class="form-check-label" for="reuSize16384"> REU (16MB) </label>
                          </div>
                        </div>
                      </span>
                    </div>
                    <div class="settings-box">
                      <span class="setting">
                        <div class="form-check">
                          <label class="form-check-label" for="sfxSoundExpander">
                            {{ $t("convertMessages.sfxSoundExpander") }}
                            <input
                              class="form-check-input"
                              type="checkbox"
                              id="sfxSoundExpander"
                              style="float: right; margin-left: 8px"
                              v-model="convertOptions.sfxSoundExpander"
                            />
                          </label>
                        </div>
                      </span>
                    </div>

                    <div class="settings-box">
                      <span class="setting">
                        <label class="form-check-label" for="sfxSoundExpanderType0">
                          {{ $t("convertMessages.sfxSoundExpanderType") }}
                        </label>
                        <div class="input-group" style="justify-content: flex-end">
                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="0"
                              id="sfxSoundExpanderType0"
                              v-model="convertOptions.sfxSoundExpanderType"
                            />
                            <label class="form-check-label" for="sfxSoundExpanderType0"> OPL1 (YM3526) </label>
                          </div>

                          <div class="form-check">
                            <input
                              class="form-check-input"
                              type="radio"
                              value="1"
                              id="sfxSoundExpanderType1"
                              v-model="convertOptions.sfxSoundExpanderType"
                            />
                            <label class="form-check-label" for="sfxSoundExpanderType1"> OPL2 (YM3812) </label>
                          </div>
                        </div>
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="tab-pane fade show" id="logs" role="tabpanel" aria-labelledby="logs-tab">
              <div class="settings-box">
                <span class="setting">
                  <label for="order">
                    <select
                      class="form-select form-select-sm right"
                      id="order"
                      v-model="order"
                      @change="
                        fetchLogs();
                        countLogs();
                      "
                    >
                      <option value="ASC">{{ $t("ASC") }}</option>
                      <option value="DESC">{{ $t("DESC") }}</option>
                    </select>
                  </label>
                  <input
                    type="date"
                    class="form-control right"
                    v-model="Logs.date"
                    :locale="$i18n.locale"
                    @input="
                      fetchLogs();
                      countLogs();
                    "
                  />

                  <input
                    type="time"
                    class="form-control right"
                    v-model="Logs.time"
                    @input="
                      fetchLogs();
                      countLogs();
                    "
                  />
                </span>
              </div>

              <div class="settings-box">
                <span class="setting">
                  <div class="form-check">
                    <label class="form-check-label" for="tooMuchLogging">
                      {{ $t("tooMuchLogging") }}
                      <input
                        class="form-check-input"
                        type="checkbox"
                        id="tooMuchLogging"
                        style="float: right; margin-left: 8px"
                        v-model="tooMuchLogging"
                        v-on:change="
                          fetchLogs();
                          countLogs();
                        "
                      />
                    </label>
                  </div>
                </span>
              </div>

              <button
                type="button"
                class="btn btn-success btn-sm mb-2"
                v-on:click="
                  fetchLogs();
                  countLogs();
                "
                style="float: left"
              >
                <span>Request LOGS</span>
              </button>
              <div class="settings-box">
                <span class="setting">
                  <label for="maxResults"
                    >{{ $t("maxResults") }}
                    <input
                      class="form-control right"
                      type="number"
                      id="maxResults"
                      class="form-control"
                      min="0"
                      max="10000"
                      v-model.number="maxResults"
                      @change="
                        fetchLogs();
                        countLogs();
                      "
                    />
                  </label>
                </span>
              </div>
              <div class="settings-box">
                <span class="setting">
                  <span class="right" style="font-size: small; font-style: italic; color: #6c757d"
                    >{{ logsCount.toLocaleString() }} {{ $t("results") }}</span
                  >
                </span>
              </div>

              <table class="table table-sm table-striped table-bordered w-auto">
                <thead>
                  <tr>
                    <th style="vertical-align: top; width: 15%">
                      <div>
                        <i class="bi bi-calendar2-date"></i>
                      </div>
                      <div class="d-none d-sm-block" style="margin-left: 0px; word-wrap: normal">
                        {{ $t("Logs.instant") }}
                      </div>
                    </th>
                    <th>
                      <div>
                        <i class="bi bi-at"></i>
                      </div>
                      <label
                        for="sourceClassName"
                        class="d-none d-sm-block"
                        style="margin-left: 0px; word-wrap: normal"
                        >{{ $t("Logs.sourceClassName") }}</label
                      >
                      <input
                        class="form-control"
                        type="text"
                        id="sourceClassName"
                        v-model="Logs.sourceClassName"
                        @change="
                          fetchLogs();
                          countLogs();
                        "
                        style="max-width: 100%; padding: 0.175em 0em"
                        autocomplete="off"
                        autocorrect="off"
                        autocapitalize="off"
                        spellcheck="false"
                      />
                    </th>
                    <th>
                      <div>
                        <i class="bi bi-hash"></i>
                      </div>
                      <label
                        for="sourceMethodName"
                        class="d-none d-sm-block"
                        style="margin-left: 0px; word-wrap: normal"
                        >{{ $t("Logs.sourceMethodName") }}</label
                      >
                      <input
                        class="form-control"
                        type="text"
                        id="sourceMethodName"
                        v-model="Logs.sourceMethodName"
                        @change="
                          fetchLogs();
                          countLogs();
                        "
                        style="max-width: 100%; padding: 0.175em 0em"
                        autocomplete="off"
                        autocorrect="off"
                        autocapitalize="off"
                        spellcheck="false"
                      />
                    </th>
                    <th>
                      <div>
                        <i class="bi bi-info-lg"></i>
                      </div>
                      <label for="level" class="d-none d-sm-block" style="margin-left: 0px; word-wrap: normal">{{
                        $t("Logs.level")
                      }}</label>
                      <input
                        class="form-control"
                        type="text"
                        id="level"
                        v-model="Logs.level"
                        @change="
                          fetchLogs();
                          countLogs();
                        "
                        style="max-width: 100%; padding: 0.175em 0em"
                        autocomplete="off"
                        autocorrect="off"
                        autocapitalize="off"
                        spellcheck="false"
                      />
                    </th>
                    <th>
                      <div>
                        <i class="bi bi-chat-text"></i>
                      </div>
                      <label for="message" class="d-none d-sm-block" style="margin-left: 0px; word-wrap: normal">{{
                        $t("Logs.message")
                      }}</label>
                      <input
                        class="form-control"
                        type="text"
                        id="message"
                        v-model="Logs.message"
                        @change="
                          fetchLogs();
                          countLogs();
                        "
                        style="max-width: 100%; padding: 0.175em 0em"
                        autocomplete="off"
                        autocorrect="off"
                        autocapitalize="off"
                        spellcheck="false"
                      />
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in logs">
                    <td style="vertical-align: top; width: 15%; word-break: break-all">
                      <span> {{ toDateTime(row.instant) }} </span>
                    </td>
                    <td style="word-break: break-all">
                      <span> {{ row.sourceClassName }} </span>
                    </td>
                    <td style="word-break: break-all">
                      <span> {{ row.sourceMethodName }} </span>
                    </td>
                    <td style="word-break: break-all">
                      <span> {{ row.level }} </span>
                    </td>
                    <td style="width: 50%; word-break: break-all">
                      <span> {{ row.message }} </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </form>
    </div>

    <script>
      const { createApp, ref } = Vue;

      const { createI18n } = VueI18n;

      var gumStream; //stream from getUserMedia()
      var recorder; //WebAudioRecorder object

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
        // untested characters: !*=,
        // tested characters: /~@#$&():;+''?
        return encodeURI(entry)
          .replace(/\+/g, "%2B")
          .replace(/#/g, "%23")
          .replace(/&/g, "%26")
          .replace(/\?/g, "%3F")
          .replace(/;/g, "%3B");
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
      function messageListener(event) {
        if (event.origin !== "${baseUrl}") {
          console.log("Ignored: Got a message from bad origin: " + event.origin);
        } else {
          var myMsg = event.data;
          if (myMsg == "1") {
            var iframe = document.getElementById("c64");
            iframe.setAttribute("data-isloaded", "1");
          }
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
      function closeiframe() {
        var iframe = document.getElementById("c64");
        if (iframe) {
          document.getElementById("app").removeChild(iframe);
          document.getElementById("main").classList.remove("hide");
        }
      }

      var i18n = createI18n({
        legacy: false, // You might need to set this option to true if you're using Vue 2 syntax
        locale: "en", // default locale
        messages: {
          en: {
            LOGS: "Logs",
            ABOUT: "About",
            SIDS: "Directory",
            ASSEMBLY64: "Search",
            SID: "SID",
            STIL: "Info",
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
            HARDWARE_PREPARATION_1: "On MacOSX it works out-of-the-box.",
            HARDWARE_PREPARATION_2: "On a Windows-PC WinUSB is required, download from",
            HARDWARE_PREPARATION_3: "On Linux proper permission is required",
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
            theaterMode: "Random Play",
            currentlyPlaying: "Playing: ",
            parentDirectoryHint: "Go up one Level",
            sidInfoKey: "Name",
            sidInfoValue: "Value",
            HVSCEntry: {
              path: "Full Path",
              name: "File Name",
              title: "Title",
              author: "Author",
              released: "Release",
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
            Search: {
              category: "Content",
              name: "Name",
              event: "Event",
              release: "Release",
              handle: "Handle",
              rating: "Rating",
            },
            Logs: {
              instant: "Date Time",
              sourceClassName: "Class Name",
              sourceMethodName: "Method Name",
              level: "Level",
              message: "Message",
            },
            results: "results",
            username: "Username",
            password: "Password",
            maxResults: "Max. Results",
            tooMuchLogging: "Show LOG messages normally filtered",
            ASC: "As from",
            DESC: "Until",
            filter: "Top",
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
            importExport: "Import/Export",
            startImport: "Import",
            fetchFavorites: "Examples:",
            removePlaylist: "Remove All",
            confirmationTitle: "Confirmation Dialogue",
            removePlaylistReally: "Do you really want to remove ALL playlist entries?",
            browse: "Import...",
            exportPlaylist: "Export",
            importPlaceholder: "",
            importDropPlaceholder: "Drop here...",
            searchPlaceholder: "Quick filter",
            random: "Random Playback",
            mobileProfile: "Mobile profile",
            wifiProfile: "WiFi profile",
            stereoMode: "Stereo Mode",
            streamingCfgHeader: "Streaming",
            audioStreamingCfgHeader: "Audio",
            textToSpeechTypeOff: "Off",
            textToSpeechLocaleAuto: "Auto",
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
            reSIDFilter6581Header: "SID",
            reSIDStereoFilter6581Header: "Stereo SID",
            reSIDThirdSIDFilter6581Header: "3rd SID",
            reSIDFilter8580Header: "SID",
            reSIDStereoFilter8580Header: "Stereo SID",
            reSIDThirdSIDFilter8580Header: "3rd SID",
            mutingCfgHeader: "Muting",
            muteSidHeader: "SID",
            muteStereoSidHeader: "Stereo SID",
            muteThirdSidHeader: "3rd SID",
            floppyCartCfgHeader: "Floppy/Cart",
            firstSid: "Main SID",
            secondSid: "Stereo SID",
            thirdSid: "3-SID",
            setDefault: "Restore Defaults",
            setDefaultUser: "Restore Default User",
            setDefaultReally: "Do you really want to restore defaults?",
            setDefaultUserReally: "Do you really want to restore default user?",
            firstCategory: "",
            pleaseWait: "Please wait...",

            convertMessages: ${convertMessagesEn},
          },
          de: {
            LOGS: "Logs",
            ABOUT: "\u00dcber",
            SIDS: "Verzeichnis",
            ASSEMBLY64: "Suche",
            SID: "SID",
            STIL: "Info",
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
            HARDWARE_PREPARATION_1: "Auf MacOSX funktioniert es einfach so.",
            HARDWARE_PREPARATION_2: "Auf einem Windows-PC ist WinUSB erforderlich",
            HARDWARE_PREPARATION_3: "Auf Linux sind Berechtigungen erforderlich",
            HERE: "download",
            HARDSID: "HardSID 4U, HardSID UPlay and HardSID Uno",
            EXSID: "ExSID, ExSID+",
            SIDBLASTER: "SIDBlaster",
            CONNECT: "Verbinden...",
            USE_MOBILE_DEVICES_1: "Um Mobilger\u00e4te zu verwenden, verwenden Sie bitte einen",
            USE_MOBILE_DEVICES_2: "USBC nach USB adapter",
            STREAMING_NOTES:
              "Diese Funktion macht von intensivem Streaming der SID-Register Schreibbefehle vom Server zum Browser gebrauch! Bitte stellen Sie sicher, dass sie mit einem freien WLAN verbunden sind. Ich \u00fcbernehme keine Verantwortung f\u00fcr jegliche Kosten, die f\u00fcr das Streaming \u00fcber das Internet entstehen k\u00f6nnten!",
            CART_NOTES: "Wichtig: Es es kann nur eine Cartridge zur selben Zeit eingesteckt sein!",
            theaterMode: "Zufalls-Mix",
            currentlyPlaying: "Es spielt: ",
            parentDirectoryHint: "Gehe eine Ebene h\u00f6her",
            sidInfoKey: "Name",
            sidInfoValue: "Wert",
            HVSCEntry: {
              path: "Dateipfad",
              name: "Dateiname",
              title: "Titel",
              author: "Autor",
              released: "Release",
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
            Search: {
              category: "Inhalt",
              name: "Name",
              event: "Event",
              release: "Release",
              handle: "Handle",
              rating: "Wertung",
            },
            Logs: {
              instant: "Datum Uhrzeit",
              sourceClassName: "Klasse",
              sourceMethodName: "Methode",
              level: "Level",
              message: "Meldung",
            },
            results: "Ergebnisse",
            username: "Benutzername",
            password: "Passwort",
            maxResults: "Max. Ergebnisse",
            tooMuchLogging: "Zeige auch Log Nachrichten, die normalerweise gefiltert werden",
            ASC: "Ab dem",
            DESC: "Bis zum",
            filter: "Top",
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
            importExport: "Import/Export",
            startImport: "Importieren",
            fetchFavorites: "Beispiele:",
            removePlaylist: "L\u00f6schen",
            confirmationTitle: "Sicherheitsabfrage",
            removePlaylistReally: "Wollen sie wirklich ALL Favoriten l\u00f6schen?",
            browse: "Importieren...",
            exportPlaylist: "Exportieren",
            importPlaceholder: "",
            importDropPlaceholder: "DnD hier...",
            searchPlaceholder: "Schnellfilter",
            random: "Zuf\u00e4llige Wiedergabe",
            mobileProfile: "Mobiles Profil",
            wifiProfile: "WiFi Profil",
            stereoMode: "Stereo Mode",
            streamingCfgHeader: "Streaming",
            audioStreamingCfgHeader: "Audio",
            textToSpeechTypeOff: "Aus",
            textToSpeechLocaleAuto: "Auto",
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
            reSIDFilter6581Header: "SID",
            reSIDStereoFilter6581Header: "Stereo SID",
            reSIDThirdSIDFilter6581Header: "3. SID",
            reSIDFilter8580Header: "SID",
            reSIDStereoFilter8580Header: "Stereo SID",
            reSIDThirdSIDFilter8580Header: "3. SID",
            mutingCfgHeader: "Stummschalten",
            muteSidHeader: "SID",
            muteStereoSidHeader: "Stereo SID",
            muteThirdSidHeader: "3. SID",
            floppyCartCfgHeader: "Floppy/Cart",
            firstSid: "Haupt SID",
            secondSid: "Stereo SID",
            thirdSid: "3-SID",
            setDefault: "Standardeinstellungen wiederherstellen",
            setDefaultUser: "Standardbenutzer wiederherstellen",
            setDefaultReally: "Wollen sie wirklich die Standardeinstellungen wiederherstellen?",
            setDefaultUserReally: "Wollen sie wirklich den Standardbenutzer wiederherstellen?",
            firstCategory: "",
            pleaseWait: "Bitte warten...",
            DateLabels: {
              labelPrevDecade: "Vorheriges Jahrzehnt",
              labelPrevYear: "Vorheriges Jahr",
              labelPrevMonth: "Vorheriger Monat",
              labelCurrentMonth: "Aktueller Monat",
              labelNextMonth: "Nächster Monat",
              labelNextYear: "Nächstes Jahr",
              labelNextDecade: "Nächstes Jahrzehnt",
              labelToday: "Heute",
              labelSelected: "Ausgewähltes Datum",
              labelNoDateSelected: "Kein Datum gewählt",
              labelCalendar: "Kalender",
              labelNav: "Kalendernavigation",
              labelHelp: "Mit den Pfeiltasten durch den Kalender navigieren",
              labelResetButton: "Zurücksetzen",
            },
            TimeLabels: {
              labelHours: "Stunden",
              labelMinutes: "Minuten",
              labelSeconds: "Sekunden",
              labelIncrement: "Erhöhen",
              labelDecrement: "Verringern",
              labelSelected: "Ausgewählte Zeit",
              labelNoTimeSelected: "Keine Zeit ausgewählt",
              labelNowButton: "Aktuelle Zeit",
              labelResetButton: "Zurücksetzen",
              labelCloseButton: "Schließen",
            },

            convertMessages: ${convertMessagesDe},
          },
        },
      });

      // Define the 'fromJava' function (mapped to the Java method)
      function fromJava(message) {
        console.log("Received message from Java:", message);
      }

      TeaVM.wasm
        .load("/static/teavm/wasm/jsidplay2.wasm", {
          installImports(o, controller) {
            o.sidplay2section = {
              getPalEmulation: () => {},
            };
            o.audiosection = {
              getBufferSize: () => {},
              getAudioBufferSize: () => {},
              getSamplingRate: () => {},
              getSamplingMethodResample: () => {},
              getReverbBypass: () => {},
            };
            o.emulationsection = {
              getDefaultClockSpeed: () => {},
              getDefaultEmulationReSid: () => {},
              getDefaultSidModel8580: () => {},
            };
            o.audiodriver = {
              processSamples: () => {},
              processPixels: () => {},
              processSidWrite: () => {},
            };
            o.c1541section = {
              isJiffyDosInstalled: () => {},
            };
          },
        })
        .then((teavm) => {
          window.instance = teavm.instance;
        });

      let app = Vue.createApp({
        data: function () {
          return {
            modalIndex: 0,
            modalFavoriteIndex: 0,
            msg: "",
            timeoutId: undefined,
            theaterMode: false,
            carouselImageHeight:
              window.innerHeight > window.innerWidth ? window.innerHeight * 0.3 : window.innerHeight * 0.8,
            showAudio: false,
            showHardwarePlayer: false,
            langs: ["de", "en"],
            // ABOUT
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
            maxResults: 100,
            tooMuchLogging: false,
            order: "ASC",
            logs: [],
            logsCount: 0,
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
              },
              {
                key: "name",
              },
              {
                key: "event",
              },
              {
                key: "released",
              },
              {
                key: "handle",
              },
              {
                key: "rating",
              },
              { key: "actions" },
            ],
            name: "",
            event: "",
            released: "",
            rating: "",
            handle: "",
            Logs: {
              date: "",
              time: "",
              sourceClassName: "",
              sourceMethodName: "",
              level: "",
              message: "",
            },
            // PL (Playlist)
            importFile: null,
            importExportVisible: false,
            playlist: [],
            playlistIndex: 0,
            favoritesNames: [],
            random: true,
            filterText: "",
            filterText2: "",
            // CFG (configuration)
            // pre-fetched filter definitions
            reSIDFilters6581: [],
            reSIDFilters8580: [],
            reSIDfpFilters6581: [],
            reSIDfpFilters8580: [],
            cbrs: [-1, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320],
            vbrQualities: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
            // Misc.
            tabIndex: 0,
            loadingSid: false,
            loadingStil: false,
            loadingLogs: false,
            loadingAssembly64: false,
            loadingPl: false,
            loadingCfg: false,
            convertOptions: ${convertOptions},
            defaultConvertOptions: ${convertOptions},
          };
        },
        computed: {
          instant: {
            get: function () {
              try {
                var date;
                if (this.Logs.date && this.Logs.time) {
                  date = new Date(this.Logs.date + "T" + this.Logs.time);
                } else if (this.Logs.date) {
                  date = new Date(this.Logs.date + "T00:00:00");
                } else if (this.Logs.time) {
                  date = new Date("1070-01-01T" + this.Logs.time);
                }
                return date.getTime();
              } catch (e) {
                return 0;
              }
            },
          },
          stereoMode: {
            set: function (val) {
              if (val === "FORCE_3SID") {
                this.convertOptions.config.emulationSection.forceStereoTune = true;
                this.convertOptions.config.emulationSection.force3SIDTune = true;
              } else if (val === "FORCE_2SID") {
                this.convertOptions.config.emulationSection.forceStereoTune = true;
                this.convertOptions.config.emulationSection.force3SIDTune = false;
              } else {
                this.convertOptions.config.emulationSection.forceStereoTune = false;
                this.convertOptions.config.emulationSection.force3SIDTune = false;
              }
            },
            get: function () {
              if (this.convertOptions.config.emulationSection.force3SIDTune) {
                return "FORCE_3SID";
              } else if (this.convertOptions.config.emulationSection.forceStereoTune) {
                return "FORCE_2SID";
              } else {
                return "AUTO";
              }
            },
          },
          translatedInfos: function () {
            if (!this.infos) {
              return [];
            }
            let outer = this;
            return this.infos.map(function (obj) {
              return {
                Name: outer.$t(obj.Name),
                Value: obj.Value,
                opacity: obj.Name == "HVSCEntry.path",
              };
            });
          },
        },
        methods: {
          toDateTime: function (millis) {
            return new Date(millis * 1000).toLocaleString(this.$i18n.locale);
          },
          startRecording: function () {
            let outer = this;
            navigator.mediaDevices
              .getUserMedia({
                audio: true,
                video: false,
              })
              .then(function (stream) {
                var AudioContext = window.AudioContext || window.webkitAudioContext;
                var audioContext = new AudioContext();

                //assign to gumStream for later use
                gumStream = stream;
                /* use the stream */
                var input = audioContext.createMediaStreamSource(stream);
                //stop the input from playing back through the speakers
                input.connect(audioContext.destination);

                recorder = new WebAudioRecorder(input, {
                  workerDir: "../webjars/web-audio-recorder-js/0.0.2/${lib}/",
                  encoding: "wav",
                });
                recorder.onComplete = function (recorder, blob) {
                  axios({
                    method: "post",
                    url: "/jsidplay2service/JSIDPlay2REST/speech2text",
                    data: blob,
                    auth: {
                      username: outer.username,
                      password: outer.password,
                    },
                  }).then((response) => {
                    let result = response.data;
                    console.log(result);
                    outer.msg = result.text;
                  });
                };
                recorder.setOptions({
                  timeLimit: 10,
                  encodeAfterRecord: true,
                });
                recorder.startRecording();
              })
              .catch(function (err) {
                console.log(err);
              });
          },
          stopRecording: function () {
            console.log("stopRecording() called");
            //stop microphone access
            gumStream.getAudioTracks()[0].stop();
            //tell the recorder to finish the recording (stop recording + encode the recorded audio)
            recorder.finishRecording();
          },
          openiframe: function (url) {
            this.pause();
            closeiframe();
            document.getElementById("main").classList.add("hide");

            var iframe = document.createElement("iframe");
            iframe.setAttribute("id", "c64");
            iframe.setAttribute("name", Date.now());
            iframe.setAttribute("data-isloaded", "0");
            iframe.classList.add("iframe_c64");
            iframe.onload = function () {
              window.scrollTo(0, 0);
              iframe.onload = function () {
                var isLoaded = iframe.getAttribute("data-isloaded");
                if (isLoaded != "1") {
                  setTimeout(() => closeiframe(), 5000);
                }
              };
              iframe.src = url;
            };
            iframe.src =
              "data:text/html;charset=utf-8,<!DOCTYPE html> <html> <head><link type='text/css' rel='Stylesheet' href='${baseUrl}/static/please_wait.css' /></head><body><div class='loading'><p>" +
              this.$t("pleaseWait") +
              "</p><span><i></i><i></i></span></div></body>";

            document.getElementById("app").appendChild(iframe);
          },
          hardware_hardsid_init: function () {
            HardwareFunctions.init = init_hardsid;
            HardwareFunctions.reset = reset_hardsid;
            HardwareFunctions.write = write_hardsid;
            HardwareFunctions.next = next_hardsid;
            HardwareFunctions.quit = quit_hardsid;
            HardwareFunctions.mapping = "hardsid-mapping/";
            this.init();
          },
          hardware_exsid_init: function () {
            HardwareFunctions.init = init_exsid;
            HardwareFunctions.reset = reset_exsid;
            HardwareFunctions.write = write_exsid;
            HardwareFunctions.next = next_exsid;
            HardwareFunctions.quit = quit_exsid;
            HardwareFunctions.mapping = "exsid-mapping/";
            this.init();
          },
          hardware_sidblaster_init: function () {
            HardwareFunctions.init = init_sidblaster;
            HardwareFunctions.reset = reset_sidblaster;
            HardwareFunctions.write = write_sidblaster;
            HardwareFunctions.next = next_sidblaster;
            HardwareFunctions.quit = quit_sidblaster;
            HardwareFunctions.mapping = "sidblaster-mapping/";
            this.init();
          },
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
              this.showHardwarePlayer = true;
            }
          },
          doPlay: async function () {
            while (sidWriteQueue.isNotEmpty()) {
              write = sidWriteQueue.dequeue();

              if (write.chip == Chip.QUIT) {
                await HardwareFunctions.quit();
                if (ajaxRequest) {
                  ajaxRequest.cancel();
                }
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
                    const dataChunk = progressEvent.event.currentTarget.response;

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
          end: function () {
            sidWriteQueue.clear();
            sidWriteQueue.enqueue({
              chip: Chip.RESET,
            });
            sidWriteQueue.enqueue({
              chip: Chip.QUIT,
            });
            deviceCount = 0;
            this.showAudio = true;
            this.showHardwarePlayer = false;
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
            return "secondary";
          },
          pause: function () {
            this.$refs.audioElm.pause();
            if (deviceCount > 0) {
              if (ajaxRequest) {
                ajaxRequest.cancel();
              }
              sidWriteQueue.clear();
              sidWriteQueue.enqueue({
                chip: Chip.RESET,
              });
            }
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
                this.$refs.formFileSm.value = null;
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
                this.importExportVisible = false;
              };
              reader.readAsText(this.importFile);
            } else if (extension === "json") {
              reader.onload = (res) => {
                this.playlist = JSON.parse(res.target.result);
                this.playlistIndex = 0;
                this.importFile = null;
                this.$refs.formFileSm.value = null;
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
                this.importExportVisible = false;
              };
              reader.readAsText(this.importFile);
            } else {
              let files = new FormData();
              files.append("file", this.importFile, this.importFile.name);
              axios({
                method: "post",
                url: "/jsidplay2service/JSIDPlay2REST/upload/" + this.importFile.name,
                data: files,
                auth: {
                  username: this.username,
                  password: this.password,
                },
              })
                .then((response) => {
                  if (response.data != "null") {
                    this.importFile = null;
                    this.$refs.formFileSm.value = null;
                    this.importExportVisible = false;
                    let entry = {
                      filename: response.data,
                    };
                    if (this.isMusic(entry)) {
                      this.currentSid = this.shortEntry(entry.filename);
                      this.updateSid(entry.filename);
                      this.play(undefined, entry.filename);
                    } else if (this.isVideo(entry)) {
                      this.openiframe(this.createConvertUrl(undefined, entry.filename) + "&audioTuneAsVideo=true");
                    }
                  }
                })
                .catch((error) => {
                  console.log(error);
                });
            }
          },
          exportPlaylist: function () {
            this.importExportVisible = false;
            download("jsidplay2.json", "application/json; charset=utf-8; ", JSON.stringify(this.playlist));
          },
          setNextPlaylistEntry: function () {
            this.pause();

            if (deviceCount <= 0 && this.convertOptions.config.sidplay2Section.loop) {
              this.$refs.audioElm.currentTime = 0;
              this.$refs.audioElm.play();
            } else {
              if (this.theaterMode) {
                this.playRandomHVSC();
                return;
              }
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
            }
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
            this.convertOptions.textToSpeechLocale = "en";
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

            this.convertOptions.config.emulationSection.filter6581 = this.reSIDFilters6581[3];
            this.convertOptions.config.emulationSection.filter8580 = this.reSIDFilters8580[1];
            this.convertOptions.config.emulationSection.stereoFilter6581 = this.reSIDFilters6581[3];
            this.convertOptions.config.emulationSection.stereoFilter8580 = this.reSIDFilters8580[1];
            this.convertOptions.config.emulationSection.thirdSIDFilter6581 = this.reSIDFilters6581[3];
            this.convertOptions.config.emulationSection.thirdSIDFilter8580 = this.reSIDFilters8580[1];
          },
          updateSid: function (entry, itemId, categoryId) {
            if (entry) {
              this.fetchInfo(entry, itemId, categoryId);
              this.fetchStil(entry, itemId, categoryId);
              this.fetchPhoto(entry, itemId, categoryId);
            }
          },
          createDownloadUrl: function (entry, itemId, categoryId) {
            return (
              window.location.protocol +
              "//" +
              window.location.host +
              "/jsidplay2service/JSIDPlay2REST/convert/" +
              uriEncode(entry) +
              "?itemId=" +
              itemId +
              "&categoryId=" +
              categoryId
            );
          },
          createConvertUrl: function (autostart, entry, itemId, categoryId) {
            return (
              window.location.protocol +
              "//" +
              window.location.host +
              "/jsidplay2service/JSIDPlay2REST/convert/" +
              uriEncode(entry) +
              "?enableSidDatabase=" +
              this.convertOptions.config.sidplay2Section.enableDatabase +
              "&startTime=" +
              (this.showHardwarePlayer ? "0" : this.convertOptions.config.sidplay2Section.startTime) +
              "&defaultLength=" +
              this.convertOptions.config.sidplay2Section.defaultPlayLength +
              "&fadeIn=" +
              this.convertOptions.config.sidplay2Section.fadeInTime +
              "&fadeOut=" +
              this.convertOptions.config.sidplay2Section.fadeOutTime +
              "&loop=" +
              "false" + // this.convertOptions.config.sidplay2Section.loop +
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
              (this.showHardwarePlayer
                ? ""
                : "&dualSID=" +
                  this.convertOptions.config.emulationSection.forceStereoTune +
                  "&dualSIDBase=" +
                  this.convertOptions.config.emulationSection.dualSidBase +
                  "&thirdSID=" +
                  this.convertOptions.config.emulationSection.force3SIDTune +
                  "&thirdSIDBase=" +
                  this.convertOptions.config.emulationSection.thirdSIDBase) +
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
              "&sfxSoundExpander=" +
              this.convertOptions.sfxSoundExpander +
              "&sfxSoundExpanderType=" +
              this.convertOptions.sfxSoundExpanderType +
              "&reuSize=" +
              this.convertOptions.reuSize +
              "&textToSpeechType=" +
              this.convertOptions.textToSpeechType +
              "&textToSpeechLocale=" +
              this.convertOptions.textToSpeechLocale +
              "&locale=" +
              this.$i18n.locale +
              "&itemId=" +
              itemId +
              "&categoryId=" +
              categoryId +
              "&autostart=" +
              uriEncode(autostart) +
              "&devtools=" +
              ("$min" !== ".min")
            );
          },
          createSIDMappingUrl: function (entry, itemId, categoryId) {
            return (
              window.location.protocol +
              "//" +
              window.location.host +
              "/jsidplay2service/JSIDPlay2REST/" +
              HardwareFunctions.mapping +
              uriEncode(entry) +
              "?defaultModel=" +
              this.convertOptions.config.emulationSection.defaultSidModel +
              "&fakeStereo=" +
              this.convertOptions.config.emulationSection.fakeStereo +
              "&exsidFakeStereo=" +
              this.convertOptions.config.emulationSection.fakeStereo +
              "&hardSid6581=" +
              this.convertOptions.config.emulationSection.hardsid6581 +
              "&hardSid8580=" +
              this.convertOptions.config.emulationSection.hardsid8580 +
              (HardwareFunctions.mapping === "hardsid-mapping/" ? "&chipCount=" + chipCount : "") +
              "&itemId=" +
              itemId +
              "&categoryId=" +
              categoryId
            );
          },
          openDownloadMP3Url: function (entry, itemId, categoryId) {
            window.open(this.createConvertUrl(undefined, entry, itemId, categoryId) + "&download=true");
          },
          openDownloadSIDUrl: function (entry, itemId, categoryId) {
            window.open(
              window.location.protocol +
                "//" +
                this.username +
                ":" +
                this.password +
                "@" +
                window.location.host +
                "/jsidplay2service/JSIDPlay2REST/download/" +
                uriEncode(entry) +
                "?itemId=" +
                itemId +
                "&categoryId=" +
                categoryId
            );
          },
          openDownloadUrl: function (entry, itemId, categoryId) {
            window.open(this.createDownloadUrl(entry, itemId, categoryId));
          },
          delayedFetchDirectory: function (entry) {
            let outer = this;
            clearTimeout(this.timeoutId);
            this.timeoutId = setTimeout(function () {
              outer.fetchDirectory(entry);
            }, 1000);
          },
          fetchLogs: function () {
            this.loadingLogs = true; //the loading begin
            axios({
              method: "get",
              url:
                "/jsidplay2service/JSIDPlay2REST/logs" +
                "?instant=" +
                this.instant +
                "&sourceClassName=" +
                uriEncode(this.Logs.sourceClassName) +
                "&sourceMethodName=" +
                uriEncode(this.Logs.sourceMethodName) +
                "&level=" +
                uriEncode(this.Logs.level) +
                "&message=" +
                uriEncode(this.Logs.message) +
                "&maxResults=" +
                this.maxResults +
                "&order=" +
                this.order +
                "&tooMuchLogging=" +
                this.tooMuchLogging,
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                this.logs = response.data;
                if (!this.logs) {
                  this.logs = [];
                }
              })
              .catch((error) => {
                this.logs = [];
                console.log(error);
              })
              .finally(() => (this.loadingLogs = false));
          },
          countLogs: function () {
            this.loadingLogs = true; //the loading begin
            axios({
              method: "get",
              url:
                "/jsidplay2service/JSIDPlay2REST/count-logs" +
                "?instant=" +
                this.instant +
                "&sourceClassName=" +
                uriEncode(this.Logs.sourceClassName) +
                "&sourceMethodName=" +
                uriEncode(this.Logs.sourceMethodName) +
                "&level=" +
                uriEncode(this.Logs.level) +
                "&message=" +
                uriEncode(this.Logs.message) +
                "&order=" +
                this.order +
                "&tooMuchLogging=" +
                this.tooMuchLogging,
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                this.logsCount = response.data;
                if (!this.logs) {
                  this.logsCount = 0;
                }
              })
              .catch((error) => {
                logsCount = 0;
                console.log(error);
              })
              .finally(() => (this.loadingLogs = false));
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
            axios({
              method: "get",
              url:
                "/jsidplay2service/JSIDPlay2REST/info/" +
                uriEncode(entry) +
                "?list=true" +
                "&itemId=" +
                itemId +
                "&categoryId=" +
                categoryId,
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
            axios({
              method: "get",
              url:
                "/jsidplay2service/JSIDPlay2REST/stil/" +
                uriEncode(entry) +
                "?itemId=" +
                itemId +
                "&categoryId=" +
                categoryId,
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                this.stil = response.data;
                if (!this.stil) {
                  this.stil = [];
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
            axios({
              method: "get",
              url:
                "/jsidplay2service/JSIDPlay2REST/photo/" +
                uriEncode(entry) +
                "?itemId=" +
                itemId +
                "&categoryId=" +
                categoryId,
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
          fetchFavorites: function (number) {
            this.loadingPl = true; //the loading begin
            axios({
              method: "get",
              url: "/jsidplay2service/JSIDPlay2REST/favorites?favoritesNumber=" + number,
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                this.playlist = response.data;
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
                this.importExportVisible = false;
              })
              .catch((error) => {
                this.playlist = [];
                this.playlistIndex = 0;
                console.log(error);
              })
              .finally(() => (this.loadingPl = false));
          },
          fetchFavoritesNames: function () {
            this.loadingPl = true; //the loading begin
            axios({
              method: "get",
              url: "/jsidplay2service/JSIDPlay2REST/favorite_names",
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                this.favoritesNames = response.data;
              })
              .catch((error) => {
                this.favoritesNames = [];
                console.log(error);
              })
              .finally(() => (this.loadingPl = false));
          },
          playRandomHVSC: function () {
            this.loadingCfg = true; //the loading begin
            axios({
              method: "get",
              url: "/jsidplay2service/JSIDPlay2REST/random-hvsc",
              auth: {
                username: this.username,
                password: this.password,
              },
            })
              .then((response) => {
                this.currentSid = this.shortEntry(response.data);
                this.updateSid(response.data);
                this.fetchDirectory({
                  filename: response.data.substring(0, response.data.lastIndexOf("/")),
                  loading: false,
                });
                this.play(undefined, response.data);
              })
              .catch((error) => {
                console.log(error);
              })
              .finally(() => (this.loadingCfg = false));
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
                this.reSIDFilters6581 = filters
                  .filter((filter) => filter.startsWith("RESID_MOS6581_"))
                  .map((filter) => filter.substring("RESID_MOS6581_".length));
                this.reSIDFilters8580 = filters
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
                this.reSIDFilters6581 = [];
                this.reSIDFilters8580 = [];
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
            axios({
              method: "get",
              url:
                "/jsidplay2service/JSIDPlay2REST/disk-directory/" +
                uriEncode(entry.filename) +
                "?itemId=" +
                itemId +
                "&categoryId=" +
                categoryId,
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
              this.category = "hvscmusic";
              this.event = "";
              this.released = "";
              this.rating = "";
              this.handle = token;
            }
            if (this.name !== "") {
              parameterList.push('name:"' + uriEncode(this.name) + '"');
            }
            if (this.category !== "") {
              parameterList.push("subcat:" + this.category);
            }
            if (this.event !== "") {
              parameterList.push('event:"' + uriEncode(this.event) + '"');
            }
            if (this.released.length === 4) {
              parameterList.push("date:" + this.released + "0101-" + this.released + "1231");
            }
            if (this.released.length === 7) {
              var splitted = this.released.split("-");
              var year = splitted[0];
              var month = splitted[1];
              parameterList.push("date:" + year + month + "01-" + year + month + daysInMonth(month, year));
            }
            if (this.released.length === 10) {
              var splitted = this.released.split("-");
              var year = splitted[0];
              var month = splitted[1];
              var day = splitted[2];
              parameterList.push("date:" + year + month + day + "-" + year + month + day);
            }
            if (this.rating !== "") {
              parameterList.push("rating:>=" + this.rating);
            }
            if (this.handle !== "") {
              parameterList.push('handle:"' + uriEncode(this.handle) + '"');
            }
            return parameterList.length > 0 ? "?query=" + parameterList.join("+") : "";
          },
          fetchCategories: function () {
            this.loadingAssembly64 = true; //the loading begin
            axios({
              method: "get",
              url: "${assembly64Url}/leet/search/aql/presets",
            })
              .then((response) => {
                let presets = response.data;
                this.categories = presets.filter(function (item) {
                  return item.type == "subcat";
                })[0].values;
                this.categories.sort((a, b) => {
                  return a.name.localeCompare(b.name);
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
              url: "${assembly64Url}/leet/search/aql/0/200" + url,
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
                      })[0]?.name,
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
              url: "${assembly64Url}/leet/search/legacy/entries/" + btoa(searchResult.id) + "/" + searchResult.categoryId,
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
          window.addEventListener("resize", () => {
            let outer = this;
            clearTimeout(window.resizedFinished);
            window.resizedFinished = setTimeout(function () {
              outer.carouselImageHeight =
                window.innerHeight > window.innerWidth ? window.innerHeight / 2 : window.innerHeight * 0.8;
            }, 1000);
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
          this.fetchFavoritesNames();
          this.fetchCategories();
          if (localStorage.convertOptions) {
            // restore configuration from last run
            this.convertOptions = JSON.parse(localStorage.convertOptions);
            // migration:
            if (typeof this.convertOptions.textToSpeechType === "undefined") {
              this.convertOptions.textToSpeechType = "PICO2WAVE";
            }
            if (typeof this.convertOptions.textToSpeechLocale === "undefined") {
              this.convertOptions.textToSpeechLocale = "en";
            }
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
            this.convertOptions.textToSpeechLocale = "en";
            this.mobileProfile();
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
          playlist: {
            handler: function (after, before) {
              localStorage.playlistV2 = JSON.stringify(this.playlist);
            },
            deep: true,
          },
          convertOptions: {
            handler: function (after, before) {
              localStorage.convertOptions = JSON.stringify(this.convertOptions);
            },
            deep: true,
          },
        },
      })
        .use(i18n)
        .mount("#app");

      // prevent back button
      history.pushState(null, null, document.URL);
      window.addEventListener("popstate", function () {
        closeiframe();
        history.pushState(null, null, document.URL);
      });
      window.addEventListener("message", messageListener, false);

      document.getElementById("main").addEventListener(
        "keypress",
        function (e) {
          if (e.keyCode == 13) {
            e.preventDefault();
            return false;
          }
        },
        false
      );
    </script>
  </body>
</html>
