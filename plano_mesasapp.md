# Plano do Mesasapp

Atualizado em 2026-11-17. Este arquivo é o estado do projeto: o que foi decidido,
o que está feito, o que falta e o que ficou pendente de decisão.
**Ler antes de retomar o trabalho.**

App Android para gerenciar **aluguel de mesas e cadeiras para festas**. Vai para a
Play Store. Tudo local (SQLite/Room): nenhum dado sai do aparelho, nenhum servidor,
nenhum custo de operação.

- Repositório (privado): https://github.com/Murfera/Mesasapp
- `applicationId = br.com.mesasapp` — **não muda depois da primeira publicação**.

---

## 1. Estrutura das telas

O usuário comparou com um app parecido (zaraeventos.web.app, feito por um amigo) e
pediu a **mesma estrutura**: menu inferior com cinco abas, painel de abertura e o
pedido dividido em blocos com o resumo do dinheiro fixo no rodapé.
O que ficou **diferente de propósito**: o estoque é mais simples e as cores são só
**Branca, Preta, Vermelha e "Outra"** — ele achou o do amigo cheio de opção demais.

| Aba | O que faz |
|---|---|
| **Início** | Painel: livres agora, entregas e retiradas de hoje, a receber dos pedidos em aberto, próximos eventos. Engrenagem abre Configurações |
| **Pedidos** | Lista com busca (cliente ou número) e filtro por situação; muda o status no cartão; toque abre o detalhe |
| **Novo** | Datas → Itens → Cliente → Local → Valores, com o resumo do dinheiro fixo no rodapé |
| **Agenda** | Calendário do mês com ponto de entrega e de retirada, o que acontece no dia e quanto está na rua |
| **Estoque** | Modelos com livres/alugadas/quebradas, total, diária e reposição |

**O princípio da aba Novo**: é uma calculadora, não um formulário. A pessoa abre o
app no meio de uma conversa de WhatsApp e precisa responder "10 jogos, 1 dia, quanto
fica?" em segundos. Por isso os itens vêm logo depois das datas, o total recalcula a
cada toque, e cliente/endereço ficam no fim — só fazem falta na hora de salvar.

**Detalhe do pedido** (abre pela lista, pela agenda ou pelo painel): valores, peças
reservadas, situação, **registrar devolução**, **cobrar por Pix** e compartilhar o
resumo por WhatsApp.

---

## 2. Modelo de dados (Room / SQLite, versão 2)

| Tabela | Para que serve |
|---|---|
| `item` | Catálogo e estoque: nome, categoria (MESA/CADEIRA), material, comBraco, cor, quantidadeTotal, precoDiariaCentavos, valorReposicaoCentavos, ativo |
| `combo` | O "jogo": nome, itemMesaId, itemCadeiraId, qtdCadeiras, precoDiariaCentavos |
| `cliente` | nome, telefone, documento (CPF/CNPJ), endereço, observações |
| `locacao` | O pedido: cliente (id e dados copiados), endereço, ponto de referência, início e fim em epoch de segundos, frete, montagem, desconto, sinal, caução, observações, status |
| `locacao_linha` | O que foi **vendido**: tipo (ITEM/COMBO), refId, descrição, quantidade e **preço congelado** |
| `locacao_peca` | O que foi **reservado** em peças físicas, já com os jogos desmontados: itemId, quantidade, quantidadeDevolvida, quantidadeQuebrada e **valor de reposição congelado** |

Duas tabelas para o pedido porque as perguntas são diferentes: `locacao_linha`
responde "quanto custa" e `locacao_peca` responde "quantas cadeiras estão ocupadas
no dia 18" e recebe a conferência da devolução.

**Status:** ORCAMENTO → CONFIRMADA → ENTREGUE → DEVOLVIDA (ou CANCELADA).
Só CONFIRMADA e ENTREGUE ocupam estoque — orçamento não reserva nada, senão um
orçamento esquecido bloquearia mesas livres.

Configuração fica em **DataStore**, não no banco: nome do negócio, chave Pix, nome
do recebedor, cidade, frete padrão, hora padrão de entrega e de retirada.

### Decisões técnicas que não devem ser revertidas por acidente

1. **Dinheiro é `Long` em centavos**, nunca `Double` — arredondamento binário
   corrompe somas. Conversão só por `util/Money.kt`, coberta por testes.
2. **Datas em epoch de segundos (`Long`)**, para a sobreposição de janelas ser uma
   comparação numérica no SQL.
3. **Preço e valor de reposição são congelados na linha do pedido.** Mexer no
   catálogo não pode mudar o valor de um pedido fechado.
4. **O jogo não tem estoque próprio** (ver seção 4).
5. **Nenhuma permissão declarada.** É o que permite dizer "nenhum dado coletado" no
   formulário de Segurança de Dados do Play.
6. **Modelo já usado em pedido não é excluído, é desativado** — o histórico não pode
   ficar sem descrição.
7. Uma consulta única (`observarResumos`) soma os totais no SQL; a lista, o painel e
   a agenda bebem dela, em vez de carregar as linhas pedido a pedido.
8. Injeção manual (`di/AppContainer`), sem Hilt. Kotlin + Compose + Material 3,
   mesma stack e versões do Finapp (AGP 9.4.1, Kotlin 2.3.21, Compose BOM 2026.09.00,
   Room 2.8.5, minSdk 26, targetSdk 36, compileSdk 37).

⚠️ **Pendência antes de publicar:** o banco ainda usa
`fallbackToDestructiveMigration` — mudança de schema **apaga os dados**. Isso é
aceitável agora, com o app só no celular de teste, mas tem que virar migração de
verdade antes da primeira versão na Play Store.

---

