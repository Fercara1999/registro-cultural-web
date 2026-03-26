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
    s.addEventListener('mouseenter', () => { highlight(i); if (label) label.textContent = i; });
    s.addEventListener('mouseleave', () => { highlight(current); if (label) label.textContent = current; });
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
  if (!hasDoneCheckbox) { ratingBlock.style.display = ''; return; }
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
    if (n === 'finished' || n === 'seriesFinished' || n === 'seasonFinished') syncStarsVisibility();
  });
  initCoverDropZone();
});

// ── helpers drop zone visibility ────────────────────────────────
function _showDropZone() {
  const wrap = document.getElementById('coverDropZone')?.parentElement;
  if (wrap) wrap.style.display = '';
}
function _hideDropZone() {
  const wrap = document.getElementById('coverDropZone')?.parentElement;
  if (wrap) wrap.style.display = 'none';
}

// ── COVER DROP ZONE ────────────────────────────────────────────
function initCoverDropZone() {
  const zone = document.getElementById('coverDropZone');
  if (!zone) return;
  zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('drag-over'); });
  zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
  zone.addEventListener('drop', e => {
    e.preventDefault();
    zone.classList.remove('drag-over');
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      if (file.type.startsWith('image/')) { _setDropFile(file); return; }
    }
    const url = e.dataTransfer.getData('text/uri-list') || e.dataTransfer.getData('text/plain');
    if (url && url.startsWith('http')) { _setDropUrl(url); return; }
    const html = e.dataTransfer.getData('text/html');
    if (html) {
      const match = html.match(/src=["']([^"']+)["']/);
      if (match && match[1].startsWith('http')) { _setDropUrl(match[1]); return; }
    }
  });
}

function _setDropFile(file) {
  const dt = new DataTransfer();
  dt.items.add(file);
  const fileInput = document.getElementById('coverFileInput');
  if (fileInput) fileInput.files = dt.files;
  const hidden = document.getElementById('autoCoverUrl');
  if (hidden) hidden.value = '';
  _clearExistingCoverPath();
  const reader = new FileReader();
  reader.onload = ev => _showDropPreview(ev.target.result);
  reader.readAsDataURL(file);
}

function _setDropUrl(url) {
  const hidden = document.getElementById('autoCoverUrl');
  if (hidden) hidden.value = url;
  const fileInput = document.getElementById('coverFileInput');
  if (fileInput) fileInput.value = '';
  _clearExistingCoverPath();
  _showDropPreview(url);
}

function _showDropPreview(src) {
  const zone     = document.getElementById('coverDropZone');
  const label    = document.getElementById('coverDropLabel');
  const preview  = document.getElementById('coverDropPreview');
  const clearBtn = document.getElementById('coverDropClear');
  if (!zone || !preview) return;
  preview.src = src;
  preview.style.display = 'block';
  if (label)    label.style.display    = 'none';
  if (clearBtn) clearBtn.style.display = 'block';
}

function clearCoverDrop(event) {
  if (event) event.stopPropagation();
  const fileInput = document.getElementById('coverFileInput');
  const hidden    = document.getElementById('autoCoverUrl');
  const preview   = document.getElementById('coverDropPreview');
  const label     = document.getElementById('coverDropLabel');
  const clearBtn  = document.getElementById('coverDropClear');
  if (fileInput) fileInput.value = '';
  if (hidden)    hidden.value    = '';
  if (preview)   { preview.src = ''; preview.style.display = 'none'; }
  if (label)     label.style.display    = 'block';
  if (clearBtn)  clearBtn.style.display = 'none';
  _clearExistingCoverPath();
  const autoCoverImg = document.getElementById('autoCoverImg');
  if (autoCoverImg && autoCoverImg.src) hidden.value = autoCoverImg.src;
}

function onCoverFileSelected(input) {
  if (input.files && input.files[0]) _setDropFile(input.files[0]);
}

function _clearExistingCoverPath() {
  const el = document.getElementById('existingCoverPath');
  if (el) el.value = '';
}

// ── PORTADA AUTOMÁTICA ─────────────────────────────────────────
let coverDebounce = null;

