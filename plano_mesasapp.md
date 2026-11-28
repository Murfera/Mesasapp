# Plano do Mesasapp

Atualizado em 2026-11-28. Este arquivo é o estado do projeto: o que foi decidido,
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
  (**Material**: Todos/Plástico/Metal, **Preços**, **Config. Pix**, **Exibir**, **Backup**,
  **Privacidade**, **Tema**).
- **Backup e Privacidade são cópias exatas do Finapp** (pedido do usuário em 2026-11-24:
  "idêntico"): mesmos textos, posições, cores do sistema (dinâmicas, do papel de parede),
  fonte padrão do Material e funcionamento. Claro/escuro nessas telas **segue o celular**,
  como no Finapp, e não o Tema do menu (`ui/comum/TemaDoFinapp.kt`). Referência:
  `Finapp/.../ui/settings/SettingsScreen.kt` (Backup), `ui/home/HomeScreen.kt` (janelinha
  Privacidade), `MainActivity.kt` (tela Bloqueado) e `ui/lock/AppLock.kt` (trava).
  - **Backup** ocupa a tela inteira (sem barra de cima nem abas). Manual: JSON pelo seletor
    do sistema. Automático: escolhe a pasta uma vez; grava 1× por dia com o celular
    carregando (WorkManager) na **subpasta "Backup Mesasapp"** criada dentro da pasta
    escolhida; mantém os **4** mais recentes (decisão dele; o Finapp mantém 7 e não tem
    subpasta). Rodízio por data de modificação, só dentro da subpasta.
  - **Privacidade** é uma janelinha por cima da tela atual (Sem senha / Senha do celular /
    Digital). Trocar pede a proteção atual e depois a nova. Ao abrir o app: tela
    "Mesasapp / Bloqueado / Desbloquear"; trava de novo só depois de 30 s fora (escolher a
    pasta do backup sai do app). Se o jeito escolhido sumir do aparelho, desliga sozinho.
