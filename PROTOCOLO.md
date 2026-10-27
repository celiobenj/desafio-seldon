# PROTOCOLO — Classificação de saúde da frota (Seldon Fleet)

Versão 1, registrada antes da implementação da regra de classificação.

Este documento declara o critério de classificação **antes** dos resultados. Quem o ler deve conseguir prever, sem executar o app, o status que o app atribui a qualquer registro de `frota.json`. A ordem do documento segue a de um protocolo experimental: critério, variáveis, tratamento dos dados defeituosos, controles, ameaças à validade e, por último, resultados.

## 1. Critério declarado

Adoto a **regra de referência do enunciado**, sem alterar fórmulas nem limiares.

Raciocínio:
- É a regra definida no enunciado, sem fórmulas ou limiares próprios. Uma regra própria (percentil, por tier, ponderada) exigiria uma justificativa e uma linha de base que a amostra de 30 registros, sem rótulo de "defeito real", não sustenta (ver ameaça 4).
- A grandeza que classifica é o **dreno relativo** (%/h), e não o absoluto (mAh/h). O dreno relativo divide pela capacidade da bateria e, por isso, torna comparáveis aparelhos com baterias diferentes: 900 mAh/h é grave em 3000 mAh e apenas ruim em 5000 mAh.

## 2. Variáveis

### 2.1 Entradas (por registro)

| Campo | Uso |
|---|---|
| `batteryCapacityMah` | Divisor do dreno relativo |
| `screenOnMinutes` | Divisor do dreno absoluto |
| `energyConsumed` + `energyUnit` (+ `nominalVoltage` quando `mWh`) | Energia consumida, normalizada para mAh |
| `crashCount7d` | Sinal de falhas recentes |
| `jankFramesPct` | Sinal de quadros perdidos |

Os demais campos (`id`, `model`, `soc`, `tier`, `ramMb`, `androidApi`, `batteryLevelPct`, `lastSeen`) são exibidos, mas **não** participam da classificação.

### 2.2 Normalização (antes de qualquer cálculo ou comparação)

```
energiaMah = indisponível                        se energyConsumed ausente ou < 0
energiaMah = energyConsumed                      senão, se energyUnit = "mAh"
energiaMah = energyConsumed ÷ nominalVoltage     senão, se energyUnit = "mWh" e nominalVoltage > 0
energiaMah = indisponível                        em qualquer outro caso
```

### 2.3 Derivadas

```
drenoAbsoluto (mAh/h) = energiaMah ÷ (screenOnMinutes ÷ 60)
drenoRelativo (%/h)   = (drenoAbsoluto ÷ batteryCapacityMah) × 100
```

Uma derivada só é calculada quando todos os seus divisores são válidos (> 0). Quando não pode ser calculada, ela fica **ausente**: nunca vira `NaN`, `Infinity`, zero ou outro valor inventado.

### 2.4 Regra de classificação

Os estados de domínio são `OK`, `ATENCAO`, `CRITICO` e `INDETERMINADO`. As condições são avaliadas **nesta ordem**, e a primeira que casar define o status:

| Ordem | Status | Condição (qualquer uma basta) |
|---|---|---|
| 1 | `INDETERMINADO` | capacidade ausente ou ≤ 0; **ou** `screenOnMinutes` ausente ou ≤ 0; **ou** energia indisponível |
| 2 | `CRITICO` | drenoRelativo ≥ 12 %/h; **ou** `crashCount7d` ≥ 3; **ou** `jankFramesPct` ≥ 15 % |
| 3 | `ATENCAO` | drenoRelativo ≥ 7 %/h; **ou** `crashCount7d` ≥ 1; **ou** `jankFramesPct` ≥ 8 % |
| 4 | `OK` | nenhuma das anteriores |

- Todos os limiares são **inclusivos** (`≥`): um valor exatamente igual ao limiar promove o status.
- Precedência: `INDETERMINADO` vence tudo; `CRITICO` vence `ATENCAO`.
- A justificativa exibida no detalhe cita **todos** os sinais que atingiram o limiar do status atribuído, nesta ordem: dreno relativo, falhas, quadros perdidos. Para `INDETERMINADO`, cita o motivo da falta de dados.
- Os rótulos de tela ("ATENÇÃO", "CRÍTICO", "SEM DADOS") pertencem à interface. O domínio só conhece os quatro nomes acima.

### 2.5 Decisão numérica

A fórmula em ponto flutuante pode produzir um valor ligeiramente abaixo do exato. Exemplo: capacidade 5000 mAh, tela de 249 min e energia de 1452,5 mAh. O valor exato do dreno relativo é 7 %/h, mas a conta na ordem da implementação, `energia ÷ (min ÷ 60) ÷ capacidade × 100`, resulta em `6,999999999999999` em `Double`. Sem tratamento, um registro exatamente no limiar seria classificado abaixo dele por ruído de arredondamento.

Decisão: as comparações do **dreno relativo** com os limiares usam uma tolerância declarada de **1e-9 %/h**, ou seja, `drenoRelativo ≥ limiar − 1e-9`. A tolerância é uma constante nomeada no domínio. Ela não se aplica a falhas (inteiro) nem a quadros perdidos (valor lido diretamente do arquivo, sem cálculo).