function fetchAutoCover() {
  clearTimeout(coverDebounce);
  coverDebounce = setTimeout(_doFetchCover, 700);
}

function _doFetchCover() {
  // Si ya hay portada existente activa, no buscar
  const existingPath = document.getElementById('existingCoverPath');
  if (existingPath && existingPath.value.trim()) return;

  const titleEl = document.querySelector('input[name="title"]');
  const typeEl  = document.getElementById('typeSelect') || document.getElementById('typeSelectPending');
  if (!titleEl || !typeEl) return;
  const title = titleEl.value.trim();
  const type  = typeEl.value;
  if (title.length < 2) return;
  const hasApi = type.includes('Pel') || type.includes('Serie') || type.includes('Libro') || type.includes('mic');
  if (!hasApi) return;
  let extra = '';
  if (type.includes('Pel')) {
    const dirEl = document.querySelector('input[name="director"]');
    if (dirEl) extra = dirEl.value.split(',')[0].trim();
  } else if (type.includes('Libro')) {
    const authEl = document.querySelector('input[name="author"]');
    if (authEl) extra = authEl.value.trim();
  }
  const params = new URLSearchParams({ type, title, extra });
  fetch('/api/cover/search?' + params)
    .then(r => r.json())
    .then(data => {
      // Doble check: si mientras esperaba la respuesta se activó una portada existente, ignorar
      const ep = document.getElementById('existingCoverPath');
      if (ep && ep.value.trim()) return;
      if (data.url) _showCoverPreview(data.url); else _hideCoverPreview();
    })
    .catch(() => {});
}

function _showCoverPreview(url) {
  const hiddenInput = document.getElementById('autoCoverUrl');
  const fileInput = document.getElementById('coverFileInput');
  const hasManual = (fileInput && fileInput.files && fileInput.files.length > 0)
                  || document.getElementById('coverDropPreview')?.style.display === 'block';
  if (!hasManual && hiddenInput) hiddenInput.value = url;
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
    const dropZone = document.getElementById('coverDropZone');
    if (dropZone) dropZone.parentNode.insertBefore(box, dropZone);
  }
  document.getElementById('autoCoverImg').src = url;
  box.style.display = 'block';
}

function _hideCoverPreview() {
  const box = document.getElementById('autoCoverBox');
  if (box) box.style.display = 'none';
  const hiddenInput = document.getElementById('autoCoverUrl');
  if (hiddenInput) hiddenInput.value = '';
}

// ── PORTADA EXISTENTE (reutilizar de registro previo) ────────────────
function _showExistingCoverHint(coverPath) {
  clearTimeout(coverDebounce);  // cancelar cualquier búsqueda de TMDB pendiente
  _hideCoverPreview();
  _hideDropZone();
  const existing = document.getElementById('existingCoverPath');
  if (existing) existing.value = coverPath;

  let box = document.getElementById('existingCoverBox');
  if (!box) {
    box = document.createElement('div');
    box.id = 'existingCoverBox';
    box.innerHTML = `
      <div style="display:flex;align-items:center;gap:12px;margin-top:8px;padding:10px 14px;
                  background:var(--card);border-radius:10px;border:1.5px solid var(--border);">
        <img id="existingCoverImg" src="" alt="portada existente"
             style="width:90px;height:130px;object-fit:cover;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,.18);"/>
        <div style="flex:1">
          <div style="font-size:0.85rem;font-weight:600;color:var(--text)">📂 Portada del registro anterior</div>
          <div style="font-size:0.78rem;color:var(--muted);margin-top:4px">Se reutilizará automáticamente.</div>
          <button type="button" onclick="_clearExistingCover()"
                  style="margin-top:8px;font-size:0.78rem;background:transparent;border:1px solid var(--muted);border-radius:6px;padding:2px 10px;color:var(--muted);cursor:pointer;">✖ No usar esta portada</button>
        </div>
      </div>`;
    const dropZone = document.getElementById('coverDropZone');
    if (dropZone) dropZone.parentNode.insertBefore(box, dropZone);
  }
  document.getElementById('existingCoverImg').src = '/covers/' + coverPath;
  box.style.display = 'block';
}

