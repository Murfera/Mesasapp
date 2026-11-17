# Plano do Mesasapp

Criado em 2026-11-17. Este arquivo é o estado do projeto: o que foi decidido, o que
está feito, o que falta e as ideias. Decisões técnicas detalhadas vão para o
`README.md` quando o código existir. **Ler este arquivo antes de retomar o trabalho.**

App Android para gerenciar **aluguel de mesas e cadeiras para festas**. Vai para a
Play Store. Tudo local (SQLite/Room), sem servidor e sem custo de operação.

---

## 1. Estrutura (revisada em 2026-11-17)

O usuário comparou com um app parecido (zaraeventos.web.app) e pediu a **mesma
estrutura**: menu inferior com cinco abas, painel de abertura e o pedido dividido
em blocos com o resumo do dinheiro fixo no rodapé. O que ficou **diferente de
propósito**: o estoque continua simples e as cores são só **Branca, Preta,
Vermelha e "Outra"** — ele achou o do amigo cheio de opção demais.

| Aba | O que faz |
|---|---|
| Início | Painel: livres agora, entregas/retiradas de hoje, a receber, próximos eventos |
| Pedidos | Lista com busca e filtro por situação; muda o status ali mesmo |
| Novo | Cliente → Local → Datas e horários → Itens → Valores, com resumo fixo |
| Agenda | Calendário do mês com pontos de entrega/retirada e o que acontece no dia |
| Estoque | Modelos com livres/alugadas/quebradas, total, diária e reposição |

A calculadora de orçamento virou a aba **Novo**, mantendo o princípio original:

A tela de pedido é uma **calculadora**, não um formulário.
A pessoa abre o app no meio de uma conversa de WhatsApp e precisa responder
"10 jogos, 1 dia, quanto fica?" em menos de 5 segundos.

```
┌──────────────────────────────┐
│  Mesasapp              ⋮     │
│                              │
│  Jogos      [ − ]  10  [ + ] │   (1 mesa + 4 cadeiras)
│  Mesas      [ − ]   0  [ + ] │
│  Cadeiras   [ − ]   0  [ + ] │
│                              │
│  Entrega 18/11  Retirada 19/11
│  Dias: 1            Frete R$ 20
│                              │
│  ✓ Disponível para a data    │   (ou: "Faltam 2 jogos no dia 18")
├──────────────────────────────┤
│  TOTAL           R$ 470,00   │  ← barra fixa no rodapé, sempre visível
│  [ Pix ]       [ Salvar ]    │
└──────────────────────────────┘
```

Regras da tela:
- O total recalcula a cada toque, sem botão "calcular".
- Nada de formulário obrigatório para ver preço: cliente, endereço e telefone só
  são pedidos quando a pessoa toca em **Salvar** (vira locação).
- A disponibilidade aparece como uma linha de status, verde ou vermelha,
  consultando a data escolhida.
- Menu `⋮`: Locações, Estoque/Itens, Clientes, Configurações.

---

## 2. Modelo de dados (Room / SQLite)

### `item` — catálogo e estoque
| campo | tipo | observação |
|---|---|---|
| id | Long | |
| nome | String | "Cadeira plástica branca" |
| categoria | enum | MESA, CADEIRA |
| material | enum | PLASTICO, METAL, MADEIRA, OUTRO |
| comBraco | Boolean? | só faz sentido em cadeira |
| cor | String | |
| quantidadeTotal | Int | quantas peças existem |
| precoDiariaCentavos | Long | preço de 1 diária |
| valorReposicaoCentavos | Long | cobrado quando quebra ou some |
| ativo | Boolean | |

### `combo` — o "jogo"
Um jogo **não tem estoque próprio**: é uma forma de vender. 1 jogo = 1 mesa + N
cadeiras, e a reserva baixa mesa e cadeira do estoque real. Isso evita estoque
duplicado (o erro clássico: 10 jogos e 40 cadeiras virarem 80 cadeiras).
Campos: nome, itemMesaId, itemCadeiraId, qtdCadeiras (padrão 4), precoDiariaCentavos.

### `locacao` — o pedido
clienteId, endereço de entrega, dataHoraEntrega, dataHoraRetirada, dias,
freteCentavos, montagemCentavos, descontoCentavos, sinalCentavos,
caucaoCentavos, observações, status, criadoEm.

**Status:** ORCAMENTO → CONFIRMADA → ENTREGUE → DEVOLVIDA (ou CANCELADA).
Só CONFIRMADA e ENTREGUE ocupam estoque; orçamento não reserva nada.

