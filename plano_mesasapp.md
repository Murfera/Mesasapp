# Plano do Mesasapp

Atualizado em 2026-11-18. Este arquivo é o estado do projeto: o que foi decidido,
o que está feito, o que falta e o que ficou pendente de decisão.
**Ler antes de retomar o trabalho.**

App Android para gerenciar **aluguel de mesas e cadeiras para festas**. Vai para a
Play Store. Tudo local (SQLite/Room): nenhum dado sai do aparelho, nenhum servidor,
nenhum custo de operação.

- Repositório (privado): https://github.com/Murfera/Mesasapp
- `applicationId = br.com.mesasapp` — **não muda depois da primeira publicação**.

---

## 1. Estrutura das telas

**Desde 2026-11-18 a interface segue o protótipo que o usuário fez no Claude chat**
(`handoff/`: `SPEC.md`, `prototype/Main.dc.html`, `reference/core.mjs`). Ele pediu
"deixe igual a interface"; regras de negócio ele decide depois. Na dúvida sobre
visual, texto ou fluxo, **o protótipo manda**.

- Barra de cima: "Locação de festas" + etiqueta do material filtrado + menu ⋮
  (**Material**: Todos/Plástico/Metal, **Preços**, **Config. Pix**, **Exibir**, **Tema**).
- **Exibir** (menu ⋮): chaves "Sinal pago" e "Caução", **desligadas por padrão**; só
  com elas ligadas os campos aparecem no Novo pedido. Desligar zera o valor digitado.
- Abas: **Início, Pedidos, Novo, Agenda, Estoque**. Detalhe do pedido, Preços e
  Config. Pix abrem por cima (o voltar do Android volta).
- **Novo** (refinado pelo usuário em 2026-11-18, mais enxuto que o protótipo):
  - **Cliente**: só o campo nome (sem rótulo) + botão "Contato e endereço" que abre
    tela cheia com WhatsApp, CPF/CNPJ, endereço e referência. **Só o nome é
    obrigatório**; o botão fica verde quando há algum dado.
  - **Datas**: "N diárias" na linha do título, à direita.
  - **Itens**: barra Plástico | Metal → barra Jogo | Mesa | Cadeira → barra Sem braço |
    Com braço → 3 cartões por cor com "livres" nas datas. **Só aparece o que existe no
    estoque** (material, braço e cor); se o marcado some, marca o que existe. A cor vale
    para mesa e cadeiras do jogo (não dá mais para misturar cores num jogo).
  - **Valores**: frete, desconto, forma de pagamento (sinal/caução só se ligados em Exibir).
  - **Sem rodapé fixo**: botão "Confirmar pedido · R$ X" no fim abre um painel de baixo
    (resumo + "Salvar orçamento" / "Emitir pedido"; emitir = reserva estoque).
- **Detalhe**: dados, itens e valores, vistoria (quebradas, faltando, dias de atraso)
  com total refeito, Google Agenda (só link), Pix (Sinal 50% / Saldo / Após vistoria
  / Outro valor, QR, copia e cola, WhatsApp) e o botão da próxima situação.
- **Estoque**: aba Plástico | Metal, 3 cartões de jogos completos (um por cor, com
  "Cadeiras por mesa") e um bloco por peça com 3 cores. Só quantidades.
