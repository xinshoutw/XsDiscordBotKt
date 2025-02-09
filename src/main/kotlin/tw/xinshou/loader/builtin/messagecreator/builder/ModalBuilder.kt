package tw.xinshou.loader.builtin.messagecreator.builder

import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.modals.Modal
import tw.xinshou.loader.builtin.messagecreator.serializer.ModalDataSerializer
import tw.xinshou.loader.builtin.placeholder.Placeholder
import tw.xinshou.loader.builtin.placeholder.Substitutor
import tw.xinshou.loader.util.ComponentIdManager

class ModalBuilder(
    private val modalData: ModalDataSerializer,
    private val substitutor: Substitutor? = Placeholder.globalSubstitutor,
    private val componentIdManager: ComponentIdManager,
    private val modelMapper: Map<String, Any>?,
) {
    private lateinit var builder: Modal.Builder

    fun getBuilder(): Modal.Builder {
        if (setupModelKeys()) return builder

        builder = Modal.create(
            parsePlaceholder(componentIdManager.build(modalData.uid)),
            parsePlaceholder(modalData.title)
        )
        setupTextInputs()

        return builder
    }

    private fun setupModelKeys(): Boolean {
        modalData.modelKey?.let {
            val model = requireNotNull(modelMapper?.get(it)) { "Model with key '$it' not found!" }
            when (model) {
                is Modal.Builder -> model
                else -> throw IllegalArgumentException("Unknown message model: $model")
            }
            return true
        }
        return false
    }

    private fun setupTextInputs() {
        modalData.textInputs.let { textInputs ->
            textInputs.forEach { textInput ->
                builder.addActionRow(
                    buildTextInput(textInput)
                )
            }
        }
    }

    private fun buildTextInput(textInput: ModalDataSerializer.TextInputSetting): TextInput {
        textInput.modelKey?.let { modelKey ->
            val model = requireNotNull(modelMapper?.get(modelKey)) { "Model with key '$modelKey' not found!" }
            when (model) {
                is TextInput -> return model
                else -> throw IllegalArgumentException("Unknown message model: $model")
            }
        }

        return TextInput.create(
            parsePlaceholder(textInput.uid),
            parsePlaceholder(textInput.label),
            textInput.style
        ).apply {
            value = textInput.value?.let { parsePlaceholder(it) }
            placeholder = textInput.placeholder?.let { parsePlaceholder(it) }
            minLength = textInput.minLength
            maxLength = textInput.maxLength
            isRequired = textInput.required
        }.build()
    }


    /**
     * 嘗試使用 substitutor 解析文字；若 substitutor == null，回傳原值。
     */
    private fun parsePlaceholder(text: String): String {
        return substitutor?.parse(text) ?: text
    }
}
