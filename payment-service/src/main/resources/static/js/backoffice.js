document.addEventListener('DOMContentLoaded', function () {
  // enable bootstrap tooltips
  var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
  tooltipTriggerList.map(function (tooltipTriggerEl) {
    return new bootstrap.Tooltip(tooltipTriggerEl)
  })

  // Export visible rows to CSV
  var exportBtn = document.getElementById('exportCsvBtn');
  if (exportBtn) {
    exportBtn.addEventListener('click', function () {
      var rows = Array.from(document.querySelectorAll('table tbody tr'));
      var csv = [];
      csv.push(['ID','Fecha','Monto','Estado','Cuenta Origen','Tarjeta','Cuenta Destino'].join(','));
      rows.forEach(function (r) {
        var cols = r.querySelectorAll('td');
        if (!cols || cols.length === 0) return;
        var values = [
          cols[0].innerText.trim(),
          cols[1].innerText.trim(),
          cols[2].innerText.trim(),
          cols[3].innerText.trim(),
          cols[4].innerText.trim(),
          cols[5].innerText.trim(),
          cols[6].innerText.trim()
        ];
        csv.push(values.map(function(v){ return '"'+v.replace(/"/g,'""')+'"'; }).join(','));
      });
      var blob = new Blob([csv.join('\n')], {type: 'text/csv;charset=utf-8;'});
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url;
      a.download = 'payments_export.csv';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    });
  }
});
