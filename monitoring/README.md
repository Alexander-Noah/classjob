# Prometheus + Grafana

适用于当前部署方式：Spring Boot 在宿主机运行，监控组件在 Docker Desktop（Linux 容器）运行。

## 启动

1. 安装并启动 Docker Desktop，确认 `docker compose version` 可用。
2. 直接使用 IDEA 原有运行配置启动后端，或在项目根目录的 PowerShell 中启动（数据库等配置仍需正常提供）：

```powershell
./Backend/start-local.ps1
```

监控已写入 `Backend/src/main/resources/application.yaml`，默认启用，不需要设置 profile 或新增 IDEA 环境变量。修改配置后重启后端即可。之前添加过的 `SPRING_PROFILES_INCLUDE=monitoring` 可以删除。

3. 在另一个终端中启动监控：

```powershell
cd monitoring
Copy-Item .env.example .env
# 编辑 .env，将 GRAFANA_ADMIN_PASSWORD 改为自己的随机密码。
docker compose config --quiet
docker compose run --rm --no-deps --entrypoint promtool prometheus check config /etc/prometheus/prometheus.yml
docker compose up -d
```

4. 打开以下地址：

- Grafana：http://localhost:3000，账号 `admin`，密码为 `.env` 中设置的值。
- Grafana 仪表盘：http://localhost:3000/d/zhiyun-backend，包含在线状态、请求速率、P95、5xx、堆内存、CPU、线程和 HikariCP 活跃连接。
- Prometheus：http://localhost:9090/targets，`zhiyun-backend` 应显示 `UP`。
- 后端指标：http://localhost:8081/actuator/prometheus。

Grafana 默认首页已设为项目仪表盘。若有图表但没有数据，先查看 Prometheus targets；后端目标连接 8081 被拒绝时，检查后端是否已加载最新配置并重启，以及 8081 是否被占用或被外部配置覆盖。

HTTP 请求和延迟图需要先访问业务接口并等待约一分钟；没有请求的指标可能暂不出现。

## 镜像下载超时

如果出现 `registry-1.docker.io:443` 连接超时或 `Docker Desktop has no HTTPS proxy`，请在 Docker Desktop 的 Settings → Resources → Proxies 中启用系统代理，或手动填写本机实际可用的 HTTP 代理地址。HTTP 和 HTTPS 代理栏通常都填写 `http://127.0.0.1:代理端口`，保存应用后重试 `docker compose pull` 和 `docker compose up -d`。容器内的环境变量不能替代 Docker Desktop 下载镜像使用的代理设置。

`No such image` 若出现在下载失败之后，通常表示镜像尚未下载完成，先解决下载问题。

## 端口与数据

业务端口仍是 8080；默认配置使用独立管理端口 8081，只暴露 health 和 prometheus。JWT 过滤器仅在独立管理端口允许无登录采集指标，不对业务端口新增匿名放行。

Grafana 和 Prometheus 的网页只绑定本机。管理端口为方便 Docker Desktop 采集监听 `0.0.0.0`，部署到服务器时必须通过防火墙限制为监控网络访问。可通过 `SMART_LAB_MANAGEMENT_ADDRESS` 修改监听地址。仅宿主机原生采集时可设为 `127.0.0.1`；Docker Desktop 采集需保留可达地址。

修改 `SMART_LAB_MANAGEMENT_PORT` 时同步修改 `prometheus/prometheus.yml` 的 target。Linux Docker 需要宿主机管理端口可从 Docker 网桥访问；如果后端也容器化，将 target 改成后端服务名，并将两个服务接入同一 Docker 网络。

Prometheus 保留 15 天数据，两个组件均使用命名卷持久化。`docker compose down` 停止服务并保留数据，`down -v` 会删除监控历史和 Grafana 数据。初始管理员密码仅在 Grafana 数据卷首次初始化时生效，之后在 Grafana 中修改密码。

本配置提供基础设施和应用技术指标，不包含业务统计、自定义埋点或告警通知。

参考：[Spring Boot 指标](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)、[Grafana 自动配置](https://grafana.com/docs/grafana/latest/administration/provisioning/)。
