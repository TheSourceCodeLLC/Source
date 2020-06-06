package net.sourcebot.api.command.argument

class ArgumentInfo(private vararg val args: Argument) {
    fun asList(): String =
        if (args.isEmpty()) ""
        else args.joinToString(" ") {
            if (it is OptionalArgument) "(${it.name})" else "<${it.name}>"
        }

    fun getParameterDetail() =
        if (args.isEmpty()) "This command has no parameters."
        else args.joinToString("\n") {
            val optional = it is OptionalArgument
            val name = if (optional) "(${it.name})" else "<${it.name}>"
            var detail = "**$name**: ${it.description}"
            if (optional) detail += "\n\t(Default: ${(it as OptionalArgument).default})"
            detail
        }
}

open class Argument(
    val name: String,
    val description: String
)

class OptionalArgument @JvmOverloads constructor(
    name: String,
    description: String,
    val default: Any = "_none_"
) : Argument(name, description)