### `locacao_item` — linhas do pedido
locacaoId, itemId (ou comboId), quantidade, **precoDiariaCentavos congelado**
(mudar o preço do catálogo não pode alterar pedido antigo), qtdDevolvida,
qtdQuebrada, valorQuebraCentavos.

### `cliente`
nome, telefone (WhatsApp), endereço padrão, observações.

### `pagamento`
locacaoId, valorCentavos, data, forma (PIX, DINHEIRO, CARTAO, OUTRO).

### Configurações (DataStore)
nome da empresa, chave Pix, nome do recebedor, cidade, frete padrão,
diária adicional (%), hora padrão de entrega e de retirada.

### Decisões técnicas herdadas do Finapp
- Dinheiro é **`Long` em centavos**, nunca `Double`.
- Datas como `epochDay` / `epochSecond` (`Long`), para comparar intervalo no SQL.
- **Nenhuma permissão perigosa** — é o que permite declarar "nenhum dado coletado"
  no formulário de Segurança de Dados do Play.
- Room com schema exportado e versionado; injeção manual (`di/AppContainer`), sem Hilt.
- Kotlin + Compose + Material 3, mesma stack e versões do Finapp.

---

## 3. Cálculo do total

```
subtotal_itens = Σ (quantidade × preço_diária × fator_dias)
fator_dias     = 1 + (dias − 1) × percentual_diária_adicional
total          = subtotal_itens + frete + montagem − desconto
a_receber      = total − sinal (+ quebras na devolução)
```

- `dias` = diferença entre entrega e retirada, **mínimo 1**.
- `percentual_diária_adicional` é configurável (padrão a definir — ver perguntas).
- Na devolução, as quebras somam `qtdQuebrada × valorReposição` e o total é refeito.

## 4. Controle de estoque por data

Disponível de um item numa janela = `quantidadeTotal − Σ quantidade das locações
CONFIRMADA/ENTREGUE cuja janela [entrega, retirada] se sobrepõe à janela pedida`.

A **hora importa**: se a retirada é às 10h do dia 18 e a próxima entrega é às 14h
do dia 18, o mesmo jogo atende os dois eventos. Sem hora, o app recusaria uma
locação possível. A tela continua simples: a pessoa escolhe só as datas e o app
usa as horas padrão das Configurações; quem quiser, ajusta o campo de hora.

## 5. Pix

Gerar **BR Code estático (EMV)** com a chave Pix do usuário, offline, sem conta de
API e sem custo: o payload é montado em Kotlin e o QR é desenhado com ZXing.
Gera os dois formatos — "copia e cola" e imagem do QR — para mandar no WhatsApp
junto com o resumo do orçamento (`ACTION_SEND`).

Limitação a saber: QR estático **não avisa quando o cliente paga**. A baixa é
manual (marcar pagamento recebido). Confirmação automática exigiria conta PJ com
API Pix de banco — fora do escopo e do custo zero.

## 6. Sugestões (da pesquisa e da prática do ramo)

Já previstas no modelo acima:
- **Sinal/entrada** e saldo a receber — reclamação comum do ramo é sobre devolução de sinal.
- **Caução** — valor, o que cobre e prazo de devolução.
- **Frete e montagem separados** do aluguel (contabilmente são receitas diferentes).
- **Valor de reposição por item**, que é o que torna o cálculo de quebra automático.
- **Status do pedido** + conferência na devolução (quantos voltaram, quantos
  quebrados) — atraso e item faltando são as queixas mais frequentes do setor.

Ideias para depois (não entram na v1):
- Contrato/recibo em PDF com a lista de itens e o estado de cada um.
- Multa por atraso na devolução (diárias extras automáticas).
- Fotos da entrega e da devolução anexadas ao pedido.
- Relatório de faturamento do mês e taxa de utilização do estoque.
- Itens extras: toalha, lona, kit infantil.

## 7. Escopo da v1 (nesta ordem)

1. Projeto Gradle + tema + navegação + Room com o schema acima.
2. Cadastro de itens e combos (estoque, preços, material, cor, braço).
3. **Tela principal: orçamento rápido com total ao vivo.**
4. Disponibilidade por data (linha de status na tela principal).
5. Salvar como locação: cliente, endereço, datas, frete, sinal.
6. Lista de locações, detalhe e mudança de status.
7. Devolução com quebras e total refeito.
8. Pix (copia e cola + QR) e compartilhar orçamento no WhatsApp.
9. Configurações (empresa, chave Pix, padrões).

