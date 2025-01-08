package tw.xserver.loader.base

import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tw.xserver.loader.base.BotLoader.jdaBot
import tw.xserver.loader.builtin.consolelogger.ConsoleLogger
import tw.xserver.loader.builtin.statuschanger.StatusChanger

/**
 * This class manages the initialization of all listeners and the registration of guild-specific commands.
 */
class ListenerManager(
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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
