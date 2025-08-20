(function(){
  // helper to POST formdata-like via fetch and return json
  function postJson(url, data){
    var form = new URLSearchParams();
    for(var k in data) form.append(k, data[k]);
    return fetch(url, { method: 'POST', body: form, credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } }).then(function(r){ if(!r.ok) throw r; return r.json(); });
  }

  function updateRowCount(btn){
    var trigger = btn;
    var row = trigger.closest('tr');
    if(!row) return;
    var pid = row.getAttribute('data-pid');
    var input = row.querySelector('.cart-qty-input');
    var val = parseInt(input.value || '0', 10);
    // detect if trigger is an inc/dec button or the input itself
    if(trigger && trigger.tagName === 'BUTTON'){
      if(trigger.classList.contains('cart-inc-btn')) val = val + 1;
      else if(trigger.classList.contains('cart-dec-btn')) val = Math.max(0, val - 1);
      input.value = val;
    } else {
      // trigger could be the input element (on change) - value already read
      val = Math.max(0, parseInt(input.value || '0', 10));
      input.value = val;
    }
    // send update
    postJson('/cart/update', { productId: pid, quantity: val }).then(function(j){
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
      if(val === 0) row.remove();
    }).catch(function(){ alert('Error actualizando el carrito'); });
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
    }).catch(function(){ alert('No se pudo eliminar el item'); });
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
          updateRowCount(t);
        }
      });
    }
    var clearBtn = document.getElementById('cart-clear-btn');
  if(clearBtn) clearBtn.addEventListener('click', function(){ fetch('/cart/clear', { method: 'POST', credentials: 'same-origin', headers: { 'X-Requested-With':'XMLHttpRequest' } }).then(function(r){ if(!r.ok) throw r; return r.json(); }).then(function(j){ document.querySelectorAll('tr[data-pid]').forEach(function(tr){ tr.remove(); }); var totalEl = document.querySelector('#cart-total-val'); if(totalEl) totalEl.textContent = '0.00'; var badge = document.getElementById('cart-badge'); if(!badge && j.count > 0){ var cartLink = document.querySelector('a[href="/cart"]'); if(cartLink){ badge = document.createElement('span'); badge.id = 'cart-badge'; badge.className = 'position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger'; badge.style.fontSize='0.75rem'; cartLink.appendChild(badge); } } if(badge){ if(j.count <= 0) badge.remove(); else badge.textContent = j.count < 100 ? String(j.count) : '99+'; } }).catch(function(){ alert('No se pudo vaciar el carrito'); }); });
    // sync badge from server to ensure counts match (useful if server-side cart differs)
    try {
      fetch('/api/cart/refresh-badge', { credentials: 'same-origin' }).then(function(r){ if(!r.ok) return; return r.json(); }).then(function(j){ if(!j) return; var c=j.count||0; var el=document.getElementById('cart-badge'); if(!el) return; if(c<=0) { el.remove(); return; } var display = c<100?String(c):'99+'; el.textContent = display; el.setAttribute('title', c + ' items'); });
    } catch(e) { /* ignore */ }
  }

  document.addEventListener('DOMContentLoaded', wire);
})();
