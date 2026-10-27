package dev.celiobenj.seldonfleet.domain.repository

/**
 * Falha ao carregar a frota inteira (defeito do arquivo, não de um registro).
 * Faz parte do contrato de [DeviceRepository], para a interface distinguir os casos
 * sem conhecer a camada de dados.
 */
class FleetLoadException(val reason: Reason, cause: Throwable) : Exception(cause) {
    enum class Reason {
        // arquivo ausente ou ilegível
        FILE_UNAVAILABLE,

        // JSON inválido ou fora do esquema (campo obrigatório ausente, tier desconhecido)
        INVALID_CONTENT,
    }
}