A tolerância é cerca de 10 ordens de grandeza menor que a resolução dos dados (0,1 mAh em 5000 mAh ≈ 0,002 %/h), portanto não altera nenhum caso real. Os testes de fronteira usam 6,99 e 7,0, muito além dela, e continuam prendendo o limiar.

### 2.6 Exibição

Os valores derivados são exibidos com 1 casa decimal e vírgula (por exemplo, "13,4 %/h"). A comparação com os limiares usa o valor sem arredondamento. Por isso, um dreno de 6,99 %/h pode aparecer como "7,0 %/h" com status inferior ao do limiar de 7: a decisão vale para o valor exato, não para o exibido.

## 3. Tratamento dos dados defeituosos

### 3.1 Defeito 1 — unidade de energia inconsistente

10 dos 30 registros informam `energyConsumed` em `mWh`; os demais, em `mAh`. Comparar os dois sem converter produz um erro de cerca de 3,85×.

Tratamento: a conversão `mAh = mWh ÷ nominalVoltage` é feita na **camada de dados**, no mapeamento do registro lido para o modelo de domínio. O domínio recebe apenas energia em mAh e não conhece a unidade original. Se `nominalVoltage` estiver ausente, nulo ou ≤ 0, a energia fica indisponível e o registro é `INDETERMINADO`. Uma unidade desconhecida (diferente de `mAh` e `mWh`) também resulta em energia indisponível: o app não adivinha o fator de conversão.

### 3.2 Defeito 2 — registros incompletos (divisores nulos)

No arquivo: `DEV-0311` tem `batteryCapacityMah = 0` e `DEV-0745` tem `screenOnMinutes = 0`.

O enunciado admite três respostas: classificar como indeterminado, excluir da lista ou falhar na ingestão. **Decisão: classificar como `INDETERMINADO` e manter o registro na lista.**

Raciocínio:
- Excluir violaria "exibe todos os dispositivos" e faria a soma das contagens por status (28) divergir do total de registros do arquivo (30).
- Falhar na ingestão por causa de 2 registros em 30 esconderia os outros 28. Falhar é a resposta adequada a um defeito do **arquivo** (JSON inválido), não a um defeito de **registro**.
- O estado `INDETERMINADO` já está previsto na regra, e manter o registro visível preserva a informação: o detalhe explica por que ele não foi classificado.

### 3.3 Ingestão tolerante (outros arquivos no mesmo esquema)

O arquivo recebido não exercita todos os casos previstos pelo esquema. O app trata explicitamente:

| Situação | Resultado |
|---|---|
| `batteryCapacityMah` ausente, nulo ou ≤ 0 | `INDETERMINADO` (capacidade inválida) |
| `screenOnMinutes` ausente, nulo ou ≤ 0 | `INDETERMINADO` (tempo de tela inválido) |
| `energyConsumed` ausente ou nulo | `INDETERMINADO` (energia indisponível) |
| `energyConsumed` negativo | `INDETERMINADO` (energia indisponível) |
| `energyUnit = "mWh"` sem `nominalVoltage` > 0 | `INDETERMINADO` (energia indisponível) |
| `energyUnit` desconhecida | `INDETERMINADO` (energia indisponível) |
| `lastSeen` ausente ou com formato inválido | Exibido como "—"; não afeta a classificação |
| Chaves extras no JSON | Ignoradas |
| Campo obrigatório ausente em algum registro (`id`, `model`, `soc`, `tier`, `ramMb`, `androidApi`, `batteryLevelPct`, `energyUnit`, `jankFramesPct`, `crashCount7d`) ou `tier` diferente de `low`, `mid` e `high` | Arquivo inválido: tela de erro explícita, sem encerrar o app |
| Arquivo ausente, ilegível ou JSON inválido | Tela de erro explícita, sem encerrar o app |

O esquema garante a presença dos campos obrigatórios listados acima. A ausência de um deles indica um arquivo fora do esquema, ou seja, um defeito do **arquivo**. Isso é diferente de capacidade 0, tela 0 ou unidade/tensão faltante, que são defeitos de **registro** previstos pelo enunciado e tratados como `INDETERMINADO`.

Em nenhum desses casos o app produz `NaN`, `Infinity` ou exceção não tratada.

## 4. Controles

Testes automatizados na JVM (`./gradlew test`, sem emulador), executados pela integração contínua a cada push:

