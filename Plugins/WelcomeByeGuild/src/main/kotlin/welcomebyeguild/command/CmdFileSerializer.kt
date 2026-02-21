package tw.xinshou.discord.plugin.welcomebyeguild.command

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tw.xinshou.discord.core.localizations.LocalTemplate

@Serializable
internal data class CmdFileSerializer(
    @SerialName("create_welcome_bye_guild")
    val createWelcomeByeGuild: LocalTemplate.NameDescriptionString,
)
