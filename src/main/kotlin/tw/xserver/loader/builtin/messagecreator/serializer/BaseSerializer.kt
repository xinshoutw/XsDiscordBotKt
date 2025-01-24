package tw.xserver.loader.builtin.messagecreator.serializer

import kotlinx.serialization.SerialName

/**
 * 父類：提供最基本的 model_key 欄位
 */
interface BaseSerializer {
    @SerialName("!model_key")
    val modelKey: String?
}
