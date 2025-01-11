package tw.xserver.loader.builtin.messagecreator

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import org.apache.commons.lang3.StringUtils.isNumeric
import tw.xserver.loader.builtin.messagecreator.serializer.MessageDataSerializer
import tw.xserver.loader.builtin.messagecreator.serializer.MessageDataSerializer.EmbedSetting
import tw.xserver.loader.builtin.placeholder.Placeholder
import tw.xserver.loader.builtin.placeholder.Substitutor
import tw.xserver.loader.util.ComponentIdManager
import java.time.Instant
import java.time.OffsetDateTime

open class MessageBuilder(
    private val componentIdManager: ComponentIdManager?,
) {
    protected fun getCreateBuilder(
        messageData: MessageDataSerializer,
        substitutor: Substitutor = Placeholder.globalSubstitutor,
    ): MessageCreateBuilder {
        val builder = MessageCreateBuilder()
        messageData.content.let { builder.setContent(substitutor.parse(it)) }
        messageData.embeds.let { embeds ->
            builder.setEmbeds(buildEmbeds(embeds, substitutor))
        }

        val components: MutableList<LayoutComponent> = ArrayList()
        messageData.components.forEach { component ->
            requireNotNull(componentIdManager) { "You have to pass componentIdManager to create message with components!" }
            when (component) {
                is MessageDataSerializer.ComponentSetting.ButtonsComponent -> {
                    components.add(ActionRow.of(component.buttons.map { button ->
                        ButtonImpl(
                            /* id = */ substitutor.parse(button.uid?.let { componentIdManager.build(it) }
                                ?: button.url ?: throw NullPointerException("Either uid or url must be provided!")),
                            /* label = */ button.label?.let { substitutor.parse(it) },
                            /* style = */ when (button.style) {
                                1 -> ButtonStyle.PRIMARY
                                2 -> ButtonStyle.SECONDARY
                                3 -> ButtonStyle.SUCCESS
                                4 -> ButtonStyle.DANGER
                                5 -> ButtonStyle.LINK
                                else -> throw IllegalArgumentException("Unknown style code: ${button.style}")
                            },
                            /* disabled = */ button.disabled,
                            /* emoji = */ button.emoji?.let { Emoji.fromUnicode(button.emoji.name) }
                        )
                    }))
                }

                is MessageDataSerializer.ComponentSetting.StringSelectMenuSetting -> {
                    val menu = StringSelectMenu
                        .create(
                            substitutor.parse(componentIdManager.build(component.uid)),
                        ).apply {
                            placeholder = component.placeholder?.let { substitutor.parse(it) }
                            minValues = component.min
                            maxValues = component.max
                            component.options.forEach { option ->
                                addOption(
                                    /* label = */ substitutor.parse(option.label),
                                    /* value = */ substitutor.parse(option.value),
                                    /* description = */ option.description?.let { substitutor.parse(it) },
                                    /* emoji = */ option.emoji?.let { Emoji.fromUnicode(option.emoji.name) }
                                )
                            }
                        }

                    components.add(ActionRow.of(menu.build()))
                }

                is MessageDataSerializer.ComponentSetting.EntitySelectMenuSetting -> {
                    val menu =
                        EntitySelectMenu.create(
                            /* customId = */ substitutor.parse(componentIdManager.build(component.uid)),
                            /* type = */ EntitySelectMenu.SelectTarget.valueOf(component.selectTargetType.uppercase())
                        ).apply {
                            placeholder = component.placeholder?.let { substitutor.parse(it) }
                            minValues = component.min
                            maxValues = component.max
                        }
                    if (component.selectTargetType.uppercase() == "CHANNEL") {
                        require(component.channelTypes.isNotEmpty()) {
                            "'channel_types' cannot be empty when 'select_target_type' is set to 'CHANNEL'!"
                        }
                        menu.setChannelTypes(component.channelTypes.map { ChannelType.valueOf(it) })
                    }

                    components.add(ActionRow.of(menu.build()))
                }
            }
        }

        builder.setComponents(components)

        return builder
    }

    private fun buildEmbeds(embeds: List<EmbedSetting>, substitutor: Substitutor? = null): List<MessageEmbed> =
        embeds.mapNotNull { embed -> buildEmbed(embed, substitutor) }

    private fun buildEmbed(embed: EmbedSetting, substitutor: Substitutor?): MessageEmbed? {
        val builder = EmbedBuilder().apply {
            // Set author once using substitutor if available or directly if not
            embed.author?.let { author ->
                setAuthor(
                    substitutor?.parse(author.name) ?: author.name,
                    author.url?.let { substitutor?.parse(it) } ?: author.url,
                    author.iconUrl?.let { substitutor?.parse(it) } ?: author.iconUrl
                )
            }

            // Handle title, description, thumbnail, and image with or without substitutor
            if (substitutor != null) {
                embed.title?.let { title ->
                    setTitle(substitutor.parse(title.text), title.url?.let { substitutor.parse(it) })
                }
                embed.description?.let { desc -> setDescription(substitutor.parse(desc)) }
                embed.thumbnailUrl?.let { url -> setThumbnail(substitutor.parse(url)) }
                embed.imageUrl?.let { url -> setImage(substitutor.parse(url)) }
            } else {
                embed.title?.let { title -> setTitle(title.text, title.url) }
                embed.description?.let { desc -> setDescription(desc) }
                embed.thumbnailUrl?.let { url -> setThumbnail(url) }
                embed.imageUrl?.let { url -> setImage(url) }
            }

            // Apply color and timestamp directly since they don't involve parsing
            setColor(embed.colorCode)

            embed.timestamp?.let {
                when {
                    isNumeric(it) -> Instant.ofEpochMilli(it.toLong())
                    it == "%now%" -> OffsetDateTime.now().toInstant()
                    else -> throw Exception("Unknown format for timestamp!")
                }
            }

            // Set footer similarly to author
            embed.footer?.let { footer ->
                setFooter(
                    substitutor?.parse(footer.text) ?: footer.text,
                    footer.iconUrl?.let { substitutor?.parse(it) } ?: footer.iconUrl
                )
            }

            // Handle fields, applying substitutor if available
            embed.fields.forEach { field ->
                addField(
                    substitutor?.parse(field.name) ?: field.name,
                    substitutor?.parse(field.value) ?: field.value,
                    field.inline
                )
            }
        }

        // Build the embed only if it's not empty
        return if (builder.isEmpty) null else builder.build()
    }
}
