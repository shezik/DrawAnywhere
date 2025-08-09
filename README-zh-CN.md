# <image src="SVG Icon/pen-new-square-original.svg" style="width: 2em; height: 2em; vertical-align: middle;" /> DrawAnywhere

[English](README.md) | 中文

DrawAnywhere 是一个可以让你在屏幕的任意地方上绘图的安卓软件。

![](docs/title.png)

## 🎨 功能
- 基于 Jetpack Compose 的漂亮工具栏，支持横向和竖向布局
- 笔画橡皮，可由手写笔按钮激活
- 最多 50 步的撤销和重做
- 隐藏画布，以及操作画布下方的其他软件
- 免费~~的才是最贵的~~！

## ✨ 使用技巧
- 长按并拖动来移动工具栏。
- 工具栏空闲 3 秒后变为 50% 透明。
- 启用 `隐藏画布时清空` 来在隐藏画布时将其清空。<br>
提示：触摸透传也会被自动禁用。
- 禁用 `启动时显示画布` 来在软件启动时默认隐藏画布。

开发还处于早期阶段，遇到问题时欢迎提 Issue!

## 💌 感谢
[Akshay Sharma](https://github.com/akshay2211) 的 [DrawBox](https://github.com/akshay2211/DrawBox) 作为灵感来源，<br>
[480 Design](https://www.figma.com/@480design) 和 [R4IN80W](https://www.figma.com/@voidrainbow) 的 [Solar Icons Set](https://www.figma.com/community/file/1166831539721848736/solar-icons-set) 提供的漂亮得惊人的软件图标（[CC BY 4.0](SVG%20Icon/LICENSE.md)），<br>
以及 [Mauro Banze](https://stackoverflow.com/a/66958772) 和 [Yannick](https://stackoverflow.com/a/65760080) 的 Stack Overflow 回答（[CC BY-SA 4.0](app/src/main/java/com/shezik/drawanywhere/CustomLifecycleOwner.kt#L3)）！

最后，感谢[您](https://play.google.com/store/apps/details?id=com.kts.draw)把自己的软件变成订阅制付费模式！你是我最初的动力！[^1]<br>
<sub>自己快变成 yes man 的形状了</sub>

[^1]: 虽然占了很大一部分因素，但其实并不完全是这样。另一部分原因是运行 OneUI 4 的平板不自带这种功能。如果你像我一样还在用 OneUI 4，一定要来看看[Wallpaper Setter](https://github.com/shezik/WallpaperSetter)!
