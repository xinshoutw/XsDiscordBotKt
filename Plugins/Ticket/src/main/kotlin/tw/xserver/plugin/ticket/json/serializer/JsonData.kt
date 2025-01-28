package tw.xserver.plugin.ticket.json.serializer

data class DataContainer(
    val reasonTitle: String,
    val adminIds: List<Long>,
    val categoryId: Long
)


typealias JsonDataClass = Map<String, List<DataContainer>>
