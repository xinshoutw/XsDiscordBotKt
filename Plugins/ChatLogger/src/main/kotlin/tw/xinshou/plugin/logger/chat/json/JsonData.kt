package tw.xinshou.plugin.logger.chat.json

import com.squareup.moshi.Json

// {"858672865816346634":{"allow_mode":false,"allow":[],"block":[]}}
internal data class DataContainer(
    @Json(name = "allow_mode")
    var allowMode: Boolean,

    var allow: MutableSet<Long>,
    var block: MutableSet<Long>,
)


internal typealias JsonDataClass = MutableMap<String, DataContainer>
