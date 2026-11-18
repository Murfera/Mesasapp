// Lógica de negócio do app de locação de mesas e cadeiras, sem dependência de framework.
// Extraída e organizada a partir do protótipo (prototype/Main.dc.html). Valores em reais (number).
// Datas sempre no formato 'AAAA-MM-DD'. Funções puras: recebem o "ctx" e não alteram nada.
//
// ctx = {
//   modelos: [{ id, tipo:'mesa'|'cadeira', material:'Plástico'|'Metal', braco:boolean, cor, total, manut }],
//   tabela:  { 'p-mesa': { preco, rep }, ... }        // preço da diária e valor de reposição por material+item
//   precos:  { descJogo, frete, cadPorJogo: { 'Plástico|Preta': 4, ... } }
//   pedidos: [Pedido],
//   pix:     { chave, nome, cidade }
// }
// Pedido = { id, cliente, tel, doc, end, ref, entrega, retirada, status, itens:[Linha], frete, desconto, sinal, caucao, pagamento, dev?, final? }
// Linha  = { tipo:'jogo', mesa, cad, qtd } | { tipo:'mesa', mesa, qtd } | { tipo:'cadeira', cad, qtd }

export const MATERIAIS = ['Plástico', 'Metal'];
export const CORES = { Preta: '#1C1C1C', Branca: '#FFFFFF', Vermelha: '#B42318' };
export const COR_LIST = [{ k: 'pr', nome: 'Preta' }, { k: 'br', nome: 'Branca' }, { k: 'vm', nome: 'Vermelha' }];
export const ITENS = [
  { k: 'mesa', tipo: 'mesa', braco: false, label: 'Mesas', mats: ['Plástico', 'Metal'] },
  { k: 'cadeira', tipo: 'cadeira', braco: false, label: 'Cadeiras', mats: ['Plástico', 'Metal'] },
  { k: 'cadeiraBraco', tipo: 'cadeira', braco: true, label: 'Cadeiras com braço', mats: ['Plástico'] },
];
export const STATUS = ['Orçamento', 'Confirmado', 'Entregue', 'Devolvido'];
export const CADEIRAS_POR_MESA_PADRAO = 4;

// ---------- Catálogo / modelos ----------
// id do modelo = <p|m>-<mesa|cadeira|cadeiraBraco>-<pr|br|vm>. São 15 modelos fixos; só a quantidade (total) muda.
export function itemKey(m) {
  return (m.material === 'Plástico' ? 'p' : 'm') + '-' + (m.tipo === 'mesa' ? 'mesa' : m.braco ? 'cadeiraBraco' : 'cadeira');
}
export function catalogoVazio() {
  const out = [];
  MATERIAIS.forEach((mat) => ITENS.filter((it) => it.mats.includes(mat)).forEach((it) => COR_LIST.forEach((c) => {
    out.push({ id: (mat === 'Plástico' ? 'p' : 'm') + '-' + it.k + '-' + c.k, tipo: it.tipo, material: mat, braco: it.braco, cor: c.nome, total: 0, manut: 0 });
  })));
  return out;
}
export function mod(ctx, id) {
  const m = ctx.modelos.find((x) => x.id === id);
  if (!m) return { id, tipo: 'cadeira', material: '?', braco: false, cor: '?', total: 0, manut: 0, preco: 0, rep: 0 };
  const t = ctx.tabela[itemKey(m)] || { preco: 0, rep: 0 };
  return { ...m, preco: t.preco, rep: t.rep };
}
export function nomeModelo(m) {
  const cor = String(m.cor || '').toLowerCase();
  if (m.tipo === 'mesa') return 'Mesa de ' + m.material.toLowerCase() + ' ' + cor;
  return 'Cadeira de ' + m.material.toLowerCase() + (m.braco ? ' com braço ' : ' ') + cor;
}

