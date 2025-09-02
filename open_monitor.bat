@echo off
echo 🚀 正在打开 YOLO Tracker 构建监控界面...
echo.
echo 📊 监控内容:
echo    - GitHub Actions 构建状态
echo    - NCNN 库集成进度  
echo    - APK 下载链接
echo    - 实时构建日志
echo.

REM 打开监控页面
start "" "file:///c:/Users/Administrator/Desktop/RAINE-LAB/ART-CODE/output/android yolo/ncnn_build_monitor.html"

REM 同时打开 GitHub Actions 页面
timeout /t 2 /nobreak >nul
start "" "https://github.com/zhuo001/realme-neo5-yolo-tracker/actions"

echo ✅ 监控界面已启动
echo 💡 请查看浏览器中的监控页面
echo.
pause
