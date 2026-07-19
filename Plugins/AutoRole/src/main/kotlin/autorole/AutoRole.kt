package tw.xinshou.discord.plugin.autorole

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent

object AutoRole {
    private var trackedGuildIds: Set<Long> = emptySet()
    private var addRoles: Map<Long, List<Role>> = emptyMap()
    private var initialized = false

    internal fun init(jda: JDA) {
        val guilds = Event.pluginConfig.guilds
        trackedGuildIds = guilds.map { it.guildId }.toSet()
        addRoles = guilds.associate { guild ->
            guild.guildId to guild.roleIds.mapNotNull { roleId -> jda.getRoleById(roleId) }
        }
        initialized = true
    }

    internal fun reload() {
        initialized = false
    }

    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (!initialized) {
            init(event.jda)
        }

        if (event.guild.idLong !in trackedGuildIds) return

        val roles = addRoles[event.guild.idLong] ?: return
        if (roles.isNotEmpty()) {
            event.guild.modifyMemberRoles(event.member, roles).queue()
        }
    }
}