// ---------- Preço e totais ----------
// Cadeiras por mesa: configurável por material + cor da MESA do jogo (padrão 4).
export const cpjKey = (mat, cor) => mat + '|' + cor;
export function cadPorMesa(ctx, mesaId) {
  const m = mod(ctx, mesaId);
  return ctx.precos.cadPorJogo[cpjKey(m.material, m.cor)] || CADEIRAS_POR_MESA_PADRAO;
}
// Diária de um jogo = diária da mesa + (cadeiras por mesa × diária da cadeira) − desconto por jogo.
export function precoJogo(ctx, mesaId, cadId) {
  return Math.max(0, mod(ctx, mesaId).preco + cadPorMesa(ctx, mesaId) * mod(ctx, cadId).preco - ctx.precos.descJogo);
}
export function linhaDiaria(ctx, l) {
  if (l.tipo === 'jogo') return l.qtd * precoJogo(ctx, l.mesa, l.cad);
  if (l.tipo === 'mesa') return l.qtd * mod(ctx, l.mesa).preco;
  return l.qtd * mod(ctx, l.cad).preco;
}
// Quantidade usada de cada modelo no pedido: { idModelo: n }. Jogo usa qtd mesas e qtd × cadeirasPorMesa cadeiras.
export function uso(ctx, o) {
  const u = {};
  const add = (id, n) => { u[id] = (u[id] || 0) + n; };
  (o.itens || []).forEach((l) => {
    if (l.tipo === 'jogo') { add(l.mesa, l.qtd); add(l.cad, l.qtd * cadPorMesa(ctx, l.mesa)); }
    else if (l.tipo === 'mesa') add(l.mesa, l.qtd);
    else add(l.cad, l.qtd);
  });
  return u;
}
// Diárias = dias entre entrega e retirada (mínimo 1). Retirada antes da entrega conta como 1.
export function dias(o) {
  const a = Date.parse(o.entrega + 'T12:00:00'), b = Date.parse(o.retirada + 'T12:00:00');
  if (!a || !b || b < a) return 1;
  return Math.max(1, Math.round((b - a) / 86400000));
}
export function calcPedido(ctx, o) {
  const d = dias(o);
  const diaria = (o.itens || []).reduce((s, l) => s + linhaDiaria(ctx, l), 0);
  const aluguel = diaria * d;
  const total = Math.max(0, aluguel + (Number(o.frete) || 0) - (Number(o.desconto) || 0));
  const u = uso(ctx, o);
  let mesasTot = 0, cadTot = 0;
  Object.keys(u).forEach((id) => { if (mod(ctx, id).tipo === 'mesa') mesasTot += u[id]; else cadTot += u[id]; });
  return { d, diaria, aluguel, total, saldo: total - (Number(o.sinal) || 0), mesasTot, cadTot, uso: u };
}

// ---------- Disponibilidade por data e modelo ----------
// Reservam estoque: pedidos Confirmado e Entregue. Orçamento e Devolvido não reservam.
// Dois períodos conflitam se entrega <= fim && retirada >= início (o dia da retirada ainda conta como ocupado).
export function reservado(ctx, entrega, retirada, ignorarPedidoId) {
  const res = {};
  ctx.pedidos.forEach((o) => {
    if (o.id === ignorarPedidoId || o.status === 'Devolvido' || o.status === 'Orçamento') return;
    if (o.entrega <= retirada && o.retirada >= entrega) {
      const u = uso(ctx, o);
      Object.keys(u).forEach((id) => { res[id] = (res[id] || 0) + u[id]; });
    }
  });
  return res;
}
export const livre = (m, res) => m.total - m.manut - (res[m.id] || 0);
// Modelos que faltariam se este pedido fosse confirmado (lista de ids).
export function falta(ctx, o, ignorarPedidoId) {
  const res = reservado(ctx, o.entrega, o.retirada, ignorarPedidoId);
  const u = uso(ctx, o);
  return Object.keys(u).filter((id) => u[id] > livre(mod(ctx, id), res));
}
// Jogos completos de um material + cor: min(mesas, floor(cadeiras sem braço / cadeiras por mesa)).
export function jogosCompletos(ctx, material, cor) {
  const soma = (tipo) => ctx.modelos.filter((m) => m.material === material && m.cor === cor && m.tipo === tipo && !m.braco).reduce((s, m) => s + m.total, 0);
  const cp = ctx.precos.cadPorJogo[cpjKey(material, cor)] || CADEIRAS_POR_MESA_PADRAO;
  return Math.min(soma('mesa'), Math.floor(soma('cadeira') / cp));
}

