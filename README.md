# Live2 Image

> 安卓版 Live2D 桌面应用 —— 把 `nizimaLive`（Windows 桌面 VTuber 软件）的**核心体验移植/复刻到 Android**，基于 **Live2D Cubism SDK for Native 5 r.5** 实现**真实 Live2D 渲染**（非占位桩）。

## 当前状态（已在沙箱完整编译验证）

| 验证项 | 结果 |
|--------|------|
| 官方 Cubism Android Sample 原位编译 | ✅ BUILD SUCCESSFUL（工具链基准） |
| 本工程 `assembleDebug` | ✅ BUILD SUCCESSFUL |
| 产物 | `app-debug.apk`（**8.6MB**，`com.live2image.app`，minSdk 24 / target 36） |
| Native 库 | `libLive2Image.so` × `arm64-v8a` / `x86` / `x86_64`（各 ~1.2MB，静态链入 Cubism Core + Framework） |
| JNI 桥 | 10 个 `Java_com_live2image_app_JniBridgeJava_*` 导出符号全部就位 |
| 模型资产 | `assets/Haru/`（`Haru.moc3` + `model3.json` + 物理 + 纹理）+ 全部 ES2 shader |

> **关于"移植"的说明（重要）**
> 上游仓库 `hit-droid/zaiwujian` 里只有 `nizimaLive 2.7.1` 的发布说明，其 Release 附件是 nizima 的**闭源商业 Windows 桌面软件**安装包（约 220MB，内含 `.exe/.dll/资源`），**没有可移植到安卓的源码**；其内置 Live2D 模型亦为 nizima 版权、不宜二次分发。
> 因此本项目采用**基于官方 Cubism SDK 的重新实现**，在功能与体验上对齐 nizimaLive，而非对其二进制做代码级移植。

## 架构

采用 Cubism 官方 Android Sample（`proj.android.cmake/Full`）的同款架构——**薄 Java + 重 C++**，真实渲染逻辑全部在 Native 层：

```
app/src/main/
├── java/com/live2image/app/     # Java 层仅 3 个文件
│   ├── MainActivity.java        #   生命周期 + GLSurfaceView + 触摸事件
│   ├── GLRenderer.java          #   GLSurfaceView.Renderer → JNI
│   └── JniBridgeJava.java       #   native 方法声明 + AssetManager 桥
├── cpp/                         # C++ 层（真实 Live2D 实现）
│   ├── JniBridgeC.cpp           #   JNI 实现 → LAppDelegate
│   ├── LAppDelegate             #   Framework StartUp/Dispose、GL 环境、视图
│   ├── LAppLive2DManager        #   场景/模型管理、视图矩阵
│   ├── LAppModel                #   模型加载/更新/绘制/命中判定
│   ├── LAppView / LAppSprite    #   背景精灵、UI 齿轮/电源按钮
│   ├── LAppTextureManager       #   纹理加载与缓存
│   └── LAppDefine               #   常量（视图逻辑坐标、动作组、命中区名…）
└── assets/
    ├── Haru/                    # 官方示例模型（moc3 + cdi3 + physics3 + 纹理）
    └── *.vert / *.frag          # Cubism Framework ES2 shader

cubism/                          # Cubism SDK for Native 5 r.5（源码 + 静态库）
├── Core/include                 # Core API 头文件
├── Core/lib/android/<abi>/      # libLive2DCubismCore.a（arm64-v8a/x86/x86_64）
├── Framework/                   # Cubism Framework C++ 源码（CMake 引入编译）
└── Samples/Common + thirdParty/stb
```

渲染管线：`GLSurfaceView`(ES 2.0) → `JniBridgeC` → `LAppDelegate::run` → `LAppLive2DManager` → `LAppModel::update/draw` → `CubismRenderer_OpenGLES2` → `libLive2DCubismCore.a`。

## 功能特性

| 能力 | 状态 | 说明 |
|------|------|------|
| 加载 `.moc3` / `.model3.json` 模型 | ✅ | Cubism Framework 从 assets 读取 |
| OpenGL ES 2.0 实时渲染 | ✅ | 预乘 alpha 混合，3 ABI |
| 待机动作 / 眨眼 / 呼吸 | ✅ | 模型自带物理（`.physics3.json`）+ `Idle` 动作组 |
| 单指拖拽移动 | ✅ | 平移视图矩阵（`LAppView` 逻辑坐标 ±2.0） |
| 双指捏合缩放 | ✅ | 0.8x ~ 2.0x（`ViewMinScale`~`ViewMaxScale`） |
| 点击触发动作 | ✅ | 命中判定 `Head`/`Body`，播放 `TapBody` |
| 背景精灵 + 齿轮/电源按钮 | ✅ | `LAppSprite`（点击齿轮切换模型、电源退到后台） |
| 摄像头面部驱动 | 🚧 | 见「路线图」 |
| 音频/麦克风驱动 | 🚧 | 见「路线图」 |

## 编译运行

**环境要求**：JDK 17、Android SDK Platform 36、Build-Tools 36.0.0、NDK `26.3.11579264`、CMake `3.22.1`。

命令行：

```bash
# SDK 已内置在 cubism/，无需额外下载；写 local.properties 后直接构建
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleDebug          # 产出 app/build/outputs/apk/debug/app-debug.apk
```

Android Studio：直接 Open 本仓库根目录，Gradle Sync 后 Run（真机或模拟器需支持 OpenGL ES 2.0）。

## 路线图

- [ ] 多模型切换（assets 内置更多官方示例模型或允许用户导入 `.moc3`）
- [ ] 摄像头面部驱动：CameraX + ML Kit Face Mesh → 映射嘴型/视线/头部姿态到模型参数
- [ ] 音频/麦克风驱动：音量/基频 → 张嘴（LipSync）
- [ ] 透明背景/悬浮窗（桌面宠物形态，`LAppView` 背景可关）
- [ ] 渲染到纹理 + 截图导出（`app/CMakeLists.txt` 内 `USE_RENDER_TARGET` 开关已预留）

## 版权与许可

- 本仓库**自有代码**（`app/src/main/java/` 下 3 个 Java 文件及 Gradle 配置的改动部分）采用 [MIT License](./LICENSE)。
- `cubism/`（Cubism SDK for Native）、`app/src/main/cpp/`、`app/src/main/assets/` 下的 **Live2D 官方代码与资源**，其版权归 Live2D Inc.，使用受 [Live2D Open Software License](https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html) 约束，详见 [cubism/NOTICE.md](./cubism/NOTICE.md)；商业场景请自行确认授权要求。
- 示例模型 `Haru` 为 Cubism SDK 自带示例资产，仅用于演示；**请勿**将 nizimaLive 等第三方专有模型打包进本仓库。
