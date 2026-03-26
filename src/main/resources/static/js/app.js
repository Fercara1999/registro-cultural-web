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
        <div class="form-group flex-grow"><label>✍️ Autor</label><input type="text" name="author" ${fs} value="${d.author||''}"/></div>
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
        <div class="form-group flex-grow"><label>🎬 Director</label><input type="text" name="director" ${fs} value="${d.director||''}"/></div>
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
  // Set today's date as default
  const dateIn = document.getElementById('dateInput');
  if (dateIn && !dateIn.value) dateIn.value = new Date().toISOString().split('T')[0];
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
    // Estrellas con valor guardado
    const ri = document.getElementById('ratingInput');
    if (ri) initStars(parseInt(ri.value) || 0);
  } catch(e) { console.warn('initEditForm error', e); }
}
