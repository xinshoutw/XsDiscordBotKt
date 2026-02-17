package tw.xinshou.discord.core.builtin.messagecreator.v2.serializer

import kotlinx.serialization.SerialName

interface BaseSerializer {
    @SerialName("model_key")
    val modelKey: String?
}