// ---------- Devolução / vistoria ----------
// dev = { q: {idModelo: quebradas}, f: {idModelo: faltando}, atraso: diasDeAtraso }
// Total final = total + (quebradas + faltando) × reposição + atraso × diária do pedido.
// Resto = total final − (sinal + caução). Positivo: cobrar do cliente. Negativo: devolver ao cliente.
export function fechamento(ctx, o, dev) {
  const c = calcPedido(ctx, o);
  let avarias = 0;
  Object.keys(c.uso).forEach((id) => { avarias += ((dev.q[id] || 0) + (dev.f[id] || 0)) * mod(ctx, id).rep; });
  const atraso = (dev.atraso || 0) * c.diaria;
  const final = c.total + avarias + atraso;
  const abatido = (Number(o.sinal) || 0) + (Number(o.caucao) || 0);
  return { ...c, avarias, atraso, final, abatido, resto: final - abatido };
}
// Ao confirmar a devolução: quebradas e faltantes saem do estoque (total) e o pedido vira Devolvido com o total final gravado.
export function aplicarDevolucao(ctx, o, dev) {
  const f = fechamento(ctx, o, dev);
  const modelos = ctx.modelos.map((m) => ({ ...m, total: Math.max(0, m.total - (dev.q[m.id] || 0) - (dev.f[m.id] || 0)) }));
  const pedido = { ...o, status: 'Devolvido', dev, final: f.final };
  return { modelos, pedido };
}

// ---------- Pix estático (BR Code, padrão do Banco Central) ----------
const emv = (id, v) => { v = String(v); return id + String(v.length).padStart(2, '0') + v; };
export function crc16(str) {
  let crc = 0xFFFF;
  for (let i = 0; i < str.length; i++) {
    crc ^= str.charCodeAt(i) << 8;
    for (let j = 0; j < 8; j++) { crc = (crc & 0x8000) ? ((crc << 1) ^ 0x1021) : (crc << 1); crc &= 0xFFFF; }
  }
  return crc.toString(16).toUpperCase().padStart(4, '0');
}
const semAcento = (t) => String(t || '').normalize('NFD').replace(/[̀-ͯ]/g, '');
// valor = 0 gera um código sem valor (o cliente digita). txid = 'PED' + id do pedido.
export function pixPayload(pix, valor, txid) {
  const nome = semAcento(pix.nome).replace(/[^A-Za-z0-9 ]/g, '').toUpperCase().trim().slice(0, 25) || 'RECEBEDOR';
  const cidade = semAcento(pix.cidade).replace(/[^A-Za-z0-9 ]/g, '').toUpperCase().trim().slice(0, 15) || 'CIDADE';
  const tx = String(txid || '***').replace(/[^A-Za-z0-9]/g, '').slice(0, 25) || '***';
  let p = emv('00', '01') + emv('26', emv('00', 'br.gov.bcb.pix') + emv('01', String(pix.chave).trim())) + emv('52', '0000') + emv('53', '986');
  if (valor > 0) p += emv('54', valor.toFixed(2));
  p += emv('58', 'BR') + emv('59', nome) + emv('60', cidade) + emv('62', emv('05', tx)) + '6304';
  return { code: p + crc16(p), nome, cidade };
}
// Opções de cobrança Pix na tela do pedido: sinal = 50% do total; saldo = total − sinal; valor após vistoria (se houver resto).
export function opcoesPix(ctx, o, resto) {
  const c = calcPedido(ctx, o);
  const opts = [['sinal', 'Sinal (50%)', Math.round(c.total * 50) / 100], ['saldo', 'Saldo (total − sinal)', c.saldo]];
  if ((o.status === 'Entregue' || o.status === 'Devolvido') && resto > 0) opts.push(['vistoria', 'Valor após vistoria', resto]);
  opts.push(['outro', 'Outro valor', 0]);
  return opts;
}
// QR Code: use uma biblioteca (protótipo usa qrcode-generator, MIT, em reference/qrcode-generator-1.4.4.MIT.js).
// Gere o QR a partir de pixPayload(...).code, correção de erro nível 'M'.

