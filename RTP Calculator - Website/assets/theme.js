/* RTP Calculator - mode clair / mode sombre (rtpcalculator.com)
   Chargé en bloquant dans le <head> des 45 pages, avant site.css, pour que le mode soit posé avant le premier
   affichage. Le site est clair par défaut : le mode sombre ne s'active qu'au clic sur le bouton de l'en-tête
   (le réglage clair / sombre de l'appareil n'est pas suivi). Choix mémorisé dans localStorage['rtp_theme'] = 'dark'
   (préférence d'affichage, pas un traceur : indépendant du bandeau cookies).
   Sans JavaScript : site clair, pas de bouton. Les couleurs des deux modes sont les jetons de site.css. */
(function(){
"use strict";
var KEY = 'rtp_theme', root = document.documentElement;
var T = {
  fr: ["Passer en mode sombre", "Passer en mode clair"],
  en: ["Switch to dark mode", "Switch to light mode"],
  es: ["Cambiar a modo oscuro", "Cambiar a modo claro"],
  de: ["Zum dunklen Modus wechseln", "Zum hellen Modus wechseln"],
  pt: ["Mudar para o modo escuro", "Mudar para o modo claro"]
};
var MOON = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z"/></svg>';
var SUN = '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="4"/><path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.3 5.3l1.8 1.8M16.9 16.9l1.8 1.8M5.3 18.7l1.8-1.8M16.9 7.1l1.8-1.8"/></svg>';

function stored(){ try { return localStorage.getItem(KEY) === 'dark'; } catch(e){ return false; } }
function isDark(){ return root.getAttribute('data-theme') === 'dark'; }
function apply(dark){ if(dark) root.setAttribute('data-theme', 'dark'); else root.removeAttribute('data-theme'); }

apply(stored());

var btn = null;
function paint(){
  if(!btn) return;
  var t = T[(root.lang || 'en').slice(0, 2).toLowerCase()] || T.en, dark = isDark();
  btn.innerHTML = dark ? SUN : MOON;
  btn.setAttribute('aria-label', dark ? t[1] : t[0]);
  btn.title = dark ? t[1] : t[0];
}
function build(){
  var nav = document.querySelector('header.site .nav');
  if(!nav || btn) return;
  btn = document.createElement('button');
  btn.type = 'button'; btn.className = 'theme-btn';
  btn.addEventListener('click', function(){
    var dark = !isDark();
    apply(dark);
    try { if(dark) localStorage.setItem(KEY, 'dark'); else localStorage.removeItem(KEY); } catch(e){}
    paint();
  });
  nav.appendChild(btn);
  paint();
}
if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', build); else build();
/* le choix fait dans un autre onglet s'applique ici aussi */
window.addEventListener('storage', function(e){ if(e.key === KEY){ apply(stored()); paint(); } });
})();
