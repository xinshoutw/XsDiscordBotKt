package tw.xinshou.discord.core.builtin.messagecreator.serializer

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDataSerializer(
    @SerialName("model_key")
    override val modelKey: String? = null,

    val content: String = "", // 2000 length limit
    val embeds: List<EmbedSetting> = emptyList(), // 10 size limit
    val components: List<ActionRowSetting> = emptyList(), // 5 size limit, allowed format: [ "buttons", "string_select_menu", "entity_select_menu" ]
) : BaseSerializer {
    @Serializable
    data class EmbedSetting(
        @SerialName("model_key")
        override val modelKey: String? = null,

        val author: AuthorSetting? = null,
        val title: TitleSetting? = null,
        val description: String? = null, // 4096 length limit

        @SerialName("thumbnail_url")
        val thumbnailUrl: String? = null, // 2000 length limit

        @SerialName("image_url")
        val imageUrl: String? = null, // 2000 length limit

        @Contextual
        @SerialName("color_code")
        val colorCode: String? = null, // default: "#FFFFFF", allowed format: [ "0xFFFFFF", "#FFFFFF" ]

        val footer: FooterSetting? = null,
        val timestamp: String? = null, // allowed format: [ "%now%", "1723675788491" ]
        val fields: List<FieldSetting> = emptyList(), // 25 size limit
    ) : BaseSerializer {
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
            @SerialName("model_key")
            override val modelKey: String? = null,

            val name: String, // 256 length limit
            val value: String, // 1024 length limit
            val inline: Boolean = false,
        ) : BaseSerializer
    }


    @Serializable
    sealed class ActionRowSetting : BaseSerializer {
        @Serializable
        @SerialName("!ButtonsComponent")
        data class ButtonsSetting(
            @SerialName("model_key")
            override val modelKey: String? = null,

            val buttons: List<ButtonSetting>
        ) : ActionRowSetting() {
            @Serializable
            data class ButtonSetting(
                @SerialName("model_key")
                override val modelKey: String? = null,

                val uid: Map<String, String>? = null,
                val url: String? = null,
                val style: Int = 1,
                val label: String? = null,
                val disabled: Boolean = false,
                val emoji: EmojiSetting? = null
            ) : BaseSerializer {
                @Serializable
                data class EmojiSetting(
                    @SerialName("model_key")
                    override val modelKey: String? = null,

                    val formatted: String? = null,
                ) : BaseSerializer {
                    init {
                        require((modelKey != null) xor (formatted != null)) {
                            "Either model_key or formatted must be provided for EmojiSetting"
                        }
                    }
                }
            }
        }

        @Serializable
        @SerialName("!StringSelectMenu")
        data class StringSelectMenuSetting(
            @SerialName("model_key")
            override val modelKey: String? = null,

            val uid: Map<String, String>,
            val placeholder: String? = null,
            val min: Int = 1,
            val max: Int = 1,
            val options: List<OptionSetting>
        ) : ActionRowSetting() {
            @Serializable
            data class OptionSetting(
                @SerialName("model_key")
                override val modelKey: String? = null,

                val label: String,
                val value: String,
                val description: String? = null,
                val default: Boolean = false,
                val emoji: EmojiSetting? = null
            ) : BaseSerializer {
                @Serializable
                data class EmojiSetting(
                    @SerialName("model_key")
                    override val modelKey: String? = null,

                    val name: String,
                    val animated: Boolean = false
                ) : BaseSerializer
            }
        }

        @Serializable
        @SerialName("!EntitySelectMenu")
        data class EntitySelectMenuSetting(
            @SerialName("model_key")
            override val modelKey: String? = null,

            val uid: Map<String, String>,
            val placeholder: String? = null,
            val min: Int = 1,
            val max: Int = 1,

            @SerialName("select_target_type")
            val selectTargetType: String,

            @SerialName("channel_types")
            val channelTypes: List<String> = emptyList(),
        ) : ActionRowSetting()
    }
}
