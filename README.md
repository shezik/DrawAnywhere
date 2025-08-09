# <image src="SVG Icon/pen-new-square-original.svg" style="width: 2em; height: 2em; vertical-align: middle;" /> DrawAnywhere

English | [中文](README-zh-CN.md)

DrawAnywhere is an Android application that enables you to draw over other apps.

![](docs/title.png)

## 🎨 Features
- Pretty toolbar UI based on Jetpack Compose, available in both horizontal and vertical mode
- Stroke eraser, can be activated via stylus buttons
- Undo and redo up to 50 operations
- Hide canvas, or pass down touch events to the app below
- It's free ~~real estate~~ software!

## ✨ Tips
- Long press and drag to move the toolbar around.
- After 3 seconds of inactivity, the toolbar becomes 50% transparent.
- Turn on `Clear on hiding canvas` to let the app clear the canvas when you hide it.<br>
Note: Touch passthrough will be turned off automatically as well.
- Turn off `Canvas visible on start` to hide the canvas on startup by default.

This app is in its early stage, feel free to open an issue if you encounter problems!

## 💌 Shoutouts
to [Akshay Sharma](https://github.com/akshay2211)'s [DrawBox](https://github.com/akshay2211/DrawBox) for inspirations,<br>
[480 Design](https://www.figma.com/@480design) and [R4IN80W](https://www.figma.com/@voidrainbow)'s [Solar Icons Set](https://www.figma.com/community/file/1166831539721848736/solar-icons-set) for the astounding app icon ([CC BY 4.0](SVG%20Icon/LICENSE.md)),<br>
and [Mauro Banze](https://stackoverflow.com/a/66958772) & [Yannick](https://stackoverflow.com/a/65760080) for their Stack Overflow answers ([CC BY-SA 4.0](app/src/main/java/com/shezik/drawanywhere/CustomLifecycleOwner.kt#L3))!

Finally, thank [you](https://play.google.com/store/apps/details?id=com.kts.draw) for making your app subscription-based! You are my original motivation![^1]<br>
<sub>i'll stop before starting to sound like yes man</sub>

[^1]: Despite being a large factor, this is only partially true. The other reason is that tablets running OneUI 4 do not come with such functionalities. If you are using OneUI 4 like I do, be sure to check out [Wallpaper Setter](https://github.com/shezik/WallpaperSetter)!
