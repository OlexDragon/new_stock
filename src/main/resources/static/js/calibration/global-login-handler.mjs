import { signIn, showLoginNotification, hideLoginNotification } from './sign-in.mjs';
import { unitSerialNumber } from '../calibration.mjs';

let loginInProgress = false;
let loginPromise = null;

export function handleLoginIfNeeded(responseText) {
    if (typeof responseText !== "string") return null;

    // Case 1: login HTML page
    if (responseText.startsWith("<!DOCTYPE html")) {
        return triggerLogin();
    }

    // Case 2: empty JS variable (session expired)
    const trimmed = responseText.trim();
    if (trimmed === "var calib_rw_info = { };" ||
        trimmed === "var calib_rw_info = {};") {
        return triggerLogin();
    }

    // Case 3: full JS variable → VALID, do NOT trigger login
    if (trimmed.startsWith("var calib_rw_info = {") &&
        !trimmed.includes("{ }")) {
        return null; // valid data
    }

    return null;
}

function triggerLogin() {
    if (loginInProgress) return loginPromise;

    loginInProgress = true;
    showLoginNotification();

    loginPromise = new Promise(resolve => {
        signIn(unitSerialNumber, () => {
            loginInProgress = false;
            hideLoginNotification();
            resolve();
        });
    });

    return loginPromise;
}