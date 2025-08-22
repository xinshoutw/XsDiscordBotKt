package tw.xinshou.plugin.autorole

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import tw.xinshou.core.plugin.PluginEventConfigure
import tw.xinshou.plugin.autorole.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        AutoRole.onGuildMemberJoin(event)
    }
}
