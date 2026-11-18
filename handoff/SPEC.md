# App de locação de mesas e cadeiras para festas — Especificação

Protótipo navegável de referência: `prototype/Main.dc.html` (formato do canvas do Claude; serve como referência visual e de comportamento, não roda fora dele).
Lógica de negócio pronta e testada: `reference/core.mjs` (+ `core.test.mjs`, `sample-data.json`).

## 1. Objetivo
App mobile (390×844 como base) para quem aluga mesas e cadeiras para festas gerenciar pedidos, estoque por data, cobrança por Pix e agenda de entregas/retiradas. Idioma: português do Brasil. Moeda: BRL.

## 2. Estoque
- **Materiais:** Plástico e Metal.
- **Itens por material:** Plástico = Mesas, Cadeiras, Cadeiras com braço. Metal = Mesas, Cadeiras.
- **Cores (para os dois materiais):** Preta, Branca, Vermelha.
- São **15 modelos fixos**, com id `<p|m>-<mesa|cadeira|cadeiraBraco>-<pr|br|vm>`. O usuário só informa a **quantidade (total)** de cada modelo. Padrão: 0 (o protótipo traz valores de exemplo).
- Layout da tela Estoque: título → aba **Plástico | Metal** (some quando o filtro de material do menu não é "Todos") → 3 cartões de **jogos completos** (um por cor) → um bloco por item, com título e "N no total" à direita e 3 colunas de cor (bolinha + nome + campo numérico). O usuário só preenche o que tem.
- **Jogo completo** (por material + cor) = `min(mesas, floor(cadeiras sem braço / cadeiras por mesa))`.
- **Cadeiras por mesa:** campo por material + cor, padrão 4. Vale para o preço do jogo, o uso do estoque e os textos do pedido, sempre pelo material e cor da **mesa** do jogo.
- Não há tela de "disponível" nem de conserto no Estoque. Quebradas/faltantes são baixadas do total na devolução (ver seção 6).

## 3. Preços (menu ⋮ → Preços)
- Por **material + item** (igual para todas as cores): valor da diária e valor de reposição.
- **Desconto por jogo** (padrão R$ 2) e **frete padrão** (padrão R$ 40, pré-preenche o pedido novo).
- Preço do jogo/dia = diária da mesa + cadeiras por mesa × diária da cadeira − desconto por jogo (mínimo 0).
- Valores de exemplo: p-mesa 8/120, p-cadeira 3/45, p-cadeiraBraco 4/60, m-mesa 12/180, m-cadeira 5/80 (diária/reposição).

## 4. Pedido
Campos: cliente, telefone, CPF/CNPJ (opcional), endereço, ponto de referência, data de entrega, data de retirada, itens, frete, desconto, sinal, caução, forma de pagamento (Pix, Dinheiro, Cartão, Transferência).
- **Itens (linhas):** `jogo` (1 mesa + N cadeiras), `mesa` avulsa, `cadeira` avulsa; cada uma com modelo (cor/material/braço) e quantidade.
- **Diárias** = dias entre entrega e retirada, mínimo 1.
- **Total** = Σ(diária das linhas) × diárias + frete − desconto (mínimo 0). **Saldo** = total − sinal.
- **Status:** Orçamento → Confirmado → Entregue → Devolvido.
- Validação ao salvar: cliente, telefone, endereço, retirada ≥ entrega, pelo menos 1 item. Confirmar exige estoque disponível; Orçamento pode ser salvo sem estoque.
- Ações por status: Orçamento → "Confirmar pedido"; Confirmado → "Marcar como entregue"; Entregue → "Confirmar devolução" (com vistoria).

## 5. Disponibilidade por data e modelo
- Reservam estoque os pedidos **Confirmado** e **Entregue**. **Orçamento** e **Devolvido** não reservam.
- Dois períodos conflitam se `entrega <= fimDoOutro && retirada >= inícioDoOutro` (o dia da retirada ainda conta como ocupado).
- Livre(modelo) = total − manutenção − reservado no período. Exemplo do usuário: 10 jogos alugados para o dia 18/11 → 0 livres naquele dia.
- Na tela de novo pedido, mostrar por modelo "usa N de M livres" e avisar quando faltar.
- **Pendente (não implementado):** horário de entrega/retirada e folga entre pedidos (hoje a granularidade é o dia).