function _hideExistingCoverHint() {
  const box = document.getElementById('existingCoverBox');
  if (box) box.style.display = 'none';
  _clearExistingCoverPath();
}

function _clearExistingCover() {
  _hideExistingCoverHint();
  _showDropZone();
  fetchAutoCover();
}

// ── HINT ─────────────────────────────────────────────────────────
let hintDebounce = null;

function fetchEntryHint() {
  clearTimeout(hintDebounce);
  hintDebounce = setTimeout(_doFetchHint, 500);
}

function _doFetchHint() {
  const titleEl = document.querySelector('input[name="title"]');
  const typeEl  = document.getElementById('typeSelect');
  if (!titleEl || !typeEl) return;
  const title = titleEl.value.trim();
  const type  = typeEl.value;
  if (!type || (!type.includes('Serie') && !type.includes('Libro') && !type.includes('mic'))) return;

  const seasonEl  = document.querySelector('input[name="season"]');
  const seasonVal = seasonEl && seasonEl.value ? parseInt(seasonEl.value) : null;
  const params    = new URLSearchParams({ title, type });
  if (seasonVal) params.set('season', seasonVal);

  fetch('/api/entry/hint?' + params)
    .then(r => r.json())
    .then(data => {
      const dl = document.getElementById('titleSuggestions');
      if (dl && data.titles) dl.innerHTML = data.titles.map(t => `<option value="${t}"></option>`).join('');

      if (data.coverLocalPath) {
        // Hay portada existente: mostrarla y NO buscar por API
        _showExistingCoverHint(data.coverLocalPath);
      } else {
        // No hay portada existente: ocultar hint y dejar que TMDB busque
        _hideExistingCoverHint();
        _showDropZone();
        fetchAutoCover();
      }

      if (type.includes('Serie')) {
        const seasonEl2 = document.querySelector('input[name="season"]');
        const episodeEl = document.querySelector('input[name="episode"]');
        if (seasonEl2 && data.season  != null) seasonEl2.value = data.season;
        if (episodeEl && data.episode != null) episodeEl.value = data.episode;
      } else if (type.includes('Libro')) {
        const chapEl = document.querySelector('input[name="chapters"]');
        const authEl = document.querySelector('input[name="author"]');
        if (chapEl && data.chapters != null) chapEl.value = data.chapters;
        if (authEl && data.author   != null && authEl.value === '') authEl.value = data.author;
      } else if (type.includes('mic')) {
        const volEl   = document.querySelector('input[name="comicVolume"]');
        const issueEl = document.getElementById('comicIssueInput');
        if (volEl   && data.comicVolume != null) volEl.value   = data.comicVolume;
        if (issueEl && data.comicIssue  != null) issueEl.value = data.comicIssue;
      }
    })
    .catch(() => {});
}

