package org.andrew.enzona.core.logging

internal class RenderedMessage(val text: String, val arguments: Map<String, Any?>)

/**
 * Renderiza una plantilla con marcadores con nombre (`"Sesión de {Usuario}"`) como .NET.
 * Los argumentos se asignan por posición. Nunca lanza: un marcador sin argumento queda tal
 * cual, los argumentos de más se ignoran y una llave sin cerrar es texto literal.
 * `{{` y `}}` escapan las llaves.
 */
internal fun renderTemplate(template: String, args: Array<out Any?>): RenderedMessage {
    val text = StringBuilder(template.length + 16)
    val arguments = LinkedHashMap<String, Any?>()
    var next = 0
    var i = 0
    while (i < template.length) {
        val c = template[i]
        if (c == '{' && template.startsWith("{{", i)) {
            text.append('{'); i += 2
        } else if (c == '}' && template.startsWith("}}", i)) {
            text.append('}'); i += 2
        } else if (c == '{') {
            val end = template.indexOf('}', i + 1)
            val name = if (end < 0) null else template.substring(i + 1, end)
            if (name == null || name.isEmpty() || '{' in name) {
                text.append('{'); i++
            } else {
                if (next < args.size) {
                    val value = args[next++]
                    arguments[name] = redact(value)
                    text.append(display(value))
                } else {
                    text.append(template, i, end + 1)
                }
                i = end + 1
            }
        } else {
            text.append(c); i++
        }
    }
    return RenderedMessage(text.toString(), arguments)
}

/** Valor que viaja como propiedad estructurada: lo secreto nunca sale de aquí en claro. */
internal fun redact(value: Any?): Any? = if (value is Sensitive<*>) Sensitive.REDACTED else value

private fun display(value: Any?): String = when (value) {
    null -> "null"
    is Sensitive<*> -> Sensitive.REDACTED
    else -> try {
        value.toString()
    } catch (e: Exception) {
        "<toString falló>"
    }
}
