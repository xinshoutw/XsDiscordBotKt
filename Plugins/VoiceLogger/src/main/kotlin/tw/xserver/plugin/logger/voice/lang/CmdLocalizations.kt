package tw.xserver.plugin.logger.voice.lang

import tw.xserver.loader.localizations.LocalTemplate
import tw.xserver.loader.localizations.LocaleData

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