package tw.xinshou.plugin.logger.voice.command.lang

import tw.xinshou.loader.localizations.LocalTemplate
import tw.xinshou.loader.localizations.LocaleData

internal object CmdLocalizations {
    val voiceLogger = CommandWithMemberLd()

    internal class CommandWithMemberLd {
        val name = LocaleData()
        val description = LocaleData()
        val subcommands = SubCommandsLd()

        internal class SubCommandsLd {
            val setting = LocalTemplate.NDLocalData()
        }
    }
}