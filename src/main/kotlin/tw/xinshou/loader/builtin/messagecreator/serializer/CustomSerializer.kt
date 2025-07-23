package tw.xinshou.loader.builtin.messagecreator.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle

internal object TextInputStyleSerializer : KSerializer<TextInputStyle> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TextInputStyle", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TextInputStyle) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): TextInputStyle {
        return TextInputStyle.valueOf(decoder.decodeString())
    }
}