package net.sourcebot.module.moderation.data

abstract class SourceIncident(
    final override val type: Incident.Type
) : Incident {
    private var last: Long = 0
    final override fun computeId() = ++last
}