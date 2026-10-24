/* RTP Calculator - partage d'une analyse en image (rtpcalculator.com)
   Chargé par les 5 calculateurs (/app/, /fr/app/, /es/app/, /de/app/, /pt/app/).
   Dessine la carte d'historique dans un canvas de 1080 px de large (la carte à 360 px, à l'échelle 3,
   comme l'image produite par l'app Android), puis la remet au système dans cet ordre :
   feuille de partage (fichier PNG), presse-papiers (image), téléchargement.
   API : window.RTPShare.render(vm) → Promise<Blob> ; window.RTPShare.deliver(blob, opts) → Promise.
   vm = {meta, eventName, rtp:{text,color}|null, outcomes:[{label, odds, book}], signature, site}
   opts = {filename, toast(msg), T:{copiedImg, downloaded}} */
(function(){
"use strict";
var S = 3, W = 360, MARGIN = 12, BAR = 4, R_CARD = 8, R_S = 6, GAP = 5;
var FONT = "'Archivo', system-ui, -apple-system, sans-serif";
var C = {bg:'#0A0A0A', card:'#141414', block:'#1E1E1E', divider:'#1F1F1F', yellow:'#FFD600',
         text:'#FFFFFF', text2:'#AAAAAA', text3:'#8A8A8A', onPill:'#0A0A0A', ring:'#3A3B40'};

function font(weight, size, italic){ return (italic ? 'italic ' : '') + weight + ' ' + size + 'px ' + FONT; }

/* Les graisses Archivo sont déclarées par la page (Google Fonts) mais ne sont chargées qu'à l'usage :
   on les demande explicitement, sinon le canvas dessine avec la police de repli. */
function warm(){
  if(!document.fonts || !document.fonts.load) return Promise.resolve();
  var faces = [font(400, 12), font(700, 13), font(800, 16), font(400, 10, true)];
  return Promise.all(faces.map(function(f){ return document.fonts.load(f).catch(function(){}); }));
}

function rrect(ctx, x, y, w, h, r){
  r = Math.min(r, w/2, h/2);
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

/* Tronque avec « … » si le texte dépasse maxW (équivalent de text-overflow:ellipsis). */
function fit(ctx, text, maxW){
  text = String(text);
  if(ctx.measureText(text).width <= maxW) return text;
  var t = text;
  while(t.length > 1 && ctx.measureText(t + '…').width > maxW) t = t.slice(0, -1);
  return t.replace(/\s+$/, '') + '…';
}

/* Logo du site (le SVG de l'en-tête, viewBox 100×100, redessiné en primitives). */
function logo(ctx, x, y, size){
  var k = size / 100, D = Math.PI / 180;
  ctx.save(); ctx.translate(x, y); ctx.scale(k, k);
  ctx.lineWidth = 16; ctx.lineCap = 'butt';
  ctx.strokeStyle = C.ring; ctx.beginPath(); ctx.arc(50, 50, 31, 0, 2 * Math.PI); ctx.stroke();
  ctx.strokeStyle = C.yellow; ctx.beginPath(); ctx.arc(50, 50, 31, -25 * D, 295 * D, false); ctx.stroke();
  ctx.strokeStyle = C.card; ctx.lineWidth = 4;
  ctx.beginPath(); ctx.moveTo(69.032, 41.125); ctx.lineTo(87.159, 32.673); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(58.875, 30.968); ctx.lineTo(67.327, 12.841); ctx.stroke();
  ctx.restore();
}

/* Dessin de la carte. Toutes les cotes sont en pixels CSS de la carte à 360 px ; le canvas est à l'échelle S. */
function draw(vm){
  var outs = vm.outcomes || [], n = outs.length;
  var cols = n > 3 ? 3 : Math.max(n, 1), rows = Math.ceil(n / cols);
  var cardX = MARGIN, cardW = W - 2 * MARGIN;
  var cx0 = cardX + BAR + 12, contentW = cardW - BAR - 24;
  var blockW = (contentW - GAP * (cols - 1)) / cols;
  var BPAD = 4, LABEL_H = 13, FRAME_H = 27, BOOK_H = 13;
  var blockH = BPAD + LABEL_H + 3 + FRAME_H + 3 + BOOK_H + BPAD;
  var HEAD_H = 20, EVENT_H = 17, SIG_H = 24, LOGO = 20;

  var y = MARGIN + 10;
  var headY = y; y += HEAD_H;
  var eventY = -1;
  if(vm.eventName){ y += 7; eventY = y; y += EVENT_H + 9; } else { y += 8; }
  var gridY = y; y += rows * blockH + (rows - 1) * GAP;
  y += 6; var divY = y; y += 1 + 8;
  var sigY = y; y += SIG_H + 4;
  var cardBottom = y + 8, cardH = cardBottom - MARGIN;
  var H = cardBottom + MARGIN;

  var canvas = document.createElement('canvas');
  canvas.width = W * S; canvas.height = Math.round(H * S);
  var ctx = canvas.getContext('2d');
  ctx.scale(S, S);
  ctx.textBaseline = 'middle';

  ctx.fillStyle = C.bg; ctx.fillRect(0, 0, W, H);

  ctx.save();
  rrect(ctx, cardX, MARGIN, cardW, cardH, R_CARD); ctx.clip();
  ctx.fillStyle = C.card; ctx.fillRect(cardX, MARGIN, cardW, cardH);
  if(vm.rtp){ ctx.fillStyle = vm.rtp.color; ctx.fillRect(cardX, MARGIN, BAR, cardH); }
  ctx.restore();

  /* en-tête : méta à gauche, pastille TRJ à droite */
  var pillW = 0;
  if(vm.rtp){
    ctx.font = font(800, 12);
    pillW = Math.ceil(ctx.measureText(vm.rtp.text).width) + 18;
    var px = cx0 + contentW - pillW;
    ctx.fillStyle = vm.rtp.color; rrect(ctx, px, headY, pillW, HEAD_H, R_S); ctx.fill();
    ctx.fillStyle = C.onPill; ctx.textAlign = 'center';
    ctx.fillText(vm.rtp.text, px + pillW / 2, headY + HEAD_H / 2 + 0.5);
  }
  ctx.font = font(400, 12); ctx.fillStyle = C.text2; ctx.textAlign = 'left';
  ctx.fillText(fit(ctx, vm.meta || '–', contentW - pillW - 10), cx0, headY + HEAD_H / 2 + 0.5);

  if(eventY >= 0){
    ctx.font = font(700, 13.5); ctx.fillStyle = C.text; ctx.textAlign = 'center';
    ctx.fillText(fit(ctx, vm.eventName, contentW), cx0 + contentW / 2, eventY + EVENT_H / 2 + 0.5);
  }

  /* blocs de cotes, lignes de 3 ; les cellules vides d'une dernière ligne incomplète restent vides */
  ctx.textAlign = 'center';
  for(var i = 0; i < n; i++){
    var o = outs[i], r = Math.floor(i / cols), c = i % cols;
    var bx = cx0 + c * (blockW + GAP), by = gridY + r * (blockH + GAP);
    var tx = bx + blockW / 2, innerW = blockW - 2 * BPAD - 4;
    ctx.fillStyle = C.block; rrect(ctx, bx, by, blockW, blockH, R_S); ctx.fill();
    if(o.label){ ctx.font = font(400, 11); ctx.fillStyle = C.text2; ctx.fillText(fit(ctx, o.label, innerW), tx, by + BPAD + LABEL_H / 2 + 0.5); }
    var fy = by + BPAD + LABEL_H + 3;
    ctx.fillStyle = C.bg; rrect(ctx, bx + BPAD, fy, blockW - 2 * BPAD, FRAME_H, 4); ctx.fill();
    ctx.font = font(800, 16); ctx.fillStyle = C.yellow;
    ctx.fillText(fit(ctx, o.odds || '–', innerW), tx, fy + FRAME_H / 2 + 0.5);
    if(o.book){ ctx.font = font(400, 11); ctx.fillStyle = C.text3; ctx.fillText(fit(ctx, o.book, innerW), tx, fy + FRAME_H + 3 + BOOK_H / 2 + 0.5); }
  }

  /* filet puis signature : logo, nom en gras, site en italique */
  ctx.fillStyle = C.divider; ctx.fillRect(cx0, divY, contentW, 1);
  logo(ctx, cx0, sigY + (SIG_H - LOGO) / 2, LOGO);
  ctx.textAlign = 'left';
  var sx = cx0 + LOGO + 8, sy = sigY + SIG_H / 2 + 0.5;
  ctx.font = font(700, 10.5); ctx.fillStyle = C.text; ctx.fillText(vm.signature, sx, sy);
  sx += ctx.measureText(vm.signature).width;
  ctx.font = font(400, 10.5); ctx.fillStyle = C.text3; ctx.fillText(' · ', sx, sy);
  sx += ctx.measureText(' · ').width;
  ctx.font = font(400, 10.5, true); ctx.fillText(vm.site, sx, sy);
  return canvas;
}

function toBlob(canvas){
  return new Promise(function(resolve, reject){
    if(!canvas.toBlob) return reject(new Error('toBlob'));
    canvas.toBlob(function(b){ b ? resolve(b) : reject(new Error('toBlob')); }, 'image/png');
  });
}

function render(vm){
  return warm().then(function(){ return toBlob(draw(vm)); });
}

function viaClipboard(blob){
  if(!(navigator.clipboard && navigator.clipboard.write && typeof ClipboardItem === 'function')) return Promise.reject(new Error('clipboard'));
  return navigator.clipboard.write([new ClipboardItem({'image/png': blob})]);
}

function download(blob, filename){
  var a = document.createElement('a'), url = URL.createObjectURL(blob);
  a.href = url; a.download = filename; a.style.display = 'none';
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(function(){ URL.revokeObjectURL(url); }, 4000);
}

/* 1. feuille de partage avec fichier (Chrome Android, Chrome/Edge Windows, Safari) ;
   2. image dans le presse-papiers (Firefox ≥ 127, Chrome bureau) ; 3. téléchargement du PNG. */
function deliver(blob, o){
  var file = null;
  try { file = new File([blob], o.filename, {type: 'image/png'}); } catch(e){}
  var canShare = !!(file && navigator.share && navigator.canShare && navigator.canShare({files: [file]}));
  var p = canShare ? navigator.share({files: [file]}) : Promise.reject(new Error('share'));
  return p.catch(function(e){
    if(e && e.name === 'AbortError') return; /* annulé par l'utilisateur */
    return viaClipboard(blob).then(function(){ o.toast(o.T.copiedImg); }, function(){
      download(blob, o.filename); o.toast(o.T.downloaded);
    });
  });
}

window.RTPShare = {warm: warm, render: render, deliver: deliver};
if(document.readyState === 'complete') warm(); else window.addEventListener('load', function(){ warm(); });
})();
