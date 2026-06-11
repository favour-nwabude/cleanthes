package dev.favourdevlabs.cleanthes.domain.usecase

import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import javax.crypto.SecretKey
import javax.inject.Inject

class GetVaultEntryUseCase @Inject constructor(
    private val repository: VaultRepository,
) {
    @Throws(Exception::class)
    operator fun invoke(id: Long, key: SecretKey): VaultEntry? =
        repository.getEntryById(id, key)
}
