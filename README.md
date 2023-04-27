# GalleryLoader
* 갤러리 로더 입니다.
* 아래와 같이 빌더패턴을 이용해 GalleryLoader의 객체를 생성합니다.
```java
GalleryLoader loader = new GalleryLoader.Builder(MainActivity.this)
                        .setOnImageSelectedListener(onImageSelectedListener)
                        .create();
```
* 갤러리 로더 실행
``` java
loader.show(getSupportFragmentManager());
```

* 아래의 리스너를 통해 이미지의 URI가 콜백됩니다.
```java
private GalleryLoader.OnImageSelectedListener onImageSelectedListener = new GalleryLoader.OnImageSelectedListener() {
        @Override
        public void onImageSelected(Uri uri) {
		...
        }
    };
```
# 필요 권한
* Manifest.permission.WRITE_EXTERNAL_STORAGE


# 라이센스
 ```code
Copyright 2017 Karrel

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
