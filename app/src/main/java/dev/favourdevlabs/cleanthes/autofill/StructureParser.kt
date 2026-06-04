package dev.favourdevlabs.cleanthes.autofill

import android.app.assist.AssistStructure
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId

object StructureParser {

    data class ParsedFields(
        var usernameId: AutofillId? = null, // AutofillId of the email/username field
        var passwordId: AutofillId? = null, // AutofillId of the password field
        var packageName: String?    = null, // e.g. "com.google.android.gm"
        var webDomain: String?      = null  // actual site domain if it's a browser
    )

    fun parse(structure: AssistStructure): ParsedFields {
        val result = ParsedFields()
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            // Window title format: "packageName/ActivityName"
            windowNode.title?.toString()?.let { title ->
                if (title.contains("/")) result.packageName = title.split("/")[0]
            }
            traverseNode(windowNode.rootViewNode, result)
        }
        return result
    }

    private fun traverseNode(node: AssistStructure.ViewNode?, result: ParsedFields) {
        if (node == null) return

        node.webDomain?.let { result.webDomain = it }

        // --- Primary detection: autofillHints ---
        node.autofillHints?.forEach { hint ->
            if (isUsernameHint(hint)) result.usernameId = node.autofillId
            if (isPasswordHint(hint)) result.passwordId = node.autofillId
        }

        // --- Fallback detection: inputType ---
        if (result.passwordId == null) {
            val inputType = node.inputType
            val isPassword = (inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD)         != 0 ||
                             (inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)     != 0 ||
                             (inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0
            if (isPassword) result.passwordId = node.autofillId
        }

        // --- Fallback detection: hint text ---
        if (result.usernameId == null) {
            node.hint?.toString()?.lowercase()?.let { hintStr ->
                if (hintStr.contains("email") || hintStr.contains("username") ||
                    hintStr.contains("user name") || hintStr.contains("phone")) {
                    result.usernameId = node.autofillId
                }
            }
        }

        for (i in 0 until node.childCount) traverseNode(node.getChildAt(i), result)
    }

    private fun isUsernameHint(hint: String): Boolean =
        hint.equals(View.AUTOFILL_HINT_USERNAME, ignoreCase = true)     ||
        hint.equals(View.AUTOFILL_HINT_EMAIL_ADDRESS, ignoreCase = true)||
        hint.equals(View.AUTOFILL_HINT_PHONE, ignoreCase = true)        ||
        hint.equals("email", ignoreCase = true)                         ||
        hint.equals("username", ignoreCase = true)

    private fun isPasswordHint(hint: String): Boolean =
        hint.equals(View.AUTOFILL_HINT_PASSWORD, ignoreCase = true) ||
        hint.equals("password", ignoreCase = true)                  ||
        hint.equals("current-password", ignoreCase = true)
}
