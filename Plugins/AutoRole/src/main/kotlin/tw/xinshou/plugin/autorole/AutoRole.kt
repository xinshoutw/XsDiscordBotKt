package tw.xinshou.plugin.autorole

import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import tw.xinshou.loader.base.BotLoader.jdaBot

object AutoRole {
    private val trackedGuildIds: Set<Long> = Event.config.guilds.map { it.guildId }.toSet()
    private val addRoles: Map<Long, List<Role>> = Event.config.guilds.associate {
        it.guildId to it.roleIds.mapNotNull { roleId -> jdaBot.getRoleById(roleId) }
    }

    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (event.guild.idLong !in trackedGuildIds) return

        event.guild.modifyMemberRoles(event.member, addRoles.getValue(event.guild.idLong)).queue()
    }
}