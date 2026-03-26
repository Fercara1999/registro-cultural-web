// Chart.js - Estadísticas
(function () {
  const isDark = document.body.getAttribute('data-theme') === 'dark' ||
                 localStorage.getItem('theme') === 'dark';
  const gridColor  = isDark ? '#3b3b55' : '#e8e8f0';
  const textColor  = isDark ? '#9090aa' : '#7070aa';
  const COLORS = ['#6c5ce7','#00b4cc','#27ae60','#e67e22','#9b59b6','#e74c3c'];

  const defaults = {
    responsive: true,
    plugins: { legend: { labels: { color: textColor } } },
    scales: {
      x: { ticks: { color: textColor }, grid: { color: gridColor } },
      y: { ticks: { color: textColor }, grid: { color: gridColor }, beginAtZero: true }
    }
  };

  // Barras por tipo
  const barEl = document.getElementById('barChart');
  if (barEl && typeof porTipo !== 'undefined') {
    new Chart(barEl, {
      type: 'bar',
      data: {
        labels: Object.keys(porTipo),
        datasets: [{ data: Object.values(porTipo), backgroundColor: COLORS, borderRadius: 6 }]
      },
      options: { ...defaults, plugins: { legend: { display: false } } }
    });
  }

  // Tarta distribución
  const pieEl = document.getElementById('pieChart');
  if (pieEl && typeof porTipo !== 'undefined') {
    new Chart(pieEl, {
      type: 'doughnut',
      data: {
        labels: Object.keys(porTipo),
        datasets: [{ data: Object.values(porTipo), backgroundColor: COLORS }]
      },
      options: { responsive: true, plugins: { legend: { labels: { color: textColor } } } }
    });
  }

  // Línea actividad mensual
  const lineEl = document.getElementById('lineChart');
  if (lineEl && typeof porMes !== 'undefined') {
    new Chart(lineEl, {
      type: 'line',
      data: {
        labels: Object.keys(porMes),
        datasets: [{
          data: Object.values(porMes), borderColor: '#6c5ce7',
          backgroundColor: 'rgba(108,92,231,0.12)', fill: true, tension: 0.3, pointRadius: 4
        }]
      },
      options: defaults
    });
  }

  // Barras por día
  const dayEl = document.getElementById('dayChart');
  if (dayEl && typeof porDia !== 'undefined') {
    new Chart(dayEl, {
      type: 'bar',
      data: {
        labels: diasLbls,
        datasets: [{ data: porDia, backgroundColor: COLORS, borderRadius: 6 }]
      },
      options: { ...defaults, plugins: { legend: { display: false } } }
    });
  }
})();
