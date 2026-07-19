<div align="center">

# XsDiscordBotKt

![Version](https://img.shields.io/badge/version-4.0.*-blue?style=for-the-badge&logo=github)
![License](https://img.shields.io/badge/License-Apache%202.0-green?style=for-the-badge&logo=apache)
![Java](https://img.shields.io/badge/Java-21+-orange?style=for-the-badge&logo=openjdk)

</div>

以 Kotlin 撰寫的模組化 Discord 機器人框架，基於 [JDA](https://github.com/discord-jda/JDA)。

核心（Core）只負責啟動、插件載入與生命週期管理；所有功能都以插件形式提供。插件是獨立編譯的 JAR，啟動時從 `plugins/` 目錄載入，各自帶有自己的設定檔與語言檔，可透過控制台指令重新載入，不需重啟程式。

## 需求

- JDK 21 以上
- Discord Bot Token
- MongoDB：預設使用內嵌實例，開箱即用；也可在設定檔中改連外部資料庫

## 安裝

從 [Releases](https://github.com/xinshoutw/XsDiscordBotKt/releases) 下載打包好的版本，或自行編譯：

```bash
git clone https://github.com/xinshoutw/XsDiscordBotKt.git
cd XsDiscordBotKt
./gradlew build
```

編譯完成後，Core 的 JAR 會輸出到 `DevServer/`，插件 JAR 輸出到 `DevServer/plugins/`。

## 設定

第一次執行會在工作目錄產生 `config.yaml`，填入 Bot Token 後重新啟動即可：

```yaml
bot_token: "YOUR_BOT_TOKEN_HERE"

# 預設使用內嵌 MongoDB；改用外部資料庫時將 embedded 設為 false
database:
  embedded: true
  connection_string: "mongodb://localhost:27017"

# 機器人狀態輪播，格式為「類型;文字;間隔毫秒」
status_changer:
  activities:
    - "COMPETING;Developing...;5000"

# 將互動紀錄轉發到指定的 Discord 頻道
console_loggers:
  - guild_id: 0
    channel_id: 0
    log_types: [ "command", "button", "modal" ]
    format: "[%cl_type%] %user_name% `%cl_interaction_string%`"

# 自動 defer 斜線指令回覆
auto_defer_replies: true
```

各插件的設定檔在 `plugins/<插件名稱>/` 底下，第一次載入時會自動產生預設值。

## 執行

```bash
java -jar XsDiscordBotKt.jar
```

可用的命令列參數：

| 參數 | 簡寫 | 說明 |
|------|------|------|
| `--token <token>` | `-t` | 覆寫設定檔中的 Bot Token |
| `--log-level <level>` | `-l` | 日誌等級，預設 `INFO` |
| `--no-online` | `-N` | 離線模式，不連線 Discord |
| `--ignore-update` | `-I` | 跳過啟動時的版本檢查 |
| `--no-auto-defer-interactions` | | 關閉互動回覆的自動 defer |
| `--force-renew-lang-resources` | `-Flang` | 強制重新匯出所有插件的語言檔，會覆寫既有檔案 |

啟動後可在控制台輸入：

- `reload` — 重新載入插件與設定
- `stop`（或 `exit`、`shutdown`）— 關閉機器人

## 內建插件

伺服器管理：

- **AutoRole** — 成員加入時自動分配角色
- **DynamicVoiceChannel** — 動態語音頻道，進入母頻道自動建立、離開自動清除
- **Ticket / TicketAddons** — 工單系統與擴充功能
- **WelcomeByeGuild** — 成員加入與離開通知

紀錄與資訊：

- **ChatLogger** — 訊息紀錄
- **VoiceLogger** — 語音頻道活動紀錄
- **BotInfo** — 機器人狀態與統計

其他功能：

- **MusicPlayer** — 音樂播放
- **Economy** — 虛擬貨幣與經濟系統
- **Giveaway** — 抽獎活動
- **SimpleCommand** — 自訂指令
- **Feedbacker** — 回饋收集
- **IntervalPusher** — 定時訊息推送
- **BasicCalculator** — 計算機
- **NtustManager / NtustCourse** — 台科大專用功能

API 插件（供其他插件使用）：

- **AudioAPI** — 音訊處理
- **GoogleSheetAPI** — Google 試算表整合
- **SQLiteAPI** — SQLite 資料庫存取

## 專案結構

```
XsDiscordBotKt/
├── Core/            # 核心：啟動流程、插件載入、指令註冊、資料層
├── Plugins/         # 各插件模組（含 _Example 範例插件）
│   └── API/         # 供其他插件使用的 API 插件
├── Web-Dashboard/   # 網頁管理介面
├── build-logic/     # Gradle convention plugins
└── DevServer/       # 開發環境的建構輸出目錄
```

## 插件開發

插件的進入點是一個實作 `Plugin` 介面的 Kotlin `object`：

```kotlin
object Event : Plugin {
    override lateinit var config: PluginConfig

    override fun PluginContext.onLoad() {
        // 載入設定、註冊資源
    }

    override fun commands(): List<CommandHandler> {
        // 回傳此插件提供的斜線指令
        return emptyList()
    }
}
```

建立新插件的步驟：

1. 在 `Plugins/` 下建立模組，`build.gradle.kts` 套用 `id("xs-plugin")`（需打包依賴時改用 `xs-plugin-shadow`）
2. 在模組的 `gradle.properties` 加上 `version.patch`
3. 在根目錄 `settings.gradle.kts` 加入 include
4. 在 `src/main/resources/info.yaml` 的 `main` 欄位指向進入點 object
5. 若在執行期依賴其他插件，build 中宣告 `compileOnly(project(...))`，並在 `info.yaml` 的 `depend_plugins` 列出

完整範例請參考 `Plugins/_Example/`。

## 貢獻

歡迎發 PR。請先 fork 並開分支，commit 訊息遵循 conventional commits 格式，送出前確認 `./gradlew build` 通過。

問題回報請走 [GitHub Issues](https://github.com/xinshoutw/XsDiscordBotKt/issues)。

## 授權

Apache License 2.0，詳見 [LICENSE](LICENSE)。
