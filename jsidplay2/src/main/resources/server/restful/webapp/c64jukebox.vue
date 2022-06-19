<!doctype html>
<html>

<head>

<style lang="scss" scoped>
@import "/static/c64jukebox.scss";
</style>

<!-- Load required Bootstrap and BootstrapVue CSS -->
<link type="text/css" rel="stylesheet"
	href="https://unpkg.com/bootstrap/dist/css/bootstrap.min.css" />
<link type="text/css" rel="stylesheet"
	href="https://unpkg.com/bootstrap-vue@latest/dist/bootstrap-vue.min.css" />

<!-- Load Vue followed by BootstrapVue -->
<script src="https://cdn.jsdelivr.net/npm/vue@2.6.0"></script>
<script
	src="https://unpkg.com/bootstrap-vue@latest/dist/bootstrap-vue.min.js"></script>

<script src="https://unpkg.com/axios/dist/axios.min.js"></script>

<script src="https://unpkg.com/vue-i18n@8"></script>

<meta charset="UTF-8" />
<meta name="viewport"
	content="width=device-width, initial-scale=1, shrink-to-fit=no">

<title>C64 Jukebox</title>

</head>
<body>
	<div id="app">

		<form>

			<div>
				<div class="locale-changer">
					<select v-model="$i18n.locale" style="float: right">
						<option v-for="(lang, i) in langs" :key="`Lang${i}`" :value="lang">
							{{ lang }}</option>
					</select>
					<h1>C64 Jukebox</h1>
					<span>{{currentSid}}</span>
				</div>
				<div class="audio">
					<audio ref="audioElm" v-bind:src="playlistEntryUrl"
						v-on:ended="setNextPlaylistEntry" type="audio/mpeg" controls
						autoplay> I'm sorry. You're browser doesn't support HTML5
						audio
					</audio>
				</div>
			</div>
			<div>
				<b-card no-body> <b-tabs v-model="tabIndex" card>
				<b-tab title="CON">

				<template #title>CON</template>

				<b-card-text>
				<div>
					<label for="username">{{ $t( 'username' ) }}</label>
					<input type="text" id="username" v-model="username">
					<label for="password">{{ $t( 'password' ) }}</label>
					<input type="password" id="password" v-model="password">
				</div>

				</b-card-text> </b-tab> <b-tab active>

				<template #title>
					SIDS
					<b-spinner type="border" small v-if="loadingSids"></b-spinner>
				</template>

				<b-card-text>
				<div v-if="!loadingSids">
					<!-- request finished -->
					<ul>
						<li v-for="entry in directory" :key="entry">
							<!-- HVSC root -->
							<div v-if="entry.endsWith('/')">
								<a href="#" v-on:click="fetchDirectory(entry)"> {{entry}} </a>
							</div> <!-- HVSC music -->
							<div
								v-else-if="entry.endsWith('.sid') || entry.endsWith('.dat') || entry.endsWith('.mus') || entry.endsWith('.str')">
								<div>
									<a href="#" v-on:click="updateSid(entry); tabIndex = 2;">
										{{entry}} </a>
								</div>
							</div> <!-- others -->
							<div v-else>
								<div style="white-space: nowrap;">
									{{entry}} <a v-bind:href="createConvertUrl(entry)"
										target="_blank"> Load </a> <span
										v-if='entry.toLowerCase().endsWith(".d64")'> <span>
											or </span> <a
										v-bind:href="createConvertUrl(entry) + '&jiffydos=true'"
										target="_blank"> Fastload </a>
									</span>
								</div>
							</div>
						</li>
					</ul>
				</div>
				</b-card-text> </b-tab> <b-tab>

				<template #title>
					SID
					<b-spinner type="border" small v-if="loadingSid"></b-spinner>
				</template>

				<b-card-text>
				<div v-if='currentSid'>
					<b-button
						v-on:click="$refs.audioElm.src=createConvertUrl(currentSid); $refs.audioElm.play()">{{
					$t( 'play' ) }}</b-button>
					<b-button
						v-on:click="playlist.push(currentSid); tabIndex = 3; playlistIndex = 0;">{{
					$t( 'addToPlaylist' ) }}</b-button>
					<b-button v-on:click="createDownloadMP3Url(currentSid);">{{
					$t( 'downloadMP3' ) }}</b-button>
					<b-button v-on:click="createDownloadSIDUrl(currentSid);">{{
					$t( 'downloadSID' ) }}</b-button>
				</div>

				<div class="sid">
					<div>
						<table>
							<thead>
							</thead>
							<tbody>
								<tr v-for="(value, key) in infos">
									<td>{{ $t( key ) }}</td>
									<td>{{value}}</td>
								</tr>
							</tbody>
						</table>
					</div>
					<div>
						<img :src="picture" id="img">
					</div>
				</div>
				</b-card-text> </b-tab> <b-tab>

				<template #title>
					PL
					<b-spinner type="border" small v-if="loadingPl"></b-spinner>
				</template>

				<b-card-text>

				<div>
					<div class="button-box">
						<b-button v-on:click="playlist.pop()">{{ $t( 'remove' )
						}}</b-button>
						<b-button v-on:click="setNextPlaylistEntry">{{ $t(
						'next' ) }}</b-button>

						<b-form-checkbox id="random" v-model="random">
						{{ $t( 'random' ) }} </b-form-checkbox>

					</div>
					<div class="button-box">
						<b-button v-on:click="fetchFavorites()">{{ $t(
						'fetchFavorites' ) }}</b-button>
						<b-button v-on:click="playlist=[]">{{ $t(
						'removePlaylist' ) }}</b-button>
					</div>
				</div>

				<ol>
					<li v-for="(entry,index) in playlist" :key="index"><a
						:class="index==playlistIndex ? 'highlighted' : ''" href="#"
						v-on:click="playlistIndex = index; updateSid(playlist[playlistIndex]);">{{entry}}
					</a></li>
				</ol>

				</b-card-text> </b-tab> <b-tab>

				<template #title>
					CFG
					<b-spinner type="border" small v-if="loadingCfg"></b-spinner>
				</template>

				<b-card-text>

				<div class="settings-box">
					<label for="startTime">{{ $t( 'startTime' ) }}</label>
					<input type="number" min="0" oninput="validity.valid||(value='');"
						id="startTime" v-model.number="startTime" />
					<label for="defaultLength">{{ $t( 'defaultLength' ) }}</label>
					<input type="number" min="0" oninput="validity.valid||(value='');"
						id="defaultLength" v-model.number="defaultLength" />
				</div>
				<div class="settings-box">
					<label for="fadeIn">{{ $t( 'fadeIn' ) }}</label>
					<input type="number" min="0" oninput="validity.valid||(value='');"
						id="fadeIn" v-model.number="fadeIn" />
					<label for="fadeOut">{{ $t( 'fadeOut' ) }}</label>
					<input type="number" min="0" oninput="validity.valid||(value='');"
						id="fadeOut" v-model.number="fadeOut" />
				</div>
				<div class="settings-box">
					<b-form-checkbox id="detectSongLength" v-model="detectSongLength">
					{{ $t( 'detectSongLength' ) }} </b-form-checkbox>
					<b-form-checkbox id="singleSong" v-model="singleSong">
					{{ $t( 'singleSong' ) }} </b-form-checkbox>
					<b-form-checkbox id="loopSong" v-model="loopSong">
					{{ $t( 'loopSong' ) }} </b-form-checkbox>
				</div>
				<div class="settings-box">
					<b-form-checkbox id="digiboost8580" v-model="digiboost8580">
					{{ $t( 'digiboost8580' ) }} </b-form-checkbox>
					<b-form-checkbox id="fakeStereo" v-model="fakeStereo">
					{{ $t( 'fakeStereo' ) }} </b-form-checkbox>
					<b-form-checkbox id="bypassReverb" v-model="bypassReverb">
					{{ $t( 'bypassReverb' ) }} </b-form-checkbox>
				</div>

				<div>
					<span>{{ $t( 'muteSid' ) }}</span>
				</div>
				<div class="settings-box">
					<b-form-checkbox id="sidMuteVoice1" v-model="sidMuteVoice1">
					{{ $t( 'sidMuteVoice1' ) }} </b-form-checkbox>
					<b-form-checkbox id="sidMuteVoice2" v-model="sidMuteVoice2">
					{{ $t( 'sidMuteVoice2' ) }} </b-form-checkbox>
					<b-form-checkbox id="sidMuteVoice3" v-model="sidMuteVoice3">
					{{ $t( 'sidMuteVoice3' ) }} </b-form-checkbox>
					<b-form-checkbox id="sidMuteSamples" v-model="sidMuteSamples">
					{{ $t( 'sidMuteSamples' ) }} </b-form-checkbox>
				</div>
				<div>
					<span>{{ $t( 'muteStereoSid' ) }}</span>
				</div>
				<div class="settings-box">
					<b-form-checkbox id="stereoSidMuteVoice1"
						v-model="stereoSidMuteVoice1"> {{ $t(
					'sidMuteVoice1' ) }} </b-form-checkbox>
					<b-form-checkbox id="stereoSidMuteVoice2"
						v-model="stereoSidMuteVoice2"> {{ $t(
					'sidMuteVoice2' ) }} </b-form-checkbox>
					<b-form-checkbox id="stereoSidMuteVoice3"
						v-model="stereoSidMuteVoice3"> {{ $t(
					'sidMuteVoice3' ) }} </b-form-checkbox>
					<b-form-checkbox id="stereoSidMuteSamples"
						v-model="stereoSidMuteSamples"> {{ $t(
					'sidMuteSamples' ) }} </b-form-checkbox>
				</div>
				<div>
					<span>{{ $t( 'muteThreeSid' ) }}</span>
				</div>
				<div class="settings-box">
					<b-form-checkbox id="threeSidMuteVoice1"
						v-model="threeSidMuteVoice1"> {{ $t(
					'sidMuteVoice1' ) }} </b-form-checkbox>
					<b-form-checkbox id="threeSidMuteVoice2"
						v-model="threeSidMuteVoice2"> {{ $t(
					'sidMuteVoice2' ) }} </b-form-checkbox>
					<b-form-checkbox id="threeSidMuteVoice3"
						v-model="threeSidMuteVoice3"> {{ $t(
					'sidMuteVoice3' ) }} </b-form-checkbox>
					<b-form-checkbox id="threeSidMuteSamples"
						v-model="threeSidMuteSamples"> {{ $t(
					'sidMuteSamples' ) }} </b-form-checkbox>
				</div>
				<div class="settings-box">
					<label for="bufferSize">{{ $t( 'bufferSize' ) }}</label>
					<input type="number" min="0" oninput="validity.valid||(value='');"
						id="bufferSize" v-model.number="bufferSize" />
				</div>
				<div class="settings-box">
					<b-form-group> <b-form-radio-group
						v-model="defaultEngine" style="display: flex;">
					<b-form-radio value="RESIDFP" @change="updateFilters('RESIDFP')">RESIDFP</b-form-radio>
					<b-form-radio value="RESID" @change="updateFilters('RESID')">RESID</b-form-radio>
					</b-form-radio-group> </b-form-group>
				</div>
				<div class="settings-box">
					<b-form-group> <b-form-radio-group
						v-model="samplingMethod" style="display: flex;">
					<b-form-radio value="DECIMATE">DECIMATE</b-form-radio> <b-form-radio
						value="RESAMPLE">RESAMPLE</b-form-radio> </b-form-radio-group> </b-form-group>
				</div>
				<div class="settings-box">
					<b-form-group> <b-form-radio-group
						v-model="samplingRate" style="display: flex;">
					<b-form-radio value="LOW">LOW</b-form-radio> <b-form-radio
						value="MEDIUM">MEDIUM</b-form-radio> <b-form-radio value="HIGH">HIGH</b-form-radio>
					</b-form-radio-group> </b-form-group>
				</div>
				<div class="settings-box">
					<b-form-group> <b-form-radio-group
						v-model="defaultModel" style="display: flex;">
					<b-form-radio value="MOS6581">MOS6581</b-form-radio> <b-form-radio
						value="MOS8580">MOS8580</b-form-radio> </b-form-radio-group> </b-form-group>
				</div>
				<div class="settings-box">
					<div>
						<div>
							<span>{{ $t( 'filter6581' )}}</span>
							<b-form-select v-model="filter6581" :options="filters6581"
								size="sm" class="mt-3" :select-size="3"></b-form-select>
						</div>
						<div>
							<span>{{ $t( 'filter8580' )}}</span>
							<b-form-select v-model="filter8580" :options="filters8580"
								size="sm" class="mt-3" :select-size="3"></b-form-select>
						</div>
					</div>
					<div>
						<div>
							<span>{{ $t( 'stereoFilter6581' )}}</span>
							<b-form-select v-model="stereoFilter6581" :options="filters6581"
								size="sm" class="mt-3" :select-size="3"></b-form-select>
						</div>
						<div>
							<span>{{ $t( 'stereoFilter8580' )}}</span>
							<b-form-select v-model="stereoFilter8580" :options="filters8580"
								size="sm" class="mt-3" :select-size="3"></b-form-select>
						</div>
					</div>
					<div>
						<div>
							<span>{{ $t( 'threeFilter6581' )}}</span>
							<b-form-select v-model="threeFilter6581" :options="filters6581"
								size="sm" class="mt-3" :select-size="3"></b-form-select>
						</div>
						<div>
							<span>{{ $t( 'threeFilter8580' )}}</span>
							<b-form-select v-model="threeFilter8580" :options="filters8580"
								size="sm" class="mt-3" :select-size="3"></b-form-select>
						</div>
					</div>
				</div>
				<div class="settings-box">
					<label for="volumeSid">{{ $t( 'volumeSid' ) }}</label> <span
						class="value">{{ volumeSid }}db</span>
					<b-form-input id="volumeSid" v-model="volumeSid" type="range"
						min="-6" max="6" step="1"></b-form-input>

					<label for="volumeStereoSid">{{ $t( 'volumeStereoSid' ) }}</label>
					<span class="value">{{ volumeStereoSid }}db</span>
					<b-form-input id="volumeStereoSid" v-model="volumeStereoSid"
						type="range" min="-6" max="6" step="1"></b-form-input>

					<label for="volumeThreeSid">{{ $t( 'volumeThreeSid' ) }}</label> <span
						class="value">{{ volumeThreeSid }}db</span>
					<b-form-input id="volumeThreeSid" v-model="volumeThreeSid"
						type="range" min="-6" max="6" step="1"></b-form-input>
				</div>
				<div class="settings-box">
					<label for="balanceSid">{{ $t( 'balanceSid' ) }}</label> <span
						class="value">{{ balanceSid }}</span>
					<b-form-input id="balanceSid" v-model="balanceSid" type="range"
						min="0" max="1" step="0.1"></b-form-input>
					<label for="balanceStereoSid">{{ $t( 'balanceStereoSid' )
						}}</label> <span class="value">{{ balanceStereoSid }}</span>
					<b-form-input id="balanceStereoSid" v-model="balanceStereoSid"
						type="range" min="0" max="1" step="0.1"></b-form-input>
					<label for="balanceThreeSid">{{ $t( 'balanceThreeSid' ) }}</label>
					<span class="value">{{ balanceThreeSid }}</span>
					<b-form-input id="balanceThreeSid" v-model="balanceThreeSid"
						type="range" min="0" max="1" step="0.1"></b-form-input>
				</div>
				<div class="settings-box">
					<label for="delaySid">{{ $t( 'delaySid' ) }}</label> <span
						class="value">{{ delaySid }}ms</span>
					<b-form-input id="delaySid" v-model="delaySid" type="range" min="0"
						max="100" step="10"></b-form-input>
					<label for="delayStereoSid">{{ $t( 'delayStereoSid' ) }}</label> <span
						class="value">{{ delayStereoSid }}ms</span>
					<b-form-input id="delayStereoSid" v-model="delayStereoSid"
						type="range" min="0" max="100" step="10"></b-form-input>
					<label for="delayThreeSid">{{ $t( 'delayThreeSid' ) }}</label> <span
						class="value">{{ delayThreeSid }}ms</span>
					<b-form-input id="delayThreeSid" v-model="delayThreeSid"
						type="range" min="0" max="100" step="10"></b-form-input>
				</div>
				<div class="settings-box">
					<div class="button-box">
						<b-button v-on:click="mobileProfile">{{ $t(
						'mobileProfile' ) }}</b-button>
						<b-button v-on:click="wifiProfile">{{ $t( 'wifiProfile'
						) }}</b-button>
					</div>
				</div>
				<div class="settings-box">
					<b-form-checkbox id="vbr" v-model="vbr"> {{ $t(
					'vbr' ) }} </b-form-checkbox>
					<div>
						<label for="cbr">{{ $t ( 'cbr' ) }}</label> <select id="cbr"
							v-model="cbr">
							<option v-for="cbr in cbrs">{{ cbr }}</option>
						</select>
					</div>
					<div>
						<label for="vbrQuality">{{ $t ( 'vbrQuality' ) }}</label> <select
							id="vbrQuality" v-model="vbrQuality">
							<option v-for="vbrQuality in vbrQualities">{{ vbrQuality
								}}</option>
						</select>
					</div>
					<div>
						<label for="vcBitRate">{{ $t( 'vcBitRate' ) }}</label>
						<input type="number" min="0" oninput="validity.valid||(value='');"
							id="vcBitRate" v-model.number="vcBitRate" />
					</div>
				</div>
				<div class="settings-box">
					<b-form-checkbox id="status" v-model="status"> {{
					$t( 'status' ) }} </b-form-checkbox>
				</div>
				<div class="settings-box">
					<label for="pressSpaceInterval">{{ $t(
						'pressSpacePeriodically' ) }}</label>
					<input type="number" id="pressSpaceInterval"
						v-model.number="pressSpaceInterval" />
				</div>
				<div class="settings-box">
					<b-form-group label="REU size"> <b-form-radio-group
						v-model="reuSize" style="display: flex;"> <b-form-radio
						value="auto">Autodetect</b-form-radio> <b-form-radio value="kb64">64kb</b-form-radio>
					<b-form-radio value="kb128">128kb</b-form-radio> <b-form-radio
						value="kb256">256kb</b-form-radio> <b-form-radio value="kb512">512kb</b-form-radio>
					<b-form-radio value="kb1024">1024kb</b-form-radio> <b-form-radio
						value="kb2048">2048kb</b-form-radio> </b-form-radio-group> </b-form-group>
				</div>

				</b-card-text> </b-tab> </b-tabs> </b-card>
			</div>

		</form>

	</div>

	<script>
