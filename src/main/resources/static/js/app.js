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

// ── COVER DROP ZONE ────────────────────────────────────────────
function initCoverDropZone() {
  const zone = document.getElementById('coverDropZone');
  if (!zone) return;

  zone.addEventListener('dragover', e => {
    e.preventDefault();
    zone.classList.add('drag-over');
  });
  zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
  zone.addEventListener('drop', e => {
    e.preventDefault();
    zone.classList.remove('drag-over');

    // 1. Archivo soltado directamente (desde el explorador de archivos)
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      if (file.type.startsWith('image/')) {
        _setDropFile(file);
        return;
      }
    }

    // 2. URL de imagen arrastrada desde el navegador
    const url = e.dataTransfer.getData('text/uri-list') || e.dataTransfer.getData('text/plain');
    if (url && url.startsWith('http')) {
      _setDropUrl(url);
      return;
    }

    // 3. HTML con src (arrastrar imagen desde página)
    const html = e.dataTransfer.getData('text/html');
    if (html) {
      const match = html.match(/src=["']([^"']+)["']/);
      if (match && match[1].startsWith('http')) {
        _setDropUrl(match[1]);
        return;
      }
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
  const reader = new FileReader();
  reader.onload = ev => _showDropPreview(ev.target.result);
  reader.readAsDataURL(file);
}

function _setDropUrl(url) {
  const hidden = document.getElementById('autoCoverUrl');
  if (hidden) hidden.value = url;
  const fileInput = document.getElementById('coverFileInput');
  if (fileInput) fileInput.value = '';
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
  const autoCoverImg = document.getElementById('autoCoverImg');
  if (autoCoverImg && autoCoverImg.src) {
    hidden.value = autoCoverImg.src;
  }
}

function onCoverFileSelected(input) {
  if (input.files && input.files[0]) {
    _setDropFile(input.files[0]);
  }
}

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

// ── HINT: sugerencia de siguiente episodio/capítulo ──────────────────
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

  fetch('/api/entry/hint?' + new URLSearchParams({ title, type }))
    .then(r => r.json())
    .then(data => {
      const dl = document.getElementById('titleSuggestions');
      if (dl && data.titles) {
        dl.innerHTML = data.titles.map(t => `<option value="${t}"></option>`).join('');
      }
      if (type.includes('Serie')) {
        const seasonEl  = document.querySelector('input[name="season"]');
        const episodeEl = document.querySelector('input[name="episode"]');
        if (seasonEl  && data.season  != null) seasonEl.value  = data.season;
        if (episodeEl && data.episode != null) episodeEl.value = data.episode;
      } else if (type.includes('Libro')) {
        const chapEl = document.querySelector('input[name="chapters"]');
        const authEl = document.querySelector('input[name="author"]');
        if (chapEl && data.chapters != null) chapEl.value = data.chapters;
        if (authEl && data.author   != null && authEl.value === '') authEl.value = data.author;
      } else if (type.includes('mic')) {
        const volEl = document.querySelector('input[name="comicVolume"]');
        if (volEl && data.comicVolume != null) volEl.value = data.comicVolume;
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
        <div class="form-group flex-grow"><label>\u270d\ufe0f Autor</label><input type="text" name="author" ${fs} value="${d.author||''}" oninput="fetchAutoCover()"/></div>
        <div class="form-group"><label>\ud83d\udcd6 Cap\u00edtulo le\u00eddo</label><input type="number" name="chapters" ${fs} min="1" value="${d.chapters||''}"/></div>
      </div>
      <div class="check-row"><label><input type="checkbox" name="finished" value="true" ${d.finished=='true'?'checked':''} onchange="syncStarsVisibility()"/> \ud83d\udcd8 Libro terminado</label></div>`;
  } else if (type.includes('Serie')) {
    html = `
      <div class="form-row">
        <div class="form-group"><label>\ud83d\udcfa Temporada</label><input type="number" name="season" ${fs} min="1" value="${d.season||''}"/></div>
        <div class="form-group"><label>\ud83c\udf9e\ufe0f Cap\u00edtulo</label><input type="number" name="episode" ${fs} min="1" value="${d.episode||''}"/></div>
      </div>
      <div class="check-row">
        <label><input type="checkbox" name="seasonFinished" value="true" ${d.seasonFinished=='true'?'checked':''} onchange="syncStarsVisibility()"/> \ud83c\udf1f Fin de temporada</label>
        <label><input type="checkbox" name="seriesFinished" value="true" ${d.seriesFinished=='true'?'checked':''} onchange="syncStarsVisibility()"/> \ud83c\udfc6 Serie terminada</label>
      </div>`;
  } else if (type.includes('Pel')) {
    html = `
      <div class="form-row">
        <div class="form-group flex-grow"><label>\ud83c\udfac Director</label><input type="text" name="director" ${fs} value="${d.director||''}" oninput="fetchAutoCover()"/></div>
      </div>
      <div class="check-row"><label><input type="checkbox" name="seenInCinema" value="true" ${d.seenInCinema=='true'?'checked':''}"/> \ud83c\udfab Vista en el cine</label></div>`;
  } else if (type.includes('Teatro')) {
    html = `
      <div class="form-row">
        <div class="form-group flex-grow"><label>\ud83c\udfa4 Lugar</label><input type="text" name="venue" ${fs} value="${d.venue||''}"/></div>
      </div>`;
  } else if (type.includes('mic')) {
    const single = d.isSingleVolume === 'true';
    html = `
      <div class="check-row" style="margin-bottom:10px">
        <label><input type="checkbox" name="isSingleVolume" id="singleVol" value="true" ${single?'checked':''} onchange="toggleComicFields()"/> \u00bfEs tomo \u00fanico?</label>
      </div>
      <div id="comicNumRow" class="form-row" style="display:${single?'none':'flex'}">
        <div class="form-group"><label>\ud83d\udcd5 N\u00ba tomo</label><input type="number" name="comicVolume" ${fs} min="1" value="${d.comicVolume||''}" oninput="fetchAutoCover()"/></div>
        <div class="form-group"><label>\ud83d\udcd6 N\u00ba serie</label><input type="number" name="comicIssue" ${fs} min="1" value="${d.comicIssue||''}"/></div>
      </div>
      <div id="comicChecks" class="check-row" style="display:${single?'none':'flex'}">
        <label><input type="checkbox" name="finished" value="true" ${d.finished=='true'?'checked':''} onchange="syncStarsVisibility()"/> \ud83d\udcd8 Tomo terminado</label>
        <label><input type="checkbox" name="seriesFinished" value="true" ${d.seriesFinished=='true'?'checked':''} onchange="syncStarsVisibility()"/> \ud83c\udfc6 Serie terminada</label>
      </div>`;
  }
  if (type) { box.style.display = ''; box.innerHTML = html; }
  else       { box.style.display = 'none'; box.innerHTML = ''; }
  syncStarsVisibility();
  fetchAutoCover();
  fetchEntryHint();
}

function toggleComicFields() {
  const single = document.getElementById('singleVol')?.checked;
  const numRow = document.getElementById('comicNumRow');
  const checks = document.getElementById('comicChecks');
  if (numRow) numRow.style.display = single ? 'none' : 'flex';
  if (checks) checks.style.display = single ? 'none' : 'flex';
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
  if (type.includes('Libro'))       html = `<div class="form-row"><div class="form-group flex-grow"><label>\u270d\ufe0f Autor</label><input type="text" name="author" ${fs} oninput="fetchAutoCover()"/></div></div>`;
  else if (type.includes('Serie'))  html = `<div class="form-row"><div class="form-group flex-grow"><label>\ud83d\udcfa T\u00edtulo exacto</label><input type="text" name="seriesHint" ${fs} oninput="fetchAutoCover()"/></div></div>`;
  else if (type.includes('Pel'))    html = `<div class="form-row"><div class="form-group flex-grow"><label>\ud83c\udfac Director</label><input type="text" name="director" ${fs} oninput="fetchAutoCover()"/></div></div>`;
  else if (type.includes('Teatro')) html = `<div class="form-row"><div class="form-group flex-grow"><label>\ud83c\udfa4 Lugar</label><input type="text" name="venue" ${fs}/></div></div>`;
  box.innerHTML = html;
}

// ── DOMContentLoaded ──────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  updateDynamicFields();
  updateDynamicFieldsPending();
  const dateIn = document.getElementById('dateInput');
  if (dateIn && !dateIn.value) dateIn.value = new Date().toISOString().split('T')[0];
  const titleEl = document.querySelector('input[name="title"]');
  if (titleEl) titleEl.addEventListener('input', () => { fetchAutoCover(); fetchEntryHint(); });
  const typeEl = document.getElementById('typeSelect');
  if (typeEl) typeEl.addEventListener('change', () => { _hideCoverPreview(); setTimeout(() => { fetchAutoCover(); fetchEntryHint(); }, 300); });
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
