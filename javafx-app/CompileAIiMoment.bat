@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ========================================
echo AIiMoment - 编译（clean + compile）
echo 目录: %CD%
echo ========================================
echo.
echo 不要直接双击 mvnw.cmd（没有参数，窗口会立刻关掉）。
echo 请用本脚本编译，或在本目录打开 CMD 手动执行：
echo   mvnw.cmd -DskipTests clean compile
echo.

if not exist "%~dp0mvnw.cmd" (
    echo [错误] 找不到 mvnw.cmd
    pause
    exit /b 1
)

call "%~dp0mvnw.cmd" -DskipTests clean compile
set "EC=%ERRORLEVEL%"

echo.
if "%EC%"=="0" (
    echo [成功] 编译完成。请再运行 RunAIiMoment.bat 启动程序。
) else (
    echo [失败] Maven 退出码: %EC%
    echo 请把上面窗口里的红色报错复制下来排查（常见：JAVA_HOME 未指向 JDK）。
)
echo.
pause
