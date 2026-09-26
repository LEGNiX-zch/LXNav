# LXNav
Lightweight web browser built for Android wearable watches.
Liquid glass UI, custom dynamic island with stacked tab multitask.

## Features
- DynamicIsland: single tap cycle info(time / memory / page info). Long press trigger stacked tab manager.
- Tab card gesture: scroll up/down to browse cards; swipe left/right to close tab; click to switch webpage.
- Left side drawer toolbar, slide in from left, half width panel. Contains forward, back, refresh, new tab, bookmark.
- WebView engine, forward / back / refresh
- Bookmark collection
- Settings: max tab count, theme, page zoom, UA toggle, animation switch, cache clear

## Screen Adapt
Auto detect watch screen shape (square / round).
Prioritize square screen (OPPO Watch Gen1).
Round screen auto add safe padding to prevent UI cut off.

## Support Device
Android 8.1+ smart watch, OPPO Watch Gen1 (46mm) recommended

## Build
Project written in Kotlin, can compile in Trae / Android Studio.

## Notes
This project is for learning purpose.
Old watch has limited RAM, keep tabs 2~3 to avoid crash.
Android8.1 WebView version is old, some modern websites may render abnormally.

## License
MIT
