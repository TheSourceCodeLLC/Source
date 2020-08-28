package net.sourcebot.api.command.argument

/**
 * Represents a listing of [Argument]s to be rendered on command help information.
 */
class ArgumentInfo(private vararg val args: Argument) {
    /**
     * Joins the arguments to a space-delimited string
     * Required arguments are surrounded by <>
     * Optional arguments are surrounded by ()
     */
    fun asList(): String =
        if (args.isEmpty()) ""
        else args.joinToString(" ") {
            if (it is OptionalArgument) "(${it.name})" else "<${it.name}>"
        }

    /**
     * Builds the parameter detail for command help information.
     * This works similarly to [asList] but arguments are joined by newlines, and argument defaults are shown.
     */
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

/**
 * Represents a required argument
 * @param[name] The name of this argument
 * @param[description] The description of this argument
 */
open class Argument(
    val name: String,
    val description: String
)

/**
 * Represents an optional argument
 * @param[name] The name of this argument
 * @param[description] The description of this argument
 * @param[default] The default value of this argument. '_none_' by default.
 */
class OptionalArgument @JvmOverloads constructor(
    name: String,
    description: String,
    val default: Any = "_none_"
) : Argument(name, description)