- **Ícone**: arte do usuário (`app/src/main/ic_launcher.svg`; 512 px para a Play Store em
  `ic_launcher-playstore.png`). Ícone adaptativo gerado em `res/mipmap-*` (arte em 66 dp
  dentro de 108 dp, fundo #0F6E56, versão monocromática). A pasta `icone/` na raiz é a de
  trabalho dele (fora do Git).
- **Exibir** (menu ⋮): chaves "Sinal pago" e "Caução", **desligadas por padrão**; só
  com elas ligadas os campos aparecem no Novo pedido. Desligar zera o valor digitado.
  Cartão "Tela de Pedidos" (2026-11-27): chave **"Filtro Semana | Mês"** (ligada por padrão) e
  **"Abrir mostrando" Semana | Mês** (padrão Mês). Com o filtro escondido vale sempre o padrão
  (em Semana continuam as setas da semana). Trocar na tela de Pedidos vale até fechar o app.
  Cartão "Cobrar com Pix": chave **"QR Code"** (**desligada por padrão**, pedido dele); desligada, o Pix do Detalhe mostra
  só o valor, o copia e cola e o botão Copiar.
  Chaves novas no DataStore e no backup (`mostrarPeriodoPedidos`, `pedidosPorSemana`, `mostrarQrCode`).
- Abas: **Início, Pedidos, Novo, Agenda, Estoque**. Detalhe do pedido, Preços e
  Config. Pix abrem por cima (o voltar do Android volta).
- **Início** (2026-11-24): abaixo da saudação, quadro "Jogos disponíveis hoje" na
  largura toda, com uma linha por tipo de cadeira (Plástico sem braço, Plástico com braço,
  Metal; só "Sem braço"/"Com braço" com o filtro em Plástico), cada uma "livres de total".
  As linhas **não se somam** (as mesas são as mesmas) — frase avisando no quadro. Jogo =
  1 mesa + N cadeiras do mesmo material e cor, N de "Cadeiras por mesa" do Estoque. A linha
  "Plástico sem braço" bate com os jogos completos do Estoque. Quadros "Mesas livres" e
  "Cadeiras livres" com a divisão embaixo (mesas por material; cadeiras por tipo), de mesma
  altura. **Só aparece o material/tipo que existe no estoque** (qualquer cor); jogo exige
  mesa e cadeira do mesmo material. Divisão só quando há mais de um tipo.
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
- **Pedidos** (2026-11-26): linha 1 = título + ‹ mês › (tocar no mês volta para hoje); linha 2 =
  chave **Semana | Mês** (padrão **Mês**, no mês de hoje; em Semana aparecem setas ‹ 23/11 a 29/11 ›,
  segunda a domingo); linha 3 = Orçamento, Confirmado, Entregue, Devolvido **marcáveis juntos**
  (aparece exatamente o que está marcado; abre sem o Devolvido; não existe mais "Todos"). Entra todo
  pedido cujo período toca a semana/o mês; ordem do começo para o fim. Cartão inteiro na cor da
  situação: **orçamento amarelo, confirmado azul, entregue verde, devolvido cinza** (`corStatus`,
  `fundoCartaoStatus`), filtros na mesma cor. Testado "tudo numa linha só": ele não gostou.
- **Detalhe** (arranjo do usuário em 2026-11-27/28, compacto: cartões a 6 dp um do outro, margem interna 14/12 dp):
  - Linha 1: "‹ Pedidos" + "#N · Emissão dd/MM/aaaa  HH:mm" (12 sp, à esquerda) + selo da situação à direita.
  - Nome do cliente numa linha (corta com "...") com **ícones** lápis (editar) e lixeira (excluir) à direita,
    14 dp de vão. Editar só Orçamento/Confirmado (abre o Novo como "Editando pedido #N", mesmo número, itens
    antigos com preço congelado; editar grava a hora nova); excluir qualquer um, com confirmação.
  - Avisos de sucesso (alterado, confirmado, entregue, devolvido) são **pop-up (Toast)**; erro fica fixo.
  - Quadro de contato: "Entrega dd/MM/aaaa · Retirada dd/MM/aaaa" (11 sp) + botão **📞 WhatsApp** compacto
    (abre a conversa; não existe link que inicie a chamada); embaixo o endereço; na linha de baixo a
    referência e, à direita, o telefone (abre o discador).
  - **Itens e valores**: título com "Jogo: 1 mesa × N cadeiras" à direita (N congelado no pedido); cada item numa
    linha ("1 jogo · Plástico Preta", "2 mesas avulsas · Plástico Preta", "+ com braço") com valor/dia à direita;
    aluguel, frete, desconto; **"Total - Pix ... R$ X"** em 28 sp (fonte dos números), forma de pagamento no meio.
    Sem "Sai do estoque" e **sem Google Agenda**.
  - **Cobrar com Pix** (cartão próprio, logo abaixo): título + botão **"Enviar itens"** (print do cartão Itens e
    valores direto para o WhatsApp, só a imagem); linha "Chave: ..."; botões Copiar código / Enviar no WhatsApp;
    QR só se ligado em Exibir. Um valor só ("Valor do pedido" = total − sinal; depois de entregue = com
    quebras/faltas/atraso, menos sinal e caução). Mensagem do WhatsApp: título, Emissão, Entrega/Retirada com ano,
    Recebedor, Valor, copia e cola (sem número do pedido; txid `PED`+ddMMaaHHmm). Uma mensagem só.
  - Vistoria com total refeito (depois de entregue). Por último, o botão da próxima situação.
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
5. **Nenhuma permissão declarada pelo app.** Ligar abre o discador; WhatsApp e Google
   Agenda são links. As bibliotecas somam só permissões normais (WorkManager e biometria);
   `FOREGROUND_SERVICE` é removida no manifesto. Lista explicada no `AndroidManifest.xml`.
6. Todas as telas leem a lista de pedidos completa (3 consultas no total, com
   `@Relation`) e somam em memória, como o protótipo. Regras puras em `domain/Regras.kt`.
7. Um `MesasViewModel` só, para o app todo (o formulário do Novo sobrevive à troca de aba).
8. Kotlin + Compose + Material 3, mesma stack do Finapp; injeção manual.
9. **`androidx.fragment` declarado explicitamente (1.8.9).** O biometric puxa o 1.2.5, cujo
   `FragmentActivity` derruba o app ao abrir qualquer seletor de arquivo ("Can only use
   lower 16 bits for requestCode"). O Finapp tem a mesma dependência antiga — o Backup dele
   funciona no aparelho do usuário, mas vale conferir se mexer lá.

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
`RegrasTest`). QR com ZXing, nível M. Txid `PED` + ddMMaaHHmm da emissão (`idPix` no Detalhe, desde 2026-11-28; pedido sem hora gravada fica `PED<id>`). QR estático **não avisa
quando o cliente paga** — baixa manual.

---

## 6. Feito

- Interface inteira do protótipo (2026-11-18), com as regras de `core.mjs` portadas.
- **26 testes passando** (inclui o modo escuro abrindo todas as abas): 8 de regra (paridade com os números do protótipo, Pix e
  link do Google Agenda idênticos), 4 que clicam pelas telas (estoque, novo pedido,
  pedido confirmado → entregue → devolvido com quebra e Pix, filtro de material),
  6 de dinheiro, 7 do Pix.

## 7. Falta / decisões em aberto

0. **Os testes de interface estão desatualizados** (também pelo Detalhe de 2026-11-27: Pix sem os botões
   `pix_sinal`/`pix_saldo`/`pix_outro`, QR desligado por padrão, Google Agenda removida) desde o refinamento do Novo pedido
   (campos movidos para a janela de contato, botões no painel de resumo, cartões por cor) e, desde
   2026-11-26, também pela máscara de dinheiro (digitar "100" agora vale R$ 1,00), filtros de Pedidos
   e o "Todos" que saiu.
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
7. ~~**Cobrar com Pix com o sinal desligado**~~ resolvido em 2026-11-27 (um valor só, sem botões): os botões "Sinal (50%)" e "Saldo (total −
   sinal)" ainda aparecem. Perguntado em 2026-11-24 se esconde o de sinal e troca o outro
   por "Total"; sem resposta ainda.
