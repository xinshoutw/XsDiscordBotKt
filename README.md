# XsDiscordBotKt

<div align="center">

![Version](https://img.shields.io/badge/version-4.0.*-blue?style=for-the-badge&logo=github)
![License](https://img.shields.io/badge/License-Apache%202.0-green?style=for-the-badge&logo=apache)

![Java](https://img.shields.io/badge/Java-21+-orange?style=for-the-badge&logo=openjdk)
![Kotlin](https://img.shields.io/badge/Kotlin-JVM-purple?style=for-the-badge&logo=kotlin)
![Build](https://img.shields.io/badge/Build-Gradle-blue?style=for-the-badge&logo=gradle)

**模組化 Discord 伺服器管理機器人框架**

🚀 雲原生架構 • 🔥 熱重載支援 • 🧩 18+ 插件生態系統 • ⚡ i10n 支援

</div>

## 🌟 專案簡介

XsDiscordBotKt 是一個採用**雲原生架構**設計的 Discord 伺服器管理機器人框架，基於 **Kotlin** 和**響應式程式設計**原則開發。提供
**插件驅動**的模組化系統，支援**熱重載**、**零停機部署**，讓開發者能夠快速構建高效能、高可用的 Discord 機器人應用。

> 💡 **設計理念**：將複雜的伺服器管理功能拆解為獨立的插件模組，透過事件驅動架構實現鬆耦合設計，提供卓越的開發者體驗和運維效率。

### ✨ 核心特色

<table>
<tr>
<td width="50%">

**🏗️ 架構設計**

- 🧩 **插件驅動架構** - 18+ 功能模組，按需載入
- 🔄 **熱重載機制** - 零停機更新插件與配置
- 📡 **事件驅動模式** - 響應式程式設計範式
- 🏛️ **分層架構** - 核心/插件/API 清晰分離

</td>
<td width="50%">

**⚡ 技術優勢**

- 🚀 **Kotlin 協程** - 高併發異步處理
- 💾 **內嵌資料庫** - 開箱即用的 MongoDB
- 🎛️ **互動控制台** - JLine 增強型 CLI
- 🛡️ **生產就緒** - 完整監控與日誌系統

</td>
</tr>
<tr>
<td colspan="2">

**🔧 DevOps 自動化**

- 🚀 **CI/CD 流水線** - GitHub Actions 全自動構建發佈
- 📦 **容器化支援** - Docker 友好的部署方式
- 🔍 **可觀測性** - 結構化日誌與效能監控
- 🧪 **測試驅動** - 自動化測試與品質保證

</td>
</tr>
</table>

## 📦 功能模組

### 🎵 娛樂功能

- **MusicPlayer** - 完整的音樂播放器系統
- **BasicCalculator** - 數學計算功能
- **Giveaway** - 抽獎活動管理

### 👥 伺服器管理

- **AutoRole** - 自動角色分配系統
- **DynamicVoiceChannel** - 動態語音頻道管理
- **ChannelConverter** - 頻道類型轉換（支援論壇頻道）
- **Ticket & TicketAddons** - 完整工單系統

### 📊 資料記錄與監控

- **ChatLogger** - 聊天訊息記錄
- **VoiceLogger** - 語音頻道活動記錄
- **BotInfo** - 機器人狀態和統計資訊

### 💰 經濟系統

- **Economy** - 虛擬貨幣和經濟管理
- **RentSystem** - 租賃系統管理

### 🔧 實用工具

- **SimpleCommand** - 自定義命令系統
- **Feedbacker** - 用戶反饋收集
- **IntervalPusher** - 定時訊息推送
- **NtustManager** - 學校專用管理功能

### 🔌 API 整合

- **GoogleSheetAPI** - Google 試算表整合
- **SQLiteAPI** - 資料庫操作介面
- **AudioAPI** - 音頻處理功能

## 🛠️ 技術棧與需求

<details>
<summary><strong>📋 系統需求</strong></summary>

| 項目          | 需求        | 建議版本                           |
|-------------|-----------|--------------------------------|
| **JDK**     | Java 21+  | JetBrains Runtime 21.0.4b569.1 |
| **資料庫**     | MongoDB   | 7.0+                           |
| **記憶體**     | 最低 512MB  | 建議 2GB+                        |
| **Discord** | Bot Token | 具備必要權限                         |

</details>

### 🏗️ 核心技術棧

[//]: # (<table>)

[//]: # (<tr>)

[//]: # (<td width="33%">)

[//]: # ()

[//]: # (**🔧 開發框架**)

[//]: # (- ![Kotlin]&#40;https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white&#41; **協程與響應式**)

[//]: # (- ![JDA]&#40;https://img.shields.io/badge/JDA-5.0+-7289DA?logo=discord&logoColor=white&#41; **Discord API**)

[//]: # (- ![Gradle]&#40;https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white&#41; **建構系統**)

[//]: # ()

[//]: # (</td>)

[//]: # (<td width="33%">)

[//]: # ()

[//]: # (**💾 資料與配置**)

[//]: # (- ![MongoDB]&#40;https://img.shields.io/badge/MongoDB-47A248?logo=mongodb&logoColor=white&#41; **文件資料庫**)

[//]: # (- ![YAML]&#40;https://img.shields.io/badge/YAML-CB171E?logo=yaml&logoColor=white&#41; **配置管理**)

[//]: # (- ![JSON]&#40;https://img.shields.io/badge/JSON-000000?logo=json&logoColor=white&#41; **資料交換**)

[//]: # ()

[//]: # (</td>)

[//]: # (<td width="33%">)

[//]: # ()

[//]: # (**🔍 監控與工具**)

[//]: # (- ![Logback]&#40;https://img.shields.io/badge/Logback-FF6B6B?logo=&logoColor=white&#41; **日誌系統**)

[//]: # (- ![JLine]&#40;https://img.shields.io/badge/JLine-4CAF50?logo=&logoColor=white&#41; **CLI 介面**)

[//]: # (- ![Actions]&#40;https://img.shields.io/badge/GitHub_Actions-2088FF?logo=github-actions&logoColor=white&#41; **CI/CD**)

[//]: # ()

[//]: # (</td>)

[//]: # (</tr>)

[//]: # (</table>)

## 🚀 快速開始

### 1. 下載與直執行

#### 方法一：下載預構建版本（推薦）

```bash
# 從 GitHub Releases 下載最新版本
wget https://github.com/xinshoutw/XsDiscordBotKt/releases/download/v3.3.0/BotPack-v3.3.0.zip
```

#### 方法二：手動構建

```bash
# 複製專案
git clone https://github.com/xinshoutw/XsDiscordBotKt.git
cd XsDiscordBotKt

# 構建專案（需使用 JDK 21+）
./gradlew build

# 構建完成後的 JAR 檔案位於 `DevServer/` 與 `DevServer/plugins/`
```

### 2. 配置設定

首次運行會自動生成 `config.yaml` 配置檔案：

```yaml
general_settings:
  bot_token: "YOUR_DISCORD_BOT_TOKEN_HERE"

builtin_settings:
  status_changer_settings:
    activity_messages:
      - "🎵 播放音樂中..."
      - "🛡️ 守護伺服器"
      - "⚡ Kotlin 驅動"

  console_logger_settings:
    - guild_id: 123456789012345678
      channel_id: 987654321098765432
      log_type: [ "INTERACTION", "ERROR" ]
      format: "[%timestamp%] %level%: %message%"
```

### 3. 運行機器人

```bash
# 基本運行
java -jar XsDiscordBotKt.jar

# 使用自定義 token（開發模式）
java -jar XsDiscordBotKt.jar --token "YOUR_TOKEN_HERE"

# 離線模式（不讓機器人上線）
java -jar XsDiscordBotKt.jar --no-online

# 忽略版本檢查
java -jar XsDiscordBotKt.jar --ignore-update

# 設定日誌等級
java -jar XsDiscordBotKt.jar --level INFO

# 強制重新產生語言資源檔案（警告：會覆寫現有檔案）
java -jar XsDiscordBotKt.jar --force-renew-lang-resources
```

#### 📝 命令列參數詳細說明

| 參數                             | 簡寫       | 說明                              | 預設值   |
|--------------------------------|----------|---------------------------------|-------|
| `--token`                      | `-t`     | 設定機器人 Token                     | 無     |
| `--level`                      | `-l`     | 設定日誌記錄等級                        | INFO  |
| `--no-online`                  | `-N`     | 不讓機器人上線（離線模式）                   | false |
| `--ignore-update`              | `-I`     | 忽略來自 GitHub 的版本檢查               | false |
| `--force-renew-lang-resources` | `-Flang` | **警告**：強制重新匯出所有插件語言資源檔案，會覆寫現有檔案 | false |

#### 💡 使用範例

```bash
# 開發環境：使用自定義 token 並設定 DEBUG 日誌等級
java -jar XsDiscordBotKt.jar -t "YOUR_DEV_TOKEN" -l DEBUG

# 測試環境：離線模式，忽略版本檢查
java -jar XsDiscordBotKt.jar -N -I

# 維護模式：強制更新語言資源並使用 WARN 等級日誌
java -jar XsDiscordBotKt.jar --force-renew-lang-resources --level WARN
```

### 4. 插件管理

插件會自動從 `plugins/` 目錄載入。您可以：

- 將 `.jar` 插件檔案放入 `plugins/` 目錄
- 使用控制台命令 `reload` 重新載入插件
- 透過配置檔案啟用/停用特定功能

## 📋 使用指南

### 控制台命令

- `help` - 顯示所有可用命令
- `reload` - 重新載入插件和設定
- `stop` / `exit` - 安全停止機器人
- `status` - 顯示機器人狀態

### Discord 斜線命令

機器人支援 Discord 的斜線命令系統，具體命令取決於啟用的插件：

- `/info` - 顯示機器人資訊
- `/music play <歌曲>` - 播放音樂
- `/ticket create` - 建立工單
- `/economy balance` - 查看經濟狀態
- ...

## 🔧 開發指南

### 專案結構

```
XsDiscordBotKt/
├── Core/                    # 核心系統
│   └── src/main/kotlin/
│       └── tw/xinshou/core/
├── Plugins/                 # 插件模組
│   ├── API/                # API 插件
│   │   ├── AudioAPI/       # 音頻處理 API
│   │   ├── GoogleSheetAPI/ # Google 試算表 API
│   │   └── .../            # 其他 API 插件
│   ├── AutoRole/           # 角色管理
│   ├── MusicPlayer/        # 音樂播放器
│   └── .../                # 其他插件
├── Server/                 # 生產環境配置
├── DevServer/              # 開發環境配置
└── build.gradle.kts        # 構建配置
```

### 本地開發環境

1. **環境準備**
   ```bash
   # 確保安裝 JDK 21+
   java -version
   
   # 克隆專案
   git clone https://github.com/xinshoutw/XsDiscordBotKt.git
   cd XsDiscordBotKt
   ```

2. **IDE 設定**
   - 推薦使用 IntelliJ IDEA
   - 啟用 Kotlin 插件支援
   - 配置 JDK 21 專案設定

3. **開發環境運行**
   ```bash
   # 使用開發環境配置運行
   ./gradlew run
   
   # 或者構建並運行
   ./gradlew build
   java -jar build/libs/XsDiscordBotKt-*.jar
   ```

### 插件開發

插件開發需要實作核心介面並遵循插件生命週期：

```kotlin
class MyPlugin : Plugin {
    override fun load() {
        // 插件載入邏輯
    }

    override fun unload() {
        // 插件卸載邏輯  
    }
}
```

### 建構和部署

專案使用 GitHub Actions 進行自動化 CI/CD：

- 推送至 `main` 分支觸發自動構建
- 標籤推送觸發自動發佈
- 支援多環境部署配置

## 📊 版本資訊

### v3.0.0 主要更新

- ✨ 新增 GitHub Actions 自動化 CI/CD 流程
- 🔄 核心架構重構，遷移 loader 包至 core
- 🆕 新增 ChannelConverter 插件支援論壇頻道遷移
- 🔒 強化 URL 驗證安全性
- ⚡ 改進成員快取策略和效能
- 🎛️ 配置化 NTUST 監控功能
- 🚦 添加 API 限流機制，提升穩定性
- 🔧 標準化專案配置和依賴管理

## 🤝 貢獻指南

歡迎貢獻代碼！請遵循以下步驟：

1. Fork 此專案
2. 建立功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

### 開發規範

- 遵循 Kotlin 程式碼風格指南
- 為新功能添加適當的測試
- 更新相關文件
- 確保所有測試通過

## 📞 支援與回饋

- **Issues**: [GitHub Issues](https://github.com/xinshoutw/XsDiscordBotKt/issues)
- **文件**: 查看各插件目錄下的 README 檔案

## 📄 授權資訊

本專案採用 **Apache License 2.0** 開源授權。
詳細授權條款請參閱 [LICENSE](LICENSE) 檔案。

---

<div align="center">

**使用 ❤️ 和 Kotlin 開發**

[⭐ 給個星星](https://github.com/xinshoutw/XsDiscordBotKt) • [🐛 回報問題](https://github.com/xinshoutw/XsDiscordBotKt/issues) • [💡 功能建議](https://github.com/xinshoutw/XsDiscordBotKt/issues)

</div>