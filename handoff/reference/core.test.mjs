// Testes de paridade: rode com `node reference/core.test.mjs`. Valida a lógica contra os dados de exemplo.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as core from './core.mjs';

const data = JSON.parse(readFileSync(new URL('./sample-data.json', import.meta.url), 'utf8'));
const ctx = { modelos: data.modelos, tabela: data.tabela, precos: data.precos, pedidos: data.pedidos, pix: data.pix };

// CRC16 do BR Code (valor de referência conhecido do algoritmo CCITT-FALSE para "123456789")
assert.equal(core.crc16('123456789'), '29B1');

// Pedido de exemplo #1043: 8 jogos (mesa plástica branca + cadeira com braço branca) + cadeiras avulsas
const p1043 = ctx.pedidos.find((o) => o.id === 1043);
const c = core.calcPedido(ctx, p1043);
assert.ok(c.total > 0 && c.d === 2);

// Reserva: Orçamento e Devolvido não reservam; Confirmado e Entregue reservam no período
const res = core.reservado(ctx, '2026-11-17', '2026-11-18');
assert.ok(Object.keys(res).length > 0);

// Faltando: pedido que usa mais mesas do que o estoque
const grande = { ...p1043, id: 9999, entrega: '2026-11-21', retirada: '2026-11-22', itens: [{ tipo: 'mesa', mesa: 'p-mesa-pr', qtd: 999 }] };
assert.deepEqual(core.falta(ctx, grande, 9999), ['p-mesa-pr']);

// Vistoria: 1 quebrada + 2 faltando + 1 dia de atraso
const dev = { q: { 'p-mesa-br': 1 }, f: { 'p-cadeira-br': 2 }, atraso: 1 };
const o1037 = ctx.pedidos.find((o) => o.id === 1037);
const f = core.fechamento(ctx, o1037, dev);
assert.equal(f.avarias, 1 * 120 + 2 * 45);
assert.equal(f.final, f.total + f.avarias + f.atraso);

// Pix: código com CRC válido (últimos 4 caracteres = CRC do restante)
const pay = core.pixPayload(ctx.pix, 120.5, 'PED1044');
assert.equal(pay.code.slice(-4), core.crc16(pay.code.slice(0, -4)));
assert.ok(pay.code.includes('br.gov.bcb.pix') && pay.code.includes('120.50'));

// Filtro por material
assert.ok(core.pedidoTemMaterial(ctx, p1043, 'Plástico'));
// Paridade exata com o protótipo (valores gerados pelo código do protótipo)
const exp = JSON.parse(readFileSync(new URL('./expected-from-prototype.json', import.meta.url), 'utf8'));
ctx.pedidos.forEach((o) => {
  const k = core.calcPedido(ctx, o), e = exp[o.id];
  assert.equal(k.total, e.total); assert.equal(k.saldo, e.saldo); assert.equal(k.d, e.d);
  assert.equal(k.mesasTot, e.mesasTot); assert.equal(k.cadTot, e.cadTot); assert.deepEqual(k.uso, e.uso);
});
assert.deepEqual(core.reservado(ctx, '2026-11-17', '2026-11-18'), exp.reservado);
assert.equal(core.pixPayload(ctx.pix, 120.5, 'PED1044').code, exp.pix);
assert.equal(core.linkGoogleAgenda(ctx, ctx.pedidos[0], 'entrega'), exp.gcal);
console.log('core.test.mjs: ok');
