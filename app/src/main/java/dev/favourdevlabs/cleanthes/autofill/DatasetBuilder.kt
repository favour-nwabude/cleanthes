package dev.favourdevlabs.cleanthes.autofill

import android.content.Context
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry

object DatasetBuilder {

    fun build(
        context: Context,
        usernameId: AutofillId,
        passwordId: AutofillId,
        entry: VaultEntry
    ): Dataset {
        val view = RemoteViews(context.packageName, R.layout.autofill_item).apply {
            setTextViewText(R.id.autofill_label, entry.username)
        }
        return Dataset.Builder(view)
            .setValue(usernameId, AutofillValue.forText(entry.username), view)
            .setValue(passwordId, AutofillValue.forText(entry.encryptedPassword), view)
            .build()
    }
}
