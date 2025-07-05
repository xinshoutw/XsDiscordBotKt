package tw.xinshou.plugin.ticket.command.lang

import tw.xinshou.loader.localizations.LocalTemplate
import tw.xinshou.loader.localizations.LocaleData
import tw.xinshou.loader.localizations.LocalTemplate as LocalTemplate1

internal object CmdLocalizations {
    val createTicket = LocalTemplate.NDLocalData()
    val addTicket = CommandWithMessageIdLd()

    internal class CommandWithMessageIdLd {
        val name = LocaleData()
        val description = LocaleData()
        val options = OptionsLd()

        internal class OptionsLd {
            val messageId = LocalTemplate1.NDLocalData()
        }
    }
}