// ── CAMPOS DINÁMICOS (REGISTRAR) ─────────────────────────────────
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
    const epVal = d.episode != null ? String(d.episode) : '';
    html = `
      <div class="form-row">
        <div class="form-group"><label>📺 Temporada</label>
          <input type="number" name="season" ${fs} min="1" value="${d.season||''}" oninput="fetchEntryHint()"/>
        </div>
        <div class="form-group">
          <label>🎞️ Capítulo <small style="color:var(--muted);font-weight:400">(p.ej: 3 ó 3-5 ó 3,4)</small></label>
          <input type="text" name="episode" id="episodeInput" ${fs} placeholder="3 ó 3-5 ó 3,4" value="${epVal}" oninput="previewEpisodes()"/>
          <span id="episodePreview" style="font-size:0.75rem;color:var(--accent);margin-top:3px"></span>
        </div>
      </div>
      <div class="check-row">
        <label><input type="checkbox" name="seasonFinished" value="true" ${d.seasonFinished=='true'?'checked':''} onchange="syncStarsVisibility()"/> 🌟 Fin de temporada</label>
        <label><input type="checkbox" name="seriesFinished" value="true" ${d.seriesFinished=='true'?'checked':''} onchange="syncStarsVisibility()"/> 🏆 Serie terminada</label>
      </div>`;
  } else if (type.includes('Pel')) {
    html = `
      <div class="form-row">
        <div class="form-group flex-grow">
          <label>🎬 Director(es) <small style="color:var(--muted);font-weight:400">(varios separados por coma)</small></label>
          <input type="text" name="director" ${fs} placeholder="Ej: Spielberg, Kubrick" value="${d.director||''}" oninput="fetchAutoCover()"/>
        </div>
      </div>
      <div class="check-row"><label><input type="checkbox" name="seenInCinema" value="true" ${d.seenInCinema=='true'?'checked':''}/> 🎫 Vista en el cine</label></div>`;
  } else if (type.includes('Teatro')) {
    html = `
      <div class="form-row">
        <div class="form-group flex-grow"><label>🎤 Lugar</label><input type="text" name="venue" ${fs} value="${d.venue||''}"/></div>
      </div>`;
  } else if (type.includes('mic')) {
    const single = d.isSingleVolume === 'true';
    const issueVal = d.comicIssue != null ? String(d.comicIssue) : '';
    html = `
      <div class="check-row" style="margin-bottom:10px">
        <label><input type="checkbox" name="isSingleVolume" id="singleVol" value="true" ${single?'checked':''} onchange="toggleComicFields()"/> ¿Es tomo único?</label>
      </div>
      <div class="form-row">
        <div id="comicVolumeGroup" class="form-group" style="display:${single?'none':'flex'};flex-direction:column">
          <label>📕 Nº tomo</label>
          <input type="number" name="comicVolume" ${fs} min="1" value="${d.comicVolume||''}" oninput="fetchAutoCover()"/>
        </div>
        <div class="form-group">
          <label>📖 Nº serie <small style="color:var(--muted);font-weight:400">(p.ej: 3 ó 3-5 ó 3,4,5)</small></label>
          <input type="text" name="comicIssue" id="comicIssueInput" ${fs} placeholder="3 ó 3-5 ó 3,4" value="${issueVal}" oninput="previewComicIssues()"/>
          <span id="comicIssuePreview" style="font-size:0.75rem;color:var(--accent);margin-top:3px"></span>
        </div>
      </div>
      <div id="comicChecks" class="check-row">
        <label><input type="checkbox" name="finished" value="true" ${d.finished=='true'?'checked':''} onchange="syncStarsVisibility()"/> 📘 Tomo terminado</label>
        <span id="comicSeriesFinBlock" style="display:${single?'none':'inline'}">
          <label><input type="checkbox" name="seriesFinished" value="true" ${d.seriesFinished=='true'?'checked':''} onchange="syncStarsVisibility()"/> 🏆 Serie terminada</label>
        </span>
      </div>`;
  }
  if (type) { box.style.display = ''; box.innerHTML = html; }
  else       { box.style.display = 'none'; box.innerHTML = ''; }
  syncStarsVisibility();

  // Para tipos con hint (Serie/Libro/Cómic): el hint decide si lanzar fetchAutoCover
  // Para tipos sin hint (Película/Teatro): lanzar fetchAutoCover directamente
  const usesHint = type.includes('Serie') || type.includes('Libro') || type.includes('mic');
  if (usesHint) {
    fetchEntryHint(); // el hint llamará a fetchAutoCover solo si no hay portada existente
  } else {
    fetchAutoCover();
    fetchEntryHint();
  }
}

function previewEpisodes() {
  const input   = document.getElementById('episodeInput');
  const preview = document.getElementById('episodePreview');
  if (!input || !preview) return;
  const nums = parseIssueRange(input.value);
  if (nums.length <= 1) { preview.textContent = ''; return; }
  preview.textContent = `ℹ️ Se crearán ${nums.length} registros: cap. ${nums.join(', ')}`;
}