function uriEncode(entry) {
  // escape is deprecated and cannot handle utf8
  // encodeURI() will not encode: ~!@#$&*()=:/,;?+'
  // untested: !*=/,;?
  // tested: ~@#$&():+''
  return encodeURI(entry)
    .replace(/\+/g, "%2B")
    .replace(/#/g, "%23");
}

const messages = {
  en: {
	  HVSCEntry: {
		  path: 'Full Path',
		  name: 'File Name',
		  title: 'Title',
		  author: 'Author',
		  released: 'Released',
		  format: 'Format',
		  playerId: 'Player ID',
		  noOfSongs: 'No. of Songs',
		  startSong: 'Start Song',
		  clockFreq: 'Clock Freq.',
		  speed: 'Speed',
		  sidModel1: 'SID Model 1',
		  sidModel2: 'SID Model 2',
		  sidModel3: 'SID Model 3',
		  compatibility: 'Compatibility',
		  tuneLength: 'Tune Length (s)',
		  audio: 'Audio',
		  sidChipBase1: 'SID Chip Base 1',
		  sidChipBase2: 'SID Chip Base 2',
		  sidChipBase3: 'SID Chip Base 3',
		  driverAddress: 'Driver Address',
		  loadAddress: 'Load Address',
		  loadLength: 'Load Length',
		  initAddress: 'Init Address',
		  playerAddress: 'Player Address',
		  fileDate: 'File Date',
		  fileSizeKb: 'File Size (kb)',
		  tuneSizeB: 'Tune Size (b)',
		  relocStartPage: 'Reloc. Start Page',
		  relocNoPages: 'Reloc. no. Pages',
		  stilGlbComment: 'Tune Size (b)',
	  },
	  username: 'Username',
	  password: 'Password',
	  play: 'Play',
	  downloadMP3: 'Download MP3',
	  downloadSID: 'Download SID',
	  addToPlaylist: 'Add To Playlist',
	  remove: 'Remove',
	  next: 'Next',
	  fetchFavorites: 'Download Playlist',
	  removePlaylist: 'Remove Playlist',
	  random: 'Random',
	  detectSongLength: 'Detect Song Length',
	  singleSong: 'Single Song',
	  loopSong: 'Loop Song',
	  digiboost8580: 'Digi Boost 8580',
	  fakeStereo: 'Fake Stereo',
	  bypassReverb: 'Bypass Reverb',
	  startTime: 'Start Time in sec.',
	  defaultLength: 'Default Length in sec.',
	  fadeIn: 'Fade-In in sec.',
	  fadeOut: 'Fade-Out in sec.',
	  volumeSid: 'Increase volume of SID:',
	  volumeStereoSid: 'Increase volume of Stereo-SID:',
	  volumeThreeSid: 'Increase volume of 3-SID:',
	  balanceSid: 'Balance of SID:',
	  balanceStereoSid: 'Balance of Stereo-SID:',
	  balanceThreeSid: 'Balance of 3-SID:',
	  delaySid: 'Delay of SID:',
	  delayStereoSid: 'Delay of Stereo-SID:',
	  delayThreeSid: 'Delay of 3-SID:',
	  vbr: 'variable bitrate instead of constant bitrate',
	  cbr: 'constant bitrate in kbps',
	  vbrQuality: 'Quality of variable bitrate',
	  vcBitRate: 'Video Bit Rate',
	  filter6581: 'SID Filter 6581',
	  filter8580: 'SID Filter 8580',
	  stereoFilter6581: 'Stereo-SID Filter 6581',
	  stereoFilter8580: 'Stereo-SID Filter 8580',
	  threeFilter6581: '3-SID Filter 6581',
	  threeFilter8580: '3-SID Filter 8580',
	  muteSid: 'Mute Mono SID',
	  muteStereoSid: 'Mute Stereo SID',
	  muteThreeSid: 'Mute 3-SID',
	  sidMuteVoice1: 'Voice 1',
	  sidMuteVoice2: 'Voice 2',
	  sidMuteVoice3: 'Voice 3',
	  sidMuteSamples: 'Samples',
	  bufferSize: 'Buffer Size',
	  pressSpacePeriodically: 'Press Space periodically in s',
	  status: 'Show Status Line'
  },
  de: {
	  HVSCEntry: {
		  path: 'Dateipfad',
		  name: 'Dateiname',
		  title: 'Titel',
		  author: 'Autor',
		  released: 'Publiziert',
		  format: 'Format',
		  playerId: 'Player ID',
		  noOfSongs: 'Song Anzahl',
		  startSong: 'Start Song',
		  clockFreq: 'Takt Frequenz',
		  speed: 'Geschwindigkeit',
		  sidModel1: 'SID Model 1',
		  sidModel2: 'SID Model 2',
		  sidModel3: 'SID Model 3',
		  compatibility: 'Kompatibilit\u00e4t',
		  tuneLength: 'Tune L\u00e4nge (s)',
		  audio: 'Ton',
		  sidChipBase1: 'SID Chip Basisadresse 1',
		  sidChipBase2: 'SID Chip Basisadresse 2',
		  sidChipBase3: 'SID Chip Basisadresse 3',
		  driverAddress: 'Treiberaddresse',
		  loadAddress: 'Ladeaddresse',
		  loadLength: 'Ladel\u00e4nge',
		  initAddress: 'Init-Addresse',
		  playerAddress: 'Player-Addresse',
		  fileDate: 'File Datum',
		  fileSizeKb: 'File Gr\u00f6sse (kb)',
		  tuneSizeB: 'Tune Gr\u00f6sse (b)',
		  relocStartPage: 'Reloc. Start Seite',
		  relocNoPages: 'Reloc. Seitenanzahl',
		  stilGlbComment: 'STIL glb. Kommentar',
	  },
	  username: 'Benutzername',
	  password: 'Passwort',
	  play: 'Abspielen',
	  downloadMP3: 'Download MP3',
	  downloadSID: 'Download SID',
	  addToPlaylist: 'Zu Favoriten hinzufügen',
	  remove: 'Löschen',
	  next: 'Nächster',
	  fetchFavorites: 'Favoriten herunterladen',
	  removePlaylist: 'Favoriten löschen',
	  random: 'Zufällig',
	  detectSongLength: 'Songlänge berücksichtigen',
	  singleSong: 'Nur den Startsong spielen',
	  loopSong: 'Song wiederholen',
	  digiboost8580: 'Digi Boost 8580',
	  fakeStereo: 'Fake Stereo',
	  bypassReverb: 'Reverb überbrücken',
	  startTime: 'Startzeit in Sek.',
	  defaultLength: 'Default Länge in Sek.',
	  fadeIn: 'Fade-In in Sek.',
	  fadeOut: 'Fade-Out in Sek.',
	  volumeSid: 'Lautstärke anheben des SID:',
	  volumeStereoSid: 'Lautstärke anheben des Stereo-SID:',
	  volumeThreeSid: 'Lautstärke anheben des 3-SID:',
	  balanceSid: 'Balance des SID:',
	  balanceStereoSid: 'Balance des Stereo-SID:',
	  balanceThreeSid: 'Balance des 3-SID:',
	  delaySid: 'Verzögerung des SID:',
	  delayStereoSid: 'Verzögerung des SID:',
	  delayThreeSid: 'Verzögerung des SID:',
	  vbr: 'Variable Bitrate verwenden anstatt fester',
	  cbr: 'Konstante Bitrate in kbps',
	  vbrQuality: 'Qualität der variablen Bitrate',
	  vcBitRate: 'Video Bit Rate',
	  filter6581: 'Filter 6581',
	  filter8580: 'Filter 8580',
	  stereoFilter6581: 'Stereo-SID Filter 6581',
	  stereoFilter8580: 'Stereo-SID Filter 8580',
	  threeFilter6581: '3-SID Filter 6581',
	  threeFilter8580: '3-SID Filter 8580',
	  muteSid: 'Mono SID stummschalten',
	  muteStereoSid: 'Stereo SID stummschalten',
	  muteThreeSid: '3-SID stummschalten',
	  sidMuteVoice1: 'Voice 1',
	  sidMuteVoice2: 'Voice 2',
	  sidMuteVoice3: 'Voice 3',
	  sidMuteSamples: 'Samples',
	  bufferSize: 'Puffergröße',
	  pressSpacePeriodically: 'Leertaste wiederholt drücken in s',
	  status: 'Statuszeile anzeigen'
  }
}

const i18n = new VueI18n({
	  locale: 'en', // set locale
	  messages // set locale messages
	})
	
new Vue({
  el: "#app",
  i18n, //import mutil-lang
  data: {
	langs: ['de', 'en'],
	// CON (connection parameters)
    username: "jsidplay2",
    password: "jsidplay2!",
    // SIDS (directories containing SIDS)
	directory: "",
	// SID (info + picture)
    infos: "",
    picture: '',
    currentSid: '',
	// PL (Playlist)
	playlist: [],
	playlistIndex: 0,
    random: true,
	// CFG (configuration)
    startTime: 0,
    defaultLength: 0,
    fadeIn: 0,
    fadeOut: 0,
    detectSongLength: true,
    singleSong: false,
    loopSong: false,
    digiboost8580: false,
    fakeStereo: false,
    bypassReverb: false,
    sidMuteVoice1: false,
    sidMuteVoice2: false,
    sidMuteVoice3: false,
    sidMuteSamples: false,
    stereoSidMuteVoice1: false,
    stereoSidMuteVoice2: false,
    stereoSidMuteVoice3: false,
    stereoSidMuteSamples: false,
    threeSidMuteVoice1: false,
    threeSidMuteVoice2: false,
    threeSidMuteVoice3: false,
    threeSidMuteSamples: false,
    bufferSize: 65536,
    defaultEngine: 'RESIDFP',
    samplingMethod: 'DECIMATE',
    samplingRate: 'MEDIUM',
    defaultModel: 'MOS8580',

    // pre-fetched filter definitions
	filtersResid6581: [],
	filtersResid8580: [],
	filtersResidFp6581: [],
	filtersResidFp8580: [],
	// current filters according to defaultEngine
	filters6581: [],
	filters8580: [],
	// chosen filters
	filter6581: '',
	filter8580: '',
	stereoFilter6581: '',
	stereoFilter8580: '',
	threeFilter6581: '',
	threeFilter8580: '',
    
	volumeSid: 0,
    volumeStereoSid: 0,
    volumeThreeSid: 0,
    balanceSid: 0.3,
    balanceStereoSid: 0.7,
    balanceThreeSid: 0.5,
    delaySid: 0,
    delayStereoSid: 20,
    delayThreeSid: 0,
    vbr: false,
    cbr: -1,
    cbrs: [-1, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320],
    vbrQuality: 0,
    vbrQualities: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
	vcBitRate: 600000,
    reuSize: 'auto',
    status: true,
    pressSpaceInterval: 90,
    // Misc.
	tabIndex: 0,
    loadingSids: false,
    loadingSid: false,
    loadingPl: false,
    loadingCfg: false
  },
  computed: {
	playlistEntryUrl: function() {
    	if (this.playlist.length === 0) {
    		return undefined;
    	} else {
			return this.createConvertUrl(this.playlist[this.playlistIndex]);
    	}
    },
    reuParameters: function() {
      if (this.reuSize === "kb64") {
        return "&reuSize=64";
      } else if (this.reuSize === "kb128") {
        return "&reuSize=128";
      } else if (this.reuSize === "kb512") {
        return "&reuSize=512";
      } else if (this.reuSize === "kb1024") {
        return "&reuSize=1024";
      } else if (this.reuSize === "kb2048") {
        return "&reuSize=2048";
      }
      return "";
    }
  },
  created: function() {
    this.fetchDirectory("/");
    this.fetchFilters();
  },
  methods: {
	  setNextPlaylistEntry: function () {
		if (this.playlist.length === 0) {
			return;
		}
		if (this.random) {
			this.playlistIndex = Math.ceil(Math.random() * this.playlist.length);
		} else {
		    if (this.playlistIndex === this.playlist.length - 1) {
		    	this.playlistIndex = 0;
		    } else {
		    	this.playlistIndex++;
		    }
		}
		this.updateSid(this.playlist[this.playlistIndex]);
	},
	mobileProfile: function() {
		this.vbr = false;
		this.cbr = 64;
	},
	wifiProfile: function() {
		this.vbr = true;
		this.vbrQuality = 0;
	},
    updateFilters: function(engine) {
		if (engine==='RESIDFP') {
			this.filters6581 = this.filtersResidFp6581;
		 	this.filters8580 = this.filtersResidFp8580;

			this.filter6581 = this.filters6581[1];
			this.filter8580 = this.filters8580[1];
			this.stereoFilter6581 = this.filters6581[1];
			this.stereoFilter8580 = this.filters8580[1];
			this.threeFilter6581 = this.filters6581[1];
			this.threeFilter8580 = this.filters8580[1];
		} else { // RESID
			this.filters6581 = this.filtersResid6581;
			this.filters8580 = this.filtersResid8580;

			this.filter6581 = this.filters6581[3];
			this.filter8580 = this.filters8580[1];
			this.stereoFilter6581 = this.filters6581[3];
			this.stereoFilter8580 = this.filters8580[1];
			this.threeFilter6581 = this.filters6581[3];
			this.threeFilter8580 = this.filters8580[1];
		}
      },
    updateSid: function(entry) {
    	  this.fetchInfo(entry);
    	  this.fetchPhoto(entry);
        },
    createConvertUrl: function(entry) {
      	return window.location.protocol + '//' + this.username + ':' + this.password + '@' + window.location.host + '/jsidplay2service/JSIDPlay2REST/convert' + uriEncode(entry)
      		+ '?enableSidDatabase=' + this.detectSongLength + '&single=' + this.singleSong + '&loop=' + this.loopSong
      		+ '&muteVoice1=' + this.sidMuteVoice1 + '&muteVoice2=' + this.sidMuteVoice2 + '&muteVoice3=' + this.sidMuteVoice3 + '&muteVoice4=' + this.sidMuteSamples
      		+ '&muteStereoVoice1=' + this.stereoSidMuteVoice1 + '&muteStereoVoice2=' + this.stereoSidMuteVoice2 + '&muteStereoVoice3=' + this.stereoSidMuteVoice3 + '&muteStereoVoice4=' + this.stereoSidMuteSamples
      		+ '&muteThirdSidVoice1=' + this.threeSidMuteVoice1 + '&muteThirdSidVoice2=' + this.threeSidMuteVoice2 + '&muteThirdSidVoice3=' + this.threeSidMuteVoice3 + '&muteThirdSidVoice4=' + this.threeSidMuteSamples
      		+ '&bufferSize=' + this.bufferSize + '&sampling=' + this.samplingMethod + '&frequency=' + this.samplingRate
      		+ '&defaultEmulation=' + this.defaultEngine + '&defaultModel=' + this.defaultModel + '&startTime=' + this.startTime
      		+ '&defaultLength=' + this.defaultLength + '&fadeIn=' + this.fadeIn + '&fadeOut=' + this.fadeOut
      		+ '&mainVolume=' + this.volumeSid + '&secondVolume=' + this.volumeStereoSid + '&thirdVolume=' + this.volumeThreeSid
      		+ '&mainBalance=' + this.balanceSid + '&secondBalance=' + this.balanceStereoSid + '&thirdBalance=' + this.balanceThreeSid
      		+ '&mainDelay=' + this.delaySid + '&secondDelay=' + this.delayStereoSid + '&thirdDelay=' + this.delayThreeSid
      		+ '&filter6581=' + this.filter6581 + '&stereoFilter6581=' + this.stereoFilter6581 + '&thirdFilter6581=' + this.threeFilter6581
      		+ '&filter8580=' + this.filter8580 + '&stereoFilter8580=' + this.stereoFilter8580 + '&thirdFilter8580=' + this.threeFilter8580
      		+ '&reSIDfpFilter6581=' + this.filter6581 + '&reSIDfpStereoFilter6581=' + this.stereoFilter6581 + '&reSIDfpThirdFilter6581=' + this.threeFilter6581
      		+ '&reSIDfpFilter8580=' + this.filter8580 + '&reSIDfpStereoFilter8580=' + this.stereoFilter8580 + '&reSIDfpThirdFilter8580=' + this.threeFilter8580
      		+ '&digiBoosted8580=' + this.digiboost8580 + '&fakeStereo=' + this.fakeStereo + '&reverbBypass=' + this.bypassReverb
      		+ '&cbr=' + this.cbr + '&vbrQuality=' + this.vbrQuality + '&vbr=' + this.vbr + "&vcBitRate=" + this.vcBitRate
      		+ '&pressSpaceInterval=' + this.pressSpaceInterval+'&status=' + this.status + this.reuParameters;
      },
    createDownloadMP3Url: function(entry) {
          window.open(this.createConvertUrl(entry) + '&download=true');
      },
    createDownloadSIDUrl: function(entry) {
          window.open(window.location.protocol + '//' + this.username + ':' + this.password + '@' + window.location.host + '/jsidplay2service/JSIDPlay2REST/download' + uriEncode(entry));
      },
    fetchDirectory: function(entry) {
       this.loadingSids = true; //the loading begin
       axios({
         method: "get",
         url:
           "/jsidplay2service/JSIDPlay2REST/directory" + uriEncode(entry) + "?filter=.*%5C.(sid%7Cdat%7Cmus%7Cstr%7Cmp3%7Cmp4%7Cdv%7Cvob%7Ctxt%7Cjpg%7Cprg%7Cd64%7Cg64%7Cnib%7Creu%7Cima%7Ccrt%7Cimg%7Ctap%7Ct64%7Cp00)$",
         auth: {
           username: this.username,
           password: this.password
         }
       })
         .then(response => {
           this.directory = response.data;
         })
         .finally(() => (this.loadingSids = false));
    },
    fetchInfo: function(entry) {
        this.loadingSid = true; //the loading begin
        axios({
          method: "get",
          url:
            "/jsidplay2service/JSIDPlay2REST/info" +
            uriEncode(entry),
          auth: {
            username: this.username,
            password: this.password
          }
        })
          .then(response => {
            this.infos = response.data;
            this.currentSid = entry;
          })
          .finally(() => (this.loadingSid = false));
      },
      fetchPhoto: function(entry) {
          this.loadingSid = true; //the loading begin
          axios({
            method: "get",
            url:
              "/jsidplay2service/JSIDPlay2REST/photo" + uriEncode(entry),
              auth: {
              username: this.username,
              password: this.password
            },
            responseType:"blob",
          })
            .then(response => {
            	var reader = new window.FileReader();
                reader.readAsDataURL(response.data); 
                reader.onload = function() {

                	this.picture = reader.result;
                    document.getElementById('img').setAttribute('src', this.picture);
                }
           	})
            .finally(() => (this.loadingSid = false));
        },
        fetchFavorites: function() {
            this.loadingPl = true; //the loading begin
            axios({
              method: "get",
              url:
                "/jsidplay2service/JSIDPlay2REST/favorites",
              auth: {
                username: this.username,
                password: this.password
              }
            })
              .then(response => {
                this.playlist = response.data;
                this.playlistIndex = 0;
                if (this.playlist.length === 0) {
                	return;
                }
        		this.updateSid(this.playlist[this.playlistIndex]);
              })
              .finally(() => (this.loadingPl = false));
          },
          fetchFilters: function() {
              this.loadingCfg = true; //the loading begin
              axios({
                method: "get",
                url:
                  "/jsidplay2service/JSIDPlay2REST/filters",
                auth: {
                  username: this.username,
                  password: this.password
                }
              })
                .then(response => {
                	const filters = response.data;
                	this.filtersResid6581 = filters.filter(filter => filter.startsWith('RESID_MOS6581_')).map(filter => filter.substring('RESID_MOS6581_'.length));
                	this.filtersResid8580 = filters.filter(filter => filter.startsWith('RESID_MOS8580_')).map(filter => filter.substring('RESID_MOS8580_'.length));
                	this.filtersResidFp6581 = filters.filter(filter => filter.startsWith('RESIDFP_MOS6581_')).map(filter => filter.substring('RESIDFP_MOS6581_'.length));
                	this.filtersResidFp8580 = filters.filter(filter => filter.startsWith('RESIDFP_MOS8580_')).map(filter => filter.substring('RESIDFP_MOS8580_'.length));

                	this.updateFilters(this.defaultEngine);
                })
                .finally(() => (this.loadingCfg = false));
            }
  }
});

// prevent back button
history.pushState(null, null, document.URL);
window.addEventListener("popstate", function() {
  history.pushState(null, null, document.URL);
});
</script>
</body>
</html>
