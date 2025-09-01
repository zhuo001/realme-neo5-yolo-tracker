# 🎯 GitHub操作详细指导 - 手把手教学

## 🚀 第1步：创建GitHub仓库（详细步骤）

### 📱 步骤1.1：访问GitHub
```
1. 在浏览器地址栏输入: https://github.com
2. 确保您已经登录账户
3. 查看页面右上角是否显示您的用户头像
```

### 📁 步骤1.2：创建新仓库
```
1. 在GitHub页面右上角找到 "+" 号按钮
2. 点击 "+" 号，出现下拉菜单
3. 在下拉菜单中点击 "New repository"
4. 进入仓库创建页面
```

### ✏️ 步骤1.3：填写仓库信息
```
仓库名称 (Repository name):
📝 输入: realme-neo5-yolo-tracker

描述 (Description):
📝 输入: Realme Neo 5 150W YOLO Camera Tracker with 120Hz optimization

可见性 (Visibility):
🔘 选择 "Public" (公开仓库)
❌ 不要选择 "Private"

初始化选项:
☑️ 勾选 "Add a README file"
☑️ 勾选 "Add .gitignore" → 选择 "Android"
❌ 不勾选 "Choose a license"

最后点击绿色按钮 "Create repository"
```

## 🚀 第2步：准备上传文件（重要）

### 📦 步骤2.1：整理要上传的文件
在您的项目目录中，我已经准备好了需要上传的文件：

```
必须上传的文件夹：
📁 app/ (完整的应用代码)
📁 gradle/ (Gradle配置文件)
📁 .github/ (自动构建配置)

必须上传的文件：
📄 build.gradle (项目构建配置)
📄 settings.gradle (项目设置)
📄 gradle.properties (Gradle属性)
📄 gradlew.bat (Windows构建脚本)
📄 .gitignore (Git忽略配置)
```

### 📋 步骤2.2：检查文件完整性
在上传前，请确认以下文件存在：
```
✅ app/src/main/java/com/example/tracker/MainActivity.kt
✅ app/src/main/java/com/example/tracker/DeviceDetector.kt
✅ app/src/main/java/com/example/tracker/RealmeNeo5Config.kt
✅ app/src/main/res/layout/activity_main.xml
✅ app/build.gradle
✅ .github/workflows/build.yml
```

## 🚀 第3步：上传文件到GitHub

### 📤 步骤3.1：进入上传界面
```
1. 在新创建的仓库页面
2. 找到 "uploading an existing file" 链接
3. 点击这个链接进入文件上传页面
```

### 🖱️ 步骤3.2：选择和拖拽文件
```
方法A - 拖拽文件夹：
1. 打开Windows文件管理器
2. 导航到: C:\Users\Administrator\Desktop\RAINE-LAB\ART-CODE\output\android yolo
3. 选中 app 文件夹，拖拽到GitHub页面
4. 等待上传完成
5. 重复对 gradle 和 .github 文件夹

方法B - 选择文件：
1. 点击 "choose your files"
2. 选择所有需要的文件和文件夹
3. 点击"打开"
```

### 📝 步骤3.3：填写提交信息
```
Commit changes 部分：

提交标题 (Commit title):
📝 输入: Initial commit: Realme Neo 5 YOLO tracker

详细描述 (Extended description):
📝 输入: 
Complete Android project with:
- CameraX integration (480 lines)
- Device detection for Realme Neo 5 150W
- 120Hz optimization
- GPU acceleration support
- Automatic build with GitHub Actions

提交选项:
🔘 选择 "Commit directly to the main branch"

最后点击绿色按钮 "Commit changes"
```

## 🚀 第4步：监控自动构建

### 👀 步骤4.1：查看Actions页面
```
1. 上传完成后，在仓库页面顶部找到 "Actions" 标签
2. 点击 "Actions" 标签
3. 您会看到 "Build Realme Neo 5 150W YOLO APK" 工作流
4. 如果有黄色圆圈表示正在构建
5. 绿色勾号表示构建成功
6. 红色X表示构建失败
```

### ⏱️ 步骤4.2：等待构建完成
```
首次构建时间: 10-15分钟
进度显示:
📋 "Checkout code" ✅
📋 "Set up JDK 17" ✅
📋 "Setup Android SDK" ✅
📋 "Build debug APK" ⏳ (最耗时)
📋 "Upload APK artifact" ✅
```

### 🔍 步骤4.3：查看构建详情（如有问题）
```
如果构建失败：
1. 点击失败的构建任务
2. 查看红色X的步骤
3. 点击展开查看错误详情
4. 将错误信息告诉我，我来帮您解决
```

## 🚀 第5步：下载APK文件

### 📱 步骤5.1：找到构建产物
```
构建成功后：
1. 在Actions页面点击最新的绿色✅构建
2. 滚动到页面底部
3. 找到 "Artifacts" 部分
4. 您会看到 "realme-neo5-yolo-debug" 文件
```

### 💾 步骤5.2：下载和解压
```
1. 点击 "realme-neo5-yolo-debug" 下载
2. 下载的是一个ZIP文件
3. 解压ZIP文件
4. 找到 "app-debug.apk" 文件
5. 这就是可以安装的APK文件！
```

## 🚀 第6步：安装到设备

### 📱 步骤6.1：传输APK到手机
```
方法A - USB传输：
1. 用USB线连接Realme Neo 5 150W到电脑
2. 将app-debug.apk复制到手机下载文件夹

方法B - 无线传输：
1. 将APK上传到云盘（如百度网盘）
2. 在手机上下载APK文件
```

### 🔧 步骤6.2：安装APK
```
在Realme Neo 5 150W上：
1. 打开文件管理器
2. 找到app-debug.apk文件
3. 点击APK文件
4. 如果提示"未知来源"，点击设置允许
5. 点击"安装"
6. 安装完成后点击"打开"
```

## 🎊 预期运行效果

### ✅ 应用启动时
```
📱 显示相机权限请求弹窗
🎯 点击"允许"授予相机权限
📸 看到相机预览界面
📊 在通知栏或Logcat中看到设备检测信息
```

### ⚡ Realme Neo 5 150W特有效果
```
设备日志输出：
I/MainActivity: 检测到设备: Realme Neo 5 150W (RMX3706)
D/MainActivity: 最大帧率: 120fps
D/MainActivity: GPU加速: true
I/MainActivity: 启用Realme Neo 5 150W高性能模式
```

## 🛠️ 常见问题解决

### ❌ 问题1：构建失败
```
解决方案：
1. 检查是否所有文件都上传了
2. 确认.github/workflows/build.yml存在
3. 将错误信息发给我分析
```

### ❌ 问题2：APK无法安装
```
解决方案：
1. 确认手机允许安装未知来源应用
2. 检查Android版本是否8.0+
3. 尝试重新下载APK文件
```

### ❌ 问题3：应用崩溃
```
解决方案：
1. 确认已授予相机权限
2. 重启应用再试
3. 查看设备是否支持CameraX
```

---

## 🎯 操作检查清单

请按顺序完成，每完成一项告诉我：

- [ ] 第1步：创建GitHub仓库 ✅
- [ ] 第2步：确认文件完整 ✅  
- [ ] 第3步：上传项目文件 ✅
- [ ] 第4步：等待构建完成 ⏳
- [ ] 第5步：下载APK文件 📱
- [ ] 第6步：安装到设备 🎯

**现在请开始第1步：创建GitHub仓库，完成后告诉我！** 🚀
