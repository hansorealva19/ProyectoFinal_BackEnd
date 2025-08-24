(function(){
  // helper to POST formdata-like via fetch and return json
  function postJson(url, data){
    var form = new URLSearchParams();
    for(var k in data) form.append(k, data[k]);
    return fetch(url, { method: 'POST', body: form, credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } }).then(function(r){ if(!r.ok) throw r; return r.json(); });
  }

  // small toast helper (uses same container as other scripts)
  function showToast(message, variant){
    variant = variant || 'danger';
    var container = document.getElementById('ecom-toast-container');
    if(!container){
      // create container if missing
      container = document.createElement('div'); container.id = 'ecom-toast-container'; container.style.position='fixed'; container.style.right='16px'; container.style.bottom='16px'; container.style.zIndex='1080'; document.body.appendChild(container);
    }
    var id = 't' + Date.now();
    var html = '<div id="'+id+'" class="toast align-items-center text-bg-'+variant+' border-0 mb-2" role="alert" aria-live="assertive" aria-atomic="true">'
              + '<div class="d-flex"><div class="toast-body">'+message+'</div>'
              + '<button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div></div>';
    container.insertAdjacentHTML('beforeend', html);
    var el = document.getElementById(id);
    try{ var bs = bootstrap.Toast.getOrCreateInstance(el, { delay: 3500 }); bs.show(); el.addEventListener('hidden.bs.toast', function(){ el.remove(); }); }catch(e){ setTimeout(function(){ el.remove(); }, 3500); }
  }

  // helper: show/hide a small 'Stock' badge next to the increment button when max reached
  function toggleStockBadge(row, show){
    try{
      var inc = row.querySelector('.cart-inc-btn');
      if(!inc) return;
      var existing = row.querySelector('.ecom-stock-badge');
      if(show){
        if(existing) return;
        var span = document.createElement('span');
        // use warning style so it's visually distinct (yellow) and accessible
        span.className = 'badge bg-warning text-dark ms-1 ecom-stock-badge';
        span.setAttribute('title', 'Stock agotado');
        span.setAttribute('aria-label', 'Stock agotado');
        span.textContent = 'Agotado';
        inc.insertAdjacentElement('afterend', span);
      } else {
        if(existing) existing.remove();
      }
    }catch(e){}
  }

  // helper: update inc/dec disabled state and badge for a row based on input value and max
  function updateIncDecState(row){
    try{
      var input = row.querySelector('.cart-qty-input');
      if(!input) return;
      var val = Number(input.value || 0);
      var max = Number(input.max || 0);
      var inc = row.querySelector('.cart-inc-btn');
      var dec = row.querySelector('.cart-dec-btn');
  if(inc) inc.disabled = (max > 0 && val >= max);
  // do not disable decrement button to allow user to always reduce quantity
      toggleStockBadge(row, (max > 0 && val >= max));
    }catch(e){}
  }

  function updateRowCount(btn){
    var trigger = btn;
    var row = trigger.closest('tr');
    if(!row) return;
    var pid = row.getAttribute('data-pid');
    var input = row.querySelector('.cart-qty-input');
    var prev = Number(input.getAttribute('data-prev') || input.value || 0);
    var desired = prev;
    // detect if trigger is an inc/dec button or the input itself
    if(trigger && trigger.tagName === 'BUTTON'){
      if(trigger.classList.contains('cart-inc-btn')) desired = prev + 1;
      else if(trigger.classList.contains('cart-dec-btn')) desired = Math.max(0, prev - 1);
    } else {
      // trigger could be the input element (on change) - use typed value but don't commit UI until confirmed
      desired = Math.max(0, parseInt(input.value || String(prev), 10));
    }
    if(desired === prev) return; // nothing to do
    // client-side: respect input.max (stock) if provided
    var max = Number(input.max || 0);
    if(max > 0 && desired > max){
      // if user attempted to increase beyond stock, show friendly message and do nothing
      showToast('La cantidad seleccionada supera el stock', 'danger');
      // reset visible value back to previous known-good value
      input.value = prev;
      try { input.setAttribute('data-prev', String(prev)); } catch(e){}
      return;
    }
    // If the desired quantity is zero, show the confirmation modal first and
    // only call the remove endpoint when the user confirms. Do NOT commit the
    // UI change (or update data-prev) until removal is confirmed. If the
    // user cancels, restore the previous value and re-enable controls.
    if(desired === 0){
      try{
        var modalEl = document.getElementById('confirmRemoveModal');
        if(modalEl){
          var msg = modalEl.querySelector('#confirmRemoveMessage'); if(msg) msg.textContent = 'Eliminar este producto del carrito?';
          var confirmBtn = modalEl.querySelector('#confirmRemoveBtn');
          // replace button to clear previous listeners
          var newBtn = confirmBtn.cloneNode(true);
          confirmBtn.parentNode.replaceChild(newBtn, confirmBtn);
          // ensure controls are disabled while modal is open to avoid races
          try { input.disabled = true; row.querySelectorAll('button').forEach(function(b){ b.disabled = true; }); } catch(e){}
          newBtn.addEventListener('click', function(){
            postJson('/cart/remove', { productId: pid }).then(function(j){
              // update totals and badge
              if(j && j.cartTotal !== undefined){ var totalEl = document.querySelector('#cart-total-val'); if(totalEl) totalEl.textContent = 'S/ ' + (Number(j.cartTotal) || 0).toFixed(2); }
              var badge = document.getElementById('cart-badge'); if(badge){ if(j.count <= 0) badge.remove(); else badge.textContent = j.count < 100 ? String(j.count) : '99+'; }
              row.remove(); showToast('Art\u00edculo eliminado', 'success');
            }).catch(function(){ showToast('No se pudo eliminar el art\u00edculo', 'danger'); })
            .finally(function(){ var bs = bootstrap.Modal.getInstance(modalEl); if(bs) bs.hide(); });
          });
          // when modal is hidden (user cancelled or after confirm), re-enable controls
          var bs = new bootstrap.Modal(modalEl);
          modalEl.addEventListener('hidden.bs.modal', function onHide(){
            modalEl.removeEventListener('hidden.bs.modal', onHide);
            try { input.disabled = false; row.querySelectorAll('button').forEach(function(b){ b.disabled = false; }); } catch(e){}
            // restore previous visible value since we didn't commit the 0 change
            try{ input.value = String(prev); input.setAttribute('data-prev', String(prev)); }catch(e){}
            try{ updateIncDecState(row); }catch(e){}
          });
          bs.show();
          return;
        }
      }catch(e){ /* fall through to removal without modal */ }
      // If modal is not available for some reason, fall back to immediate remove
      try { input.disabled = true; row.querySelectorAll('button').forEach(function(b){ b.disabled = true; }); } catch(e){}
      postJson('/cart/remove', { productId: pid }).then(function(j){ if(j && j.cartTotal !== undefined){ var totalEl = document.querySelector('#cart-total-val'); if(totalEl) totalEl.textContent = 'S/ ' + (Number(j.cartTotal) || 0).toFixed(2); } var badge = document.getElementById('cart-badge'); if(badge){ if(j.count <= 0) badge.remove(); else badge.textContent = j.count < 100 ? String(j.count) : '99+'; } row.remove(); showToast('Art\u00edculo eliminado', 'success'); }).catch(function(){ showToast('No se pudo eliminar el art\u00edculo', 'danger'); }).finally(function(){ try { input.disabled = false; row.querySelectorAll('button').forEach(function(b){ b.disabled = false; }); } catch(e){} });
      return;
    }

    // disable controls while pending for regular updates (non-zero)
    try { input.disabled = true; row.querySelectorAll('button').forEach(function(b){ b.disabled = true; }); } catch(e){}
    // send update (confirmatory): only update UI after success
    postJson('/cart/update', { productId: pid, quantity: desired }).then(function(j){
      // update totals
      if(j.cartTotal !== undefined){
        var totalEl = document.querySelector('#cart-total-val');
        if(totalEl) totalEl.textContent = 'S/ ' + (Number(j.cartTotal) || 0).toFixed(2);
      }
      var badge = document.getElementById('cart-badge');
      if(!badge && j.count > 0){
        var cartLink = document.querySelector('a[href="/cart"]');
        if(cartLink){ badge = document.createElement('span'); badge.id = 'cart-badge'; badge.className = 'position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger'; badge.style.fontSize='0.75rem'; cartLink.appendChild(badge); }
      }
      if(badge){ if(j.count <= 0) badge.remove(); else badge.textContent = j.count < 100 ? String(j.count) : '99+'; }
      // commit the UI change (only for non-zero updates)
      input.value = desired;
      input.setAttribute('data-prev', String(desired));
      // re-enable controls and update inc/dec state
      try { input.disabled = false; row.querySelectorAll('button').forEach(function(b){ b.disabled = false; }); } catch(e){}
      try{ updateIncDecState(row); }catch(e){}
      showToast('Cantidad actualizada', 'success');
    }).catch(function(err){
      // re-enable controls
      try { input.disabled = false; row.querySelectorAll('button').forEach(function(b){ b.disabled = false; }); } catch(e){}
      // extract friendly message
      var friendly = 'No se pudo actualizar el carrito';
      try {
        if (err && err.json) {
          err.json().then(function(body){
            var msg = (body && body.message) ? body.message : '';
            if (/stock|sin stock|exceed|excede|supera|cantidad/i.test(msg)) friendly = 'La cantidad seleccionada supera el stock';
            showToast(friendly, 'danger');
          }).catch(function(){ showToast(friendly, 'danger'); });
          return;
        }
      } catch(e) {}
  var raw = err && err.message ? err.message : '';
  try{ updateIncDecState(row); }catch(e){}
      if (/stock|sin stock|exceed|excede|supera|cantidad/i.test(raw)) friendly = 'La cantidad seleccionada supera el stock';
      showToast(friendly, 'danger');
    });
  }

  function removeRow(button){
    var row = button && button.closest ? button.closest('tr') : null;
    if(!row) return; // nothing to remove
    var pid = row.getAttribute('data-pid');
    postJson('/cart/remove', { productId: pid }).then(function(j){
      var badge = document.getElementById('cart-badge');
      if(!badge && j.count > 0){
        var cartLink = document.querySelector('a[href="/cart"]');
        if(cartLink){ badge = document.createElement('span'); badge.id = 'cart-badge'; badge.className = 'position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger'; badge.style.fontSize='0.75rem'; cartLink.appendChild(badge); }
      }
      if(badge){ if(j.count <= 0) badge.remove(); else badge.textContent = j.count < 100 ? String(j.count) : '99+'; }
  var totalEl = document.querySelector('#cart-total-val');
  if(totalEl) totalEl.textContent = 'S/ ' + (Number(j.cartTotal) || 0).toFixed(2);
  row.remove();
  }).catch(function(){ showToast('No se pudo eliminar el artículo del carrito', 'danger'); });
  }

  function wire(){
    // event delegation on table body so dynamically added/changed rows still work
    var tbody = document.querySelector('table.table tbody');
    if(tbody){
      tbody.addEventListener('click', function(e){
        var t = e.target;
        // find closest button if click on inner element
        var btn = t.closest && t.closest('button');
        if(btn && btn.classList.contains('cart-inc-btn') ) { updateRowCount(btn); return; }
        if(btn && btn.classList.contains('cart-dec-btn') ) { updateRowCount(btn); return; }
        if(btn && btn.classList.contains('cart-remove-btn') ) { removeRow(btn); return; }
      });
      tbody.addEventListener('change', function(e){
        var t = e.target;
        if(t && t.classList && t.classList.contains('cart-qty-input')){
          try{
            var raw = parseInt(t.value, 10);
            if(isNaN(raw)) raw = 0;
            if(raw < 0){
              t.value = 0;
              showToast('La cantidad mínima es 0', 'warning');
            }
          }catch(ex){}
          updateRowCount(t);
          // after change, adjust inc/dec button disabled state based on max
          try{ var row = t.closest('tr'); updateIncDecState(row); }catch(e){}
        }
      });
    }
    // initial pass: ensure inc/dec buttons reflect current quantity vs stock (input.max)
    try{
      document.querySelectorAll('table.table tbody tr[data-pid]').forEach(function(row){
        var input = row.querySelector('.cart-qty-input');
        if(!input) return;
        var val = Number(input.value || 0);
        // store prev if missing
        if(!input.getAttribute('data-prev')) input.setAttribute('data-prev', String(val));
        // delegate to centralized state updater
        updateIncDecState(row);
      });
  // (use event delegation on tbody to handle button clicks; no per-button listeners needed)
    }catch(e){}
    var clearBtn = document.getElementById('cart-clear-btn');
  if(clearBtn) clearBtn.addEventListener('click', function(){
    try {
      // If user is authenticated, attempt to cancel any pending orders for this user
      // We rely on a server-side session attribute USERNAME; call a backend endpoint that will forward to order-service
      fetch('/internal/cancel-pending-before-clear', { method: 'POST', credentials: 'same-origin', headers: { 'X-Requested-With':'XMLHttpRequest' } })
        .catch(function(){ /* non-fatal, continue to clear cart even if cancel failed */ });
    } catch(e) { /* ignore */ }
    fetch('/cart/clear', { method: 'POST', credentials: 'same-origin', headers: { 'X-Requested-With':'XMLHttpRequest' } }).then(function(r){ if(!r.ok) throw r; return r.json(); }).then(function(j){ document.querySelectorAll('tr[data-pid]').forEach(function(tr){ tr.remove(); }); var totalEl = document.querySelector('#cart-total-val'); if(totalEl) totalEl.textContent = '0.00'; var badge = document.getElementById('cart-badge'); if(!badge && j.count > 0){ var cartLink = document.querySelector('a[href="/cart"]'); if(cartLink){ badge = document.createElement('span'); badge.id = 'cart-badge'; badge.className = 'position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger'; badge.style.fontSize='0.75rem'; cartLink.appendChild(badge); } } if(badge){ if(j.count <= 0) badge.remove(); else badge.textContent = j.count < 100 ? String(j.count) : '99+'; } }).catch(function(){ showToast('No se pudo vaciar el carrito', 'danger'); }); });
    // sync badge from server to ensure counts match (useful if server-side cart differs)
    try {
      fetch('/api/cart/refresh-badge', { credentials: 'same-origin' }).then(function(r){ if(!r.ok) return; return r.json(); }).then(function(j){ if(!j) return; var c=j.count||0; var el=document.getElementById('cart-badge'); if(!el) return; if(c<=0) { el.remove(); return; } var display = c<100?String(c):'99+'; el.textContent = display; el.setAttribute('title', c + ' items'); });
    } catch(e) { /* ignore */ }
  }

  document.addEventListener('DOMContentLoaded', wire);
})();
