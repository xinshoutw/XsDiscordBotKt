package tw.xinshou.discord.plugin.giveaway

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.discord.core.localizations.StringLocalizer
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.core.util.GlobalUtil
import tw.xinshou.discord.plugin.giveaway.command.CmdFileSerializer
import tw.xinshou.discord.plugin.giveaway.command.commandNameSet
import tw.xinshou.discord.plugin.giveaway.command.guildCommands
import tw.xinshou.discord.plugin.giveaway.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {
    private lateinit var localizer: StringLocalizer<CmdFileSerializer>

    private fun resolveLocale(localeTag: String): DiscordLocale {
        return runCatching { DiscordLocale.from(localeTag) }
            .getOrDefault(DiscordLocale.CHINESE_TAIWAN)
    }

    override fun load() {
        super.load()
        Giveaway.stopAutoDrawScheduler()

        if (!config.enabled) {
            logger.warn("Giveaway is disabled.")
            return
        }

        val defaultLocale = resolveLocale(config.defaultLocale)

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = defaultLocale,
            clazzSerializer = CmdFileSerializer::class,
        )

        Giveaway.reload(defaultLocale)
        Giveaway.startAutoDrawScheduler(config.autoDrawIntervalSeconds)
    }

    override fun reload() {
        super.reload()
        Giveaway.stopAutoDrawScheduler()

        if (!config.enabled) {
            logger.warn("Giveaway is disabled.")
            return
        }

        val defaultLocale = resolveLocale(config.defaultLocale)

        localizer = StringLocalizer(
            pluginDirFile = pluginDirectory,
            defaultLocale = defaultLocale,
            clazzSerializer = CmdFileSerializer::class,
        )

        Giveaway.reload(defaultLocale)
        Giveaway.startAutoDrawScheduler(config.autoDrawIntervalSeconds)
    }

    override fun guildCommands(): Array<CommandData> {
        return if (!config.enabled) {
            emptyArray()
        } else if (!::localizer.isInitialized) {
            emptyArray()
        } else {
            guildCommands(localizer)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkSlashCommand(event, commandNameSet)) return
        Giveaway.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Giveaway.onButtonInteraction(event)
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Giveaway.onStringSelectInteraction(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Giveaway.onEntitySelectInteraction(event)
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkModalIdPrefix(event, componentPrefix)) return
        Giveaway.onModalInteraction(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (!config.enabled) return
        Giveaway.onGuildLeave(event)
    }

    override fun unload() {
        Giveaway.stopAutoDrawScheduler()
        super.unload()
    }
}