- Cores e fontes do protótipo (verde #0F6B5C, Inter e Space Grotesk em `res/font`).
  Títulos de página todos com 20 sp. Opção marcada e botões cheios usam **degradê
  verde** (`Cores.GradienteVerde`). **Estoque**: fundo em degradê conforme a aba —
  **Plástico cinza, Metal azul** (o usuário pediu invertido) — e cartões em tom "gelo";
  aba marcada verde; blocos de quantidade mais baixos para caber sem rolar.
  **Tema no menu ⋮ → Tema**: Automático (segue o Android, padrão), Claro ou Escuro —
  o usuário preferiu a escolha dentro do app. Paleta escura derivada em
  `ui/theme/Theme.kt`; as telas usam `Cores.X`, nunca cor fixa. `Cores.Verde` é o
  fundo de botão cheio; `Cores.Destaque` é o verde de texto/ícone/borda.
- **Campo numérico com zero aparece vazio** (vazio vale 0), pedido do usuário.
  "Cadeiras por mesa" vazio = padrão 4, mostrado em cinza no campo.

---

## 2. Modelo de dados (Room, versão 3)

**15 modelos fixos** (`domain/Catalogo.kt`): Plástico = Mesa, Cadeira, Cadeira com
braço; Metal = Mesa, Cadeira; cores Preta, Branca, Vermelha. Ids iguais aos do
protótipo (`p-mesa-br`, `m-cadeira-pr`...).

| Tabela | Para que serve |
|---|---|
| `estoque` | modeloId → total. Modelo sem linha vale 0 |
| `pedido` | cliente, telefone, CPF/CNPJ, endereço, referência, início/fim em epoch, frete, desconto, sinal, caução, pagamento, status, dias de atraso, total final |
| `pedido_linha` | JOGO/MESA/CADEIRA, mesa, cadeira, qtd, **cadeiras por mesa e preço congelados** |
| `pedido_peca` | o que o pedido tira do estoque, por modelo, com **reposição congelada** e a vistoria (quebradas, faltando) |

Preços (diária e reposição por material+peça, desconto por jogo, frete padrão,
cadeiras por mesa por material+cor), Pix e filtro de material ficam em **DataStore**.

**Status:** Orçamento → Confirmado → Entregue → Devolvido. Só Confirmado e Entregue
reservam estoque. Não há "cancelado" (o protótipo não tem).

### Decisões técnicas que não devem ser revertidas por acidente

1. **Dinheiro é `Long` em centavos**, nunca `Double` (`util/Money.kt`).
2. **Datas em epoch de segundos.** A tela só pede o dia; a entrega é gravada às 08h e
   a retirada às 18h. Com a comparação estrita, o dia da retirada continua ocupado
   (regra do protótipo) e a hora já está no banco para quando ele quiser escolher.
3. **Preço, cadeiras por mesa e reposição são congelados no pedido.**
4. **O jogo não tem estoque próprio**: 1 mesa + N cadeiras, baixa do estoque real.
5. **Nenhuma permissão declarada.** Ligar abre o discador; WhatsApp e Google Agenda
   são links.
6. Todas as telas leem a lista de pedidos completa (3 consultas no total, com
   `@Relation`) e somam em memória, como o protótipo. Regras puras em `domain/Regras.kt`.
7. Um `MesasViewModel` só, para o app todo (o formulário do Novo sobrevive à troca de aba).
8. Kotlin + Compose + Material 3, mesma stack do Finapp; injeção manual.

⚠️ **Pendência antes de publicar:** o banco ainda usa
`fallbackToDestructiveMigration` — mudança de schema **apaga os dados**. Tem que
virar migração de verdade antes da primeira versão na Play Store.

---

## 3. Cálculo do total

```
diária do jogo = diária da mesa + cadeiras por mesa × diária da cadeira − desconto por jogo
aluguel        = Σ linhas  (× diárias, SE a regra for ligada)
total          = aluguel + frete − desconto     (mínimo 0)
saldo          = total − sinal
vistoria       = total + (quebradas + faltando) × reposição + atraso × diária
resto          = vistoria − (sinal + caução)    (negativo = devolver ao cliente)
```

**Os dias ainda não multiplicam o valor** — o protótipo multiplica, mas o usuário
deixou essa regra para decidir. Chave única: `MULTIPLICAR_POR_DIAS` em
`domain/Regras.kt`. Enquanto estiver falsa, o rodapé diz "Aluguel (valor de 1
diária)" em vez de "2 diárias × R$ X", para não mostrar conta que não foi feita.

## 4. Estoque por data

Livre = total − reservado por pedidos Confirmado/Entregue cuja janela se sobrepõe
(`inicioA < fimB && fimA > inicioB`). Confirmar exige estoque; orçamento salva sem.
Na devolução, quebradas e faltando **saem do total do estoque** (regra do protótipo).

## 5. Pix

BR Code estático (`util/Pix.kt`), igual byte a byte ao do protótipo (teste em
`RegrasTest`). QR com ZXing, nível M. Txid `PED<id>`. QR estático **não avisa
quando o cliente paga** — baixa manual.