3. Bloquear jogo com mesa de um material e cadeira de outro (hoje vale a mesa).
4. Cadastrar o estoque real e conferir o cálculo no celular (com o usuário).
5. Migração de verdade do banco antes de publicar (o backup em JSON já existe).
8. **Restaurar backup** ainda não foi testado no celular (só gravar). Testar com cuidado:
   restaurar **substitui** os pedidos reais do aparelho.
6. Conferir as telas em paisagem.

9. Etiquetas "Entrega"/"Retirada" da Agenda e do Início seguem azul/laranja; perguntado se
   acompanham as cores novas das situações — sem resposta.
10. Ideias do site Simples Agenda que ele **descartou** (2026-11-26: "se não dá para usar, ignora").

11. **Recibo em PDF depois do pagamento** (pedido do usuário em 2026-11-28, "depois eu vou pensar melhor"):
    botão de emissão de recibo, para usar **depois que o cliente pagar**, que gera um PDF para mandar a ele.
    Não implementar antes de ele detalhar. Diferente da cobrança: a imagem "Pedido"/"Resumo do pedido" desenhada
    foi rejeitada em 2026-11-28; o que existe hoje é o print dos Itens e valores ("Enviar itens").
12. Ideias levantadas em 2026-11-28 e **deixadas de lado por ele** ("não faça nada"): agrupar pedidos por
    telefone (sugerido: lupa de busca por nome/telefone em todos os meses, ou "outros pedidos deste cliente");
    quadrinho de simulação do total no Novo pedido; textos "(opcional)" no Config. Pix; botão vermelho também
    quando a quantidade passa do livre.

Ideias para depois: contrato em PDF, fotos da entrega, relatório de
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

Em 2026-11-24 o IP era 192.168.15.6.

**Não mexer no aparelho**: nada de girar a tela, mudar configuração do sistema ou
navegar por toques via adb. Instalar e, se precisar, tirar print. O celular está na
mão do usuário e os toques caem no que ele estiver usando. **Exceção:** quando ele
pede para comparar telas ("abre no celular"), navegar até a tela (`uiautomator dump` +
`input tap`) e tirar print está liberado. Não tocar em opções que mudem dados ou o
bloqueio. Comparar **no celular**, não só pelo código: foi assim que apareceu o texto
escuro sobre fundo escuro na tela de Backup.

