package dev.celiobenj.seldonfleet.domain.model

/**
 * Estados de saúde do domínio. São nomes, não textos de tela:
 * os rótulos exibidos ("ATENÇÃO", "SEM DADOS"...) pertencem à camada ui.
 */
enum class DeviceStatus { OK, ATENCAO, CRITICO, INDETERMINADO }
