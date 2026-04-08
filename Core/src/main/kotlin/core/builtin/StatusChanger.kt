package tw.xinshou.discord.core.builtin

import tw.xinshou.discord.core.config.BotConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity

class StatusChanger(
    private val jda: JDA,
    private val config: BotConfig.StatusChangerConfig,
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (config.activities.isEmpty()) return
        job = scope.launch {
            while (isActive) {
                for (activity in config.activities) {
                    val parts = activity.split(";", limit = 3)
                    if (parts.size < 3) continue
                    val type = Activity.ActivityType.valueOf(parts[0].uppercase())
                    val name = parts[1]
                    val delay = parts[2].toLongOrNull() ?: 5000L
                    jda.presence.activity = Activity.of(type, name)
                    delay(delay)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
