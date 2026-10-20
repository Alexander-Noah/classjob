# Codecov 覆盖率分析

仓库地址：https://app.codecov.io/github/Alexander-Noah/classjob

现有 CI 通过 Maven / JaCoCo 和 Vitest / V8 生成覆盖率报告，不需要安装 Jest。
前后端测试成功后，`Upload coverage to Codecov` 任务下载报告并分别上传：

| 模块 | 报告 | Codecov flag |
| --- | --- | --- |
| 后端 | `Backend/target/site/jacoco/jacoco.xml` | `backend` |
| 前端 | `web/coverage/lcov.info` | `frontend` |

使用 Codecov 官方 Action 和 GitHub OIDC 认证，无需配置 `CODECOV_TOKEN`。
OIDC 权限仅授予上传任务。外部 fork PR 和 Dependabot 运行保留测试，但跳过上传。
现有前后端各 96% 行覆盖率门槛和实际覆盖率徽章继续生效。

推送后，在 GitHub Actions 的 CI 运行中查看上传任务；两个上传步骤成功后，
到 Codecov 仓库页面查看处理结果。若上传失败，任务会明确失败，不代表已成功接入。
如果提示仓库未启用或无权访问，请在 Codecov 中启用此仓库并检查其 GitHub App 授权。

官方说明：https://github.com/codecov/codecov-action#using-oidc

## 验收口径

仓库门槛为 Codecov 原始 project coverage 至少 96%，配置见根目录 codecov.yml。保持默认解析规则，不将部分覆盖行强制视为完整覆盖，也不排除原有业务源码。

README 同时显示两个指标：Line coverage 来自 JaCoCo/Vitest 的已执行行比率；Codecov coverage 使用服务端原始统计，其中执行过但分支没有测全的行会被记为 partial。两者不能互相替代，验收以 Codecov 处理完成后的提交报告为准。
