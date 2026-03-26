// ── TEMA ────────────────────────────────────────────────────────────
function toggleTheme() {
  const body = document.body;
  const isDark = body.getAttribute('data-theme') === 'dark';
  body.setAttribute('data-theme', isDark ? '' : 'dark');
  const btn = document.querySelector('.theme-btn');
  if (btn) btn.textContent = isDark ? '🌙' : '☀️';
  localStorage.setItem('theme', isDark ? 'light' : 'dark');
}
(function () {
  const saved = localStorage.getItem('theme');
  if (saved === 'dark') {
    document.body.setAttribute('data-theme', 'dark');
    const btn = document.querySelector('.theme-btn');
    if (btn) btn.textContent = '☀️';
  }
})();

// ── ESTRELLAS ────────────────────────────────────────────────────────
function initStars(initial) {
  const row   = document.getElementById('starRow');
  const input = document.getElementById('ratingInput');
  const label = document.getElementById('ratingVal');
  if (!row) return;
  let current = initial || parseInt(input?.value) || 0;
  row.innerHTML = '';
  for (let i = 1; i <= 10; i++) {
    const s = document.createElement('span');
    s.className = 'star-widget' + (i <= current ? ' on' : '');
    s.textContent = i <= current ? '★' : '☆';
    s.dataset.v = i;
    s.addEventListener('mouseenter', () => highlight(i));
    s.addEventListener('mouseleave', () => highlight(current));
    s.addEventListener('click',      () => { current = i; input.value = i; label.textContent = i; highlight(i); });
    row.appendChild(s);
  }
  function highlight(n) {
    [...row.querySelectorAll('.star-widget')].forEach((el, idx) => {
      const on = idx < n;
      el.className = 'star-widget' + (on ? ' on' : '');
      el.textContent = on ? '★' : '☆';
    });
  }
}
document.addEventListener('DOMContentLoaded', () => initStars());

// ── PORTADA AUTOMÁTICA (TMDB / Google Books) ─────────────────────────
let coverDebounce = null;

function fetchAutoCover() {
  clearTimeout(coverDebounce);
  coverDebounce = setTimeout(_doFetchCover, 700);
}

function _doFetchCover() {
  const titleEl = document.querySelector('input[name="title"]');
  const typeEl  = document.getElementById('typeSelect');
  if (!titleEl || !typeEl) return;

  const title = titleEl.value.trim();
  const type  = typeEl.value;
  if (title.length < 2) return;

  // Solo para tipos con API disponible
  if (!type.includes('Pel') && !type.includes('Serie') && !type.includes('Libro')) return;

  // extra = director para películas, autor para libros
  let extra = '';
  if (type.includes('Pel')) {
    const dirEl = document.querySelector('input[name="director"]');
    if (dirEl) extra = dirEl.value.trim();
  } else if (type.includes('Libro')) {
    const authEl = document.querySelector('input[name="author"]');
    if (authEl) extra = authEl.value.trim();
  }

  const params = new URLSearchParams({ type, title, extra });
  fetch('/api/cover/search?' + params)
    .then(r => r.json())
    .then(data => {
      if (data.url) _showCoverPreview(data.url);
      else          _hideCoverPreview();
    })
    .catch(() => {});
}

function _showCoverPreview(url) {
  let box = document.getElementById('autoCoverBox');
  if (!box) {
    box = document.createElement('div');
    box.id = 'autoCoverBox';
    box.innerHTML = `
      <div style="display:flex;align-items:center;gap:10px;margin-top:8px;padding:8px 12px;
                  background:var(--card);border-radius:10px;border:1.5px solid var(--accent);">
        <img id="autoCoverImg" src="" alt="portada" style="width:60px;height:88px;object-fit:cover;border-radius:6px;"/>
        <div style="flex:1">
          <div style="font-size:0.82rem;font-weight:600;color:var(--accent)">🎨 Portada encontrada automáticamente</div>
          <div style="font-size:0.76rem;color:var(--muted);margin-top:2px">Se usará esta imagen. Puedes subir otra manualmente para reemplazarla.</div>
          <input type="hidden" name="autoCoverUrl" id="autoCoverUrl" value=""/>
          <button type="button" onclick="_hideCoverPreview()" style="margin-top:6px;font-size:0.75rem;background:transparent;border:none;color:var(--muted);cursor:pointer;">✖ No usar esta portada</button>
        </div>
      </div>`;
    // Insertar justo antes del campo de portada manual
    const fileGroup = document.querySelector('input[type="file"]')?.closest('.form-group');
    if (fileGroup) fileGroup.parentNode.insertBefore(box, fileGroup);
  }
  document.getElementById('autoCoverImg').src = url;
  document.getElementById('autoCoverUrl').value = url;
  box.style.display = 'block';
}

function _hideCoverPreview() {
  const box = document.getElementById('autoCoverBox');
  if (box) {
    box.style.display = 'none';
    const inp = document.getElementById('autoCoverUrl');
    if (inp) inp.value = '';
  }
}

// ── CAMPOS DINÁMICOS ─────────────────────────────────────────────────
const TYPES = ['Libro', 'Serie', 'Película', 'Teatro', 'Cómic'];

