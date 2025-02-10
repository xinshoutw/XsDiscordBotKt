package tw.xinshou.plugin.ticket.json.serializer

data class DataContainer(
    val reasonTitle: String,
    val adminIds: List<Long>,
    val categoryId: Long
)


typealias JsonDataClass = Map<Long, List<DataContainer>>