---

## 6. Feito

- Interface inteira do protótipo (2026-11-18), com as regras de `core.mjs` portadas.
- **26 testes passando** (inclui o modo escuro abrindo todas as abas): 8 de regra (paridade com os números do protótipo, Pix e
  link do Google Agenda idênticos), 4 que clicam pelas telas (estoque, novo pedido,
  pedido confirmado → entregue → devolvido com quebra e Pix, filtro de material),
  6 de dinheiro, 7 do Pix.

## 7. Falta / decisões em aberto

0. **Os testes de interface estão desatualizados** desde o refinamento do Novo pedido
   (campos movidos para a janela de contato, botões no painel de resumo, cartões por cor).
   Rodar e ajustar **só quando o usuário pedir**.

1. **Multiplicar pelas diárias?** (`MULTIPLICAR_POR_DIAS`) — decisão dele.
2. **Horário de entrega/retirada** — ideia do usuário em 2026-11-24, **deixada de canto a
   pedido dele** ("depois eu penso"). Lembrar quando ele pedir pendências/ideias. O que foi
   conversado: dois campos de hora dentro do cartão Datas do Novo pedido; chave "Horário" no
   menu ⋮ → Exibir (desligada por padrão = 08:00/18:00 como hoje); mostrar a hora no detalhe
   do pedido, na mensagem do Pix no WhatsApp e no evento do Google Agenda, só com a chave
   ligada. **Decisão em aberto:** estoque continua contando o dia da retirada inteiro
   (recomendado: tempo de buscar/conferir) ou passa a usar a hora exata. O banco já guarda a
   hora (`inicioEpochSeg`/`fimEpochSeg`), não precisa mudar o schema.
7. **Cobrar com Pix com o sinal desligado**: os botões "Sinal (50%)" e "Saldo (total −
   sinal)" ainda aparecem. Perguntado em 2026-11-24 se esconde o de sinal e troca o outro
   por "Total"; sem resposta ainda.
3. Bloquear jogo com mesa de um material e cadeira de outro (hoje vale a mesa).
4. Cadastrar o estoque real e conferir o cálculo no celular (com o usuário).
5. Backup em JSON, no modelo do Finapp; migração de verdade do banco antes de publicar.
6. Conferir as telas em paisagem.

Ideias para depois: contrato/recibo em PDF, fotos da entrega, relatório de
faturamento, disponibilidade por cor no Início, itens extras (toalha, lona).

## 8. Como testar (obrigatório antes de dizer que está pronto)

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache
```

Rodam **testes de interface de verdade** (Robolectric, na JVM): abrem o app, tocam
nos botões, digitam nos campos e conferem o resultado — sem depender de o celular
estar ligado e livre. `--no-configuration-cache` é necessário: o cache de
configuração quebra com as tarefas de teste.

Cuidados que já custaram tempo:
- A tela simulada precisa de tamanho de celular
  (`@Config(qualifiers = "w411dp-h891dp-xhdpi")`); no tamanho padrão as linhas ficam
  fora da área visível e **os toques se perdem em silêncio**.
- Em lista preguiçosa, o que está fora da tela **não existe**: rolar até o controle
  antes de tocar (helper `rolarAte`).
- Procurar por marcador (`testTag`), não por texto. O selo de status guarda o texto
  num filho: `esperarNoCom` procura no nó e nos descendentes.
- **Filtro de material e preços ficam salvos entre os testes** (DataStore): teste que
  muda o filtro tem que voltar para "Todos" num `finally`, senão derruba os outros.
- O dinheiro usa espaço não separável depois do "R$": comparar por "100,00".
- **Lista rolável dentro de `AlertDialog` entra em laço infinito de layout** e trava
  a tela. Diálogo com lista tem que ser `Dialog` em tela cheia.

**Compilar e o app abrir não é teste.** Foi assim que a tela de estoque foi entregue
impossível de preencher, e o usuário teve razão em reclamar.

## 9. Como instalar no celular

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb pair 192.168.15.4:<porta de pareamento> <codigo>   # janela de pareamento aberta
& $adb mdns services                                      # revela a porta de conexao
& $adb connect 192.168.15.4:<porta de conexao>
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```

