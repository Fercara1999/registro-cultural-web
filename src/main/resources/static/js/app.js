// ── TEMA ──────────────────────────────────────────────────────────
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

// ── ESTRELLAS ─────────────────────────────────────────────────
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

function syncStarsVisibility() {
  const ratingBlock = document.getElementById('ratingBlock');
  if (!ratingBlock) return;
  const finishedCb  = document.querySelector('input[name="finished"]');
  const seriesFinCb = document.querySelector('input[name="seriesFinished"]');
  const seasonFinCb = document.querySelector('input[name="seasonFinished"]');
  const hasDoneCheckbox = finishedCb || seriesFinCb || seasonFinCb;
  if (!hasDoneCheckbox) {
    ratingBlock.style.display = '';
    return;
  }
  const done = (finishedCb?.checked) || (seriesFinCb?.checked) || (seasonFinCb?.checked);
  ratingBlock.style.display = done ? '' : 'none';
  if (!done) {
    const input = document.getElementById('ratingInput');
    const label = document.getElementById('ratingVal');
    if (input) input.value = 0;
    if (label) label.textContent = 0;
    initStars(0);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  initStars();
  syncStarsVisibility();
  document.addEventListener('change', e => {
    const n = e.target?.name;
    if (n === 'finished' || n === 'seriesFinished' || n === 'seasonFinished') {
      syncStarsVisibility();
    }
  });
});

// ── PORTADA AUTOMÁTICA ─────────────────────────────────────────
let coverDebounce = null;

function fetchAutoCover() {
  clearTimeout(coverDebounce);
  coverDebounce = setTimeout(_doFetchCover, 700);
}

function _doFetchCover() {
  const titleEl = document.querySelector('input[name="title"]');
  const typeEl  = document.getElementById('typeSelect') || document.getElementById('typeSelectPending');
  if (!titleEl || !typeEl) return;
  const title = titleEl.value.trim();
  const type  = typeEl.value;
  if (title.length < 2) return;
  const hasApi = type.includes('Pel') || type.includes('Serie')
              || type.includes('Libro') || type.includes('mic');
  if (!hasApi) return;
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
    .then(data => { if (data.url) _showCoverPreview(data.url); else _hideCoverPreview(); })
    .catch(() => {});
}

function _showCoverPreview(url) {
  // Actualizar el campo hidden fijo del form
  const hiddenInput = document.getElementById('autoCoverUrl');
  if (hiddenInput) hiddenInput.value = url;

  let box = document.getElementById('autoCoverBox');
  if (!box) {
    box = document.createElement('div');
    box.id = 'autoCoverBox';
    box.innerHTML = `
      <div style="display:flex;align-items:center;gap:12px;margin-top:8px;padding:10px 14px;
                  background:var(--card);border-radius:10px;border:1.5px solid var(--accent);">
        <img id="autoCoverImg" src="" alt="portada"
             style="width:90px;height:130px;object-fit:cover;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,.18);"/>
        <div style="flex:1">
          <div style="font-size:0.85rem;font-weight:600;color:var(--accent)">🎨 Portada encontrada automáticamente</div>
          <div style="font-size:0.78rem;color:var(--muted);margin-top:4px">Se usará esta imagen al guardar.</div>
          <button type="button" onclick="_hideCoverPreview()"
                  style="margin-top:8px;font-size:0.78rem;background:transparent;border:1px solid var(--muted);border-radius:6px;padding:2px 10px;color:var(--muted);cursor:pointer;">✖ No usar esta portada</button>
        </div>
      </div>`;
    // Insertar el preview antes del input de fichero
    const fileGroup = document.querySelector('input[type="file"]')?.closest('.form-group');
    if (fileGroup) fileGroup.parentNode.insertBefore(box, fileGroup);
  }
  document.getElementById('autoCoverImg').src = url;
  box.style.display = 'block';
}

function _hideCoverPreview() {
  const box = document.getElementById('autoCoverBox');
  if (box) box.style.display = 'none';
  // Limpiar el campo hidden fijo
  const hiddenInput = document.getElementById('autoCoverUrl');
  if (hiddenInput) hiddenInput.value = '';
}

