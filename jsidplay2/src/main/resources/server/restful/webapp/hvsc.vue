<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8" />
<title>HVSC</title>

<style lang="scss" scoped>
.loader {
  border: 16px solid #f3f3f3;
  border-top: 16px solid #3498db;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>

</head>
<body>
	<!--script src="https://unpkg.com/vue"></script-->
	<script src="https://cdn.jsdelivr.net/npm/vue@2.6.0"></script>
	<script src="https://unpkg.com/axios/dist/axios.min.js"></script>

	<h1>C64 Jukebox</h1>

	<div id="app">

		<form>
			<div>
				<h3>SID:</h3>
				<input type="radio" id="MOS6581" value="MOS6581" v-model="defaultModel">
				<label for="MOS6581">6581</label>
				<input type="radio" id="MOS8580" value="MOS8580" v-model="defaultModel">
				<label for="MOS8580">8580</label>
			</div>
			<div>
				<h3>REU:</h3>
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
				<h3>Misc.:</h3>
				<label for="status">Show status line</label>
				<input type="checkbox" id="status" v-model="status"/>
				<label for="pressSpaceInterval">Press Space periodically in s</label>
				<input type="number" id="pressSpaceInterval" v-model.number="pressSpaceInterval"/>
			</div>
	
			<div v-if="loading" class="loader">
	            <!-- here put a spinner or whatever you want to indicate that a request is in progress -->
	        </div>
	
	        <div v-else>
	            <!-- request finished -->
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
								<a v-bind:href="'https://jsidplay2:jsidplay2!@haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/convert' + uriEncode(entry) + '?enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel='+defaultModel+'&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&cbr=64&vbrQuality=0&vbr=true'" target="_blank">
									{{entry}}
								</a>
							</div>
						</div> <!-- others -->
						<div v-else>
							<div style="white-space: nowrap;">
								{{entry}}
								<a v-bind:href="'https://jsidplay2:jsidplay2!@haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/convert' + uriEncode(entry) + '?enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel='+defaultModel+'&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&pressSpaceInterval='+pressSpaceInterval+'&status='+status+reu" target="_blank">
									Load
								</a>
								<span v-if='entry.toLowerCase().endsWith(".d64")'>
									<span> or </span>
									<a v-bind:href="'https://jsidplay2:jsidplay2!@haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/convert' + uriEncode(entry) + '?enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel='+defaultModel+'&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&pressSpaceInterval='+pressSpaceInterval+'&status='+status+'&jiffydos=true'+reu" target="_blank">
										Fastload
									</a>
								</span>
							</div>
						</div>
					</li>
				</ul>
	        </div>
		</form>

	</div>

	<script>

	function uriEncode(entry) {
		// escape is deprecated and cannot handle utf8
		// encodeURI() will not encode: ~!@#$&*()=:/,;?+'
		// untested: !*=/,;?
		// tested: ~@#$&():+''
		return encodeURI(entry).replace(/\+/g,'%2B').replace(/#/g,'%23');
	}
	
	new Vue({
		el: '#app',
		data: {
			directory: '',
			imgData: [],
			defaultModel: 'MOS8580',
			reuSize: 'auto',
			pressSpaceInterval: 90,
			status: true,
			loading: false
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
					this.loading = true //the loading begin
					axios({
						method: 'get',
						url: '/jsidplay2service/JSIDPlay2REST/' + type + uriEncode(entry) + '?filter=.*%5C.(sid%7Cdat%7Cmus%7Cstr%7Cmp3%7Cmp4%7Cdv%7Cvob%7Ctxt%7Cjpg%7Cprg%7Cd64%7Cg64%7Cnib%7Creu%7Cima%7Ccrt%7Cimg%7Ctap%7Ct64%7Cp00)$',
						auth: {
						  username: 'jsidplay2',
						  password: 'jsidplay2!'
						}
					}).then(response => {
						this.directory= response.data;
					}).finally(() => (this.loading = false))
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
