package tw.xinshou.discord.plugin.ticket.json.serializer

internal data class DataContainer(
    var reasonTitle: String,
    var adminIds: MutableList<Long>,
    var categoryId: Long
)


internal typealias JsonDataClass = MutableMap<String, MutableList<DataContainer>>
