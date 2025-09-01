# 🚀 GitHub在线构建完整指南 - Realme Neo 5 150W YOLO

## 🎯 方案概述
使用GitHub Actions自动构建APK，完全绕过本地Android Studio环境问题。

## 📋 第1步：GitHub账户准备

### 🔐 登录或注册GitHub
```
1. 访问: https://github.com
2. 如果没有账户，点击 "Sign up" 注册
3. 如果已有账户，点击 "Sign in" 登录
4. 建议使用邮箱注册，方便接收通知
```

### ✅ 账户验证
```
- 确保邮箱已验证
- 建议设置头像和用户名
- 检查账户权限正常
```

## 📋 第2步：创建项目仓库

### 🆕 创建新仓库
```
1. 登录后点击右上角 "+" 号
2. 选择 "New repository"
3. 仓库名称: realme-neo5-yolo-tracker
4. 描述: Realme Neo 5 150W YOLO Camera Tracker with 120Hz optimization
5. 选择 "Public" (公开仓库，免费使用Actions)
6. 勾选 "Add a README file"
7. 点击 "Create repository"
```

### 📝 仓库设置
```
仓库信息：
- Name: realme-neo5-yolo-tracker
- Description: High-performance YOLO tracker for Realme Neo 5 150W
- Type: Public
- Language: Kotlin/Java
- Topic: android, yolo, realme, camera, tracking
```

## 📋 第3步：上传项目代码

### 方案A：网页上传（推荐新手）
```
1. 在仓库页面点击 "uploading an existing file"
2. 将以下文件夹压缩为ZIP:
   - app/ (完整应用代码)
   - gradle/ (Gradle配置)
   - gradlew.bat, gradle.properties, settings.gradle
   - local.properties (需要修改SDK路径)

3. 拖拽ZIP文件到GitHub页面
4. 添加提交信息: "Initial commit: Realme Neo 5 YOLO tracker"
5. 点击 "Commit changes"
```

### 方案B：Git命令行上传
```bash
# 在项目目录执行
git init
git add .
git commit -m "Initial commit: Realme Neo 5 YOLO tracker"
git branch -M main
git remote add origin https://github.com/[你的用户名]/realme-neo5-yolo-tracker.git
git push -u origin main
```

## 📋 第4步：配置GitHub Actions

### 🔧 创建自动构建配置
在仓库中创建文件: `.github/workflows/build.yml`

```yaml
name: Build Realme Neo 5 YOLO APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
      
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Make gradlew executable
      run: chmod +x gradlew
      
    - name: Build debug APK
      run: ./gradlew assembleDebug --stacktrace
      
    - name: Upload APK artifact
      uses: actions/upload-artifact@v3
      with:
        name: realme-neo5-yolo-debug.apk
        path: app/build/outputs/apk/debug/app-debug.apk
        
    - name: Upload APK to release
      if: github.ref == 'refs/heads/main'
      uses: actions/upload-artifact@v3
      with:
        name: realme-neo5-yolo-release
        path: app/build/outputs/apk/debug/
```

### 📝 配置文件说明
```
触发条件: 推送代码到main分支
运行环境: Ubuntu最新版本
Java版本: JDK 17 (Android推荐)
缓存机制: 加速重复构建
输出文件: app-debug.apk
```

## 📋 第5步：监控构建过程

### 🔍 查看构建状态
```
1. 推送代码后，访问仓库的 "Actions" 标签页
2. 查看构建任务运行状态
3. 绿色✅表示成功，红色❌表示失败
4. 点击具体任务查看详细日志
```

### ⏱️ 构建时间预期
```
首次构建: 8-15分钟 (下载依赖)
后续构建: 3-8分钟 (使用缓存)
总大小: 约20-50MB APK文件
```

## 📋 第6步：下载APK文件

### 📱 获取构建产物
```
1. 构建成功后，在Actions页面点击对应的构建任务
2. 滚动到底部的 "Artifacts" 部分
3. 点击 "realme-neo5-yolo-debug.apk" 下载
4. 解压ZIP文件获得APK
```

### 📲 安装到设备
```
方法1: 直接安装
- 将APK传输到Realme Neo 5 150W
- 在设备上点击APK文件安装

方法2: ADB安装
- adb install app-debug.apk
- 确保设备已启用USB调试
```

## 🎊 预期成果

### ✅ 成功标志
```
📱 APK文件成功生成 (约20-50MB)
🎯 在Realme Neo 5 150W上安装成功
⚡ 应用启动，自动检测设备型号
🎮 相机预览正常，120Hz流畅显示
🔥 Logcat显示设备优化日志
```

### 📊 功能验证
```
设备检测: "检测到设备: Realme Neo 5 150W (RMX3706)"
性能优化: "启用120Hz高刷新率模式"
GPU加速: "启用Adreno 730 GPU加速"
散热管理: "设置85°C温控阈值"
```

## 🛠️ 故障排除

### 常见构建错误
```
错误1: "Gradle sync failed"
解决: 检查 gradle-wrapper.properties 配置

错误2: "SDK not found"  
解决: Actions会自动下载SDK，无需手动配置

错误3: "Build timeout"
解决: 代码太复杂，考虑分步构建
```

### 🔧 调试技巧
```
1. 查看完整构建日志
2. 对比成功的构建配置
3. 简化依赖项配置
4. 使用GitHub社区支持
```

---

## 🎯 立即行动清单

### 您现在需要做的事情：

#### ✅ 第1步 (5分钟)
- 访问 https://github.com
- 登录或注册账户
- 验证邮箱

#### ✅ 第2步 (3分钟)  
- 创建新仓库 "realme-neo5-yolo-tracker"
- 设置为公开仓库
- 添加基本描述

#### ✅ 第3步 (10分钟)
- 上传我们的完整项目代码
- 包含所有文件夹和配置

#### ✅ 第4步 (5分钟)
- 创建 GitHub Actions 配置文件
- 提交并触发首次构建

#### ✅ 第5步 (15分钟)
- 等待自动构建完成
- 下载生成的APK文件

**总计时间**: 40分钟内完成所有步骤并获得可用APK！

现在请开始第1步：登录GitHub并告诉我您的进展！🚀
