/* RTP Calculator - bannière de consentement (v1) et Consent Mode v2.
   Extrait des pages le 24/10/2026 (lot 1a), inchangé. Chargé dans <head> juste après le bloc
   gtag('consent','default', …) et avant tout usage de window.rtpConsentOpen. Pas sur /privacy/. */
(function () {
  var KEY = 'rtp_consent_v1';

  /* --- gtag local : pousse dans dataLayer exactement comme gtag.js --- */
  window.dataLayer = window.dataLayer || [];
  function g() { window.dataLayer.push(arguments); }

  /* --- textes localisés --- */
  var T = {
    fr: {
      title: "Votre choix sur les cookies",
      body: "Ce site mesure son audience et pourra afficher des publicités sur les pages du calculateur. Aucun cookie n'est déposé sans votre accord.",
      accept: "Tout accepter", reject: "Tout refuser", custom: "Personnaliser",
      save: "Enregistrer mes choix", close: "Fermer",
      a_h: "Mesure d'audience", a_p: "Nous aide à savoir quelles pages sont consultées. Aucune donnée n'est revendue.",
      d_h: "Publicité", d_p: "Permet d'afficher des annonces sur les pages du calculateur et d'en mesurer la performance.",
      privacy: "Politique de confidentialité", privacy_url: "/fr/privacy/"
    },
    en: {
      title: "Your cookie choices",
      body: "This site measures its audience and may display ads on the calculator pages. No cookie is set without your consent.",
      accept: "Accept all", reject: "Reject all", custom: "Customise",
      save: "Save my choices", close: "Close",
      a_h: "Audience measurement", a_p: "Helps us know which pages are viewed. No data is ever sold.",
      d_h: "Advertising", d_p: "Allows ads to be shown on the calculator pages and their performance measured.",
      privacy: "Privacy policy", privacy_url: "/privacy/"
    },
    es: {
      title: "Tus opciones sobre las cookies",
      body: "Este sitio mide su audiencia y podrá mostrar anuncios en las páginas de la calculadora. No se instala ninguna cookie sin tu consentimiento.",
      accept: "Aceptar todo", reject: "Rechazar todo", custom: "Personalizar",
      save: "Guardar mis opciones", close: "Cerrar",
      a_h: "Medición de audiencia", a_p: "Nos ayuda a saber qué páginas se consultan. Ningún dato se revende.",
      d_h: "Publicidad", d_p: "Permite mostrar anuncios en las páginas de la calculadora y medir su rendimiento.",
      privacy: "Política de privacidad", privacy_url: "/es/privacy/"
    },
    pt: {
      title: "Suas escolhas sobre os cookies",
      body: "Este site mede sua audiência e poderá exibir anúncios nas páginas da calculadora. Nenhum cookie é instalado sem o seu consentimento.",
      accept: "Aceitar tudo", reject: "Recusar tudo", custom: "Personalizar",
      save: "Salvar minhas escolhas", close: "Fechar",
      a_h: "Medição de audiência", a_p: "Ajuda a saber quais páginas são acessadas. Nenhum dado é revendido.",
      d_h: "Publicidade", d_p: "Permite exibir anúncios nas páginas da calculadora e medir seu desempenho.",
      privacy: "Política de privacidade", privacy_url: "/pt/privacy/"
    },
    de: {
      title: "Ihre Cookie-Einstellungen",
      body: "Diese Website misst ihre Reichweite und kann auf den Rechner-Seiten Werbung anzeigen. Ohne Ihre Zustimmung wird kein Cookie gesetzt.",
      accept: "Alle akzeptieren", reject: "Alle ablehnen", custom: "Anpassen",
      save: "Auswahl speichern", close: "Schließen",
      a_h: "Reichweitenmessung", a_p: "Hilft uns zu erkennen, welche Seiten aufgerufen werden. Keine Daten werden verkauft.",
      d_h: "Werbung", d_p: "Ermöglicht Anzeigen auf den Rechner-Seiten und deren Erfolgsmessung.",
      privacy: "Datenschutz", privacy_url: "/de/privacy/"
    }
  };
  var lang = (document.documentElement.lang || 'en').slice(0, 2).toLowerCase();
  if (!T[lang]) lang = 'en';
  var t = T[lang];

  /* --- lecture / écriture du choix --- */
  function read() {
    try { return JSON.parse(localStorage.getItem(KEY)); } catch (e) { return null; }
  }
  function write(c) {
    try { localStorage.setItem(KEY, JSON.stringify(c)); } catch (e) {}
  }

  /* --- application au Consent Mode v2 --- */
  function apply(c, firstTime) {
    g('consent', 'update', {
      analytics_storage:   c.analytics ? 'granted' : 'denied',
      ad_storage:          c.ads ? 'granted' : 'denied',
      ad_user_data:        c.ads ? 'granted' : 'denied',
      ad_personalization:  c.ads ? 'granted' : 'denied'
    });
    // Au tout premier accord, la page vue initiale est déjà partie sans cookie :
    // on en renvoie une pour que la visite soit comptée correctement.
    if (firstTime && c.analytics) { g('event', 'page_view'); }
  }

  /* --- APPLICATION IMMÉDIATE si un choix existe (avant gtag config) --- */
  var stored = read();
  if (stored) { apply(stored, false); }

  /* --- interface --- */
  var CSS =
  '#rtp-cmp{position:fixed;left:0;right:0;bottom:0;z-index:99999;padding:14px;display:none;' +
  'font-family:Archivo,system-ui,-apple-system,sans-serif}' +
  '#rtp-cmp.on{display:block}' +
  '#rtp-cmp .box{max-width:900px;margin:0 auto;background:#141414;border:1px solid #1F1F1F;' +
  'border-radius:8px;box-shadow:0 16px 44px rgba(0,0,0,.65);padding:18px 20px}' +
  '#rtp-cmp h2{font-size:15px;font-weight:800;color:#fff;margin:0 0 6px}' +
  '#rtp-cmp p{font-size:13.5px;line-height:1.55;color:#AAA;margin:0 0 12px}' +
  '#rtp-cmp a.pl{color:#FFD600;font-weight:600;text-decoration:none}' +
  '#rtp-cmp a.pl:hover{text-decoration:underline}' +
  '#rtp-cmp .row{display:flex;flex-wrap:wrap;gap:10px;align-items:center}' +
  '#rtp-cmp button{font-family:inherit;font-size:14px;font-weight:600;letter-spacing:0;line-height:1.25;' +
  'border-radius:6px;min-height:44px;padding:10px 18px;cursor:pointer;border:1px solid transparent}' +
  '#rtp-cmp .b-yes{background:#FAFAFA;color:#0A0A0A}' +
  '#rtp-cmp .b-yes:hover{background:#fff}' +
  '#rtp-cmp .b-no{background:transparent;color:#FAFAFA;border-color:#3A3A3A}' +
  '#rtp-cmp .b-no:hover{border-color:#666}' +
  '#rtp-cmp .b-more{background:transparent;color:#AAA;border-color:transparent;padding:0 6px;' +
  'text-decoration:underline;font-weight:600}' +
  '#rtp-cmp .b-more:hover{color:#fff}' +
  '#rtp-cmp .panel{display:none;margin:14px 0 4px;border-top:1px solid #1F1F1F;padding-top:14px}' +
  '#rtp-cmp .panel.on{display:block}' +
  '#rtp-cmp .opt{display:flex;gap:12px;align-items:flex-start;margin:0 0 12px}' +
  '#rtp-cmp .opt input{margin-top:3px;width:16px;height:16px;accent-color:#FFD600;flex:none}' +
  '#rtp-cmp .opt h3{font-size:13.5px;font-weight:700;color:#fff;margin:0 0 2px}' +
  '#rtp-cmp .opt p{font-size:12.5px;margin:0;color:#888}' +
  '@media (max-width:560px){#rtp-cmp .row{flex-direction:column;align-items:stretch}' +
  '#rtp-cmp button{width:100%}}';

  function build() {
    var s = document.createElement('style'); s.textContent = CSS;
    document.head.appendChild(s);

    var d = document.createElement('div');
    d.id = 'rtp-cmp';
    d.setAttribute('role', 'dialog');
    d.setAttribute('aria-label', t.title);
    d.innerHTML =
      '<div class="box">' +
        '<h2>' + t.title + '</h2>' +
        '<p>' + t.body + ' <a class="pl" href="' + t.privacy_url + '">' + t.privacy + '</a></p>' +
        '<div class="panel" id="rtp-cmp-panel">' +
          '<div class="opt"><input type="checkbox" id="rtp-cmp-a"><div>' +
            '<h3>' + t.a_h + '</h3><p>' + t.a_p + '</p></div></div>' +
          '<div class="opt"><input type="checkbox" id="rtp-cmp-d"><div>' +
            '<h3>' + t.d_h + '</h3><p>' + t.d_p + '</p></div></div>' +
        '</div>' +
        '<div class="row">' +
          '<button class="b-yes" id="rtp-cmp-yes">' + t.accept + '</button>' +
          '<button class="b-no"  id="rtp-cmp-no">'  + t.reject + '</button>' +
          '<button class="b-more" id="rtp-cmp-more">' + t.custom + '</button>' +
          '<button class="b-no"  id="rtp-cmp-save" style="display:none">' + t.save + '</button>' +
        '</div>' +
      '</div>';
    document.body.appendChild(d);

    var panel = d.querySelector('#rtp-cmp-panel'),
        cbA   = d.querySelector('#rtp-cmp-a'),
        cbD   = d.querySelector('#rtp-cmp-d'),
        bSave = d.querySelector('#rtp-cmp-save'),
        bMore = d.querySelector('#rtp-cmp-more');

    function decide(c) { write(c); apply(c, true); d.classList.remove('on'); }

    d.querySelector('#rtp-cmp-yes').onclick = function () { decide({ analytics: true,  ads: true,  ts: Date.now() }); };
    d.querySelector('#rtp-cmp-no').onclick  = function () { decide({ analytics: false, ads: false, ts: Date.now() }); };
    bMore.onclick = function () {
      panel.classList.add('on'); bMore.style.display = 'none'; bSave.style.display = '';
      var cur = read() || { analytics: false, ads: false };
      cbA.checked = !!cur.analytics; cbD.checked = !!cur.ads;
    };
    bSave.onclick = function () { decide({ analytics: cbA.checked, ads: cbD.checked, ts: Date.now() }); };

    return d;
  }

  var el = null;
  function show() {
    if (!el) el = build();
    var cur = read();
    if (cur) {   // réouverture : on pré-affiche le détail
      el.querySelector('#rtp-cmp-more').click();
    }
    el.classList.add('on');
  }

  /* --- point d'entrée public : lien « Paramètres des cookies » --- */
  window.rtpConsentOpen = function (e) { if (e && e.preventDefault) e.preventDefault(); show(); return false; };

  /* --- affichage au premier passage --- */
  function init() { if (!read()) show(); }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else { init(); }
})();
