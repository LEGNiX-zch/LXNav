# LXNav 在 arm64 设备（Android Code Studio / AndroidIDE）上构建指南

## 一、核心问题（已定位）

在 arm64 手机上构建时出现的失败：

```
> Task :app:processDebugResources FAILED
AAPT2 aapt2-8.5.2-11315950-linux Daemon #0: Unexpected error output:
  .../aapt2-8.5.2-11315950-linux/aapt2: 2: Syntax error: "(" unexpected
> AAPT2 ... Daemon startup failed ...
```

**根因**：AGP（Android Gradle Plugin）会从 Google Maven 下载
`aapt2-8.5.2-*-linux` 这个二进制。而它是 **x86_64 架构**，
在 **arm64 手机**上无法执行（Shell 把它当脚本解析，
遇到 ELF 头里的字节就报 `Syntax error: "(" unexpected`）。

> 结论：**这与 LXNav 的源码 / gradle.properties 内容完全无关**，
> 是 IDE 构建环境的 aapt2 架构不匹配。

---

## 二、解决方案（二选一）

### 方案 A：指向本机已有的 arm64 版 aapt2（推荐）

Android Code Studio / AndroidIDE 通常**自带**一个可在 arm64 运行的 aapt2。

1. 在 **Code Studio 内置终端**中执行：

   ```sh
   sh tools/fix-aapt2-arm64.sh
   ```

   脚本会自动探测常见路径并写入 `gradle.properties`：

   ```properties
   android.aapt2FromMavenOverride=/data/data/com.tom.rv2ide/files/home/.androidide/aapt2/aapt2
   ```

2. 若脚本没找到，手动搜：

   ```sh
   find /data/data/com.tom.rv2ide -iname 'aapt2' 2>/dev/null
   ```

   找到后：

   ```sh
   sh tools/fix-aapt2-arm64.sh /找到的/绝对路径/aapt2
   ```

### 方案 B：用 Termux 安装一个 arm64 aapt2

若 IDE 内没有自带，可用 Termux 提供：

```sh
# 在 Termux 中
pkg install aapt2
# 得到：/data/data/com.termux/files/usr/bin/aapt2  （原生 arm64）
```

然后：

```sh
sh tools/fix-aapt2-arm64.sh /data/data/com.termux/files/usr/bin/aapt2
```

---

## 三、本次已做的源码/配置层优化

| 文件 | 优化内容 | 说明 |
|---|---|---|
| `gradle.properties` | `\=` 修正为 `=` | 消除非法转义（虽非根因，但更规范） |
| `gradle.properties` | `org.gradle.caching=false` | 避免跨架构脏缓存 |
| `gradle.properties` | `org.gradle.configuration-cache=false` | arm64 兼容性更稳 |
| `gradle.properties` | `org.gradle.workers.max=2` | 防低内存设备 OOM |
| `app/build.gradle.kts` | `appcompat 1.7.0 → 1.6.1` | 1.7.0 需 compileSdk 35，1.6.1 稳配 34 |
| `tools/fix-aapt2-arm64.sh` | 新增 | 一键探测并写入 arm64 aapt2 路径 |

---

## 四、构建步骤

1. **先清缓存**（旧缓存里有坏的 x86_64 aapt2）：

   ```sh
   rm -rf .gradle app/build build
   ```

2. **执行 aapt2 适配脚本**（见方案 A / B）。

3. **用 IDE 的 Build 按钮**（或内置终端）：

   ```sh
   sh gradlew assembleDebug
   ```

4. 产物位置：

   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 五、若仍失败

请回传完整日志（**不要只截汇总**），特别是含 `e:` 或 `error:` 的行：

```sh
sh gradlew assembleDebug > /sdcard/lxnav_build.log 2>&1
grep -iE 'error|fail' /sdcard/lxnav_build.log | head -50
```

把上面 `grep` 的输出发出来即可精准定位。

---

## 六、兜底建议

若 Android Code Studio 的 arm64 环境**始终无法**提供可用 aapt2，
最稳妥的方式是用 **PC 版 Android Studio**（x86_64 原生支持 aapt2）
打包——本项目源码本身完全标准，可直接导入构建。
