package tw.xinshou.discord.core.builtin.messagecreator.modal.serializer

import kotlinx.serialization.SerialName

interface BaseSerializer {
    @SerialName("model_key")
    val modelKey: String?
}
