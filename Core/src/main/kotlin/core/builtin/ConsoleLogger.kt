package core.builtin

import core.config.BotConfig
import core.placeholder.Substitutor
import net.dv8tion.jda.api.JDA

class ConsoleLogger(
    private val jda: JDA,
    private val configs: List<BotConfig.ConsoleLoggerConfig>,
) {
    fun logCommand(userName: String, commandString: String) {
        sendToChannels("command") { sub ->
            sub.put("cl_type", "CMD")
                .put("cl_interaction_string", commandString)
                .put("user_name", userName)
        }
    }

    fun logButton(userName: String, buttonId: String) {
        sendToChannels("button") { sub ->
            sub.put("cl_type", "BTN")
                .put("cl_interaction_string", buttonId)
                .put("user_name", userName)
        }
    }

    fun logModal(userName: String, modalId: String) {
        sendToChannels("modal") { sub ->
            sub.put("cl_type", "MODAL")
                .put("cl_interaction_string", modalId)
                .put("user_name", userName)
        }
    }

    private fun sendToChannels(logType: String, configureSub: (Substitutor) -> Unit) {
        for (config in configs) {
            if (logType !in config.logTypes) continue
            val channel = jda.getTextChannelById(config.channelId) ?: continue
            val sub = Substitutor()
            configureSub(sub)
            channel.sendMessage(sub.parse(config.format)).queue()
        }
    }
}
