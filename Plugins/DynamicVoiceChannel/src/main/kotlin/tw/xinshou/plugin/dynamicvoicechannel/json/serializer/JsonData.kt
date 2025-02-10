package tw.xinshou.plugin.dynamicvoicechannel.json.serializer

data class DataContainer(
    val categoryId: Long,
    val defaultName: String,
    val formatName1: String = "",
    val formatName2: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as DataContainer

        if (categoryId != other.categoryId) return false
        if (defaultName != other.defaultName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = categoryId.hashCode()
        result = 31 * result + defaultName.hashCode()
        return result
    }
}


typealias JsonDataClass = MutableList<DataContainer>