## 6. Devolução e vistoria
Por modelo usado no pedido, informar **quebradas** e **faltando** (soma ≤ quantidade do pedido) e **dias de atraso** (0 a 30).
- Total final = total + (quebradas + faltando) × reposição + atraso × diária do pedido.
- Resto = total final − (sinal + caução). Positivo → "A receber do cliente"; negativo → "Devolver ao cliente".
- Ao confirmar a devolução: status vira Devolvido, grava o total final, e as quebradas e faltantes **saem do total do estoque**.

## 7. Pix (menu ⋮ → Config. Pix)
- Config: chave Pix, nome do recebedor, cidade (não fica no Estoque).
- **BR Code estático** (padrão do Banco Central): EMV TLV, CRC16-CCITT (0x1021), `br.gov.bcb.pix`, moeda 986, nome ≤ 25 e cidade ≤ 15 caracteres, maiúsculas e sem acento, tx id `PED<id>`. Ver `pixPayload` em `core.mjs`.
- Na tela do pedido: opções Sinal (50%), Saldo, Valor após vistoria (quando houver resto) e Outro valor; mostra **QR Code** e **Pix copia e cola** (botão copiar) e botão **compartilhar no WhatsApp** (`wa.me/55<telefone>?text=…` com o código no texto).
- O protótipo gera o QR com `qrcode-generator` (MIT); está em `reference/`.

## 8. Agenda e Google Agenda
- Aba **Agenda**: calendário mensal; cada dia mostra entregas e retiradas; tocar no dia lista os pedidos.
- No pedido: botões "Adicionar entrega/retirada ao Google Agenda" via link `calendar.google.com/calendar/render?action=TEMPLATE…` (evento de dia inteiro, título, itens, telefone, referência, saldo e endereço). Ver `linkGoogleAgenda`. Integração real com a API do Google (OAuth) ficou fora do escopo.

## 9. Navegação e telas
- Barra inferior: **Início, Pedidos, Novo, Agenda, Estoque**.
- Barra superior: título "Locação de festas" + etiqueta do material filtrado + menu ⋮.
- Menu ⋮: botão **Material** (sub-seleção: Todos, Plástico, Metal; mostra a escolha atual), **Preços**, **Config. Pix**.
- **Início:** mesas e cadeiras livres hoje (de N no estoque), entregas e retiradas de hoje, "a receber" dos pedidos em aberto, agenda de hoje, próximos pedidos.
- **Pedidos:** filtro por status (Todos, Orçamento, Confirmado, Entregue, Devolvido), ordenados pela entrega (mais recente primeiro), com alerta "Entregar hoje", "Retirar hoje", "Retirada atrasada".
- **Detalhe do pedido:** dados, itens, totais, Pix, vistoria, ação do status.

## 10. Filtro por material (menu ⋮ → Material)
Todos | Plástico | Metal. Só filtra o que aparece (os dados dos outros materiais continuam). Afeta Estoque, Preços, modelos do Novo pedido, números do Início, e a lista de Pedidos/Agenda/"a receber" (um pedido aparece se tiver ao menos um item do material). Ao trocar o filtro, o item em edição no Novo pedido troca para o modelo equivalente do material escolhido.

## 11. Regras de interface importantes
- **Campos numéricos precisam poder ser apagados no celular.** Ao apagar, o campo fica vazio enquanto a pessoa digita; o mínimo (1 ou 0) só vale no cálculo. Não "forçar" o valor mínimo a cada tecla.
- Alvos de toque ≥ 44 px; campos de data nativos.
- Textos curtos; evitar excesso de rótulos (o usuário pediu simplicidade em todo o estoque).
- Fonte dos rótulos "jogos completos" e "Cadeiras por mesa": 10 px, uma linha só, para caber nos 3 cartões.

## 12. Decisões em aberto
1. Horário de entrega/retirada e disponibilidade considerando horário/folga.
2. Zerar o estoque e apagar os pedidos de exemplo para uso real.
3. Bloquear jogo com mesa de um material e cadeira de outro (hoje a regra de cadeiras por mesa segue a mesa).
4. Persistência (hoje só em memória), login/multiusuário, backup, notificações, recibo em PDF.
5. Mostrar disponibilidade por cor no Início.

## 13. Dados de exemplo
`reference/sample-data.json` traz o estoque de exemplo (Preta/Branca/Vermelha): p-mesa 10/40/8, p-cadeira 40/160/30, p-cadeiraBraco 20/40/10, m-mesa 20/10/6, m-cadeira 80/40/20; 5 pedidos de exemplo e a config Pix de exemplo. "Hoje" no protótipo é fixo em 2026-11-17.
