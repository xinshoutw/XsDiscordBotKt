package tw.xinshou.plugin.logger.chat.json

import com.squareup.moshi.Json

// {"858672865816346634":{"allow_mode":false,"allow":[],"block":[]}}
data class DataContainer(
    @Json(name = "allow_mode")
    var allowMode: Boolean,

    var allow: MutableList<Long>,
    var block: MutableList<Long>,
)


typealias JsonDataClass = MutableMap<Long, DataContainer>
