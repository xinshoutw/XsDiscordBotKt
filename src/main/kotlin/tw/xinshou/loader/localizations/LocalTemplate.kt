package tw.xinshou.loader.localizations

import kotlinx.serialization.Serializable

object LocalTemplate {
    class NDLocalData {
        val name = LocaleData()
        val description = LocaleData()
    }

    @Serializable
    data class NDString(
        val name: String,
        val description: String,
    )
}