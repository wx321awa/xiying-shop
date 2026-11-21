# XiYingShop

曦荧玩家商店 —— 一个支持 **Java 箱子 GUI** 与 **基岩版 Floodgate 表单** 的 Minecraft 玩家交易插件，替代 EasyGuiShop。

## 特性

- **双端界面**：Java 玩家走箱子 GUI，基岩玩家走 Floodgate 表单（Cumulus），功能对等
- **双币种**：金币走 Vault（CMI 等经济插件），曦荧币走 PlayerPoints
- **玩家开店**：可配置开店费用、商品位数量、单条上架上限
- **防刷与经济校验**：单价 NaN 拦截、售罄后重复下架拦截、领收益跨重启幂等、价格上限 1 亿
- **基岩适配**：9 条/页分页，物品列表显示附魔与药水效果，上架分两步（选物品 → 填数量/单价/币种）
- **配置热重载**：`/pshop admin reload`，无需重启

## 运行环境

| 项目 | 要求 |
|---|---|
| 服务端 | Purpur / Paper 26.1.2（`api-version: 1.21`） |
| Java | 25 |
| 必需依赖 | Vault + 一个经济插件（如 CMI） |
| 可选依赖 | PlayerPoints（曦荧币）、floodgate（基岩支持）、Geyser-Spigot |

## 安装

1. 把 `XiYingShop-1.0.0.jar` 放进 `plugins/`
2. 确认已安装 Vault 与经济插件
3. 重启服务器
4. 控制台出现下面两行即正常：

```
[XiYingShop] 已载入 X 家店铺、Y 条商品
[XiYingShop] 经济挂钩：Vault=... / PlayerPoints=...
```

## 命令与权限

| 命令 | 说明 |
|---|---|
| `/pshop` | 打开玩家商店 |
| `/pshop admin reload` | 重载配置 |
| 别名 | `ps` `xyshop` `eshop` `market` `mall` `store` |

| 权限 | 默认 | 说明 |
|---|---|---|
| `xiyingshop.use` | 所有人 | 使用商店 |
| `xiyingshop.admin` | OP | 管理命令 |

## 配置

`plugins/XiYingShop/config.yml`：

| 键 | 默认 | 说明 |
|---|---|---|
| `prefix` | `§8[§b曦荧商店§8] §f` | 消息前缀 |
| `create-cost` | `50.0` | 开店费用（金币） |
| `tax-money` | `0.0` | 金币交易税 |
| `tax-points` | `0.0` | 曦荧币交易税 |
| `max-listings` | `27` | 每店商品位 |
| `max-amount-per-listing` | `2304` | 单条上架数量上限 |
| `min-unit-price` | `1.0` | 最低单价 |
| `money-enabled` | `true` | 启用金币 |
| `points-enabled` | `true` | 启用曦荧币 |
| `browse-page-size` | `45` | Java 浏览页大小 |
| `bedrock-page-size` | `9` | 基岩浏览页大小 |
| `shop-name-min` / `shop-name-max` | `2` / `12` | 店名字数限制 |
| `blacklist` | 见文件 | 禁止上架的物品（基岩、屏障、命令方块等） |

## 数据

店铺与交易记录存放在 `plugins/XiYingShop/data.json`，停服时由 `onDisable` 落盘。

> 基岩与 Java 玩家的 UUID 不同，同一个人的两个账号会被视为两个独立玩家。

## 构建

```bash
mvn -s settings.xml clean package
# 产物：target/XiYingShop-1.0.0.jar
```

`libs/` 下的 `Vault.jar`、`PlayerPoints.jar`、`floodgate.jar` 是 `system` scope 的本地依赖，构建时必须保留。

> `settings.xml` 只是本机代理配置，已加入 `.gitignore`；正常网络环境下不需要它：
> `mvn clean package`

## 版本说明

- 发布附件 `XiYingShop-1.0.0.jar`（96,196 B，MD5 `F64BFB02BF6AF195C148DF9F7C8A714A`）为线上实际运行的构建
- 仓库源码已与上述构建对齐：同一份代码重新编译后，class 列表逐项一致，字节码仅在异常表偏移与调试信息上有差异，行为等价
- 源码构建产物约 103 KB（包含行号与局部变量调试信息，因此比发布附件略大，属正常现象）

## 许可证

未指定。
