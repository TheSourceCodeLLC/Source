package net.sourcebot.module.starboard.misc

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class StarboardData @JsonCreator constructor(
    @JsonProperty("channel") var channel: String? = null,
    @JsonProperty("threshold") var threshold: Int = 5
)