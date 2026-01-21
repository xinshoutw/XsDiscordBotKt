package tw.xinshou.discord.core.localizations

import kotlinx.serialization.Serializable

object LocalTemplate {
    class NDLocalData {
        val name = LocalStringMap()
        val description = LocalStringMap()
    }

    @Serializable
    data class NameDescriptionString(
        val name: String,
        val description: String,
    )
}