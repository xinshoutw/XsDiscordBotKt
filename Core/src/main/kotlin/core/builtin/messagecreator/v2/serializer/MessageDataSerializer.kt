package tw.xinshou.discord.core.builtin.messagecreator.v2.serializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import tw.xinshou.discord.core.builtin.messagecreator.v1.serializer.MessageDataSerializer.ActionRowSetting
import tw.xinshou.discord.core.builtin.messagecreator.v1.serializer.MessageDataSerializer.EmbedSetting

@Serializable
data class MessageDataSerializer(
    @SerialName("model_key")
    override val modelKey: String? = null,

    @SerialName("is_components_v2")
    val isComponentsV2: Boolean = false,

    val content: String = "",
    val embeds: List<EmbedSetting> = emptyList(),
    val components: List<ActionRowSetting> = emptyList(),

    @Transient
    val componentsV2: List<JsonObject> = emptyList(),
) : BaseSerializer
