<!DOCTYPE html>
<html>
  <head>
    <!-- favicon.ico -->
    <link rel="shortcut icon" href="/static/favicon.ico" type="image/x-icon" />
    <link id="favicon" rel="icon" href="/static/favicon.ico" type="image/x-icon" />
    <link id="favicon-16x16" rel="icon" href="/static/favicon-16x16.png" type="image/png" sizes="16x16" />

    <!-- Load Vue -->
    <script src="/webjars/vue/3.4.21/dist/vue.global${prod}.js"></script>
    <!-- helpers -->
    <script src="/webjars/axios/1.5.1/dist/axios${min}.js"></script>

    <!-- disable pull reload -->
    <style>
      html,
      body {
        overscroll-behavior: none;
      }
    </style>

    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />

    <title>WhatsSID</title>
  </head>
  <body>
    <h1>WhatsSID? Music Recognition</h1>

    <div id="app">
      <p>Upload a WAV File to detect song</p>
      <form enctype="multipart/form-data">
        <input type="file" name="file" v-on:change="fileChange($event.target.files)" />
        <button type="button" v-on:click="upload()">Upload</button>
      </form>
      <div>
        <p>{{ match }}</p>
      </div>
    </div>

    <script>
      const { createApp, ref } = Vue;

      let app = Vue.createApp({
        data: function () {
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
                this.match =
                  error.response && error.response.data
                    ? error.response.data
                    : error.name + " (" + error.code + "): " + error.message;
              }
            );
          },
        },
      }).mount("#app");
    </script>
  </body>
</html>
