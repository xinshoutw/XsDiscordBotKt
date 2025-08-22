package tw.xinshou.core.base

import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xinshou.core.base.BotLoader.jdaBot
import tw.xinshou.core.builtin.consolelogger.ConsoleLogger
import tw.xinshou.core.builtin.statuschanger.StatusChanger

/**
 * This class manages the initialization of all listeners and the registration of guild-specific commands.
 */
internal class ListenerManager(
    private val guildCommands: List<CommandData>,
) : ListenerAdapter() {
    init {
        if (guildCommands.isNotEmpty()) {
            jdaBot.guilds.forEach { guild ->
                guild.updateCommands().addCommands(guildCommands).queue()
                logger.info("Guild loaded: {} ({})", guild.name, guild.id)
            }
        }

        StatusChanger.run()
        ConsoleLogger.run()

        logger.info("Bot ready.")
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        // Register guild-specific commands when the bot joins a new guild
        event.guild.updateCommands().addCommands(guildCommands).queue()
        logger.info("Guild joined: {} ({})", event.guild.name, event.guild.id)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
