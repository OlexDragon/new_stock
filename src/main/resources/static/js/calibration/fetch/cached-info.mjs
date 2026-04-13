import TTLCache from './ttl-cache.mjs';
import TimerWorker from '../timer-worker.mjs';
import { handleLoginIfNeeded } from '../global-login-handler.mjs';

export default class CachedInfo extends TTLCache {

	static makeFetcher(url, params = {}, parser=null) {
	    return async () => {
	        const raw = await $.get(url, params);

	        const login = handleLoginIfNeeded(raw);
	        if (login) {
	            await login;
	            throw new Error("not_logged_in");
	        }

			return parser ? parser(raw) : raw;
	    };
	}

    constructor(fetchFn, cacheTimeout = 1000) {
        super(fetchFn, cacheTimeout );

    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

	get(callback) {
	    if (typeof callback !== 'function') {
	        // Promise mode
	        return this.getValueAsync();
	    }

	    // Callback mode
	    this.getValueAsync()
	        .then(callback)
	        .catch(err => callback({ error: err }));
	}

	async getValueAsync() {
		return super.get({ swr: false });
	}
    async valueOf(names, { defaultValue = undefined, timeout = 5000 } = {}) {
        const path = this.#normalizePath(names);

        try {
            const info = await this.#withTimeout(this.getValueAsync(), timeout);
            return this.#extract(info, path, defaultValue);
        } catch (err) {
            throw new Error(`CachedInfo.valueOf failed: ${err.message}`);
        }
    }

    getCachedData() {
        return this.cachedValue || null;
    }
	async valueOf(names, { defaultValue = undefined, timeout = 5000 } = {}) {
	    const path = this.#normalizePath(names);

	    try {
	        const info = await this.#withTimeout(this.getValueAsync(), timeout);
	        return this.#extract(info, path, defaultValue);
	    } catch (err) {
	        throw new Error(`CachedInfo.valueOf failed: ${err.message}`);
	    }
	}
    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    #normalizePath(names) {
        if (names == null) return [];
        if (Array.isArray(names)) return names;
        if (typeof names === 'string') return names.split('.');
        throw new TypeError('names must be array, dot-string or null');
    }

    #extract(info, path, defaultValue) {
        let v = info;
        for (const p of path) {
            v = v?.[p];
            if (v === undefined) return defaultValue;
        }
        return v === undefined ? defaultValue : v;
    }

	#withTimeout(promise, ms) {
	    if (ms <= 0) return promise;

	    return new Promise((resolve, reject) => {
	        const timer = TimerWorker.startTimeout(() => {
	            reject(new Error(`Timeout after ${ms}ms`));
	        }, ms);

	        promise.then(
	            v => { timer.stop?.(); resolve(v); },
	            e => { timer.stop?.(); reject(e); }
	        );
	    });
	}

    // ---------------------------------------------------------------------
    // Abort + destroy
    // ---------------------------------------------------------------------

    abort(reason = 'Aborted') {
        // TTLCache doesn't track $.post requests, so we manually abort here
        // If you want full abort support, we can integrate it cleanly
        super.clear(); // clear cache
    }

    destroy() {
        this.abort('Destroyed');
        super.clear();
    }
}