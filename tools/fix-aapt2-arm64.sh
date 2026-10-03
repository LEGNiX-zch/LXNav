#!/system/bin/sh
# ============================================================================
# LXNav —— arm64 (Android Code Studio / AndroidIDE) aapt2 自动适配脚本
# ----------------------------------------------------------------------------
# 用途：在 arm64 设备上，AGP 会从 Google Maven 下载 x86_64 版 aapt2，
#       导致 "Syntax error: ( unexpected" / "Daemon startup failed"。
#       本脚本自动探测「本机可用的 arm64 aapt2」路径，并写入
#       gradle.properties 的 android.aapt2FromMavenOverride。
#
# 用法（在 Android Code Studio 内置终端执行）：
#     sh tools/fix-aapt2-arm64.sh
#
# 说明：本脚本必须在「能访问 /data/data/com.tom.rv2ide 的环境」中运行，
#       即 Code Studio 自带终端（IDE Terminal），而非外部 App 的终端。
# ============================================================================

set -u

PROJ="$(cd "$(dirname "$0")/.." && pwd)"
GP="$PROJ/gradle.properties"

# 安全取环境变量（set -u 下未定义会报错）
: "${HOME:=/data/data/com.tom.rv2ide/files/home}"
: "${PREFIX:=}"

echo "== LXNav arm64 aapt2 适配 =="
echo "工程目录: $PROJ"
echo

FOUND=""

# 1) 命令行参数优先：允许直接指定路径
if [ $# -ge 1 ] && [ -n "$1" ]; then
    FOUND="$1"
    echo "使用命令行指定路径: $FOUND"
fi

# 2) 未指定则自动探测常见候选路径
if [ -z "$FOUND" ]; then
    CANDIDATES="
$HOME/.androidide/aapt2/aapt2
/data/data/com.tom.rv2ide/files/home/.androidide/aapt2/aapt2
/data/data/com.tom.rv2ide/files/usr/bin/aapt2
/data/data/com.tom.rv2ide/files/home/aapt2
/data/data/com.termux/files/usr/bin/aapt2
$PREFIX/bin/aapt2
"
    for c in $CANDIDATES; do
        [ -n "$c" ] || continue
        if [ -f "$c" ]; then
            # 校验是否真的可执行（能打印版本）
            if "$c" version >/dev/null 2>&1 || "$c" --version >/dev/null 2>&1; then
                echo "[OK]   可执行: $c"
                FOUND="$c"
                break
            else
                echo "[SKIP] 存在但不可执行: $c"
            fi
        fi
    done
fi

echo
if [ -z "$FOUND" ]; then
    echo "!! 未在常见路径找到可用的 arm64 aapt2。"
    echo "请手动搜索："
    echo "    find /data/data/com.tom.rv2ide -iname 'aapt2' 2>/dev/null"
    echo "找到后执行："
    echo "    sh tools/fix-aapt2-arm64.sh /绝对/路径/aapt2"
    exit 1
fi

echo "写入 android.aapt2FromMavenOverride=$FOUND"
# 删除旧的 override 行，再追加
grep -v '^android\.aapt2FromMavenOverride=' "$GP" > "$GP.tmp" 2>/dev/null
echo "android.aapt2FromMavenOverride=$FOUND" >> "$GP.tmp"
mv "$GP.tmp" "$GP"

echo
echo "完成。当前 override 配置："
grep 'aapt2FromMavenOverride' "$GP"
echo
echo "下一步：在 Code Studio 里 Clean Project 后重新 Build。"
