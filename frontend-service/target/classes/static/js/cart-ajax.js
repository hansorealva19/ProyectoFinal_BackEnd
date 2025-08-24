(function(){
  // intercept add-to-cart forms and submit via fetch, show toast
  function showToast(message, variant){
    variant = variant || 'success';
    var container = document.getElementById('ecom-toast-container');
    if(!container) return;
    var id = 't' + Date.now();
    var toastHtml = '\n      <div id="' + id + '" class="toast align-items-center text-bg-' + variant + ' border-0 mb-2" role="alert" aria-live="assertive" aria-atomic="true">\n' +
      '  <div class="d-flex">\n' +
      '    <div class="toast-body">' + message + '</div>\n' +
      '    <button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>\n' +
      '  </div>\n' +
      '</div>';
    container.insertAdjacentHTML('beforeend', toastHtml);
    var el = document.getElementById(id);
    var bs = bootstrap.Toast.getOrCreateInstance(el, { delay: 2500 });
    bs.show();
    // clean up after hidden
    el.addEventListener('hidden.bs.toast', function(){ el.remove(); });
  }

  function handleForm(e){
    var form = e.target;
    if(!form) return;
    var isAjax = form.matches('.add-to-cart-ajax') || (form.getAttribute && (form.getAttribute('action')||'').indexOf('/cart/add') !== -1);
    if(!isAjax) return;
    e.preventDefault();
    // Client-side check: ensure quantity does not exceed input.max (avoid native tooltip)
    try {
      var qtyInput = form.querySelector('input[name="quantity"]');
      if (qtyInput) {
        var max = Number(qtyInput.max || 0);
        var val = Number(qtyInput.value || 0);
        if (max > 0 && val > max) {
          showToast('La cantidad seleccionada supera el stock', 'danger');
          return; // do not submit
        }
      }
    } catch (ex) {
      // ignore and continue
    }
    var url = form.getAttribute('action') || '/cart/add';
  var formData = new FormData(form);
  fetch(url, { method: 'POST', body: formData, credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } })
      .then(function(resp){
        if(resp.ok) return resp.text();
        return resp.text().then(function(text){
          // robust extraction of message from various response body shapes
          function extractMessage(txt){
            if(!txt) return 'Error al agregar al carrito';
            var t = txt.trim();
            // try direct JSON parse
            try{
              var obj = JSON.parse(t);
              if(typeof obj === 'string'){
                // double-encoded JSON string
                try{ var obj2 = JSON.parse(obj); if(obj2 && (obj2.message || obj2.detail)) return obj2.message || obj2.detail; }catch(e){}
                return obj;
              }
              if(obj && (obj.message || obj.detail)) return obj.message || obj.detail;
            }catch(e){}
            // strip surrounding quotes and try again
            if((t.startsWith('"') && t.endsWith('"')) || (t.startsWith("'") && t.endsWith("'"))){
              var stripped = t.slice(1, -1);
              try{ var o = JSON.parse(stripped); if(o && (o.message || o.detail)) return o.message || o.detail; }catch(e){}
            }
            // fallback regex
            var m = /"message"\s*:\s*"([^\"]+)"/.exec(t);
            if(m && m[1]) return m[1];
            // last resort: return raw text
            return t;
          }
          var msg = extractMessage(text || '');
          throw new Error(msg || 'Error al agregar al carrito');
        });
      })
      .then(function(respText){
        showToast('Producto agregado al carrito', 'success');
        // Try to parse JSON response (our endpoint now returns {count})
        try{
          var json = JSON.parse(respText);
          var c = json.count || 0;
          var el = document.getElementById('cart-badge');
          if(!el && c > 0){ var cartLink = document.querySelector('a[href="/cart"]'); if(cartLink){ el = document.createElement('span'); el.id='cart-badge'; el.className='position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger'; el.style.fontSize='0.75rem'; cartLink.appendChild(el); } }
          if(!el) return;
          if(c <= 0){ el.remove(); return; }
          var display = c < 100 ? String(c) : '99+';
          el.setAttribute('title', c + ' items'); el.textContent = display; el.classList.remove('badge-pop'); void el.offsetWidth; el.classList.add('badge-pop');
        }catch(e){
          // if response was not JSON, fall back to refresh-badge
          return fetch('/api/cart/refresh-badge', { credentials: 'same-origin' });
        }
      })
      .then(function(resp){ if(!resp) return; if(!resp.ok) return; return resp.json(); })
      .then(function(json){ if(!json) return; var el = document.getElementById('cart-badge'); if(!el && json.count>0){ var cartLink = document.querySelector('a[href="/cart"]'); if(cartLink){ el = document.createElement('span'); el.id='cart-badge'; el.className='position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger'; el.style.fontSize='0.75rem'; cartLink.appendChild(el);} } if(el){ if(json.count<=0) el.remove(); else el.textContent = json.count < 100 ? String(json.count) : '99+'; } })
      .catch(function(err){
        var raw = (err && err.message) ? err.message : 'No se pudo agregar al carrito';
        var display = 'No se pudo agregar al carrito';
        try {
          // Try parse if it's JSON
          var j = JSON.parse(raw);
          if (j) {
            if (j.message) raw = j.message;
            else if (j.detail) raw = j.detail;
            else raw = JSON.stringify(j);
          }
        } catch(e) {
          // not JSON, continue
        }
        // Normalize known stock-related messages to a friendly Spanish message
        var lowStockPatterns = [/Requested quantity exceeds/i, /available stock/i, /exceeds available/i, /excede/i, /supera el stock/i, /sin stock/i, /cantidad seleccionada/i, /insufficient stock/i, /stock insuficiente/i];
        for (var i = 0; i < lowStockPatterns.length; i++) {
          if (lowStockPatterns[i].test(raw)) {
            display = 'La cantidad seleccionada supera el stock';
            break;
          }
        }
        // If no pattern matched, but raw looks like JSON object string, try to extract message key via regex
        if (display === 'No se pudo agregar al carrito') {
          var m = /"message"\s*:\s*"([^\"]+)"/.exec(raw);
          if (m && m[1]) display = m[1];
        }
        showToast(display, 'danger');
      });
  }

  document.addEventListener('submit', handleForm, true);
  // Dynamic input listeners: disable submit if quantity > max and show inline feedback
  document.addEventListener('input', function(ev){
    var tgt = ev.target;
    if(!tgt) return;
    if(tgt.matches('input[name="quantity"]')){
      try{
        var form = tgt.closest('form.add-to-cart-ajax');
        if(!form) return;
        var btn = form.querySelector('button[type="submit"], button#addToCartBtn');
        var feedback = tgt.parentElement ? tgt.parentElement.querySelector('.invalid-feedback') : null;
        var max = Number(tgt.max || 0);
        var val = Number(tgt.value || 0);
        if(max > 0 && val > max){
          if(btn) btn.disabled = true;
          if(feedback){ feedback.style.display = 'block'; feedback.textContent = 'La cantidad seleccionada supera el stock'; }
        } else {
          if(btn) btn.disabled = false;
          if(feedback){ feedback.style.display = 'none'; }
        }
      }catch(e){ }
    }
  }, true);
})();
