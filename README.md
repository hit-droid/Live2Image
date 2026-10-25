# Live2 Image

> 安卓版 Live2D 桌面应用 —— 把 `nizimaLive`（Windows 桌面 VTuber 软件）的**核心体验移植/复刻到 Android**。

## 这个项目是什么

- **目标**：在 Android 上加载 Live2D（Cubism）模型并实时渲染，支持待机动画、眨眼、触摸拖拽/缩放、点击触发动作，并预留面部/音频驱动扩展点——即 nizimaLive 这类 Live2D 驱动软件的安卓版。
- **技术栈**：原生 Android（Kotlin）+ `GLSurfaceView`（OpenGL ES 2.0）+ 官方 **Live2D Cubism SDK for Native**。
- **代码架构**：命名与职责对齐 Cubism 官方 Android Sample（`LAppDelegate / LAppLive2DManager / LAppModel / LAppView / LAppTextureManager / TouchManager`），便于对照官方示例调试。

> **关于"移植"的说明（重要）**
> 上游仓库 `hit-droid/zaiwujian` 里只有 `nizimaLive 2.7.1` 的发布说明，其 Release 附件是 nizima 的**闭源商业 Windows 桌面软件**安装包（约 220MB，内含 `.exe/.dll/资源`），**没有可移植到安卓的源码**；其内置 Live2D 模型亦为 nizima 版权、不宜二次分发。
> 因此本项目采用**基于官方 Cubism SDK 的重新实现**，在功能与体验上对齐 nizimaLive，而非对其二进制做代码级移植。

## 功能特性

| 能力 | 状态 | 说明 |
|------|------|------|
| 加载 `.moc3` / `.model3.json` 模型 | ✅ | 从 `assets` 读取，走 Cubism Framework |
| OpenGL ES 2.0 实时渲染 | ✅ | `GLSurfaceView` + 预乘 alpha 混合 |
| 待机动作 / 眨眼 / 呼吸 | ✅ | 由 Cubism Framework 物理与动作系统驱动 |
| 触摸拖拽移动 | ✅ | 单指拖拽平移视图矩阵 |
| 双指缩放 | ✅ | 捏合缩放（0.5x ~ 3.0x） |
| 点击触发动作 | ✅ | 点击播放 `TapBody` 动作组 |
| 模型纹理管理 | ✅ | PNG 预乘 alpha + mipmap + 缓存 |
| 摄像头面部驱动 | 🚧 预留 | 见下方「路线图」，接口已留出 |
| 音频/麦克风驱动 | 🚧 预留 | 见「路线图」 |

## 目录结构

```
Live2Image/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/live2image/app/
│       │   ├── MainActivity.kt          # 入口，桥接 Activity 生命周期
│       │   ├── LAppDelegate.kt          # 单例：Framework 初始化/释放、持有 Activity/View
│       │   ├── LAppLive2DManager.kt     # 单例：模型加载/更新/绘制/视图矩阵
│       │   ├── LAppModel.kt             # 单模型封装（继承 CubismUserModel）
│       │   ├── LAppView.kt              # GLSurfaceView + Renderer + 触摸分发
│       │   ├── LAppTextureManager.kt    # 纹理加载/缓存
│       │   ├── LAppPal.kt               # 日志/资源/着色器工具
│       │   ├── LAppDefine.kt            # 常量（模型目录、动作组、视图参数）
│       │   └── TouchManager.kt          # 单指拖拽 + 双指缩放
│       └── res/                          # 布局、主题、图标
├── build.gradle / settings.gradle / gradle.properties
└── README.md
```

## 编译运行（Android Studio）

1. **环境**：Android Studio（Hedgehog 或更新）、Android SDK 34、NDK 26（部分 Cubism 版本需要）、CMake 3.22+。
2. **打开工程**：用 Android Studio `Open` 选择本仓库根目录，等待 Gradle Sync。
3. **接入 Cubism SDK 与模型**：见下一节（**必需**，否则编译会因缺少 `com.live2d.sdk.cubism.*` 类而失败）。
4. **运行**：连接真机（OpenGL ES 2.0）或模拟器，Run `app`。

## 接入 Cubism SDK 与模型（必需）

Cubism SDK 需从 Live2D 官网下载（需注册登录，遵循 Live2D 许可，无法随仓库分发）：

1. 前往 Live2D 官网 → 开发者 → 下载 **「Cubism SDK for Native」**。
2. 解压后按下面两种方式之一接入本工程：

   **方式 A —— 用官方 Android Sample 作为蓝本（推荐，最省心）**
   - SDK 解压后自带 `Samples/Android` 工程，它已正确配置好 Framework 的 `.so`/`aar`/CMake。
   - 用该 Sample 工程为基础，把本仓库 `app/src/main/java/com/live2image/app/` 下的 `LApp*`、`TouchManager` 等源码合并进去即可（包名 `com.live2image.app`，可自行调整）。

   **方式 B —— 手动集成到本工程**
   - **Core 运行时库**：把 `Core/lib/android/<abi>/libLive2DCubismCore.so` 复制到 `app/src/main/jniLibs/<abi>/`。
   - **Framework**：若 SDK 提供 `CubismFramework.aar`（内含 Java 绑定 + `.so`），放入 `app/libs/`（`build.gradle` 已 `fileTree` 自动引入）；若为源码形式，把其 Java 绑定目录加入 `app/build.gradle` 的 `sourceSets.main.java.srcDirs`（已预留注释行）。
   - **示例模型**：SDK 自带 `Samples/Resources/Haru`（含 `Haru.model3.json`、`.moc3`、纹理 PNG），整目录复制到 `app/src/main/assets/Haru/`。
     > 模型目录/文件名由 `LAppDefine.MODEL_DIR` / `MODEL_JSON` 指定，默认即 `Haru` / `Haru.model3.json`；换成自己的模型改这两处即可。

3. Gradle Sync → Build → Run。

## 关于 API 校准

本工程对 Cubism Framework 的调用**命名对齐官方 Android Sample**（如 `CubismFramework.StartUp()/Dispose()`、`CubismUserModel.loadAssets(...)`、`draw(matrix)`、`startMotion(...)`、`setExpression(...)`、`CubismMatrix44` 等）。

不同 SDK 版本的方法签名/命名可能略有差异（例如矩阵方法可能是 `ScaleRelative/TranslateRelative`）。若编译报错，请**以你下载的 SDK 内对应 Sample 为准**做同名替换——所有接入点都集中在 `LAppDelegate.kt`、`LAppModel.kt`、`LAppLive2DManager.kt` 三个文件，改动量很小。

## 路线图

- [ ] 摄像头面部驱动：CameraX + ML Kit Face Mesh → 映射嘴型/视线/头部姿态到模型参数
- [ ] 音频/麦克风驱动：音量/基频 → 张嘴（LipSync）
- [ ] 多模型切换与列表
- [ ] 渲染到纹理 + 截图导出（`LAppDefine.USE_RENDER_TARGET` 已预留开关）
- [ ] 透明背景/悬浮窗（桌面宠物形态）

## 版权与许可

- 本仓库自有代码采用 [MIT License](./LICENSE)。
- **Cubism SDK**：其使用受 [Live2D 许可](https://www.live2d.com/zh-CHS/download/sdk/#license) 约束，请自行遵守（部分场景商业使用需授权）。
- **模型资源**：示例模型请使用官方自带（如 Haru）或你**拥有使用权**的模型；**请勿**将 nizimaLive 等第三方专有模型打包进本仓库。
