package tw.xinshou.discord.plugin.autorole

import tw.xinshou.discord.core.config.ConfigLoader
import tw.xinshou.discord.core.plugin.Plugin
import tw.xinshou.discord.core.plugin.PluginConfig
import tw.xinshou.discord.core.plugin.PluginContext
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import tw.xinshou.discord.plugin.autorole.config.ConfigSerializer


object Event : Plugin {
    override lateinit var config: PluginConfig

    internal lateinit var pluginConfig: ConfigSerializer

    override fun PluginContext.onLoad() {
        pluginConfig = ConfigLoader.load(
            file = pluginDirectory.resolve("config.yaml"),
            default = "/config.yaml",
        )

        if (!pluginConfig.enabled) {
            logger.warn("AutoRole is disabled.")
            return
        }
    }

    override fun PluginContext.onReload() {
        onLoad()

        if (!pluginConfig.enabled) return

        AutoRole.reload()
    }

    override fun listeners(): List<Any> {
        if (!pluginConfig.enabled) return emptyList()
        return listOf(AutoRoleListener)
    }
}

private object AutoRoleListener : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (!Event.pluginConfig.enabled) return
        AutoRole.onGuildMemberJoin(event)
    }
}
