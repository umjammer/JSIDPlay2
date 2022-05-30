<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8" />
<title>HVSC</title>
</head>
<body>
	<!--script src="https://unpkg.com/vue"></script-->
	<script src="https://cdn.jsdelivr.net/npm/vue@2.6.0"></script>
	<script src="https://unpkg.com/axios/dist/axios.min.js"></script>

	<h1>C64 Jukebox</h1>

	<div id="app">
		<form>
			<div>
				<span>REU:</span>
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
			<div>
				<span>SID:</span>
				<input type="radio" id="MOS6581" value="MOS6581" v-model="defaultModel">
				<label for="MOS6581">6581</label>
				<input type="radio" id="MOS8580" value="MOS8580" v-model="defaultModel">
				<label for="MOS8580">8580</label>
			</div>

			<ul>
				<li v-for="entry in directory" :key="entry">
					<!-- HVSC root -->
					<div v-if="entry.endsWith('/')">
						<a href="#"
							v-on:click="fetchData('directory', entry)">
							{{entry}} </a>
					</div> <!-- HVSC music -->
					<div
						v-else-if="entry.endsWith('.sid') || entry.endsWith('.dat') || entry.endsWith('.mus') || entry.endsWith('.str')">
						<div>
							<a v-bind:href="'https://jsidplay2:jsidplay2!@haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/convert' + escape(entry).replace(/\+/g,'%2B') + '?enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel='+defaultModel+'&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&cbr=64&vbrQuality=0&vbr=true'" target="_blank">
								{{entry}}
							</a>
						</div>
					</div> <!-- others -->
					<div v-else>
						<div style="white-space: nowrap;">
							{{entry}}
							<a v-bind:href="'https://jsidplay2:jsidplay2!@haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/convert' + escape(entry).replace(/\+/g,'%2B') + '?enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel='+defaultModel+'&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true'+reu" target="_blank">
								Load
							</a>
							<span v-if='entry.toLowerCase().endsWith(".d64")'>
								<span> or </span>
								<a v-bind:href="'https://jsidplay2:jsidplay2!@haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/convert' + escape(entry).replace(/\+/g,'%2B') + '?enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel='+defaultModel+'&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&jiffydos=true'+reu" target="_blank">
									Fastload
								</a>
							</span>
						</div>
					</div>
				</li>
			</ul>
		</form>
	</div>

	<script>
	new Vue({
		el: '#app',
		data: {
			directory: '',
			imgData: [],
			defaultModel: 'MOS8580',
			reuSize: 'auto'
		},
		computed: {
			reu: function () {
				if (this.reuSize === 'kb64') {
					return '&reuSize=64';
				} else if (this.reuSize === 'kb128') {
					return '&reuSize=128';
				} else if (this.reuSize === 'kb512') {
					return '&reuSize=512';
				} else if (this.reuSize === 'kb1024') {
					return '&reuSize=1024';
				} else if (this.reuSize === 'kb2048') {
					return '&reuSize=2048';
				}
				return '';
			}
		},
		created: function () {
			this.fetchData('directory', '/');
		},        
		methods: {
			fetchData: function (type, entry) {
				if (type == 'directory') {
					axios({
						method: 'get',
						url: '/jsidplay2service/JSIDPlay2REST/' + type + escape(entry).replace(/\+/g,'%2B') + '?filter=.*%5C.(sid%7Cdat%7Cmus%7Cstr%7Cmp3%7Cmp4%7Cdv%7Cvob%7Ctxt%7Cjpg%7Cprg%7Cd64%7Cg64%7Cnib%7Creu%7Cima%7Ccrt%7Cimg%7Ctap%7Ct64%7Cp00)$',
						auth: {
						  username: 'jsidplay2',
						  password: 'jsidplay2!'
						}
					}).then(response => {
						this.directory= response.data;
					})
				}
			}
		}
	})
	// prevent back button
	history.pushState(null, null, document.URL);
	window.addEventListener('popstate', function () {
	    history.pushState(null, null, document.URL);
	});
</script>
</body>
</html>


