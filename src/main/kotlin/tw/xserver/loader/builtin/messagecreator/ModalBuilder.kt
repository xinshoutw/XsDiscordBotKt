package tw.xserver.loader.builtin.messagecreator

import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.modals.Modal
import tw.xserver.loader.builtin.messagecreator.serializer.ModalDataSerializer
import tw.xserver.loader.builtin.placeholder.Placeholder
import tw.xserver.loader.builtin.placeholder.Substitutor
import tw.xserver.loader.util.ComponentIdManager

open class ModalBuilder(
    private val componentIdManager: ComponentIdManager,
) {
    protected fun getModalBuilder(
        modalData: ModalDataSerializer,
        substitutor: Substitutor = Placeholder.globalSubstitutor,
    ): Modal.Builder {
        val builder = Modal.create(
            substitutor.parse(componentIdManager.build(modalData.uid)),
            substitutor.parse(substitutor.parse(modalData.title))
        ).apply {
            modalData.textInputs.forEach { textInput ->
                components.add(
                    ActionRow.of(
                        TextInput.create(
                            substitutor.parse(componentIdManager.build(textInput.uid)),
                            substitutor.parse(textInput.label),
                            textInput.style
                        ).apply {
                            value = textInput.value?.let { substitutor.parse(it) }
                            placeholder = textInput.placeholder?.let { substitutor.parse(it) }
                            minLength = textInput.minLength
                            maxLength = textInput.maxLength
                            isRequired = textInput.required
                        }.build()
                    )
                )
            }
        }
        return builder
    }
}
