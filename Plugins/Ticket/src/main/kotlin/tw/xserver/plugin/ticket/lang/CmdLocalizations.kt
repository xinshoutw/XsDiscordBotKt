package tw.xserver.plugin.ticket.lang

import tw.xserver.loader.localizations.LocalTemplate
import tw.xserver.loader.localizations.LocaleData
import tw.xserver.loader.localizations.LocalTemplate as LocalTemplate1

internal object CmdLocalizations {
    val createTicket = LocalTemplate.NDLocalData()
    val addTicket = CommandWithMessageIdLd()

    class CommandWithMessageIdLd {
        val name = LocaleData()
        val description = LocaleData()
        val options = OptionsLd()

        class OptionsLd {
            val messageId = LocalTemplate1.NDLocalData()
        }
    }
}