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

<!-- Load polyfills to support older browsers -->
<script
	src="https://polyfill.io/v3/polyfill.min.js?features=es2015%2CIntersectionObserver"
	crossorigin="anonymous"></script>

<!-- Load Vue followed by BootstrapVue -->
<script src="https://cdn.jsdelivr.net/npm/vue@2.6.0"></script>
<script
	src="https://unpkg.com/bootstrap-vue@latest/dist/bootstrap-vue.min.js"></script>

<!-- Load the following for BootstrapVueIcons support -->
<script
	src="https://unpkg.com/bootstrap-vue@latest/dist/bootstrap-vue-icons.min.js"></script>

<script src="https://unpkg.com/axios/dist/axios.min.js"></script>

<script src="https://unpkg.com/vue-i18n@8"></script>

<meta charset="UTF-8" />
<title>C64 Jukebox</title>

</head>
<body>
	<h1>C64 Jukebox</h1>

	<div id="app">

		<form>

			<div>
				<b-card no-body> <b-tabs v-model="tabIndex" card>
				<b-tab title="CON" active> <b-card-text>
				<div>
					<h3>{{ $t( 'authentication' ) }}</h3>
					<label for="username">{{ $t( 'username' ) }}</label>
					<input type="text" id="username" v-model="username">
					<label for="password">{{ $t( 'password' ) }}</label>
					<input type="password" id="password" v-model="password">
				</div>

				</b-card-text> </b-tab> <b-tab title="SIDS">
				<div v-if="loading">
					<!-- here put a spinner or whatever you want to indicate that a request is in progress -->
					<div class="loader"></div>
				</div>
				<div v-else>
					<!-- request finished -->
					<ul>
						<li v-for="entry in directory" :key="entry">
							<!-- HVSC root -->
							<div v-if="entry.endsWith('/')">
								<a href="#" v-on:click="fetchData(entry)"> {{entry}} </a>
							</div> <!-- HVSC music -->
							<div
								v-else-if="entry.endsWith('.sid') || entry.endsWith('.dat') || entry.endsWith('.mus') || entry.endsWith('.str')">
								<div>
									<a href="#"
										v-on:click="setSid(entry); setPic(entry); tabIndex = 2;">
										{{entry}} </a>
								</div>
							</div> <!-- others -->
							<div v-else>
								<div style="white-space: nowrap;">
									{{entry}} <a v-bind:href="convert(entry)" target="_blank">
										Load </a> <span v-if='entry.toLowerCase().endsWith(".d64")'>
										<span> or </span> <a
										v-bind:href="convert(entry) + '&jiffydos=true'"
										target="_blank"> Fastload </a>
									</span>
								</div>
							</div>
						</li>
					</ul>
				</div>

				</b-tab> <b-tab title="SID"> <b-button
					v-on:click="$refs.audioElm.src=convert(currentSid); $refs.audioElm.play()">{{
				$t( 'play' ) }}</b-button> <b-button
					v-on:click="playlist.push(currentSid); tabIndex = 3; currentPlaylistEntry = 0;">{{
				$t( 'addToPlaylist' ) }}</b-button>

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
				</b-tab> <b-tab title="PL"> <b-card-text>

				<div>
					<div class="button-box">
						<b-button v-on:click="playlist.pop()">{{ $t( 'remove' )
						}}</b-button>
						<b-button v-on:click="nextPlaylistEntry">{{ $t( 'next'
						) }}</b-button>
						<label for="random">{{ $t( 'random' ) }}</label>
						<input type="checkbox" id="random" v-model="random" />
					</div>
					<div class="button-box">
						<b-button v-on:click="downloadPlaylist()">{{ $t(
						'downloadPlaylist' ) }}</b-button>
						<b-button v-on:click="playlist=[]">{{ $t(
						'removePlaylist' ) }}</b-button>
					</div>
				</div>

				<div class="audio">
					<audio ref="audioElm" v-bind:src="current"
						v-on:ended="nextPlaylistEntry" type="audio/mpeg" controls autoplay>
						I'm sorry. You're browser doesn't support HTML5 audio
					</audio>
					<div>{{playlist[currentPlaylistEntry]}}</div>
				</div>

				<ol>
					<li v-for="(entry,index) in playlist" :key="index"><a
						:class="index==currentPlaylistEntry ? 'highlighted' : ''" href="#"
						v-on:click="play(index);">{{entry}} </a></li>
				</ol>

				</b-card-text> </b-tab> <b-tab title="CFG"> <b-card-text>

				<div>
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
						<input type="checkbox" id="detectSongLength"
							v-model="detectSongLength" />
						<label for="detectSongLength">{{ $t( 'detectSongLength' )
							}}</label>
						<input type="checkbox" id="singleSong" v-model="singleSong" />
						<label for="singleSong">{{ $t( 'singleSong' ) }}</label>
						<input type="checkbox" id="loopSong" v-model="loopSong" />
						<label for="loopSong">{{ $t( 'loopSong' ) }}</label>
					</div>
					<div class="settings-box">
						<input type="checkbox" id="digiboost8580" v-model="digiboost8580" />
						<label for="digiboost8580">{{ $t( 'digiboost8580' ) }}</label>
						<input type="checkbox" id="fakeStereo" v-model="fakeStereo" />
						<label for="fakeStereo">{{ $t( 'fakeStereo' ) }}</label>
						<input type="checkbox" id="bypassReverb" v-model="bypassReverb" />
						<label for="bypassReverb">{{ $t( 'bypassReverb' ) }}</label>
					</div>

					<div class="settings-box">
						<div>
							<span>{{ $t( 'muteSid' ) }}</span>
						</div>
						<div>
							<input type="checkbox" id="sidMuteVoice1" v-model="sidMuteVoice1" />
							<label for="sidMuteVoice1">{{ $t( 'sidMuteVoice1' ) }}</label>
							<input type="checkbox" id="sidMuteVoice2" v-model="sidMuteVoice2" />
							<label for="sidMuteVoice2">{{ $t( 'sidMuteVoice2' ) }}</label>
							<input type="checkbox" id="sidMuteVoice3" v-model="sidMuteVoice3" />
							<label for="sidMuteVoice3">{{ $t( 'sidMuteVoice3' ) }}</label>
							<input type="checkbox" id="sidMuteSamples"
								v-model="sidMuteSamples" />
							<label for="sidMuteSamples">{{ $t( 'sidMuteSamples' ) }}</label>
						</div>
						<div>
							<span>{{ $t( 'muteStereoSid' ) }}</span>
						</div>
						<div>
							<input type="checkbox" id="stereoSidMuteVoice1"
								v-model="stereoSidMuteVoice1" />
							<label for="stereoSidMuteVoice1">{{ $t( 'sidMuteVoice1' )
								}}</label>
							<input type="checkbox" id="stereoSidMuteVoice2"
								v-model="stereoSidMuteVoice2" />
							<label for="stereoSidMuteVoice2">{{ $t( 'sidMuteVoice2' )
								}}</label>
							<input type="checkbox" id="stereoSidMuteVoice3"
								v-model="stereoSidMuteVoice3" />
							<label for="stereoSidMuteVoice3">{{ $t( 'sidMuteVoice3' )
								}}</label>
							<input type="checkbox" id="stereoSidMuteSamples"
								v-model="stereoSidMuteSamples" />
							<label for="stereoSidMuteSamples">{{ $t( 'sidMuteSamples'
								) }}</label>
						</div>
						<div>
							<span>{{ $t( 'muteThreeSid' ) }}</span>
						</div>
						<div>
							<input type="checkbox" id="threeSidMuteVoice1"
								v-model="threeSidMuteVoice1" />
							<label for="threeSidMuteVoice1">{{ $t( 'sidMuteVoice1' )
								}}</label>
							<input type="checkbox" id="threeSidMuteVoice2"
								v-model="threeSidMuteVoice2" />
							<label for="threeSidMuteVoice2">{{ $t( 'sidMuteVoice2' )
								}}</label>
							<input type="checkbox" id="threeSidMuteVoice3"
								v-model="threeSidMuteVoice3" />
							<label for="threeSidMuteVoice3">{{ $t( 'sidMuteVoice3' )
								}}</label>
							<input type="checkbox" id="threeSidMuteSamples"
								v-model="threeSidMuteSamples" />
							<label for="threeSidMuteSamples">{{ $t( 'sidMuteSamples'
								) }}</label>
						</div>
					</div>

					<div class="settings-box">
						<input type="radio" id="RESIDFP" value="RESIDFP"
							v-model="defaultEngine" v-on:click="setFilters('RESIDFP')">
						<label for="RESIDFP">RESIDFP</label>
						<input type="radio" id="RESID" value="RESID"
							v-model="defaultEngine" v-on:click="setFilters('RESID')">
						<label for="RESID">RESID</label>
					</div>
					<div class="settings-box">
						<input type="radio" id="DECIMATE" value="DECIMATE"
							v-model="samplingMethod">
						<label for="DECIMATE">DECIMATE</label>
						<input type="radio" id="RESAMPLE" value="RESAMPLE"
							v-model="samplingMethod">
						<label for="RESAMPLE">RESAMPLE</label>
					</div>
					<div class="settings-box">
						<input type="radio" id="LOW" value="LOW" v-model="samplingRate">
						<label for="LOW">LOW</label>
						<input type="radio" id="MEDIUM" value="MEDIUM"
							v-model="samplingRate">
						<label for="MEDIUM">MEDIUM</label>
						<input type="radio" id="HIGH" value="HIGH" v-model="samplingRate">
						<label for="HIGH">HIGH</label>
					</div>
					<div class="settings-box">
						<input type="radio" id="MOS6581" value="MOS6581"
							v-model="defaultModel">
						<label for="MOS6581">6581</label>
						<input type="radio" id="MOS8580" value="MOS8580"
							v-model="defaultModel">
						<label for="MOS8580">8580</label>
					</div>
				</div>
				<div class="settings-box">
					<div>
						<label for="filter6581">{{ $t( 'filter6581' ) }}</label> <select
							id="filter6581" v-model="filter6581">
							<option v-for="entry in filters6581">{{entry}}</option>
						</select> <label for="filter8580">{{ $t( 'filter8580' ) }}</label> <select
							id="filter8580" v-model="filter8580">
							<option v-for="entry in filters8580">{{entry}}</option>
						</select>
					</div>
					<div>
						<label for="stereoFilter6581">{{ $t( 'stereoFilter6581' )
							}}</label> <select id="stereoFilter6581" v-model="stereoFilter6581">
							<option v-for="entry in filters6581">{{entry}}</option>
						</select> <label for="stereoFilter8580">{{ $t( 'stereoFilter8580' )
							}}</label> <select id="stereoFilter8580" v-model="stereoFilter8580">
							<option v-for="entry in filters8580">{{entry}}</option>
						</select>
					</div>
					<div>
						<label for="threeFilter6581">{{ $t( 'threeFilter6581' ) }}</label>
						<select id="threeFilter6581" v-model="threeFilter6581">
							<option v-for="entry in filters6581">{{entry}}</option>
						</select> <label for="threeFilter8580">{{ $t( 'threeFilter8580' )
							}}</label> <select id="threeFilter8580" v-model="threeFilter8580">
							<option v-for="entry in filters8580">{{entry}}</option>
						</select>
					</div>
				</div>
				<div class="settings-box">
					<div>
						<label for="volumeSid">{{ $t( 'volumeSid' ) }}</label> <span>{{
							volumeSid }}db</span>
						<b-form-input id="volumeSid" v-model="volumeSid" type="range"
							min="-6" max="6" step="1"></b-form-input>
					</div>
					<div>
						<label for="volumeStereoSid">{{ $t( 'volumeStereoSid' ) }}</label>
						<span>{{ volumeStereoSid }}db</span>
						<b-form-input id="volumeStereoSid" v-model="volumeStereoSid"
							type="range" min="-6" max="6" step="1"></b-form-input>
					</div>
					<div>
						<label for="volumeThreeSid">{{ $t( 'volumeThreeSid' ) }}</label> <span>{{
							volumeThreeSid }}db</span>
						<b-form-input id="volumeThreeSid" v-model="volumeThreeSid"
							type="range" min="-6" max="6" step="1"></b-form-input>
					</div>
				</div>
				<div class="settings-box">
					<div>
						<label for="balanceSid">{{ $t( 'balanceSid' ) }}</label> <span>{{
							balanceSid }}</span>
						<b-form-input id="balanceSid" v-model="balanceSid" type="range"
							min="0" max="1" step="0.1"></b-form-input>
					</div>
					<div>
						<label for="balanceStereoSid">{{ $t( 'balanceStereoSid' )
							}}</label> <span>{{ balanceStereoSid }}</span>
						<b-form-input id="balanceStereoSid" v-model="balanceStereoSid"
							type="range" min="0" max="1" step="0.1"></b-form-input>
					</div>
					<div>
						<label for="balanceThreeSid">{{ $t( 'balanceThreeSid' ) }}</label>
						<span>{{ balanceThreeSid }}</span>
						<b-form-input id="balanceThreeSid" v-model="balanceThreeSid"
							type="range" min="0" max="1" step="0.1"></b-form-input>
					</div>
				</div>
				<div class="settings-box">
					<div>
						<label for="delaySid">{{ $t( 'delaySid' ) }}</label> <span>{{
							delaySid }}ms</span>
						<b-form-input id="delaySid" v-model="delaySid" type="range"
							min="0" max="100" step="10"></b-form-input>
					</div>
					<div>
						<label for="delayStereoSid">{{ $t( 'delayStereoSid' ) }}</label> <span>{{
							delayStereoSid }}ms</span>
						<b-form-input id="delayStereoSid" v-model="delayStereoSid"
							type="range" min="0" max="100" step="10"></b-form-input>
					</div>
					<div>
						<label for="delayThreeSid">{{ $t( 'delayThreeSid' ) }}</label> <span>{{
							delayThreeSid }}ms</span>
						<b-form-input id="delayThreeSid" v-model="delayThreeSid"
							type="range" min="0" max="100" step="10"></b-form-input>
					</div>
				</div>
				<div class="settings-box">
					<div>
						<label for="vbr">{{ $t ( 'vbr' ) }}</label>
						<input type="checkbox" id="vbr" v-model="vbr" />
					</div>
					<div>
						<label for="cbr">{{ $t ( 'cbr' ) }}</label> <select id="cbr"
							v-model="cbr">
							<option>-1</option>
							<option>32</option>
							<option>40</option>
							<option>48</option>
							<option>56</option>
							<option>64</option>
							<option>80</option>
							<option>96</option>
							<option>112</option>
							<option>128</option>
							<option>160</option>
							<option>192</option>
							<option>224</option>
							<option>256</option>
							<option>320</option>
						</select>
					</div>
					<div>
						<label for="vbrQuality">{{ $t ( 'vbrQuality' ) }}</label> <select
							id="vbrQuality" v-model="vbrQuality">
							<option>0</option>
							<option>1</option>
							<option>2</option>
							<option>3</option>
							<option>4</option>
							<option>5</option>
							<option>6</option>
							<option>7</option>
							<option>8</option>
							<option>9</option>
						</select>
					</div>
					<div>
						<label for="vcBitRate">{{ $t( 'vcBitRate' ) }}</label>
						<input type="number" min="0" oninput="validity.valid||(value='');"
							id="vcBitRate" v-model.number="vcBitRate" />
					</div>
				</div>
				<div class="settings-box">
					<div>
						<label for="status">Show status line</label>
						<input type="checkbox" id="status" v-model="status" />
					</div>
					<div>
						<label for="pressSpaceInterval">Press Space periodically
							in s</label>
						<input type="number" id="pressSpaceInterval"
							v-model.number="pressSpaceInterval" />
					</div>
				</div>
				<div class="settings-box">
					<input type="radio" id="auto" value="auto" v-model="reuSize">
					<label for="auto">Autodetect</label>
					<input type="radio" id="kb64" value="kb64" v-model="reuSize">
					<label for="kb64">64kb</label>
					<input type="radio" id="kb128" value="kb128" v-model="reuSize">
					<label for="kb128">128kb</label>
					<input type="radio" id="kb512" value="kb512" v-model="reuSize">
					<label for="kb512">512kb</label>
					<input type="radio" id="kb1024" value="kb1024" v-model="reuSize">
					<label for="kb1024">1024kb</label>
					<input type="radio" id="kb2048" value="kb2048" v-model="reuSize">
					<label for="kb2048">2048kb</label>
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
	  message: {
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
		  authentication: 'Authentication:',
		  username: 'Username',
		  password: 'Password',
		  play: 'Play',
		  addToPlaylist: 'Add To Playlist',
		  remove: 'Remove',
		  next: 'Next',
		  downloadPlaylist: 'Download Playlist',
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
		  vbr: 'Use variable bitrate instead of constant bitrate',
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
		  sidMuteSamples: 'Samples'
	    },
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
	  authentication: 'Anmeldedaten:',
	  username: 'Benutzername',
	  password: 'Passwort',
	  play: 'Abspielen',
	  addToPlaylist: 'Zu Favoriten hinzufügen',
	  remove: 'Löschen',
	  next: 'Nächster',
	  downloadPlaylist: 'Favoriten herunterladen',
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
	  vbr: 'Variable Bitrate verwenden anstatt fester Bitrate',
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
	  sidMuteSamples: 'Samples'
  }
}