function updateDynamicFields(prefill) {
  const sel  = document.getElementById('typeSelect');
  const box  = document.getElementById('dynamicFields');
  if (!sel || !box) return;
  const type = sel.value || '';
  const d    = prefill || {};
  const fs   = 'style="width:100%;padding:8px 12px;border-radius:8px;border:1.5px solid var(--border);background:var(--input);color:var(--text);font-size:.92rem"';
  let html   = '';

  if (type.includes('Libro')) {
    html = `
      <div class="form-row">
        <div class="form-group flex-grow"><label>✍️ Autor</label><input type="text" name="author" ${fs} value="${d.author||''}" oninput="fetchAutoCover()"/></div>
        <div class="form-group"><label>📖 Capítulo leído</label><input type="number" name="chapters" ${fs} min="1" value="${d.chapters||''}"/></div>
      </div>
      <div class="check-row"><label><input type="checkbox" name="finished" value="true" ${d.finished=='true'?'checked':''}/> 📘 Libro terminado</label></div>`;

  } else if (type.includes('Serie')) {
    html = `
      <div class="form-row">
        <div class="form-group"><label>📺 Temporada</label><input type="number" name="season" ${fs} min="1" value="${d.season||''}"/></div>
        <div class="form-group"><label>🎞️ Capítulo</label><input type="number" name="episode" ${fs} min="1" value="${d.episode||''}"/></div>
      </div>
      <div class="check-row">
        <label><input type="checkbox" name="seasonFinished" value="true" ${d.seasonFinished=='true'?'checked':''}/> 🌟 Fin de temporada</label>
        <label><input type="checkbox" name="seriesFinished" value="true" ${d.seriesFinished=='true'?'checked':''}/> 🏆 Serie terminada</label>
      </div>`;

  } else if (type.includes('Pel')) {
    html = `
      <div class="form-row">
        <div class="form-group flex-grow"><label>🎬 Director</label><input type="text" name="director" ${fs} value="${d.director||''}" oninput="fetchAutoCover()"/></div>
      </div>
      <div class="check-row"><label><input type="checkbox" name="seenInCinema" value="true" ${d.seenInCinema=='true'?'checked':''}/> 🎫 Vista en el cine</label></div>`;

  } else if (type.includes('Teatro')) {
    html = `
      <div class="form-row">
        <div class="form-group flex-grow"><label>🎤 Lugar</label><input type="text" name="venue" ${fs} value="${d.venue||''}"/></div>
      </div>`;

  } else if (type.includes('mic')) {
    const single = d.isSingleVolume === 'true';
    html = `
      <div class="check-row" style="margin-bottom:10px">
        <label><input type="checkbox" name="isSingleVolume" id="singleVol" value="true" ${single?'checked':''} onchange="toggleComicFields()"/> ¿Es tomo único?</label>
      </div>
      <div id="comicNumRow" class="form-row" style="display:${single?'none':'flex'}">
        <div class="form-group"><label>📕 Nº tomo</label><input type="number" name="comicVolume" ${fs} min="1" value="${d.comicVolume||''}"/></div>
        <div class="form-group"><label>📖 Nº serie</label><input type="number" name="comicIssue"  ${fs} min="1" value="${d.comicIssue||''}"/></div>
      </div>
      <div id="comicChecks" class="check-row" style="display:${single?'none':'flex'}">
        <label><input type="checkbox" name="finished"       value="true" ${d.finished=='true'?'checked':''}/> 📘 Tomo terminado</label>
        <label><input type="checkbox" name="seriesFinished" value="true" ${d.seriesFinished=='true'?'checked':''}/> 🏆 Serie terminada</label>
      </div>`;
  }
  box.innerHTML = html;
  _hideCoverPreview();
}

function toggleComicFields() {
  const single = document.getElementById('singleVol')?.checked;
  const numRow = document.getElementById('comicNumRow');
  const checks = document.getElementById('comicChecks');
  if (numRow) numRow.style.display = single ? 'none' : 'flex';
  if (checks) checks.style.display = single ? 'none' : 'flex';
}

document.addEventListener('DOMContentLoaded', () => {
  updateDynamicFields();
  const dateIn = document.getElementById('dateInput');
  if (dateIn && !dateIn.value) dateIn.value = new Date().toISOString().split('T')[0];
  // Escuchar cambio en el título para buscar portada
  const titleEl = document.querySelector('input[name="title"]');
  if (titleEl) titleEl.addEventListener('input', fetchAutoCover);
  // Escuchar cambio de tipo
  const typeEl = document.getElementById('typeSelect');
  if (typeEl) typeEl.addEventListener('change', () => { _hideCoverPreview(); setTimeout(fetchAutoCover, 300); });
});

// ── EDIT FORM (rellena campos con datos existentes) ──────────────────
function initEditForm() {
  const box = document.getElementById('dynamicFields');
  if (!box) return;
  try {
    const raw = box.getAttribute('data-entry');
    if (!raw) return;
    const data = JSON.parse(raw);
    const sel  = document.getElementById('typeSelect');
    if (sel) {
      sel.value = data.type || '';
      updateDynamicFields(data);
    }
    const ri = document.getElementById('ratingInput');
    if (ri) initStars(parseInt(ri.value) || 0);
  } catch(e) { console.warn('initEditForm error', e); }
}
