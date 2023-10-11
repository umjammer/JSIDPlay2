<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <title>WhatsSID</title>
  </head>
  <body>
    <!-- favicon.ico -->
    <link rel="shortcut icon" href="/static/favicon.ico" type="image/x-icon" />
    <link id="favicon" rel="icon" href="/static/favicon.ico" type="image/x-icon" />
    <link id="favicon-16x16" rel="icon" href="/static/favicon-16x16.png" type="image/png" sizes="16x16" />

    <script src="/webjars/vue/2.7.14/dist/vue$min.js"></script>
    <script src="/webjars/axios/0.27.2/dist/axios$min.js"></script>

    <h1>WhatsSID? Music Recognition</h1>

    <div id="app">
      <p>Upload a WAV File to detect song</p>
      <form enctype="multipart/form-data">
        <input type="file" name="file" v-on:change="fileChange($event.target.files)" />
        <button type="button" v-on:click="upload()">Upload</button>
      </form>
      <div>
        <p v-html="match"></p>
      </div>
    </div>

    <script>
      new Vue({
        el: "#app",
        data() {
          return {
            match: "",
            files: new FormData(),
          };
        },
        methods: {
          fileChange(fileList) {
            this.files = new FormData();
            this.files.append("file", fileList[0], fileList[0].name);
            this.match = "";
          },
          upload() {
            this.match = "Please wait...";
            axios({
              method: "post",
              url: "/jsidplay2service/JSIDPlay2REST/whatssid",
              data: this.files,
              auth: {
                username: "jsidplay2",
                password: "jsidplay2!",
              },
            }).then(
              (result) => {
                if (result.data && result.headers["content-length"]) {
                  this.match = result.data;
                } else {
                  this.match = "Sorry, no match!";
                }
              },
              (error) => {
        	    let result = error.response.data;
                this.match = result? result : error.message;
              }
            );
          },
        },
      });
    </script>
  </body>
</html>
