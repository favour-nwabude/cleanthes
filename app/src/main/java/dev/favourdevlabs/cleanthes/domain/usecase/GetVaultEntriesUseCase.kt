package dev.favourdevlabs.cleanthes.domain.usecase

import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import javax.crypto.SecretKey
import javax.inject.Inject

class GetVaultEntriesUseCase @Inject constructor(
    private val repository: VaultRepository,
) {
    data class Result(
        val entries: List<VaultEntry>,
        val categories: List<String>,
    )

    @Throws(Exception::class)
    operator fun invoke(key: SecretKey): Result = Result(
        entries    = repository.getAllEntries(key),
        categories = repository.getAllCategories(),
    )
}
