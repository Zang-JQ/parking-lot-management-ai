# 🅿️ 停车场智能管理系统（Parking Management System）

> 基于 **Spring Boot 3 + MyBatis + Redis Cluster + FastDFS + LangChain4j** 的停车场管理系统。
> 面向 **管理员 / 会员 / 普通用户** 三种角色，覆盖车位管理、车辆进出场、实时计费、会员体系、经营报表，
> 并集成 **车牌识别（视觉大模型）** 与 **AI 业务助手（工具调用）** 两项 AI 能力。

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.8-brightgreen)
![MyBatis](https://img.shields.io/badge/MyBatis-3.5-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)
![Redis](https://img.shields.io/badge/Redis-6.2%20Cluster-DC382D)
![LangChain4j](https://img.shields.io/badge/LangChain4j-1.0.1-ff69b4)

---

## 目录

- [一、功能特性](#一功能特性)
- [二、技术栈](#二技术栈)
- [三、系统架构](#三系统架构)
- [四、数据库设计](#四数据库设计)
- [五、环境依赖与端口清单](#五环境依赖与端口清单)
- [六、快速开始](#六快速开始)
- [七、配置说明](#七配置说明)
- [八、接口清单](#八接口清单)
- [九、核心设计要点](#九核心设计要点)
- [十、测试](#十测试)
- [十一、打包部署](#十一打包部署)
- [十二、安全说明](#十二安全说明)
- [十三、已知限制与后续优化](#十三已知限制与后续优化)
- [十四、目录结构](#十四目录结构)

---

## 一、功能特性

### 1.1 管理员（7 个页面）

| 页面 | 功能 |
|---|---|
| 首页仪表盘 | 在场车辆数、空闲/占用车位数、今日收入、近 7 天收入折线图、车位状态环形图、实时在场车辆列表（30 秒自动刷新） |
| 车位管理 | 车位网格视图（颜色区分状态）、状态筛选、新增/编辑/删除车位、手动锁定车位（预留位） |
| 费率规则管理 | 读写 `feerule` 表，修改后**立即生效**（无需重启），右侧带计费试算 |
| 停车记录查询 | 全场记录，支持车牌/车位、日期、在场状态三重筛选 + 分页 |
| 会员管理 | 会员列表、剩余天数、到期状态筛选（有效 / 即将到期 / 已过期） |
| 收费统计报表 | 今日/本月/本年/累计收入、本月车流量、每日明细（含迷你条形图） |
| 系统设置 | 只读展示系统参数与实时车位使用情况 |

### 1.2 会员（5 个页面）

- **会员中心**：会员卡信息、头像上传（存 FastDFS）、绑定主副车牌
- **车辆入场**：选择主车牌或副车牌免费入场
- **车辆出场**：免费出场（过期会员自动转为按普通用户计费）
- **停车记录**：分页查看历史，费用按真实金额显示
- **续费充值**：续费一个月，有效期顺延

> 家庭会员设计：一个账号绑定**主、副两个车牌**，可同时在场停放两辆车。

### 1.3 普通用户（5 个页面）

- **车位查询**：实时车位状态列表
- **车辆入场**：车牌号 + 空闲车位（仅显示 A 区普通车位），支持**拍照识别车牌**
- **车辆出场**：实时显示停放时长与应缴费用，确认支付出场
- **停车记录**：分页查看，含累计次数与累计消费
- **充值会员**：一键开通月卡（200 元 / 30 天），升级为会员角色

### 1.4 AI 能力

| 能力 | 实现 | 说明 |
|---|---|---|
| **车牌识别** | LangChain4j + `qwen3-vl-plus`（多模态视觉） | 上传照片 → 识别车牌 → 自动填入入场页输入框。前端会先把图片压缩到长边 1280 再上传，单次约 280 token |
| **AI 业务助手** | LangChain4j `AiServices` + Tool Calling + `qwen-plus` | 右下角悬浮对话窗，模型通过 5 个 `@Tool` 方法查库回答：车位概况、我的在场状态、我的历史记录、我的会员信息、收入概况（仅管理员） |

---

## 二、技术栈

| 层次 | 技术 | 版本 | 说明 |
|---|---|---|---|
| 语言 | Java | 17 | 项目以 `release 17` 编译 |
| 后端框架 | Spring Boot | 3.2.8 | 内嵌 Tomcat，`war` 打包 |
| Web | Spring MVC | 6.1.x | `@RestController` 返回 JSON |
| 持久层 | MyBatis | 3.0.4（starter） | 全注解 SQL，无 XML |
| 数据库 | MySQL | 8.0 | 数据库 `aaa`，8 张表 |
| 缓存 | Redis Cluster | 6.2.6 | 3 主 3 从，车位列表缓存 |
| 安全 | Spring Security | 6.2.x | URL 级授权 + BCrypt 密码 |
| 文件存储 | FastDFS | 5.11 | 会员头像，nginx 提供 HTTP 访问 |
| 模板引擎 | Thymeleaf | 3.1.x | 19 个页面 |
| 前端 | jQuery 1.8.3 + 原生 JS | — | Ajax 异步，无构建工具 |
| AI 框架 | LangChain4j | 1.0.1 | `langchain4j-open-ai` + JDK HTTP Client |
| 大模型 | 阿里云百炼 DashScope | — | `qwen3-vl-plus`（视觉）、`qwen-plus`（文本/工具调用） |

---

## 三、系统架构

```
┌──────────────────────────────────────────────────────────────┐
│  浏览器（Thymeleaf 模板 + jQuery + 原生 JS）                    │
│  ┌────────────┬────────────┬──────────────────────────────┐  │
│  │ 管理员 7 页 │ 会员 5 页   │ 普通用户 5 页                │  │
│  └────────────┴────────────┴──────────────────────────────┘  │
│                    AJAX / JSON                                │
└───────────────────────────┬──────────────────────────────────┘
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  Controller 层                                                │
│  SysUser / Member / ParkingSpot / ParkingRecord / Dashboard   │
│  Report / FeeRule / SysSetting / Plate / Ai / Demo            │
└───────────────────────────┬──────────────────────────────────┘
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  Service 层（@Transactional）                                  │
│  SysUser / Member / ParkingSpot / ParkingRecord / FeeRule     │
│  Income / AiAssistant / PlateRecognition                      │
└───────────────────────────┬──────────────────────────────────┘
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  Mapper 层（MyBatis 注解 SQL）                                 │
└───────┬───────────────┬───────────────┬──────────────────────┘
        ▼               ▼               ▼
   ┌─────────┐    ┌────────────┐   ┌──────────────┐
   │ MySQL   │    │ Redis 集群 │   │ FastDFS      │
   │ 唯一事实源│    │ 车位缓存   │   │ 会员头像      │
   └─────────┘    └────────────┘   └──────────────┘
        ▲
        │  工具调用（Tool Calling）
   ┌────┴──────────────┐        ┌──────────────────┐
   │ LangChain4j       │  HTTP  │ 阿里云百炼        │
   │ AiServices        │───────►│ qwen-plus / VL   │
   └───────────────────┘        └──────────────────┘
```

**关键设计**：`parkingrecord` 表中 `status='停车中'` 的记录是"用户是否在停车场"的**唯一事实源**；
`sysuser.stats` 只是镜像标记，每次出场后按"该用户剩余在场车辆数"重算，避免双车牌只出一辆时状态误关。

---

## 四、数据库设计

数据库名 `aaa`，包含 8 张业务表。仓库不附带本地数据库导出或演示数据。

### 4.1 `sysuser` 系统用户表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | int | 主键，自增 |
| username | varchar(50) | 登录账号 |
| password | varchar(100) | **BCrypt 加密**存储 |
| realname | varchar(50) | 真实姓名 |
| phone | varchar(20) | 手机号 |
| role | varchar(20) | 管理员 / 普通用户 / 会员 |
| stats | varchar(20) | 是否停车（镜像标记，非事实源） |
| status | varchar(10) | 启用 / 禁用 |

### 4.2 `member` 会员信息表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | int | 主键 |
| userid | int | 用户 ID（一个用户一条会员记录） |
| platenumber | varchar(20) | 主车牌 |
| platenumberstate | varchar(10) | 主车牌是否在场 |
| spareplatenumber | varchar(20) | 副车牌 |
| spareplatenumberstate | varchar(10) | 副车牌是否在场 |
| opendate / expiredate | datetime | 开通 / 到期时间 |
| price / perioddays | double / int | 开通价格（200）/ 有效天数（30） |
| status | varchar(20) | 有效 / 过期 |
| avatar | varchar(255) | 头像 URL（FastDFS） |

### 4.3 `parkingspot` 车位信息表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | int | 主键 |
| spotcode | varchar(20) | 车位编号（A001） |
| spottype | varchar(20) | 普通 / 会员 |
| status | varchar(20) | 空闲 / 已占用 / 维修中 |
| remark | varchar(200) | 备注 |

### 4.4 `parkingrecord` 停车记录表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | int | 主键 |
| userid / platenumber / spotcode | — | 用户、车牌、车位 |
| entrytime / exittime | datetime | 入场 / 出场时间 |
| duration | int | 停放分钟数 |
| fee | decimal(10,2) | 费用 |
| isfree | varchar(10) | 免费 / 收费 |
| status | varchar(20) | **停车中 / 已结算**（在场事实源） |

### 4.5 `paymentrecord` 支付订单表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | int | 主键 |
| userid / platenumber | — | 用户、车牌 |
| paytime | datetime | 支付时间 |
| ordertype | varchar(20) | 停车费 / 会员充值 |
| amount | double | 金额 |

> 仪表盘与报表的**所有收入统计均基于此表实时聚合**，不使用存在偏差的 `income` 表。

### 4.6 `feerule` 费率规则表（全局一行）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | int | 主键 |
| rulename | varchar(50) | 规则名称 |
| freeminutes | int | 免费时长（分钟） |
| firsthourfee | double | 首小时费用 |
| incrementalfee | double | 每小时递增 |
| maxhourfee | double | 每小时封顶 |
| maxdayfee | double | 每日封顶 |

### 4.7 `syssetting` 系统设置表（全局一行）

| 字段 | 类型 | 说明 |
|---|---|---|
| parkingname / totalspots | — | 停车场名称 / 总车位数 |
| freeminutes | int | 免费时长（仅展示） |
| contactphone / address | — | 电话 / 地址 |

### 4.8 `income` 收入汇总表（历史遗留）

`dailyincome / monthlyincome / yearlyincome / totalincome` 四个累加字段。
**注意**：该表为累加型，历史上与 `paymentrecord` 明细存在偏差，报表已不再依赖它。

---

## 五、环境依赖与端口清单

### 5.1 本项目自身服务

| 服务 | 地址 / 端口 | 用途 | 是否必需 |
|---|---|---|---|
| **应用** | `127.0.0.1:8899` | Spring Boot 主端口 | ✅ |
| **MySQL** | `127.0.0.1:3306` | 数据库 `aaa` | ✅ |
| **Redis 集群** | `192.168.126.128:7000 ~ 7005` | 车位列表缓存 | ⭕ 可选（不可用时自动降级） |
| **FastDFS Tracker** | `192.168.126.128:22122` | 获取可用的 storage 节点 | ⭕ 仅头像上传需要 |
| **FastDFS Storage** | `192.168.126.128:23000` | **文件上传（客户端直连）** | ⭕ 仅头像上传需要 |
| **FastDFS Nginx** | `192.168.126.128:8888` | 文件 HTTP 访问（头像展示） | ⭕ 仅头像上传需要 |

### 5.2 Redis 集群端口说明

6 个节点（3 主 3 从）：

| 节点 | 业务端口（客户端连接） | 集群总线端口（节点间通信） |
|---|---|---|
| redis-7000 | **7000** | **17000** |
| redis-7001 | **7001** | **17001** |
| redis-7002 | **7002** | **17002** |
| redis-7003 | **7003** | **17003** |
| redis-7004 | **7004** | **17004** |
| redis-7005 | **7005** | **17005** |

> **集群总线端口 = 业务端口 + 10000**，用于节点之间交换集群状态。
> 客户端只需访问 7000~7005；17000~17005 仅在**各节点之间**需要互通。
> 如果 Redis 和本机不在同一台机器，两端防火墙都要放行这两组端口。

放行命令（CentOS / firewalld）：

```bash
firewall-cmd --permanent --add-port=7000-7005/tcp
firewall-cmd --permanent --add-port=17000-17005/tcp
firewall-cmd --reload
firewall-cmd --list-ports
```

验证集群状态：

```bash
redis-cli -h 192.168.126.128 -p 7000 cluster info
# 期望：cluster_state:ok  cluster_slots_assigned:16384
redis-cli -h 192.168.126.128 -p 7000 cluster nodes
```

### 5.3 FastDFS 端口说明

FastDFS 的上传和下载走**两条不同的通道**，三个端口缺一不可：

```
上传文件：客户端 ──直连──► storage:23000      ← 客户端必须能访问
下载文件：浏览器 ──HTTP──► nginx:8888        ← 浏览器必须能访问
取节点地址：客户端 ──────► tracker:22122      ← 客户端必须能访问
```

| 端口 | 服务 | 必需性 |
|---|---|---|
| **22122** | Tracker | 客户端连接 tracker 查询可用 storage |
| **23000** | Storage | **客户端直连上传文件**（最容易被防火墙漏掉） |
| **8888** | Nginx | 浏览器访问已上传的文件 |

放行命令：

```bash
firewall-cmd --permanent --add-port=22122/tcp
firewall-cmd --permanent --add-port=23000/tcp
firewall-cmd --permanent --add-port=8888/tcp
firewall-cmd --reload
```

验证：

```powershell
Test-NetConnection 192.168.126.128 -Port 22122   # 期望 True
Test-NetConnection 192.168.126.128 -Port 23000   # 期望 True
Test-NetConnection 192.168.126.128 -Port 8888    # 期望 True
```

> **常见故障**：22122 和 8888 通、只有 23000 不通时，表现为"图片能打开、但头像上传一直失败"。
> `23000` 连不上时 FastDFS 客户端会报 `SocketTimeoutException: Connect timed out`。

Docker 部署参考（[`delron/fastdfs`](https://hub.docker.com/r/delron/fastdfs)）：

```bash
docker run -d --name tracker --network host \
  -v /var/fdfs/tracker:/var/fdfs delron/fastdfs tracker

docker run -d --name storage --network host \
  -e TRACKER_SERVER=192.168.126.128:22122 \
  -v /var/fdfs/storage:/var/fdfs delron/fastdfs storage
```

### 5.4 端口汇总表

| 端口 | 归属 | 谁需要访问 |
|---|---|---|
| 8899 | Spring Boot | 浏览器 |
| 3306 | MySQL | 应用服务器 |
| 7000-7005 | Redis 集群业务口 | 应用服务器 |
| 17000-17005 | Redis 集群总线 | 各 Redis 节点之间 |
| 22122 | FastDFS Tracker | 应用服务器 |
| 23000 | FastDFS Storage | 应用服务器 |
| 8888 | FastDFS Nginx | 浏览器 |

---

## 六、快速开始

### 6.1 前置条件

| 依赖 | 版本要求 | 是否必需 |
|---|---|---|
| JDK | 17+ | ✅ |
| Maven | 3.6.3+ | ✅ |
| MySQL | 8.0+ | ✅ |
| Redis Cluster | 6.x（3 主 3 从） | ⭕ 不装也能跑（自动降级查库） |
| FastDFS | 5.x + nginx | ⭕ 不装则头像上传不可用 |
| 阿里云百炼 API Key | — | ⭕ 不配则两个 AI 功能降级提示 |

### 6.2 初始化数据库

创建 MySQL 数据库 `aaa`，并准备项目所需的数据表和初始数据。出于隐私与凭据安全考虑，仓库不包含本地数据库导出文件或演示账号数据。

### 6.3 配置数据库与缓存

数据库连接通过环境变量提供，不要将真实账号或密码写入配置文件：

```powershell
# Windows PowerShell（当前会话）
$env:DB_USERNAME = "你的数据库用户"
$env:DB_PASSWORD = "你的强密码"
```

```bash
# Linux / macOS
export DB_USERNAME="你的数据库用户"
export DB_PASSWORD="你的强密码"
```

Redis 集群节点在 `src/main/resources/application.yml` 中配置为部署环境对应的地址。

FastDFS 地址在 `src/main/resources/fast.properties`：

```properties
fastdfs.tracker_servers=192.168.126.128:22122
```

### 6.4 配置大模型 Key（可选）

两个 AI 功能需要阿里云百炼的 API Key。**不要把 Key 写进代码或提交到仓库**，用环境变量：

```powershell
# Windows（PowerShell，当前用户永久生效）
[Environment]::SetEnvironmentVariable("aliQwen-api", "sk-你的key", "User")
```
```bash
# Linux / macOS
export aliQwen-api="sk-你的key"
```

配置读取顺序（见 `application.yml`）：

```yaml
api-key: ${aliQwen-api:${DASHSCOPE_API_KEY:}}
```

> 未配置时不会报错：车牌识别会返回"识别未启用"的提示，AI 助手同样降级提示，其他功能不受影响。

### 6.5 启动

```bash
# 开发运行
mvn spring-boot:run

# 或先打包再运行
mvn clean package -DskipTests
java -jar target/aaa-0.0.1-SNAPSHOT.war
```

浏览器打开 <http://127.0.0.1:8899>，未登录会自动跳转登录页。

### 6.6 账号

仓库不包含预置账号或演示数据。首次运行前，请按项目的认证逻辑在本地创建管理员和测试账号，并为每个账号设置唯一的强密码。

---

## 七、配置说明

### 7.1 `application.yml` 关键项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `server.port` | 8899 | 应用端口 |
| `spring.servlet.multipart.max-file-size` | 10MB | 车牌识别要传照片，Spring 默认 1MB 不够 |
| `spring.data.redis.timeout` | 800ms | 命令超时。默认 60 秒，Redis 挂掉时会把请求拖死 |
| `spring.data.redis.connect-timeout` | 1s | 连接超时 |
| `app.redis.fail-open-seconds` | 30 | 熔断窗口：Redis 失败后多少秒内不再尝试 |
| `vision.model-name` | `qwen3-vl-plus` | 车牌识别用的视觉模型 |
| `ai.chat.model-name` | `qwen-plus` | AI 助手用的文本模型 |
| `ai.chat.memory-idle-minutes` | 30 | AI 会话记忆空闲多久回收 |
| `ai.chat.memory-sweep-ms` | 600000 | 记忆清理任务扫描间隔（10 分钟） |

### 7.2 环境变量

| 变量名 | 说明 |
|---|---|
| `DB_USERNAME` | MySQL 数据库用户名（必需） |
| `DB_PASSWORD` | MySQL 数据库密码（必需） |
| `aliQwen-api` | 阿里云百炼 API Key（优先使用） |
| `DASHSCOPE_API_KEY` | 同上，兼容写法 |

### 7.3 `fast.properties`

```properties
fastdfs.connect_timeout_in_seconds=10
fastdfs.network_timeout_in_seconds=30
fastdfs.charset=UTF-8
fastdfs.tracker_servers=192.168.126.128:22122
```

---

## 八、接口清单

> 除特别说明外，所有接口**均需登录**（未登录返回 302 跳转登录页）。

### 8.1 用户与登录 `/SysUser/`

| 接口 | 参数 | 说明 | 鉴权 |
|---|---|---|---|
| `doLogin` | username, password | 登录，返回 `{bool, t:用户}` | 放行 |
| `addUser` | username, password, realname, phone, role | 注册 | 放行 |
| `logout` | — | 退出登录（同时清理 AI 记忆） | 放行 |
| `findByUsername` | username | 按账号查用户（页面用于取 userid） | 需登录 |
| `findstatus` | username | 查询是否在场 | 需登录 |
| `updatestatsopen` / `updatestatsclose` | username | 修改在场标记 | 需登录 |
| `findSysUserById` | id | 按 ID 查用户 | 需登录 |

### 8.2 车位 `/ParkingSpot/`

| 接口 | 参数 | 说明 |
|---|---|---|
| `findAll` | — | 全部车位（走 Redis 缓存，带熔断降级） |
| `findfreeSpot` | — | 空闲普通车位（A 区，供普通用户入场） |
| `findAllParkingSpot` | — | 全部空闲车位（供会员入场） |
| `grid` | — | 车位网格：车位 + 当前在场车辆（状态以在场记录校正） |
| `add` | spotcode, spottype, status, remark | 新增车位 |
| `update` | spotcode, spottype, status, remark | 修改车位（有车在场时不允许改状态） |
| `delete` | spotcode | 删除车位（有车在场时不允许删除） |

### 8.3 停车记录 `/ParkingRecord`

| 接口 | 参数 | 说明 |
|---|---|---|
| `addParkingRecord` | userid, platenumber, spotcode | 普通用户入场 |
| `updateParkingRecord` | userid | 普通用户出场结算 |
| `findParkingRecordById` | userid | 当前在场记录 |
| `findByUserid` | userid | 某用户全部记录 |
| `all` | — | 全场记录（管理员查询页） |
| `findCountByUserid` / `findSumFeeByUserid` | userid | 累计次数 / 累计消费 |
| `addmemberParkingRecord` | userid, platenumber, spotcode | 会员免费入场 |
| `updatememberParkingRecord` | platenumber, spotcode | 会员出场（过期则按普通用户计费） |
| `findParkingRecordByPlatenumber` | platenumber | 按车牌查在场记录 |
| `updateisfreetomember` | userid | 把在场记录标记为免费 |

### 8.4 会员 `/Member/`

| 接口 | 参数 | 说明 |
|---|---|---|
| `list` | — | 会员列表（含姓名、电话、剩余天数） |
| `addMember` | userid, platenumber, spareplatenumber | 开通会员（200 元 / 30 天） |
| `findMemberByUserId` | userid | 查会员信息 |
| `updateaddexpiredate` | userid | 续费一个月 |
| `uploadAvatar` | file, userid | 头像上传到 FastDFS |
| `updateplatenumberstateopenByplatenumber` | platenumber | 车牌置为在场 |
| `findplatenumberByUserId` / `findspareplatenumberByUserid` | userid | 查绑定车牌 |
| `findplatenumberstateByplatenumber` | platenumber | 查车牌在场状态 |

### 8.5 统计与管理 `/Dashboard/` `/Report/` `/FeeRule/` `/SysSetting/`

| 接口 | 说明 |
|---|---|
| `GET Dashboard/summary` | 仪表盘聚合数据（车位统计、在场车辆、近 7 天收入） |
| `GET Report/summary` | 报表：四档收入 + 本月车流量 + 每日明细 |
| `GET FeeRule/get` / `POST FeeRule/save` | 读取 / 保存费率规则（保存后立即生效） |
| `GET SysSetting/get` / `POST SysSetting/save` | 读取 / 保存系统设置 |

### 8.6 AI 接口

| 接口 | 参数 | 说明 |
|---|---|---|
| `POST Plate/recognize` | file（multipart 图片） | 车牌识别，返回 `{success, plate, valid, message}` |
| `POST Ai/chat` | message, username | AI 助手对话（每人每分钟限 15 次） |
| `GET Ai/history` | username | 拉取历史对话（页面刷新后回放） |
| `POST Ai/clear` | username | 清空当前对话记忆 |

---

## 九、核心设计要点

### 9.1 在场状态：单一事实源

`parkingrecord` 中 `status='停车中'` 的记录是唯一事实源；`sysuser.stats` 只是镜像。
每次出场结算后重新统计"该用户剩余在场车辆数"，为 0 才把 `stats` 置为"否"。
这样**家庭会员双车牌只出一辆时，状态不会被误关**。

### 9.2 费率规则动态化

计费不再写死在代码里：

```
feerule 表 → FeeRuleService（启动加载 + 保存刷新缓存）→ ParkingFeeUtil.calculateFee(..., rule)
                                                        ↘ AI 助手的费用预估（同一份规则）
```

- 管理页修改规则后**下一笔出场立即生效**，无需重启；
- 保存时做范围校验（免费时长 0~1440 分钟、每小时封顶不低于首小时费用）；
- 表为空时退回代码内的默认规则，保证不会因缺数据而无法计费。

**验证方式**：同一"停 20 分钟"场景，规则 30 分钟免费时收 0 元，改成 15 分钟后收 5 元。

### 9.3 车位缓存：三级降级 + 熔断 + 恢复失效

```
Redis 正常           → 命中缓存直接返回（60 秒 TTL）
Redis 故障           → 熔断打开，30 秒内完全跳过 Redis
   └─ 本地缓存（5 秒）→ 过期则查库
Redis 恢复           → 若故障期间有写操作（入场/出场/改车位），
                        先删除旧缓存再回源，避免读到故障前的快照
```

- 命令超时从默认 60 秒收紧到 **800ms**，避免慢请求；
- 故障期间只打一条告警日志，不刷屏；
- 出入场清缓存同样走熔断，不会让进出场流程陪着等超时。

### 9.4 FastDFS：懒加载 + 可重连

`FastDFSUtils` 不使用静态初始化块（原实现一次失败会导致该类在 JVM 内**永久不可用**）：

- 第一次上传时才建立连接（双检锁）；
- 任何一次调用异常都会 `reset()`，下次调用自动重连；
- tracker 无可用 storage 时给出明确原因，而不是 NPE 或干等超时。

### 9.5 AI 助手：工具调用而非"看图查库"

模型不接触 SQL，只能通过 5 个 `@Tool` 方法取数，且：

- **工具按用户构造**，`userId` 由 Java 传入而非模型参数 → 模型无法越权查询他人数据；
- 权限判断写在工具内部（收入工具校验 `role`），不依赖提示词；
- 工具返回"人话"字符串，而不是实体对象，减少 token 与幻觉；
- 对话记忆按 `userId` 分桶，窗口 12 条，空闲 30 分钟自动回收，登出即清。

### 9.6 会员过期：双保险

| 防线 | 位置 | 说明 |
|---|---|---|
| 实时判断 | 出场结算 | `expiredate < now()` 则按普通用户标准计费 |
| 定时兜底 | `MemberExpireTask` | 每天 03:00 把过期会员的 `status` 改为过期、角色降级为普通用户 |

> 注意：`@Scheduled` 只在应用运行期间生效，若应用不是 7×24 常驻，该任务可能不会触发。
> 收入正确性由"实时判断"保证，定时任务只负责权限清理。

---

## 十、测试

```bash
# 只跑缓存降级相关的单元测试（不依赖 MySQL / Redis，全部 mock）
mvn test -Dtest=ParkingCacheDegradationTest
```

覆盖 4 个场景：

| 用例 | 验证内容 |
|---|---|
| `cacheHit` | Redis 正常时命中缓存，完全不查库 |
| `degradeWhenRedisDown` | Redis 异常时降级查库，数据正确，熔断打开 |
| `skipRedisWhileOpen` | 熔断打开后不再尝试 Redis（只尝试 1 次） |
| `invalidateAfterRecovery` | 故障期间有写操作 → 恢复后先删旧缓存，再从库回源 |

> ⚠️ 不要直接执行 `mvn test`：仓库内的 `AaaApplicationTests` 是 `@SpringBootTest`，
> 需要连接真实 MySQL 与 Redis，否则会失败。请用 `-Dtest=` 指定测试类。

---

## 十一、打包部署

```bash
mvn clean package -DskipTests
```

产物为 `target/aaa-0.0.1-SNAPSHOT.war`（`pom.xml` 中 `<packaging>war</packaging>`）。

- **内嵌 Tomcat 运行**：`java -jar target/aaa-0.0.1-SNAPSHOT.war`
- **外部 Tomcat 部署**：需要把 `war` 放入 `webapps`，同时注意
  `spring-boot-starter-tomcat` 的 `provided` 范围与 `SpringBootServletInitializer` 的继承，当前项目未做这两项配置，
  **推荐直接用 `java -jar` 方式运行**。

部署前检查清单：

- [ ] 已通过 `DB_USERNAME` 和 `DB_PASSWORD` 设置数据库账号与密码
- [ ] `fast.properties` 中的 tracker 地址已修改
- [ ] 防火墙放行 8899（应用）、23000（FastDFS Storage）
- [ ] 环境变量 `aliQwen-api` 已配置（需要 AI 功能时）
- [ ] 已准备并导入符合项目 Mapper 的数据库结构和初始数据

---

## 十二、安全说明

| 措施 | 说明 |
|---|---|
| 密码加密 | BCrypt（`$2a$10$...`），启动时会自动把历史明文密码加密迁移 |
| 页面授权 | 管理员页 `/dashboard.html` 等 → `ROLE_ADMIN`；`/user*.html` → `ROLE_USER`；`/member*.html` → `ROLE_MEMBER` |
| 接口鉴权 | 除登录/注册/退出外，**所有 Ajax 接口均要求登录**（`anyRequest().authenticated()`） |
| 路径白名单 | 页面渲染只允许 19 个已知模板，避免"任意路径渲染任意模板"逃逸 |
| 出生点 | 根路径 `/` 未登录跳登录页，已登录按角色跳各自工作台 |
| AI 接口限流 | 每人每分钟 15 次，防止 Key 被刷 |
| 防越权 | AI 工具的用户 ID 由后端注入，不接受模型传参 |

> 演示项目，**未开启 CSRF**（Ajax 场景）、未做细粒度接口级角色校验（例如普通用户可以调用管理员的查询接口），
> 生产部署前需要补全。详见下一节。

---

## 十三、已知限制与后续优化

| 问题 | 现状 | 建议 |
|---|---|---|
| 注册接口接受 `role` 参数 | `/SysUser/addUser` 可指定任意角色 | 生产环境应固定为"普通用户"，或关闭注册 |
| `findByUsername` 返回完整用户对象 | 含 BCrypt 密码哈希 | 改为返回 DTO，剔除 password |
| 会话中的角色不会热更新 | 会员升级/过期后需重新登录才生效 | 登录时实时查库，或降级后主动失效会话 |
| 定时任务依赖应用常驻 | 应用不常驻时 `MemberExpireTask` 不会触发 | 启动时先执行一次，或改用调度中心 |
| `income` 表口径漂移 | 累加型，与 `paymentrecord` 明细有偏差 | 报表已改用 `paymentrecord` 实时聚合；该表可废弃 |
| AI 会话记忆存内存 | 重启即失效，多实例不共享 | 可替换为 Redis 存储（LangChain4j 提供 `ChatMessageSerializer`） |
| 前端样式重复 | 19 个模板各自内联一份外壳样式 | 可抽取为公共 `app.css`（部分已完成） |
| 车牌识别依赖公网 | 需要访问阿里云百炼 | 无法联网时功能降级提示 |

---

## 十四、目录结构

```
aaa/
├── pom.xml
├── sql/
│   └── aaa.sql                      # 完整数据库导出（8 张表 + 演示数据）
├── src/main/java/com/jr/
│   ├── AaaApplication.java          # 启动类（@EnableScheduling）
│   ├── ai/
│   │   ├── ParkingAssistant.java    # AI 助手接口定义（@SystemMessage）
│   │   └── ParkingTools.java        # 5 个 @Tool 方法（查库）
│   ├── controller/                  # 11 个控制器
│   ├── dto/                         # 接口返回结构
│   ├── entity/                      # 8 个实体
│   ├── mapper/                      # MyBatis 注解 Mapper
│   ├── service/                     # 服务接口与实现
│   ├── task/MemberExpireTask.java   # 会员过期定时任务
│   └── util/
│       ├── SecurityConfig.java      # 安全配置
│       ├── ParkingFeeUtil.java      # 计费算法
│       ├── FastDFSUtils.java        # 文件存储
│       ├── RedisGuard.java          # Redis 熔断器
│       └── RedisConfig.java         # 序列化配置
├── src/main/resources/
│   ├── application.yml
│   ├── fast.properties
│   ├── static/
│   │   ├── css/app.css              # 公共样式
│   │   └── js/                      # jquery / mini-chart / toast / assistant / admin-shell
│   └── templates/                   # 19 个 Thymeleaf 页面
└── src/test/java/com/jr/
    ├── aaa/AaaApplicationTests.java
    └── cache/ParkingCacheDegradationTest.java   # 缓存降级单元测试
```

---

## 致谢

- 大模型能力由 [阿里云百炼](https://bailian.console.aliyun.com/) 提供（`qwen3-vl-plus` / `qwen-plus`）
- AI 编排基于 [LangChain4j](https://github.com/langchain4j/langchain4j)
- 文件存储使用 [FastDFS](https://github.com/happyfish100/fastdfs) 与 [`delron/fastdfs`](https://hub.docker.com/r/delron/fastdfs) 镜像

> 本项目为个人学习 / 毕业设计用途，如用于生产请先完成「第十三节」中的安全整改项。