**Fora da v1 por decisão do usuário:** calendário / Google Agenda.
**Depois:** backup (provavelmente igual ao do Finapp — JSON manual + automático).

## 8. Como testar (obrigatório antes de dizer que está pronto)

`gradlew.bat :app:testDebugUnitTest --no-configuration-cache` roda **testes de
interface de verdade** (Robolectric): abrem o app, tocam nos botões, digitam nos
campos e conferem o resultado. Não dependem do celular estar ligado e livre.

Dois cuidados que custaram tempo:
- A tela simulada precisa de tamanho de celular (`@Config(qualifiers =
  "w411dp-h891dp-xhdpi")`); no tamanho padrão as linhas ficam fora da área
  visível e **os toques se perdem em silêncio**.
- Em lista preguiçosa, o que está fora da tela não existe: rolar até o controle
  antes de tocar (helper `rolarAte`).
- O dinheiro usa espaço não separável depois do "R$"; comparar por "100,00".

Compilar e o app abrir **não é teste**. Foi assim que a tela de estoque foi
entregue impossível de preencher.

## 9. Combinados de trabalho

- Testar no **Moto G73 por depuração Wi-Fi**; não criar emulador/máquina virtual agora.
- **Git a cada etapa salva**, para dar para voltar atrás e comparar (pedido dele em
  2026-11-17). Repositório privado e separado: https://github.com/Murfera/Mesasapp.
- Build release só quando ele disser que é a oficial.
- Apresentar opções e custo antes de implementar caminho com bifurcação.

## 10. Perguntas abertas

- Diária adicional: 2 dias custam o dobro ou tem desconto?
- `applicationId`: `br.com.mesasapp`?
- O link do Cloud Designer não pôde ser lido (chat renderizado no navegador). Se o
  menu de lá for para ser aproveitado, falta um print ou o código.

## 11. Estado

- 2026-11-17: plano criado.
- 2026-11-17: **esqueleto do app pronto e compilando** (`:app:assembleDebug` OK,
  6 testes passando, `lintDebug` limpo). Build com
  `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`.
  - Projeto Gradle igual ao do Finapp (AGP 9.4.1, Kotlin 2.3.21, Compose BOM 2026.09.00,
    Room 2.8.5, minSdk 26, targetSdk 36), `applicationId = br.com.mesasapp`.
  - Room com as 6 tabelas: item, combo, cliente, locacao, locacao_linha, locacao_peca.
  - **Tela principal (Orçamento)**: steppers de jogo/mesa/cadeira, datas e horas de
    entrega e retirada, frete, linha de disponibilidade e barra fixa com o TOTAL.
    Salvar abre o diálogo de cliente/telefone/endereço/sinal/desconto e grava como
    **orçamento** (não reserva) ou **locação confirmada** (reserva estoque).
  - **Tela Estoque**: itens (categoria, material, cor, com/sem braço, quantidade,
    diária, valor de reposição) e jogos (mesa + N cadeiras + preço).
  - Ícone próprio (vetor: mesa com duas cadeiras), tema claro/escuro com cor dinâmica.
  - Item já usado em locação não é excluído, é desativado — o histórico não pode
    ficar sem descrição.

### Próximo passo

1. Lista de locações, detalhe e mudança de status.
2. Devolução com quebra e total refeito.
3. Pix (copia e cola + QR) e compartilhar no WhatsApp.
4. Tela de Configurações (empresa, chave Pix, horas e frete padrão) — o `ConfigStore`
   já existe e é lido pela tela principal, falta a tela para editar.
5. Cadastrar o estoque real no celular e conferir o cálculo na tela.

### Como conectar no celular (funcionou em 2026-11-17)

```powershell
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-toolsdb.exe"
& $adb pair 192.168.15.9:<porta de pareamento> <codigo>   # tela de pareamento aberta
& $adb mdns services        # depois de parear, mostra a porta de conexao
& $adb connect 192.168.15.9:<porta de conexao>
```

A porta e o código de pareamento **só valem enquanto a janela está aberta** no
celular. Depois de parear, o `adb mdns services` revela a porta de conexão
sozinho — não precisa pedir o segundo número. Build:
`$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`.

### Testado no aparelho

- 2026-11-17, Moto G73: app instala, abre sem travar e mostra o catálogo semeado
  (jogo, mesa e cadeira, tudo zerado). Corrigido na hora: a barra do TOTAL ficava
  atrás da barra de navegação do Android (commit `ff2bea7`).