## 3. Cálculo do total (como está hoje)

```
aluguel   = Σ (quantidade × preço da diária do item ou do jogo)
total     = aluguel + frete + montagem − desconto + quebras
a receber = total − sinal
quebras   = Σ (quantidade quebrada × valor de reposição congelado)
```

**Os dias ainda não multiplicam o valor.** O usuário pediu para não mexer nisso por
enquanto ("de 100% a 50% não"), então o período serve só para a agenda e para o
estoque, e a tela mostra "Calculado: N diárias" como informação. Quando ele decidir,
entra como configuração (ver seção 7).

## 4. Estoque por data

Disponível de um item numa janela = `quantidadeTotal −` o que está reservado por
pedidos CONFIRMADA/ENTREGUE cuja janela se sobrepõe à pedida
(`inicioA < fimB && fimA > inicioB`, com `<` e `>` estritos).

**A hora conta**: retirada às 10h libera o item para uma entrega às 14h do mesmo
dia. Sem hora, o app recusaria um aluguel que na prática dá certo. A tela continua
simples — a pessoa escolhe as datas e o app usa as horas padrão; quem quiser ajusta,
pelos atalhos Manhã (08h) / Tarde (13h) / Noite (18h) ou pelo relógio.

**O jogo não tem estoque próprio**: 1 jogo = 1 mesa + N cadeiras e baixa do estoque
real. Dar estoque próprio ao jogo faria a mesma cadeira ser alugada duas vezes (o
erro clássico: 10 jogos e 40 cadeiras virarem 80 cadeiras).

## 5. Pix

**BR Code estático (padrão EMV do Banco Central)** montado dentro do app
(`util/Pix.kt`, com CRC-16/CCITT) e QR desenhado com ZXing. Offline, sem conta de
API, sem custo. Sai em "copia e cola" e em imagem, para mandar no WhatsApp junto com
o resumo (`ACTION_SEND`), já com o valor que falta receber.

**Limite a saber:** QR estático **não avisa quando o cliente paga** — a baixa é
manual. Confirmação automática exigiria conta PJ com API Pix de banco, fora do
escopo e do custo zero. A própria tela diz isso.

---

## 6. Feito

- **Esqueleto e banco**: projeto Gradle, tema claro/escuro com cor dinâmica, ícone
  próprio (vetor de mesa com duas cadeiras), Room com as seis tabelas.
- **Estoque**: cadastrar e editar modelos (categoria, material, braço, cor,
  quantidade, diária, reposição), jogos (mesa + N cadeiras + preço), com
  livres/alugadas/quebradas por modelo.
- **Novo pedido**: itens com quantidade livre no período, datas e horários,
  frete/desconto/sinal/caução, cliente e endereço, resumo fixo no rodapé e os dois
  caminhos — **Salvar orçamento** (não reserva) e **Confirmar pedido** (reserva).
- **Pedidos**: busca, filtro por situação e mudança de status no cartão.
- **Agenda**: calendário do mês, eventos do dia e quanto está na rua.
- **Início**: painel com livres agora, entregas/retiradas de hoje, a receber e
  próximos eventos.
- **Detalhe do pedido**: valores, peças, situação, compartilhar por WhatsApp.
- **Devolução**: quantas voltaram e quantas quebraram; o total é refeito com a
  quebra pelo valor de reposição congelado.
- **Pix**: copia e cola + QR, offline.
- **Configurações**: negócio, chave Pix (chave, recebedor, cidade), frete padrão e
  horas padrão.
- **18 testes passando**: 5 que clicam pelas telas, 6 de dinheiro, 7 do Pix.

## 7. Falta / próximo passo

1. **Cadastrar o estoque real no celular e conferir o cálculo** (com o usuário).
2. **Regra de cobrança para mais de um dia** — decisão dele, ainda em aberto.
3. **Backup em JSON**, no modelo do Finapp (manual + automático).
4. **Migração de verdade do banco** antes de publicar (hoje recria).
5. Conferir as telas em paisagem — nada trava orientação, mas nunca foi testado.

Ideias para depois, fora da v1:
- Calendário do Google (fora por decisão do usuário: "não é tão importante").
- Contrato/recibo em PDF; multa por atraso; fotos da entrega e da devolução;
  relatório de faturamento e de utilização do estoque; itens extras (toalha, lona).

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
- Procurar por marcador (`testTag`), não por texto: "Metal" também aparece nos
  cartões dos modelos atrás do diálogo.
- O dinheiro usa espaço não separável depois do "R$": comparar por "100,00".
- **Lista rolável dentro de `AlertDialog` entra em laço infinito de layout** e trava
  a tela. Diálogo com lista tem que ser `Dialog` em tela cheia.

**Compilar e o app abrir não é teste.** Foi assim que a tela de estoque foi entregue
impossível de preencher, e o usuário teve razão em reclamar.

## 9. Como instalar no celular

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb pair 192.168.15.9:<porta de pareamento> <codigo>   # janela de pareamento aberta
& $adb mdns services                                      # revela a porta de conexao
& $adb connect 192.168.15.9:<porta de conexao>
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```

A porta e o código de pareamento **só valem enquanto a janela está aberta** no
celular. Depois de parear, `adb mdns services` revela a porta de conexão sozinho —
não precisa pedir o segundo número.

**Não mexer no aparelho**: nada de girar a tela, mudar configuração do sistema ou
navegar por toques via adb. Instalar e, se precisar, tirar print. O celular está na
mão do usuário e os toques caem no que ele estiver usando.

## 10. Combinados de trabalho

- **Git a cada etapa salva**, para dar para voltar atrás e comparar.
- Testar no **Moto G73 por Wi-Fi**; não criar emulador nem abrir o Android Studio.
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
