package tw.xinshou.discord.plugin.autorole

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.plugin.autorole.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("AutoRole is disabled.")
            return
        }
    }

    override fun reload() {
        super.reload()

        if (!config.enabled) {
            logger.warn("AutoRole is disabled.")
            return
        }

        AutoRole.reload()
    }

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (!config.enabled) return
        AutoRole.onGuildMemberJoin(event)
    }
}
