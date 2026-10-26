# 私密音频发布修复（2026-10-26）

此前改版发布包从 Git 已提交源码构建。原有私密音频功能当时仍是未提交文件和本地修改，因此发布版本缺少两条前端路由、两个页面、三个后端类和对应的权限规则。本地开发目录保留这些文件，造成了本地正常、线上空白或播放接口 403 的差异。浏览器扩展的 `Unchecked runtime.lastError` 并非这次空白页面的原因。

本次将上述已有功能完整纳入源码，并统一管理页、收听页、发布窗口和提示弹窗的蓝白风格。保留发布、上传、音频直链、专属链接复制及原有 UUID 查询方式；加载失败和播放失败会显示提示与重试入口。播放由浏览器识别文件格式，不再把所有音频固定标记为 MP3。

数据库继续使用原来的 `premium_audio` 表和字段。无需删除、重建或导入此表，不重新生成已有 UUID。测试只在本地开发数据库执行，持久化测试的更改通过事务回滚。

## 发布检查

前端生产构建与后端 Maven 测试打包完成后，从仓库根目录执行：

```powershell
./scripts/verify-premium-release.ps1 `
  -FrontendDist ./portfolio-frontend/dist `
  -BackendJar ./portfolio-backend/target/portfolio-backend-0.0.1-SNAPSHOT.jar
```

此检查直接读取打包产物，确认页面、路由、后端类、权限路径和生产配置均已进入发布文件。

## 服务器替换

1. 将现有前端目录和正式 JAR 备份到网站目录外。
2. 停止 Java 项目，用新的同名 JAR 替换 `/www/wwwroot/portfolio-api/portfolio-backend-0.0.1-SNAPSHOT.jar`。
3. 沿用原启动用户、工作目录、Java 21 与生产配置参数 `--spring.profiles.active=prod`，重启 Java 项目。只上传 JAR 不会更新正在运行的旧进程。
4. 将前端包解压到 `/www/wwwroot/portfolio-web/`，覆盖同名文件，合并 `assets/`。保留 `.well-known`、`.user.ini`、旧哈希静态资源及原上传文件。
5. 使用管理员账号检查 `/premium-admin` 列表，使用原来已分享的完整链接检查收听页；再用未登录窗口确认持有链接的访客仍可播放。浏览器强制刷新后仍是旧页面时，再检查 CDN 缓存。

这次前后端都需要更新。无需修改数据库，也不要重新发布旧节目来替代原有链接。若节目页面能正常加载而音频文件仍无法播放，检查该节目的音频直链是否过期、文件是否存在及浏览器对文件格式的支持。
