package tw.xinshou.plugin.feedbacker.command.lang

import tw.xinshou.loader.localizations.LocalTemplate
import tw.xinshou.loader.localizations.LocaleData

internal object CmdLocalizations {
    val feedbacker = CommandWithMemberLd()

    internal class CommandWithMemberLd {
        val options = OptionsLd()
        val name = LocaleData()
        val description = LocaleData()

        internal class OptionsLd {
            val member = LocalTemplate.NDLocalData()
        }
    }
}