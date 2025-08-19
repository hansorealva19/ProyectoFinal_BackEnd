// auth-guard.js
// When the user logs out we set a flag so that pages in history know to redirect to /login
(function(){
  function markLoggedOut(){
    try{ localStorage.setItem('ecom.justLoggedOut','1'); }catch(e){}
  }

  // Called on pageshow to redirect if we detect recent logout
  function handlePageShow(e){
    try{
      var flag = localStorage.getItem('ecom.justLoggedOut');
      if(flag === '1'){
        // Do NOT remove the flag yet. Wait until we finish the server check so
        // multiple history entries restored by the browser will still trigger
        // the guard on each pageshow.
        // Make a quick call to the server to check session state (handles bfcache cases)
        fetch('/api/auth/check', { method: 'GET', credentials: 'same-origin' })
          .then(function(resp){
            if(resp.status === 401){
              // Not authenticated -> redirect to login. Use assign() so we add a
              // navigation entry instead of replacing the current one. That
              // makes Back behave more predictably for the user.
              window.location.assign('/login');
            } else {
              // Still authenticated for some reason -> clear the flag so we don't
              // keep forcing checks on further history entries.
              try{ localStorage.removeItem('ecom.justLoggedOut'); }catch(e){}
            }
          }).catch(function(){
            // On error, be conservative and redirect to login as above.
            window.location.assign('/login');
          });
      }
    }catch(e){ console.debug('auth-guard error', e); }
  }

  window.addEventListener('pageshow', handlePageShow);

  // Expose helper for logout buttons to call before submit
  window.ECOM_AUTH_GUARD = { markLoggedOut };
})();