O IP do celular muda (já foi .9, em 2026-11-18 era .4): usar o que aparece na janela.
A porta e o código de pareamento **só valem enquanto a janela está aberta** no
celular. Depois de parear, `adb mdns services` revela a porta de conexão sozinho —
não precisa pedir o segundo número.

**Não mexer no aparelho**: nada de girar a tela, mudar configuração do sistema ou
navegar por toques via adb. Instalar e, se precisar, tirar print. O celular está na
mão do usuário e os toques caem no que ele estiver usando.

## 10. Combinados de trabalho

- **Git e GitHub só quando o usuário pedir** (desde 2026-11-18). Testes também só quando ele pedir.
- **Emulador liberado** (2026-11-18): AVD `Moto_G73_5G`, testar nele primeiro e
  depois instalar no Moto G73 por Wi-Fi. Script de toques/fotos usado na sessão:
  `adb shell input tap/text`, `uiautomator dump`, `screencap`. Cuidado: com o teclado
  aberto a tela sobe e as coordenadas mudam — fechar o teclado (tecla 4) antes de
  tocar no próximo campo.
- Build release só quando ele disser que é a oficial (começando pelo keystore de
  upload, guardado fora do repositório).
- Apresentar opções e custo antes de implementar caminho com bifurcação.

## 11. Histórico

- **2026-11-17** — plano criado; esqueleto do app compilando e instalado no G73;
  corrigida a barra do TOTAL que ficava atrás da barra de navegação (`ff2bea7`).
- **2026-11-17** — reestruturado em cinco abas a pedido do usuário, depois da
  comparação com o app do amigo (`1fc212d`).
- **2026-11-17** — o usuário testou e os botões não funcionavam: campo da diária
  impossível de preencher, chips cortados fora da tela, botões de salvar mortos.
  Corrigido e, a partir daí, **testes que clicam de verdade a cada build**
  (`30e8d68`).
- **2026-11-17** — detalhe do pedido, devolução com quebra, Pix e Configurações;
  achado e corrigido o laço infinito do diálogo com lista (`7fde186`).
- **2026-11-17** — material do modelo reduzido a Plástico e Metal (`435ea7a`).
- **2026-11-18** — 18 testes passando; versão atual instalada no G73 para o usuário
  cadastrar o estoque real e conferir o cálculo.
- **2026-11-18** — interface refeita igual ao protótipo do usuário (`handoff/`):
  15 modelos fixos, menu ⋮ com Material/Preços/Config. Pix, vistoria com atraso e
  caução, Pix com opções, Google Agenda por link. Banco v3 (recriado). 25 testes.
- **2026-11-18** — campos com zero ficam vazios; "cadeiras por mesa" vazio volta ao
  padrão 4 (antes gravava 1 escondido); modo escuro automático do Android.
- **2026-11-18** — versão marcada `v0.2-interface-prototipo` antes das mudanças do
  usuário. Tema escolhido no app (Automático/Claro/Escuro). Fluxo completo conferido
  no emulador (estoque → pedido → entregue → devolução com quebra). 27 testes.
- **2026-11-18** — refinamento das telas pelo usuário (sem rodar testes, a pedido dele):
  Novo pedido enxuto (contato num botão, cartões por cor, painel de resumo, só nome
  obrigatório), menu Exibir (sinal/caução), títulos menores, degradê verde, fundos e
  cartões "gelo" no Estoque. A partir daqui: **Git, GitHub e testes só quando ele pedir**.
- **2026-11-24** — o usuário passou a conferir direto no celular, que já tem dados reais:
  reinstalar com `install -r` após checar que o banco do aparelho está na versão 3, nunca
  apagar dados. Novo pedido: Confirmar sem nome ou sem item não abre o resumo; marca em
  vermelho (`Cores.Erro`) o campo do nome e/ou o botão "Adicionar ao pedido" e leva até o
  primeiro que falta (nome com foco e teclado). Detalhe: linhas de sinal e caução seguem o
  menu Exibir (aparecem mesmo desligadas se o pedido tiver valor gravado); "Forma de
  pagamento" em linha própria, sempre visível. Ainda sem commit.
