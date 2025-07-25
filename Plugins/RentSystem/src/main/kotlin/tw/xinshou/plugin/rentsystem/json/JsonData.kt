package tw.xinshou.plugin.rentsystem.json

import com.squareup.moshi.Json

/*

[
{"year": 2025,"month": 6, "water_meter": 100, "meters": [
{
day: 1,
public_meter: 100,
room_a: 50,
room_b: 50,
room_c: 50
}
]}

]



 */


// {"858672865816346634":{"allow_mode":false,"allow":[],"block":[]}}
internal data class DataContainer(
    @field:Json(name = "allow_mode")
    var allowMode: Boolean,

    var allow: MutableSet<Long>,
    var block: MutableSet<Long>,
)


internal typealias JsonDataClass = MutableMap<String, DataContainer>
