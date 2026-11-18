# Pacote de handoff — App de locação de mesas e cadeiras

| Pasta/arquivo | O que é |
|---|---|
| `SPEC.md` | Especificação completa: regras de negócio, telas, decisões em aberto. Comece por aqui. |
| `reference/core.mjs` | Lógica de negócio pura e testada (preço, totais, disponibilidade por data, vistoria, Pix BR Code, links do Google Agenda e WhatsApp, filtro por material). Sem dependência de framework. |
| `reference/core.test.mjs` | Testes (`node reference/core.test.mjs`) com paridade exata com o protótipo. |
| `reference/sample-data.json` | Dados de exemplo (estoque, preços, 5 pedidos, config Pix). |
| `reference/expected-from-prototype.json` | Resultados gerados pelo código do protótipo, usados nos testes. |
| `reference/qrcode-generator-1.4.4.MIT.js` | Biblioteca de QR Code (MIT) usada pelo protótipo. |
| `prototype/Main.dc.html` | Protótipo navegável (referência visual: cores, espaçamentos, textos, fluxo). Formato do canvas do Claude, não roda fora dele. `canvas.json` é o índice. |

Protótipo online: https://claude.ai/artifact/AkVpcgzciGLAZHREAgQESn

## Como usar no Claude Code
1. Descompacte numa pasta nova e abra o Claude Code nela.
2. Escolha o stack. Sugestão: PWA em React + TypeScript (funciona no celular sem loja), dados no aparelho no início (IndexedDB/localStorage) e depois um backend.
3. Prompt sugerido:

> Leia `SPEC.md` e `reference/core.mjs`. Crie o app descrito no stack <escolhido>. Reaproveite `core.mjs` como camada de domínio (converta para TypeScript mantendo os testes de `core.test.mjs` passando). Use `prototype/Main.dc.html` só como referência visual das telas. Comece por: estoque, preços, novo pedido com disponibilidade por data, detalhe do pedido com Pix e vistoria; depois agenda e início. Faça o filtro Material do menu ⋮ valer para o app todo. Campos numéricos devem poder ser apagados no celular.

4. Rode `node reference/core.test.mjs` para conferir que a lógica de referência está íntegra antes de portar.

## Limitações conhecidas
- O protótipo foi testado por script (lógica), sem teste em celular real nem em navegador renderizado.
- Dados só em memória; sem login, sem API do Google Agenda (só links), sem horários.
