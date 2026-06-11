package dev.favourdevlabs.cleanthes.domain.usecase

import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import javax.inject.Inject

class DeleteVaultEntryUseCase @Inject constructor(
    private val repository: VaultRepository,
) {
    operator fun invoke(id: Long): Int = repository.deleteEntry(id)
}
