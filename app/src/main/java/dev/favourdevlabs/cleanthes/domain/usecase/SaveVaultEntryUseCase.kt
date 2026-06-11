package dev.favourdevlabs.cleanthes.domain.usecase

import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import javax.crypto.SecretKey
import javax.inject.Inject

class SaveVaultEntryUseCase @Inject constructor(
    private val repository: VaultRepository,
) {
    sealed interface Params {
        data class New(
            val title: String,
            val username: String,
            val plainPassword: String,
            val website: String?,
            val category: String,
            val notes: String?,
            val isFavorite: Boolean,
            val totpSecret: String?,
            val totpIssuer: String?,
            val totpDigits: Int,
            val totpPeriod: Int,
            val totpAlgorithm: String,
            val key: SecretKey,
        ) : Params

        data class Edit(
            val entry: VaultEntry,
            val plainPassword: String,
            val key: SecretKey,
        ) : Params
    }

    @Throws(Exception::class)
    operator fun invoke(params: Params): Long = when (params) {
        is Params.New  -> repository.addEntry(
            params.title, params.username, params.plainPassword,
            params.website, params.category, params.notes,
            params.isFavorite, params.totpSecret, params.totpIssuer,
            params.totpDigits, params.totpPeriod, params.totpAlgorithm,
            params.key,
        )
        is Params.Edit -> repository.updateEntry(
            params.entry, params.plainPassword, params.key
        ).toLong()
    }
}
