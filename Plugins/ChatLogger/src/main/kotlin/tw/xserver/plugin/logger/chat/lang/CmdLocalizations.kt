package tw.xserver.plugin.logger.chat.lang

import tw.xserver.loader.localizations.LocalTemplate
import tw.xserver.loader.localizations.LocaleData

internal object CmdLocalizations {
    val chatLogger = CommandWithMemberLd()

    internal class CommandWithMemberLd {
        val name = LocaleData()
        val description = LocaleData()
        val subcommands = SubCommandsLd()

        internal class SubCommandsLd {
            val setting = LocalTemplate.NDLocalData()
        }
    }
}