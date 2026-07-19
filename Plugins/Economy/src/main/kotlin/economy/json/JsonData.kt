package tw.xinshou.discord.plugin.economy.json

import kotlinx.serialization.Serializable

@Serializable
internal data class DataContainer(
    var cost: Int,
    var money: Int,
)

internal typealias JsonDataClass = MutableMap<String, DataContainer>