// ---------- Links externos ----------
export const brl = (n) => (Number(n) || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
export function linhaInfo(ctx, l) {
  if (l.tipo === 'jogo') return { titulo: l.qtd + (l.qtd === 1 ? ' jogo' : ' jogos'), desc: nomeModelo(mod(ctx, l.mesa)) + ' + ' + cadPorMesa(ctx, l.mesa) + ' × ' + nomeModelo(mod(ctx, l.cad)).toLowerCase() };
  if (l.tipo === 'mesa') return { titulo: l.qtd + (l.qtd === 1 ? ' mesa avulsa' : ' mesas avulsas'), desc: nomeModelo(mod(ctx, l.mesa)) };
  return { titulo: l.qtd + (l.qtd === 1 ? ' cadeira avulsa' : ' cadeiras avulsas'), desc: nomeModelo(mod(ctx, l.cad)) };
}
// Link para criar o evento no Google Agenda (tipo: 'entrega' | 'retirada'). Evento de dia inteiro.
export function linkGoogleAgenda(ctx, o, tipo) {
  const dia = tipo === 'entrega' ? o.entrega : o.retirada;
  const prox = new Date(dia + 'T12:00:00'); prox.setDate(prox.getDate() + 1);
  const ymd = (x) => x.replace(/-/g, '');
  const fim = prox.getFullYear() + String(prox.getMonth() + 1).padStart(2, '0') + String(prox.getDate()).padStart(2, '0');
  const c = calcPedido(ctx, o);
  const linhas = (o.itens || []).map((l) => { const i = linhaInfo(ctx, l); return '• ' + i.titulo + ': ' + i.desc; }).join('\n');
  const titulo = (tipo === 'entrega' ? 'Entrega' : 'Retirada') + ' — ' + o.cliente + ' (#' + o.id + ')';
  const det = linhas + '\n\nTotal: ' + c.mesasTot + ' mesas e ' + c.cadTot + ' cadeiras\nTelefone: ' + o.tel + (o.ref ? '\nReferência: ' + o.ref : '') + '\nSaldo a receber: ' + brl(c.saldo);
  return 'https://calendar.google.com/calendar/render?action=TEMPLATE&text=' + encodeURIComponent(titulo) + '&dates=' + ymd(dia) + '/' + fim + '&details=' + encodeURIComponent(det) + '&location=' + encodeURIComponent(o.end);
}
// WhatsApp: https://wa.me/55<telefone só dígitos>?text=<mensagem codificada com o Pix copia e cola>
export const linkWhatsApp = (tel, texto) => 'https://wa.me/55' + String(tel).replace(/\D/g, '') + '?text=' + encodeURIComponent(texto);

// ---------- Filtro por material (menu ⋮ → Material: Todos | Plástico | Metal) ----------
// Só filtra o que aparece. Um pedido entra no filtro se tiver pelo menos um item do material escolhido.
export function pedidoTemMaterial(ctx, o, material) {
  if (material === 'Todos') return true;
  return (o.itens || []).some((l) => (l.tipo === 'jogo' ? [l.mesa, l.cad] : l.tipo === 'mesa' ? [l.mesa] : [l.cad]).some((id) => mod(ctx, id).material === material));
}
