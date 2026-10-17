@echo off
rem kill-port-8080运维脚本；执行前确认参数、工作目录和目标环境，不能作为业务请求入口。

echo Looking for process using port 8080...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo Found process %%a using port 8080. Killing it...
    taskkill /F /PID %%a
    echo Done!
    pause
    exit /b
)
echo No process found listening on port 8080.
pause
