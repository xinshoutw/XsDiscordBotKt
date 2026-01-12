package tw.xinshou.plugin.dynamicvoicechannel

import com.squareup.moshi.JsonAdapter
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.core.base.BotLoader.jdaBot
import tw.xinshou.core.json.JsonFileManager
import tw.xinshou.core.json.JsonFileManager.Companion.adapterReified
import tw.xinshou.core.json.JsonGuildFileManager
import tw.xinshou.plugin.dynamicvoicechannel.Event.pluginDirectory
import tw.xinshou.plugin.dynamicvoicechannel.json.DataContainer
import tw.xinshou.plugin.dynamicvoicechannel.json.JsonDataClass
import java.io.File
import java.util.concurrent.ConcurrentHashMap


/*
[
  {
    categoryId: 12345678901233
    defaultName: "《🔊》新語音頻道",
    formatName1: "｜%dvc@custom-name% "
    formatName2: "｜%dvc@custom-name% ├ %dvc@info%"
  }
]
 */


