package tw.xinshou.discord.plugin.autorole

import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import tw.xinshou.discord.core.base.BotLoader.jdaBot
import tw.xinshou.discord.plugin.autorole.Event.config

object AutoRole {
    private var trackedGuildIds: Set<Long> = config.guilds.map { it.guildId }.toSet()
    private var addRoles: Map<Long, List<Role>> = config.guilds.associate {
        (it.guildId) to (it.roleIds.mapNotNull { roleId -> jdaBot.getRoleById(roleId) })
    }

    internal fun reload() {
        trackedGuildIds = config.guilds.map { it.guildId }.toSet()
        addRoles = config.guilds.associate {
            (it.guildId) to (it.roleIds.mapNotNull { roleId -> jdaBot.getRoleById(roleId) })
        }
    }

    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (event.guild.idLong !in trackedGuildIds) return

        event.guild.modifyMemberRoles(event.member, addRoles.getValue(event.guild.idLong)).queue()
    }
}