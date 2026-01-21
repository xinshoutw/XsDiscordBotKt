package tw.xinshou.discord.plugin.economy.json

// {"852521987557556284":{"cost":990,"money":0},}
internal data class DataContainer(
    var cost: Int,
    var money: Int,
)


internal typealias JsonDataClass = MutableMap<String, DataContainer>
