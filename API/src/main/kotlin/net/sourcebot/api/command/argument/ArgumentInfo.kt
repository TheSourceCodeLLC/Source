package net.sourcebot.api.command.argument

/**
 * Represents a listing of [Parameter]s to be rendered on command help information.
 */
class ArgumentInfo(private vararg val args: Parameter) {
    /**
     * Joins the arguments to a space-delimited string
     * Required arguments are surrounded by <>
     * Optional arguments are surrounded by ()
     */
    fun asList() = if (args.isEmpty()) null else args.joinToString(" ") {
        if (it is OptionalArgument) "(${it.name})" else "<${it.name}>"
    }

    /**
     * Builds the parameter detail for command help information.
     * This works similarly to [asList] but arguments are joined by newlines, and argument defaults are shown.
     */
    fun getParameterDetail() =
        if (args.isEmpty()) "This command has no parameters."
        else args.joinToString("\n", transform = Parameter::getDetail)
}

interface Parameter {
    val name: String
    val description: String

    fun getDetail(): String
    fun getFormattedName(): String
}

abstract class ParameterGroup(vararg val parameters: Parameter) : Parameter {
    override val name = parameters.joinToString("|") { it.name }

    final override fun getDetail() = ""
}

/**
 * Represents an optional argument
 * @param[name] The name of this argument
 * @param[description] The description of this argument
 * @param[default] The default value of this argument. '_none_' by default.
 */
open class OptionalArgument @JvmOverloads constructor(
    override val name: String,
    override val description: String,
    open val default: Any = "_none_"
) : Parameter {
    override fun getDetail() = "**${getFormattedName()}**: $description\n(Default: $default)"
    override fun getFormattedName() = "($name)"
}

/**
 * Represents a required argument
 * @param[name] The name of this argument
 * @param[description] The description of this argument
 */
open class Argument(
    override val name: String,
    override val description: String
) : Parameter {
    override fun getDetail() = "**${getFormattedName()}**: $description"
    override fun getFormattedName() = "<$name>"
}

open class OptionalGroup(
    override val description: String
) : ParameterGroup() {
    override fun getFormattedName() = "($name)"
}

open class Group(
    override val name: String,
    override val description: String
) : ParameterGroup() {
    override fun getFormattedName() = "<$name>"
}