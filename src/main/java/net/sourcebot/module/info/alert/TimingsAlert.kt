package net.sourcebot.module.info.alert

import net.sourcebot.api.alert.InfoAlert

class TimingsAlert(
    delay: Long,
    rest: Long,
    gateway: Long
) : InfoAlert(
    "Source Timings",
    "**Command Execution**: ${delay}ms\n" +
    "**REST Latency**: ${rest}ms\n" +
    "**Gateway Latency**: ${gateway}ms"
)