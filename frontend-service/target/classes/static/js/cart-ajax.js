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
    var url = form.getAttribute('action') || '/cart/add';
  var formData = new FormData(form);
  fetch(url, { method: 'POST', body: formData, credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } })
      .then(function(resp){
        if(resp.ok) return resp.text();
        throw new Error('Error al agregar al carrito');
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
        showToast(err.message || 'No se pudo agregar al carrito', 'danger');
      });
  }

  document.addEventListener('submit', handleForm, true);
})();