const i18n = new VueI18n({
	  locale: 'de', // set locale
	  messages // set locale messages
	})
	
new Vue({
  el: "#app",
  i18n, //import mutil-lang
  data: {
	tabIndex: 0,
	directory: "",
	filtersResid6581: [],
	filtersResid8580: [],
	filtersResidFp6581: [],
	filtersResidFp8580: [],
	filters6581: [],
	filters8580: [],
	filter6581: '',
	filter8580: '',
	stereoFilter6581: '',
	stereoFilter8580: '',
	threeFilter6581: '',
	threeFilter8580: '',
	playlist: [],
	currentPlaylistEntry: 0,
    infos: "",
    picture: '',
    currentSid: '',
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
    defaultEngine: 'RESIDFP',
    samplingMethod: 'DECIMATE',
    samplingRate: 'MEDIUM',
    defaultModel: 'MOS8580',
    startTime: 0,
    defaultLength: 0,
    fadeIn: 0,
    fadeOut: 0,
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
    vbrQuality: 0,
	vcBitRate: 600000,
    reuSize: 'auto',
    pressSpaceInterval: 90,
    status: true,
    random: true,
    loading: false,
    username: "jsidplay2",
    password: "jsidplay2!"
  },
  computed: {
    reu: function() {
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
    },
    current: function() {
    	if (this.playlist.length === 0) {
    		return '';
    	} else {
			return this.convert(this.playlist[this.currentPlaylistEntry]);;
    	}
    }
  },
  created: function() {
    this.fetchData("/");
    this.fetchFilters();
  },
  methods: {
	play: function(index) {
	  this.currentPlaylistEntry=index;
	  this.setSid(this.playlist[index]);
	  this.setPic(this.playlist[index]);
    },
	nextPlaylistEntry: function () {
		if (this.playlist.length === 0) {
			return;
		}
		if (this.random) {
			this.currentPlaylistEntry = Math.ceil(Math.random()*this.playlist.length);
		} else {
		    if (this.currentPlaylistEntry === this.playlist.length - 1) {
		    	this.currentPlaylistEntry = 0;
		    } else {
		    	this.currentPlaylistEntry++;
		    }
		}
		this.setSid(this.playlist[this.currentPlaylistEntry]);
		this.setPic(this.playlist[this.currentPlaylistEntry]);
	},
    fetchData: function(entry) {
       this.loading = true; //the loading begin
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
         .finally(() => (this.loading = false));
    },
    setSid: function(entry) {
        this.loading = true; //the loading begin
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
          .finally(() => (this.loading = false));
      },
      setPic: function(entry) {
          this.loading = true; //the loading begin
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
            .finally(() => (this.loading = false));
        },
        downloadPlaylist: function() {
            this.loading = true; //the loading begin
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
                this.currentPlaylistEntry = 0;
              })
              .finally(() => (this.loading = false));
          },
          setFilters: function(engine) {
			if (engine==='RESIDFP') {
				this.filters6581 = this.filtersResidFp6581;
			 	this.filters8580 = this.filtersResidFp8580;

				this.filter6581 = this.filters6581[1];
				this.filter8580 = this.filters8580[1];
				this.stereoFilter6581 = this.filters6581[1];
				this.stereoFilter8580 = this.filters8580[1];
				this.threeFilter6581 = this.filters6581[1];
				this.threeFilter8580 = this.filters8580[1];
			} else {
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
          fetchFilters: function() {
              this.loading = true; //the loading begin
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

                	this.setFilters(this.defaultEngine);
                })
                .finally(() => (this.loading = false));
            },
        convert: function(entry) {
        	return window.location.protocol + '//' + this.username + ':' + this.password + '@' + window.location.host + '/jsidplay2service/JSIDPlay2REST/convert' + uriEncode(entry)
        		+ '?enableSidDatabase=' + this.detectSongLength + '&single=' + this.singleSong + '&loop=' + this.loopSong
        		+ '&muteVoice1=' + this.sidMuteVoice1 + '&muteVoice2=' + this.sidMuteVoice2 + '&muteVoice3=' + this.sidMuteVoice3 + '&muteVoice4=' + this.sidMuteSamples
        		+ '&muteStereoVoice1=' + this.stereoSidMuteVoice1 + '&muteStereoVoice2=' + this.stereoSidMuteVoice2 + '&muteStereoVoice3=' + this.stereoSidMuteVoice3 + '&muteStereoVoice4=' + this.stereoSidMuteSamples
        		+ '&muteThirdSidVoice1=' + this.threeSidMuteVoice1 + '&muteThirdSidVoice2=' + this.threeSidMuteVoice2 + '&muteThirdSidVoice3=' + this.threeSidMuteVoice3 + '&muteThirdSidVoice4=' + this.threeSidMuteSamples
        		+ '&bufferSize=65536&sampling=' + this.samplingMethod + '&frequency=' + this.samplingRate
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
        		+ '&pressSpaceInterval='+this.pressSpaceInterval+'&status='+this.status+this.reu;
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
