package tw.xserver.loader.builtin.messagecreator.serializer

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.awt.Color

@Serializable
data class MessageDataSerializer(
    val content: String = "", // 2000 length limit
    val embeds: List<EmbedSetting> = emptyList(), // 10 size limit
    val components: List<ComponentSetting> = emptyList(), // 5 size limit, allowed format: [ "buttons", "string_select_menu", "entity_select_menu" ]
) {
    @Serializable
    data class EmbedSetting(
        val author: AuthorSetting? = null,
        val title: TitleSetting? = null,
        val description: String? = null, // 4096 length limit

        @SerialName("thumbnail_url")
        val thumbnailUrl: String? = null, // 2000 length limit

        @SerialName("image_url")
        val imageUrl: String? = null, // 2000 length limit

        @Contextual
        @SerialName("color_code")
        val colorCode: Color = Color.WHITE, // default: "#FFFFFF", allowed format: [ "0xFFFFFF", "#FFFFFF", "FFFFFFh" ]

        val footer: FooterSetting? = null,
        val timestamp: String? = null, // allowed format: [ "%now%", "1723675788491" ]
        val fields: List<FieldSetting> = emptyList(), // 25 size limit
    ) {
        @Serializable
        data class AuthorSetting(
            val name: String, // 256 length limit
            val url: String? = null, // 2000 length limit

            @SerialName("icon_url")
            val iconUrl: String? = null, // 2000 length limit
        )

        @Serializable
        data class TitleSetting(
            val text: String, // 256 length limit
            val url: String? = null, // 2000 length limit
        )

        @Serializable
        data class FooterSetting(
            val text: String, // 2048 length limit

            @SerialName("icon_url")
            val iconUrl: String? = null, // 2000 length limit
        )

        @Serializable
        data class FieldSetting(
            val name: String, // 256 length limit
            val value: String, // 1024 length limit
            val inline: Boolean = false,
        )
    }

    @Serializable
    sealed class ComponentSetting {
        @Serializable
        @SerialName("!ButtonsComponent")
        data class ButtonsComponent(
            val buttons: List<Button>
        ) : ComponentSetting() {
            @Serializable
            data class Button(
                val uid: Map<String, String>? = null,
                val url: String? = null,
                val style: Int,
                val label: String? = null,
                val disabled: Boolean = false,
                val emoji: Emoji? = null
            ) {
                @Serializable
                data class Emoji(
                    val name: String,
                    val animated: Boolean = false
                )
            }
        }

        @Serializable
        @SerialName("!StringSelectMenu")
        data class StringSelectMenuSetting(
            val uid: Map<String, String>,
            val placeholder: String? = null,
            val min: Int = 1,
            val max: Int = 1,
            val options: List<Option>
        ) : ComponentSetting() {
            @Serializable
            data class Option(
                val label: String,
                val value: String,
                val description: String? = null,
                val default: Boolean = false,
                val emoji: Emoji? = null
            ) {
                @Serializable
                data class Emoji(
                    val name: String,
                    val animated: Boolean = false
                )
            }
        }

        @Serializable
        @SerialName("!EntitySelectMenu")
        data class EntitySelectMenuSetting(
            val uid: Map<String, String>,
            val placeholder: String? = null,
            val min: Int = 1,
            val max: Int = 1,

            @SerialName("select_target_type")
            val selectTargetType: String,

            @SerialName("channel_types")
            val channelTypes: List<String> = emptyList(),
        ) : ComponentSetting()
    }
}
