JanusRTCAndroid
===============================

Video chat sample app using videoroom plugin on [janus-gateway](https://github.com/meetecho/janus-gateway) server and WebRTC.

Communication between [janus-gateway](https://github.com/meetecho/janus-gateway) and app is proceeded over http/https protocol using [OkHttp3](https://github.com/square/okhttp/tree/master/okhttp/src/main/java/okhttp3) and [Retrofit2](https://github.com/square/retrofit).

Copyright (c) 2018 saki t_saki@serenegiant.com

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

This repository only support videoroom plugin on `janus-gateway` now.

This repository is based on [AppRTCMobile](https://github.com/saki4510t/AppRTCMobile) repository that originally came from demo app of WebRTC libraries.

     https://github.com/saki4510t/AppRTCMobile

***Note: Files came from `AppRTCMobile` and original demo app from WebRTC libraries will have different license. Please see `LICENSE_WEBRTC` and `LICENSE_WEBRTC_THIRD_PARTY`.***

`janus` library module can handle multiple client but sample app can handle 1:1 connection only.