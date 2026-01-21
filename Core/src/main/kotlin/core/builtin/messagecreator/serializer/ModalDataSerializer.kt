package tw.xinshou.discord.core.builtin.messagecreator.serializer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle

@Serializable
data class ModalDataSerializer(
    @SerialName("model_key")
    override val modelKey: String? = null,

    val uid: Map<String, String>,
    val title: String,

    @SerialName("text_inputs")
    val textInputs: List<TextInputSetting> = emptyList(),
) : BaseSerializer {
    @Serializable
    data class TextInputSetting(
        @SerialName("model_key")
        override val modelKey: String? = null,

        val uid: String, // String only
        val label: String,
        val value: String? = null,
        val placeholder: String? = null,

        @SerialName("min_length")
        val minLength: Int = -1,

        @SerialName("max_length")
        val maxLength: Int = -1,

        val style: TextInputStyle,
        val required: Boolean = true,
    ) : BaseSerializer
}
