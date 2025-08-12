package tw.xinshou.plugin.autorole

import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import tw.xinshou.loader.base.BotLoader.jdaBot
import tw.xinshou.plugin.autorole.Event.config

object AutoRole {
    private val trackedGuildIds: Set<Long> by lazy { config.guilds.map { it.guildId }.toSet() }
    private val addRoles: Map<Long, List<Role>> by lazy {
        config.guilds.associate {
            (it.guildId) to (it.roleIds.mapNotNull { roleId -> jdaBot.getRoleById(roleId) })
        }
    }

    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (event.guild.idLong !in trackedGuildIds) return

        event.guild.modifyMemberRoles(event.member, addRoles.getValue(event.guild.idLong)).queue()
    }
}