package tw.xinshou.discord.plugin.giveaway

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.discord.core.plugin.PluginEventConfigure
import tw.xinshou.discord.core.util.GlobalUtil
import tw.xinshou.discord.plugin.giveaway.command.guildCommands
import tw.xinshou.discord.plugin.giveaway.config.ConfigSerializer

object Event : PluginEventConfigure<ConfigSerializer>(true, ConfigSerializer.serializer()) {

    override fun load() {
        super.load()

        if (!config.enabled) {
            logger.warn("Giveaway is disabled.")
            return
        }
    }

    override fun reload() {
        super.reload()

        if (!config.enabled) {
            logger.warn("Giveaway is disabled.")
            return
        }
    }

    override fun guildCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (!config.enabled) return
        if (GlobalUtil.checkCommandString(event, "create-giveaway")) return
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
}
