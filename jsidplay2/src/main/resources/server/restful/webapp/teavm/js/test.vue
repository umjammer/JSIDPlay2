<!DOCTYPE html>
<html>
  <head>
    <title>TeaVM example</title>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
    <script type="text/javascript" charset="utf-8" src="/static/teavm/js/jsidplay2.js"></script>
    <script>
      var AudioContext = window.AudioContext || window.webkitAudioContext;
      var audioContext;
      var nextTime;

      audioContext = new AudioContext({
        latencyHint: "interactive",
        sampleRate: 48000,
      });
      nextTime = 0;

      function clock() {
        main.api.clock();
        setTimeout(() => clock(), 0);
      }
      function processSamples(resultL, resultR, length) {
        var buffer = audioContext.createBuffer(2, length, audioContext.sampleRate);
        buffer.getChannelData(0).set(resultL);
        buffer.getChannelData(1).set(resultR);

        var sourceNode = audioContext.createBufferSource();
        sourceNode.buffer = buffer;
        sourceNode.connect(audioContext.destination);

        if (nextTime == 0) {
          audioContext.close();
          audioContext = new AudioContext({
            latencyHint: "interactive",
            sampleRate: 48000,
          });
          nextTime = audioContext.currentTime + 0.05; // add 50ms latency to work well across systems
        } else if (nextTime < audioContext.currentTime) {
          nextTime = audioContext.currentTime + 0.05; // if samples are not produced fast enough
        }
        sourceNode.start(nextTime);
        nextTime += length / audioContext.sampleRate;
      }
      function processPixels(pixels, length) {
        console.log("pixels: " + length);
      }
      function processSidWrite(relTime, addr, value) {
        console.log("write: relTime=" + relTime + ", addr=" + addr + ", value=" + value);
      }

      function startTune() {
        var element = document.getElementById("file");
        if (element.files[0]) {
          var reader = new FileReader();
          reader.onload = function () {
            main.api.open(new Uint8Array(this.result), element.files[0].name, 0, 0, false, null, null);
            clock();
          };
          reader.readAsArrayBuffer(element.files[0]);
        }
      }
    </script>
  </head>
  <body onload="main(new Array('true', '96000', '48000', '48000','false','true','50', 'true','false'));">
    <input id="file" type="file" oninput="startTune()" />

    <div id="sidplay2Section" palEmulation="true"></div>
    <div
      id="audioSection"
      samplingRate="48000"
      samplingMethodResample="false"
      reverbBypass="true"
      bufferSize="96000"
      audioBufferSize="48000"
    ></div>
    <div id="emulationSection" defaultSidModel8580="true" defaultClockSpeed="50"></div>
    <div id="c1541Section" jiffyDosInstalled="false"></div>
  </body>
</html>
