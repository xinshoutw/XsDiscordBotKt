package tw.xinshou.plugin.giveaway

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import tw.xinshou.core.plugin.PluginEvent
import tw.xinshou.core.util.GlobalUtil
import tw.xinshou.plugin.giveaway.command.guildCommands

object Event : PluginEvent(true) {

    override fun guildCommands(): Array<CommandData> = guildCommands

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (GlobalUtil.checkCommandString(event, "create-giveaway")) return
        Giveaway.onSlashCommandInteraction(event)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Giveaway.onButtonInteraction(event)
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Giveaway.onStringSelectInteraction(event)
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        if (GlobalUtil.checkComponentIdPrefix(event, componentPrefix)) return
        Giveaway.onEntitySelectInteraction(event)
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (GlobalUtil.checkModalIdPrefix(event, componentPrefix)) return
        Giveaway.onModalInteraction(event)
    }
}
