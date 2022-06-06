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
				<b-card no-body> <b-tabs card> <b-tab title="CON"
					active> <b-card-text>
				<div>
					<h3>Authentication:</h3>
					<label for="username">Username</label> <input type="text"
						id="username" value="jsidplay2" v-model="username"> <label
						for="password">Password</label> <input type="password"
						id="password" value="jsidplay2!" v-model="password">
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
								<a href="#"
									v-on:click="fetchData('directory', entry, username, password)">
									{{entry}} </a>
							</div> <!-- HVSC music -->
							<div
								v-else-if="entry.endsWith('.sid') || entry.endsWith('.dat') || entry.endsWith('.mus') || entry.endsWith('.str')">
								<div>
									<a href="#"
										v-on:click="setSid(entry, username, password);setPic(entry, username, password)">
										{{entry}} </a>
									<!--a
										v-bind:href="'https://' + username + ':' + password + '@haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/convert' + uriEncode(entry) + '?enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel='+defaultModel+'&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&cbr=64&vbrQuality=0&vbr=true'"
										target="_blank"> {{entry}} </a-->
								</div>
							</div> <!-- others -->
							<div v-else>
								<div style="white-space: nowrap;">
									{{entry}} <a
										v-bind:href="'https://' + username + ':' + password + '@haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/convert' + uriEncode(entry) + '?enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel='+defaultModel+'&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&pressSpaceInterval='+pressSpaceInterval+'&status='+status+reu"
										target="_blank"> Load </a> <span
										v-if='entry.toLowerCase().endsWith(".d64")'> <span>
											or </span> <a
										v-bind:href="'https://' + username + ':' + password + '@haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/convert' + uriEncode(entry) + '?enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel='+defaultModel+'&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&pressSpaceInterval='+pressSpaceInterval+'&status='+status+'&jiffydos=true'+reu"
										target="_blank"> Fastload </a>
									</span>
								</div>
							</div>
						</li>
					</ul>
				</div>

				</b-tab> <b-tab title="SID">

				<b-button v-on:click="playSid(currentSid, username, password)">Play</b-button>

				<img :src="picture" id="img">

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
				</b-tab> <b-tab title="CFG"> <b-card-text>

				<div>
					<h3>SID:</h3>
					<input type="radio" id="MOS6581" value="MOS6581"
						v-model="defaultModel"> <label for="MOS6581">6581</label>
					<input type="radio" id="MOS8580" value="MOS8580"
						v-model="defaultModel"> <label for="MOS8580">8580</label>
				</div>
				<div>
					<h3>REU:</h3>
					<input type="radio" id="auto" value="auto" v-model="reuSize">
					<label for="auto">Autodetect</label> <input type="radio" id="kb64"
						value="kb64" v-model="reuSize"> <label for="kb64">64kb</label>
					<input type="radio" id="kb128" value="kb128" v-model="reuSize">
					<label for="kb128">128kb</label> <input type="radio" id="kb512"
						value="kb512" v-model="reuSize"> <label for="kb512">512kb</label>
					<input type="radio" id="kb1024" value="kb1024" v-model="reuSize">
					<label for="kb1024">1024kb</label> <input type="radio" id="kb2048"
						value="kb2048" v-model="reuSize"> <label for="kb2048">2048kb</label>
				</div>
				<div>
					<h3>Misc.:</h3>
					<label for="status">Show status line</label> <input type="checkbox"
						id="status" v-model="status" /> <label for="pressSpaceInterval">Press
						Space periodically in s</label> <input type="number"
						id="pressSpaceInterval" v-model.number="pressSpaceInterval" />
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
		  }
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
	  }
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
	directory: "",
    infos: "",
    picture: '',
    imgData: [],
    currentSid: '',
    defaultModel: "MOS8580",
    reuSize: "auto",
    pressSpaceInterval: 90,
    status: true,
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
    }
  },
  created: function() {
    this.fetchData("directory", "/", this.username, this.password);
  },
  methods: {
    fetchData: function(type, entry, username, password) {
      if (type == "directory") {
        this.loading = true; //the loading begin
        axios({
          method: "get",
          url:
            "/jsidplay2service/JSIDPlay2REST/" +
            type +
            uriEncode(entry) +
            "?filter=.*%5C.(sid%7Cdat%7Cmus%7Cstr%7Cmp3%7Cmp4%7Cdv%7Cvob%7Ctxt%7Cjpg%7Cprg%7Cd64%7Cg64%7Cnib%7Creu%7Cima%7Ccrt%7Cimg%7Ctap%7Ct64%7Cp00)$",
          auth: {
            username: username,
            password: password
          }
        })
          .then(response => {
            this.directory = response.data;
          })
          .finally(() => (this.loading = false));
      }
    },
    setSid: function(entry, username, password) {
        this.loading = true; //the loading begin
        axios({
          method: "get",
          url:
            "/jsidplay2service/JSIDPlay2REST/info" +
            uriEncode(entry),
          auth: {
            username: username,
            password: password
          }
        })
          .then(response => {
            this.infos = response.data;
            this.currentSid = entry;
          })
          .finally(() => (this.loading = false));
      },
      setPic: function(entry, username, password) {
          this.loading = true; //the loading begin
          axios({
            method: "get",
            url:
              "/jsidplay2service/JSIDPlay2REST/photo" + uriEncode(entry),
              auth: {
              username: username,
              password: password
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
        playSid: function(entry, username, password) {
        	window.open('https://' + username + ':' + password + '@haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/convert' + uriEncode(entry) + '?enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel='+this.defaultModel+'&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&cbr=64&vbrQuality=0&vbr=true');
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