function previewComicIssues() {
  const input   = document.getElementById('comicIssueInput');
  const preview = document.getElementById('comicIssuePreview');
  if (!input || !preview) return;
  const nums = parseIssueRange(input.value);
  if (nums.length <= 1) { preview.textContent = ''; return; }
  preview.textContent = `ℹ️ Se crearán ${nums.length} registros: nº ${nums.join(', ')}`;
}

function parseIssueRange(val) {
  if (!val || !val.trim()) return [];
  val = val.trim();
  const rangeMatch = val.match(/^(\d+)-(\d+)$/);
  if (rangeMatch) {
    const from = parseInt(rangeMatch[1]), to = parseInt(rangeMatch[2]);
    if (from > to || to - from > 100) return [];
    const arr = [];
    for (let i = from; i <= to; i++) arr.push(i);
    return arr;
  }
  const parts = val.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n));
  if (parts.length > 0) return parts;
  const single = parseInt(val);
  return isNaN(single) ? [] : [single];
}

function toggleComicFields() {
  const single         = document.getElementById('singleVol')?.checked;
  const volumeGroup    = document.getElementById('comicVolumeGroup');
  const seriesFinBlock = document.getElementById('comicSeriesFinBlock');
  if (volumeGroup)     volumeGroup.style.display     = single ? 'none' : 'flex';
  if (seriesFinBlock)  seriesFinBlock.style.display  = single ? 'none' : 'inline';
  syncStarsVisibility();
}

// ── CAMPOS DINÁMICOS (PENDIENTES) ─────────────────────────────────
function updateDynamicFieldsPending() {
  const sel = document.getElementById('typeSelectPending');
  const box = document.getElementById('dynamicFieldsPending');
  if (!sel || !box) return;
  const type = sel.value || '';
  const fs = 'style="width:100%;padding:8px 12px;border-radius:8px;border:1.5px solid var(--border);background:var(--input);color:var(--text);font-size:.92rem"';
  let html = '';
  if (type.includes('Libro'))       html = `<div class="form-row"><div class="form-group flex-grow"><label>✍️ Autor</label><input type="text" name="author" ${fs} oninput="fetchAutoCover()"/></div></div>`;
  else if (type.includes('Serie'))  html = `<div class="form-row"><div class="form-group flex-grow"><label>📺 Título exacto</label><input type="text" name="seriesHint" ${fs} oninput="fetchAutoCover()"/></div></div>`;
  else if (type.includes('Pel'))    html = `<div class="form-row"><div class="form-group flex-grow"><label>🎬 Director(es) <small style="color:var(--muted);font-weight:400">(varios separados por coma)</small></label><input type="text" name="director" ${fs} placeholder="Ej: Spielberg, Kubrick" oninput="fetchAutoCover()"/></div></div>`;
  else if (type.includes('Teatro')) html = `<div class="form-row"><div class="form-group flex-grow"><label>🎤 Lugar</label><input type="text" name="venue" ${fs}/></div></div>`;
  box.innerHTML = html;
}

// ── DOMContentLoaded ──────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  updateDynamicFields();
  updateDynamicFieldsPending();
  const dateIn = document.getElementById('dateInput');
  if (dateIn && !dateIn.value) dateIn.value = new Date().toISOString().split('T')[0];
  const titleEl = document.querySelector('input[name="title"]');
  if (titleEl) titleEl.addEventListener('input', () => {
    const type = document.getElementById('typeSelect')?.value || '';
    const usesHint = type.includes('Serie') || type.includes('Libro') || type.includes('mic');
    if (usesHint) {
      fetchEntryHint(); // el hint decidirá si lanzar fetchAutoCover
    } else {
      fetchAutoCover();
      fetchEntryHint();
    }
  });
  const typeEl = document.getElementById('typeSelect');
  if (typeEl) typeEl.addEventListener('change', () => {
    _hideCoverPreview(); _hideExistingCoverHint(); _showDropZone();
    setTimeout(() => updateDynamicFields(), 50);
  });
  const typeElP = document.getElementById('typeSelectPending');
  if (typeElP) typeElP.addEventListener('change', () => { _hideCoverPreview(); setTimeout(fetchAutoCover, 300); });
  initCoverDropZone();
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