// ── CAMPOS DINÁMICOS (formulario REGISTRAR) ────────────────────────
function updateDynamicFields(prefill) {
  const sel = document.getElementById('typeSelect');
  const box = document.getElementById('dynamicFields');
  if (!sel || !box) return;
  const type = sel.value || '';
  const d = prefill || {};
  const fs = 'style="width:100%;padding:8px 12px;border-radius:8px;border:1.5px solid var(--border);background:var(--input);color:var(--text);font-size:.92rem"';
  let html = '';
  if (type.includes('Libro')) {
    html = `
      <div class="form-row">
        <div class="form-group flex-grow"><label>✍️ Autor</label><input type="text" name="author" ${fs} value="${d.author||''}" oninput="fetchAutoCover()"/></div>
        <div class="form-group"><label>📖 Capítulo leído</label><input type="number" name="chapters" ${fs} min="1" value="${d.chapters||''}"/></div>
      </div>
      <div class="check-row"><label><input type="checkbox" name="finished" value="true" ${d.finished=='true'?'checked':''} onchange="syncStarsVisibility()"/> 📘 Libro terminado</label></div>`;
  } else if (type.includes('Serie')) {
    html = `
      <div class="form-row">
        <div class="form-group"><label>📺 Temporada</label><input type="number" name="season" ${fs} min="1" value="${d.season||''}"/></div>
        <div class="form-group"><label>🎞️ Capítulo</label><input type="number" name="episode" ${fs} min="1" value="${d.episode||''}"/></div>
      </div>
      <div class="check-row">
        <label><input type="checkbox" name="seasonFinished" value="true" ${d.seasonFinished=='true'?'checked':''} onchange="syncStarsVisibility()"/> 🌟 Fin de temporada</label>
        <label><input type="checkbox" name="seriesFinished" value="true" ${d.seriesFinished=='true'?'checked':''} onchange="syncStarsVisibility()"/> 🏆 Serie terminada</label>
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
        <div class="form-group"><label>📕 Nº tomo</label><input type="number" name="comicVolume" ${fs} min="1" value="${d.comicVolume||''}" oninput="fetchAutoCover()"/></div>
        <div class="form-group"><label>📖 Nº serie</label><input type="number" name="comicIssue" ${fs} min="1" value="${d.comicIssue||''}"/></div>
      </div>
      <div id="comicChecks" class="check-row" style="display:${single?'none':'flex'}">
        <label><input type="checkbox" name="finished" value="true" ${d.finished=='true'?'checked':''} onchange="syncStarsVisibility()"/> 📘 Tomo terminado</label>
        <label><input type="checkbox" name="seriesFinished" value="true" ${d.seriesFinished=='true'?'checked':''} onchange="syncStarsVisibility()"/> 🏆 Serie terminada</label>
      </div>`;
  }
  box.innerHTML = html;
  // NO llamar a _hideCoverPreview() aquí: el valor ya está en el campo hidden fijo del form
  syncStarsVisibility();
  // Relanzar búsqueda de portada con el título actual
  fetchAutoCover();
}

function toggleComicFields() {
  const single = document.getElementById('singleVol')?.checked;
  const numRow = document.getElementById('comicNumRow');
  const checks = document.getElementById('comicChecks');
  if (numRow) numRow.style.display = single ? 'none' : 'flex';
  if (checks) checks.style.display = single ? 'none' : 'flex';
  syncStarsVisibility();
}

// ── CAMPOS DINÁMICOS (formulario PENDIENTES) ───────────────────────
function updateDynamicFieldsPending() {
  const sel = document.getElementById('typeSelectPending');
  const box = document.getElementById('dynamicFieldsPending');
  if (!sel || !box) return;
  const type = sel.value || '';
  const fs = 'style="width:100%;padding:8px 12px;border-radius:8px;border:1.5px solid var(--border);background:var(--input);color:var(--text);font-size:.92rem"';
  let html = '';
  if (type.includes('Libro')) {
    html = `<div class="form-row"><div class="form-group flex-grow"><label>✍️ Autor</label><input type="text" name="author" ${fs} oninput="fetchAutoCover()"/></div></div>`;
  } else if (type.includes('Serie')) {
    html = `<div class="form-row"><div class="form-group flex-grow"><label>📺 Título exacto</label><input type="text" name="seriesHint" ${fs} oninput="fetchAutoCover()"/></div></div>`;
  } else if (type.includes('Pel')) {
    html = `<div class="form-row"><div class="form-group flex-grow"><label>🎬 Director</label><input type="text" name="director" ${fs} oninput="fetchAutoCover()"/></div></div>`;
  } else if (type.includes('Teatro')) {
    html = `<div class="form-row"><div class="form-group flex-grow"><label>🎤 Lugar</label><input type="text" name="venue" ${fs}/></div></div>`;
  }
  box.innerHTML = html;
}

// ── DOMContentLoaded ──────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  updateDynamicFields();
  updateDynamicFieldsPending();
  const dateIn = document.getElementById('dateInput');
  if (dateIn && !dateIn.value) dateIn.value = new Date().toISOString().split('T')[0];
  const titleEl = document.querySelector('input[name="title"]');
  if (titleEl) titleEl.addEventListener('input', fetchAutoCover);
  const typeEl = document.getElementById('typeSelect');
  if (typeEl) typeEl.addEventListener('change', () => { _hideCoverPreview(); setTimeout(fetchAutoCover, 300); });
  const typeElP = document.getElementById('typeSelectPending');
  if (typeElP) typeElP.addEventListener('change', () => { _hideCoverPreview(); setTimeout(fetchAutoCover, 300); });
});

// ── EDIT FORM ─────────────────────────────────────────────────────
function initEditForm() {
  const box = document.getElementById('dynamicFields');
  if (!box) return;
  try {
    const raw = box.getAttribute('data-entry');
    if (!raw) return;
    const data = JSON.parse(raw);
    const sel  = document.getElementById('typeSelect');
    if (sel) { sel.value = data.type || ''; updateDynamicFields(data); }
    const ri = document.getElementById('ratingInput');
    if (ri) initStars(parseInt(ri.value) || 0);
  } catch(e) { console.warn('initEditForm error', e); }
}
