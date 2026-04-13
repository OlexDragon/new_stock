import {show as showToast} from '../toast-worker.mjs';

export function signIn(sn, callBack) {
    $.post(`/calibration/rest/login?sn=${sn}`)
    .done(data => {
        const hasError = data.startsWith('Error:');

        if (hasError) {
            console.warn('Sign-in error: ', data);
            showToast('Sign-in failed', data, 'bg-danger');
        } else {
            console.log('Sign-in', data);
            showToast('Sign-in successful', data, 'bg-success');
        }

        if (callBack)
            callBack(hasError ? new Error(data) : null);
    })
    .fail(error => {
        console.error('Sign-in failed', error);
        showToast('Sign-in failed', error.statusText || 'Unknown error', 'bg-danger');

        if (callBack)
            callBack(error);
    });
}
export function showLoginNotification() {
    if (document.getElementById("login-alert")) return;

    const div = document.createElement("div");
    div.id = "login-alert";
    div.className = "alert alert-warning alert-dismissible fade show";
    div.role = "alert";
    div.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 2000;
        min-width: 280px;
    `;
    div.innerHTML = `
        <strong>Session expired</strong><br>
        Please sign in to continue.
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(div);
}
export function hideLoginNotification() {
    const el = document.getElementById("login-alert");
    if (!el) return;
    el.classList.remove("show");
    setTimeout(() => el.remove(), 300);
}
