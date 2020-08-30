package net.sourcebot.module.counting.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class CountingData @JsonCreator constructor(
    @JsonProperty("channel") var channel: String? = null,
    @JsonProperty("record") var record: Long = 1,
    @JsonProperty("lastNumber") var lastNumber: Long = 1
)