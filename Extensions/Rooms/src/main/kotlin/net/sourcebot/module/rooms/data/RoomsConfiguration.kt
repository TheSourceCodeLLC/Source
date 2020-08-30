package net.sourcebot.module.rooms.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class RoomsConfiguration @JsonCreator constructor(
    @JsonProperty("category") val category: String? = null
)