## 10. Combinados de trabalho

- **Git e GitHub só quando o usuário pedir** (desde 2026-11-18). Testes também só quando ele pedir.
- **Desde 2026-11-24 ele confere direto no Moto G73, que tem dados reais.** A cada
  alteração: compilar, ler a versão do banco no aparelho (`run-as br.com.mesasapp.debug od
  -A n -t u1 -j 60 -N 4 databases/mesasapp.db` → tem que dar 3) e só então `install -r`.
  Nunca desinstalar, limpar dados nem mudar schema/versão do banco sem explicar antes.
  Pacote do debug: `br.com.mesasapp.debug`.
- **Nada de emulador no Mesasapp** (desde 2026-11-24, pedido dele: "é para vc instalar
  no celular"; antes era liberado). Script de toques/fotos usado nas sessões:
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
  pagamento" em linha própria, sempre visível. Commit `028238b`, marcado
  `antes-inicio-jogos` (ponto de volta da tela Início, a pedido do usuário).
- **2026-11-24** — Início: quadro de jogos disponíveis hoje por tipo de cadeira e divisão
  por material/tipo nos quadros de mesas e cadeiras. Se o usuário não gostar, voltar
  `InicioTela.kt` para a marca `antes-inicio-jogos`.
- **2026-11-24** — Backup (`22ed419`, `c4f3f84`, feitos pelo usuário em outra máquina) não
  funcionava: o app fechava ao tocar nos botões (fragment antigo, ver decisão 9). Telas de
  Backup e Privacidade refeitas **idênticas às do Finapp** (comparadas por print no celular),
  com a trava do Finapp. Backup automático: 4 arquivos, subpasta "Backup Mesasapp". Ícone
  novo do usuário. Cópia da pasta antes de puxar do GitHub:
  `C:\Projetos\Mesasapp_backup_2026-11-24.zip`; ícone original em
  `C:\Projetos\Mesasapp_icone_original.zip`.
- **2026-11-26** — instalado no G73 (`install -r`, banco v3 preservado); ainda não conferido na tela pelo usuário
  - Campos de dinheiro no formato brasileiro com máscara de banco (`mascaraDinheiro` em
    `util/Money.kt`): só dígitos, enchem da direita, sempre `1.500,00`, cursor no fim. Vale para
    Frete, Desconto, Sinal, Caução, Pix "Outro valor" e todos os de Preços. Quantidades ficaram iguais.
  - **Editar e excluir pedido** no fim do Detalhe. Editar só Orçamento/Confirmado (abre o Novo com
    "Editando pedido #N" e Cancelar; mantém o número; itens que já estavam mantêm o preço congelado;
    a disponibilidade não conta o próprio pedido). Excluir em qualquer situação, com confirmação.
    Sem mudança de schema (só consultas novas no DAO).
  - **Data e hora de emissão** (`criadoEmEpochSeg`, que já existia no banco): aparece no Detalhe e na
    mensagem do Pix no WhatsApp ("Pedido emitido em 26/11/2026 às 14:35"). Editar grava a hora nova.
  - Lista de Pedidos: cartão inteiro na cor clara da situação (orçamento roxo, confirmado azul,
    entregue laranja, devolvido cinza), etiqueta em fundo branco. **Trocadas a pedido dele:** orçamento
    amarelo, confirmado azul, entregue verde, devolvido cinza (`corStatus`); cartão e filtros de cima
    na mesma cor, mais forte que a etiqueta (`fundoCartaoStatus`).

Nota de conexão (2026-11-26): depois do `adb pair`, o `mdns services` **não** mostrou a porta de
conexão. Achei varrendo as portas TCP 30000–50000 do IP do celular (script Python com socket,
~15 s) e deu uma só aberta (36583); `adb connect` nela funcionou. IP do celular: 192.168.15.2.
- **2026-11-26** — Pedidos: "Todos" na linha do título, as 4 situações numa linha só, cartões mais
  juntos. **Filtro por mês** (setas ‹ › + nome do mês; tocar no nome volta ao mês atual): entra todo
  pedido cujo período toca o mês (24/12→01/01 aparece em dezembro e janeiro). Ordem agora do começo
  para o fim do mês (antes: entrega mais distante primeiro).
- **2026-11-26** — Filtro de situação virou **marcação múltipla** e o "Todos" saiu: nenhuma marcada
  aparece exatamente o que está marcado; abre com as quatro marcadas; nenhuma = lista vazia com aviso
  (`alternarStatus`). A 1ª versão ("nenhuma = todas") confundiu o usuário: desmarcar mostrava tudo.
- **2026-11-26** — Pedidos: mês fixo na linha do título (‹ Novembro 2026 ›; tocar volta para hoje);
  embaixo chave **Semana | Mês** (abre em Mês, ver abaixo) com as setas da semana (segunda a domingo, para o fim
  de semana ficar junto). Semana do mês = regra ISO (a da quinta-feira); trocar o mês leva à 1ª semana
  dele. Abre **sem o Devolvido marcado** (só o que ainda dá trabalho). Detalhe: Editar/Excluir no topo.
- **2026-11-26** — Testado tudo numa linha só (título + Semana | Mês + setas): o usuário **não gostou**,
  voltou para as duas linhas. **Padrão agora é Mês** (não Semana), abrindo no mês de hoje.
- **2026-11-27** — Exibir ganhou o cartão "Tela de Pedidos": mostrar/esconder o filtro Semana | Mês e
  escolher com qual período a lista abre (padrão Mês). Instalado no G73 (banco v3 preservado).
- **2026-11-27** — Pedidos: mês e título em 18 sp, mais espaço abaixo do título. Detalhe: Pix sem os botões
  Sinal/Saldo/Outro valor — um valor só, calculado do pedido ("o cliente paga na retirada").
- **2026-11-27** — Exibir: chave "QR Code" para mostrar ou esconder a imagem do QR no Pix do pedido.
- **2026-11-27** — Mensagem do Pix no WhatsApp enxuta: título "Aluguel de mesas e cadeiras", pedido #N com emissão, entrega/retirada, valor, recebedor (nome do Config. Pix) e o copia e cola no fim.
  Mantida em uma mensagem só: o WhatsApp já mostra para o cliente o botão "Copiar Pix" com só o código (conferido pelo usuário). Não fazer duas mensagens.
- **2026-11-27** — Detalhe: tirada a linha "Sai do estoque: N mesas e N cadeiras" e o cartão Google Agenda inteiro (pedido dele). `Links.googleAgenda` ficou no código, sem uso na tela.
- **2026-11-27** — Detalhe: botão "WhatsApp" ao lado do telefone (abre a conversa; tocar no número continua abrindo o discador). Não existe link que já inicie a chamada no WhatsApp.
- **2026-11-27** — Detalhe: endereço/referência só quando preenchidos (sem os dois, a linha some e as datas sobem); botão WhatsApp tirado e logo depois **devolvido** (ele quer os dois: número = discador, botão = conversa no WhatsApp).
- **2026-11-27** — Detalhe: avisos de sucesso ("Pedido #N alterado...", confirmado, entregue, devolvido) viram pop-up (Toast); erro continua fixo. Entrega/Retirada em 12 sp à direita do endereço (sem endereço, numa linha só).
- **2026-11-27** — Detalhe: Entrega/Retirada (dd/MM/aaaa, 12 sp) à direita do nome do cliente; selo da situação subiu para a linha "Pedido #N · emitido em...". Quadro de baixo ficou só endereço/referência (se houver) e telefone + WhatsApp.
- **2026-11-27** — Detalhe: "Pedido #N · emitido em..." (12 sp, à direita) e o selo da situação na mesma linha do "‹ Pedidos"; Entrega/Retirada numa linha só, à direita do nome do cliente.
- **2026-11-27** — Topo do Detalhe: "#N · emissão dd/MM/aaaa  HH:mm" em 11 sp, para caber numa linha ao lado do "‹ Pedidos" e do selo (medido com a fonte Inter: em 12 sp ficava no limite e quebrava).
- **2026-11-27** — Detalhe: topo "#N · emissão..." de volta a 12 sp, alinhado à esquerda (logo após "‹ Pedidos"), selo à direita. Cartões mais juntos (16→6 dp entre eles) e mais baixos (margem interna 14/12 dp, `PAD_CARTAO`).
- **2026-11-27** — Detalhe, arranjo do usuário: nome do cliente sozinho; quadro de contato com Entrega/Retirada (dd/MM/aaaa, uma embaixo da outra) + botão WhatsApp na mesma linha; embaixo endereço/referência e, à direita, o telefone (abre o discador).
  Ajuste: Entrega/Retirada numa linha só (11 sp) e botão WhatsApp compacto (30 dp de altura, só texto) na mesma linha.
- **2026-11-28** — Detalhe, Itens e valores: a forma de pagamento (Pix, Dinheiro...) saiu da linha própria e foi para a linha do "Total do aluguel", sem rótulo, em cinza ao lado do título separada por hífen ("Total do aluguel - Pix"); valor continua à direita.
- **2026-11-28** — Detalhe: botões "Editar pedido"/"Excluir pedido" viraram dois ícones (lápis verde, lixeira vermelha; 22 dp em área de toque de 30 dp) à direita, na linha do nome do cliente. Nome numa linha só, cortado com "...". Saiu a frase "Pedido já entregue não pode ser editado, só excluído." (entregue/devolvido mostra só a lixeira).
- **2026-11-28** — Detalhe, quadro de contato: Entrega/Retirada + WhatsApp em cima; endereço sozinho na linha dele; telefone desceu para a linha da referência, à direita (sem referência, fica sozinho à direita nessa linha).
- **2026-11-28** — Ícones editar/excluir com 14 dp de vão entre eles.
- **2026-11-28** — Detalhe: na linha do título "Itens e valores", à direita, "Jogo: 1 mesa × N cadeiras" (12 sp, cinza), com o N congelado nas linhas de jogo do pedido (veio do Estoque ao salvar). Só aparece se o pedido tem jogo; Ns diferentes viram "4 ou 6".
- **2026-11-28** — Detalhe, linhas de jogo numa linha só: "1 jogo · Plástico Preta" (+ " com braço" se a cadeira tiver braço) e o valor/dia à direita. Avulsos continuam com título + descrição (ele mexe depois). `LinhaPedido.descricao` não mudou (usada em outros lugares).
- **2026-11-28** — Detalhe: avulsos também numa linha só ("2 mesas avulsas · Plástico Preta", cadeira com braço ganha " com braço").
- **2026-11-28** — Mensagem do Pix: "emitido em" virou "emissão" e Entrega/Retirada com o ano (dd/MM/aaaa). O resto igual.
- **2026-11-28** — Mensagem do Pix sem o número do pedido nem a palavra "Pedido" (número sequencial contaria ao cliente quantos pedidos há): título, "Emissão dd/MM/aaaa às HH:mm", Entrega/Retirada com ano, Recebedor, Valor, copia e cola. 
- **2026-11-28** — Txid do Pix trocado de `PED<id>` para `PED` + data e hora da emissão (ex.: PED2811261006), para não expor a contagem de pedidos no extrato.
- **2026-11-28** — Detalhe: topo "#N · Emissão ..." com E maiúsculo; cartão Pix só com o título "Cobrar com Pix" (saiu a linha "Copia e cola para enviar ao cliente").
- **2026-11-28** — Detalhe, Itens e valores: "Total do aluguel" virou "Total"; "Total" e o valor com a mesma fonte e tamanho do valor do Pix (`Numero`, 28 sp); forma de pagamento continua entre eles. Ele vai remover a parte de baixo depois (ainda não).
- **2026-11-28** — Detalhe: botões "Copiar código" e "Enviar no WhatsApp" também logo abaixo da linha Total, no cartão Itens e valores (só os botões). O cartão "Cobrar com Pix" de baixo continua (ele vai decidir se remove). Pix calculado uma vez no topo (`codigoPix`, `textoPix`, `BotoesPix`).
- **2026-11-28** — Detalhe: cartão "Cobrar com Pix" separado **removido**. Dentro de Itens e valores, abaixo do Total (e do sinal/caução, se aparecerem): ícone QR + "Cobrar com Pix", os botões Copiar código / Enviar no WhatsApp, e a imagem do QR só com Exibir → QR Code ligado. Saíram da tela: o quadro com o valor do Pix, nome/cidade/chave e o texto do copia e cola (o valor do Pix segue na mensagem). Sem chave: aviso; nada a cobrar: frase.
- **2026-11-28** — Detalhe: "Cobrar com Pix" voltou a ser cartão próprio, logo abaixo de Itens e valores (antes da vistoria), compacto: ícone + título + a chave Pix cadastrada à direita (12 sp, cinza), botões Copiar código / Enviar no WhatsApp, QR só com Exibir ligado.
- **2026-11-28** — Cobrar com Pix: a chave saiu da linha do título e ganhou linha própria embaixo ("Chave: ...", 13 sp, largura toda).
- **2026-11-28** — **Enviar no WhatsApp agora manda imagem "Resumo do pedido"** (nome escolhido: não é recibo, vai antes do pagamento). PNG 1080 px desenhado com Canvas no celular (`util/ResumoImagem.kt`): faixa verde com título, emissão, cliente, entrega/retirada, itens numa linha ("1 jogo · Plástico Preta", formação do jogo à direita), aluguel, frete/desconto só se > 0, Total grande com forma de pagamento, sinal/caução se aparecem, avarias/atraso depois da entrega, quadro do Pix (valor a pagar, recebedor, chave, QR só se Exibir → QR ligado). Legenda = **só o código Pix** (copiar a legenda inteira cola direto no banco). Envio por `ACTION_SEND` com FileProvider (`${applicationId}.arquivos`, `res/xml/arquivos.xml`, cache/compartilhar) — sem permissão nova; tenta abrir direto a conversa com o extra não oficial `jid`, senão o WhatsApp pede o contato. `textoPix` (mensagem só de texto) ficou no código de reserva. **A conferir no celular:** se o "Copiar Pix" do WhatsApp aparece na legenda e se o `jid` abre a conversa certa.
- **2026-11-28** — Título da imagem trocado para **"Pedido"** (escolha dele, no lugar de "Resumo do pedido"); arquivo `pedido.png`.
- **2026-11-28** — Imagem **desfeita**: ele não gostou ("vou pensar em outra maneira, talvez outro botão"). Enviar no WhatsApp voltou à mensagem de texto (título, Emissão, Entrega/Retirada, Recebedor, Valor, copia e cola). `util/ResumoImagem.kt` e o FileProvider ficaram no código **sem uso**, para a ideia nova dele; apagar se ele desistir.
- **2026-11-28** — Botão WhatsApp do quadro de contato ganhou o ícone de telefone (o mesmo do número) antes do texto. Continua abrindo a conversa, não a chamada.
- **2026-11-28** — Novo pedido, lista de itens adicionados: mesmo padrão do Detalhe, "1 jogo · Plástico Preta" numa linha, valor/dia embaixo. A função `detalheLinha` (usada no Detalhe e no Novo) está em `util/ResumoImagem.kt`: se apagar a imagem, **mover essa função** antes.
- **2026-11-28** — Novo pedido: cartão de cor com 0 livres fica inteiro vermelho (fundo vermelho claro, borda e número vermelhos); com a cor sem estoque marcada, o botão "Adicionar ao pedido" fica vermelho (`EstiloBotao.ALERTA`). **Só aviso, não bloqueia** (pedido dele).
- **2026-11-28** — Lista de Pedidos: cartão mostra "#N · dd/MM/aaaa a dd/MM/aaaa" (com o ano). O Início continua sem ano (`periodo()`).
- **2026-11-28** — Cobrar com Pix: botão compacto **"Enviar itens"** na linha do título. Tira print só do cartão Itens e valores (`rememberGraphicsLayer` + `drawWithContent`), põe sobre o fundo da tela com margem e abre o WhatsApp com a imagem, sem texto (`util/Compartilhar.kt`: `imagemNoWhatsApp`, `comMargem`, e `detalheLinha` mudou para cá). `ResumoImagem.kt` (a imagem desenhada que ele rejeitou) foi **apagado**. Testado pelo usuário no celular em 2026-11-28: "ficou bom".