- **Fronteiras com valores literais.** Cada limiar é testado imediatamente abaixo e no próprio limiar: dreno 6,99/7,0 e 11,99/12,0 %/h; falhas 0/1/2/3; quadros perdidos 7,99/8,0 e 14,99/15,0 %. Os valores esperados são escritos nos testes, e não lidos das constantes de produção. Assim, alterar um limiar no código faz os testes falharem.
- **Precedência.** Sinais de `CRITICO` e `ATENCAO` simultâneos resultam em `CRITICO`; capacidade 0 com falhas altas resulta em `INDETERMINADO`.
- **Equivalência de unidades.** Um registro em mWh e outro em mAh que representam o mesmo consumo físico (3773 mWh a 3,85 V e 980 mAh) produzem o mesmo dreno e o mesmo status.
- **Registros incompletos.** Capacidade 0, nula e negativa; tela 0 e nula; energia indisponível (energia negativa, mWh sem tensão válida, unidade desconhecida). Todos resultam em `INDETERMINADO`, sem exceção e sem `NaN`/`Infinity`.
- **Classificação esperada dos 30 registros.** Uma cópia congelada de `frota.json` em `app/src/test/resources` é classificada pelo app e comparada, registro a registro, com a classificação esperada, calculada a partir da regra do enunciado.

## 5. Como prever o status de um registro

1. Converta a energia para mAh (seção 2.2). Se for impossível, o status é `INDETERMINADO`.
2. Se a capacidade ou o tempo de tela forem ausentes ou ≤ 0, o status é `INDETERMINADO`.
3. Calcule `drenoRelativo = energiaMah ÷ (screenOnMinutes ÷ 60) ÷ batteryCapacityMah × 100`.
4. Se drenoRelativo ≥ 12, ou falhas ≥ 3, ou quadros perdidos ≥ 15: `CRITICO`.
5. Senão, se drenoRelativo ≥ 7, ou falhas ≥ 1, ou quadros perdidos ≥ 8: `ATENCAO`.
6. Senão: `OK`.

### Exemplos resolvidos

| Registro | Cálculo | Status |
|---|---|---|
| `DEV-0137` (mAh) | 980 ÷ (214/60) = 274,8 mAh/h; 274,8 ÷ 4410 × 100 = **6,2 %/h** (abaixo de 7). Mas `crashCount7d` = 2 ≥ 1 | `ATENCAO` (falhas) |
| `DEV-0204` (mWh) | 3773 mWh ÷ 3,85 V = 980 mAh; 980 ÷ (186/60) = 316,1 mAh/h; ÷ 5000 × 100 = **6,3 %/h**; falhas 0; quadros 4,1 % | `OK`. Sem a conversão, daria 24,3 %/h e `CRITICO` |
| `DEV-0311` (mAh) | `batteryCapacityMah` = 0 | `INDETERMINADO`, embora `jankFramesPct` = 11,2 e 1 falha disparassem `ATENCAO`: a ordem da regra prevalece |
| `DEV-0522` (mWh) | 6136,5 mWh ÷ 3,87 V = 1585,7 mAh; 1585,7 ÷ (142/60) = 670,0 mAh/h; ÷ 5000 × 100 = **13,4 %/h** (≥ 12) | `CRITICO` (dreno relativo) |
| `DEV-0745` (mAh) | `screenOnMinutes` = 0 | `INDETERMINADO` |

## 6. Ameaças à validade

1. **Consumo atribuído integralmente à tela ligada.** O dreno divide toda a energia consumida pelo tempo de tela ligada. Um aparelho com serviço em primeiro plano e tela desligada consome energia sem acumular tempo de tela, e a métrica superestima o dreno, podendo gerar um falso `CRITICO`.
2. **Tensão nominal no lugar da tensão real.** A conversão mWh → mAh usa a tensão nominal da bateria, mas a tensão real varia com a carga e o estado de carga. Um erro de poucos por cento basta para mover aparelhos próximos de um limiar. Exemplo: `DEV-1546`, convertido de mWh, está em 7,49 %/h contra o limiar de 7.
3. **Precedência total de `INDETERMINADO`.** Um aparelho com capacidade 0 e 5 falhas em 7 dias aparece como "sem dados", embora as falhas sejam um sinal válido que independe da capacidade. A regra pode esconder um risco real. No arquivo, `DEV-0311` tem 1 falha e 11,2 % de quadros perdidos mascarados por esse motivo.
4. **Limiar único para toda a frota, sem rótulo de verdade-terreno.** Entre os registros classificáveis, 5 de 8 aparelhos `low` ficam em `ATENCAO` ou `CRITICO`, contra 3 de 10 aparelhos `high`. Sem um rótulo independente de "defeito real", e com 30 amostras, não há como saber se o limiar detecta defeito ou apenas hardware de entrada.

## 7. Resultados

Esta seção vem depois do critério de propósito: o critério acima foi fixado antes de os resultados serem observados no app.

Classificação esperada dos 30 registros de `frota.json`, calculada a partir da regra do enunciado. Será confirmada pelo teste automatizado da seção 4.

| Status | Registros |
|---|---|
| `OK` | 16 |
| `ATENCAO` | 8 |
| `CRITICO` | 4 |
| `INDETERMINADO` | 2 |
| **Total** | **30** |

Efeito do Defeito 1: sem a conversão de unidade, 8 registros mudariam de status (`DEV-0204`, `DEV-0630`, `DEV-0854`, `DEV-1142`, `DEV-1214` e `DEV-1422`, de `OK` para `CRITICO`; `DEV-1546` e `DEV-1582`, de `ATENCAO` para `CRITICO`), e o total de `CRITICO` subiria de 4 para 12.
