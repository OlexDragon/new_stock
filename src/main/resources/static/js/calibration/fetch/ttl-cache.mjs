import TimerWorker from '../timer-worker.mjs';

export default class TTLCache {
    #value = null;          // resolved value
    #promise = null;        // in-flight fetch promise
	#resolve = null;
	#reject = null;
    #timer = null;          // TTL expiration timer
    #ttl;
    #fetchFn;
    #abortController = null;
    #listeners = new Set(); // SWR listeners
    #maxRetries;

    constructor(fetchFn, ttlMs = 1000, { maxRetries = 1 } = {}) {
        this.#fetchFn = fetchFn;
        this.#ttl = ttlMs;
        this.#maxRetries = maxRetries;
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    async get({ update = false, swr = false } = {}) {
        if (update) {
            this.abort("Forced refresh");
            return this.#fetchFresh();
        }

        if (this.#value !== null && this.#isValid()) {
            return this.#value;
        }

        if (swr && this.#value !== null) {
            this.#fetchFresh(); // background refresh
            return this.#value;
        }

        return this.#fetchFresh();
    }

    clear() {
        this.#value = null;
        this.#promise = null;
        if (this.#timer) this.#timer.stop?.();
        this.#timer = null;
    }

	abort(reason = 'Aborted') {
	    if (this.#abortController) {
	        try { this.#abortController.abort(); } catch {}
	    }

	    const err = new Error(reason);
	    this.#reject?.(err);

	    this.clear();
	    this.#promise = null;
	    this.#resolve = null;
	    this.#reject  = null;
	}

    onUpdate(fn) {
        this.#listeners.add(fn);
        return () => this.#listeners.delete(fn);
    }

    destroy() {
        this.abort("Destroyed");
        this.#listeners.clear();
    }

    // ---------------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------------

    #isValid() {
        return this.#timer !== null;
    }

	#startTTL() {
	    if (this.#timer) this.#timer.stop?.();
	    this.#timer = TimerWorker.startTimeout(() => {
	        this.#timer = null;
	    }, this.#ttl);
	}

	async #fetchFresh() {
	    if (this.#promise) return this.#promise;

	    this.#abortController = new AbortController();

	    this.#promise = new Promise((resolve, reject) => {
	        this.#resolve = resolve;
	        this.#reject  = reject;
	    });

	    const doFetch = async () => {
	        let attempt = 0;

	        while (attempt <= this.#maxRetries) {
	            try {
	                const result = await this.#fetchFn({ signal: this.#abortController.signal });

	                this.#value = result;
	                this.#startTTL();

	                for (const fn of this.#listeners) fn(result);

	                this.#resolve?.(result);
	                return;

	            } catch (err) {
	                if (err.name === 'AbortError') {
	                    this.#reject?.(new Error('Fetch aborted'));
	                    return;
	                }

	                if (attempt >= this.#maxRetries) {
	                    this.clear();
	                    this.#reject?.(err);
	                    return;
	                }

	                attempt++;
	            }
	        }
	    };

	    doFetch().finally(() => {
	        this.#promise = null;
	        this.#resolve = null;
	        this.#reject  = null;
	    });

	    return this.#promise;
	}	// ---------------------------------------------------------------------
	// Cache inspection helpers
	// ---------------------------------------------------------------------

	/** Returns true if cache exists AND TTL has not expired */
	hasValidCache() {
	    return this.#value !== null && this.#isValid();
	}

	/** Returns cached value even if stale (TTL expired) */
	getStaleValue() {
	    return this.#value;
	}

	/** Forces a fresh fetch, bypassing cache entirely */
	async getFreshValue() {
	    this.abort("Forced fresh fetch");
	    return this.#fetchFresh();
	}}