import TimerWorker from './timer-worker.mjs';

export default class DestroyableComponent{

    constructor(namespace) {
        this._ns = "." + (namespace ? namespace.replace(/\s+/g, '') : 'toDestroy');
        this._trackedElements = [];
        this._timers = [];
        this._observers = [];

        // Auto-cleanup when tracked elements are removed
        this._domObserver = new MutationObserver(() => this._cleanupDetachedElements());
        this._domObserver.observe(document.body, { childList: true, subtree: true });
    }

    // ---------------------------------------------------------------------
    // DOM EVENT TRACKING
    // ---------------------------------------------------------------------

    on($el, eventName, handler) {
        $el.on(eventName + this._ns, handler);
        this.trackElement($el);
        return $el;
    }

    trackElement($el) {
        if (!this._trackedElements.includes($el))
            this._trackedElements.push($el);
        return $el;
    }

    _cleanupDetachedElements() {
        this._trackedElements = this._trackedElements.filter($el => {
            const el = $el[0];
            if (!document.body.contains(el)) {
                try { $el.off(this._ns); } catch {}
                return false;
            }
            return true;
        });
    }

    // ---------------------------------------------------------------------
    // UNIFIED TIMER ENGINE
    // ---------------------------------------------------------------------

    setTimer(type, fn, ms) {
        const isInterval = type === 'interval';

        const createHandle = () =>
            isInterval
                ? TimerWorker.startPoll(fn, ms)
                : TimerWorker.startTimeout(fn, ms);

        const timerObj = {
            type,
            fn,
            ms,
            handle: createHandle(),
            running: true,
            remaining: ms,
            startedAt: performance.now(),

            pause: () => {
                if (!timerObj.running) return;
                timerObj.running = false;

                try { timerObj.handle.stop?.(); } catch {}
                try { clearTimeout(timerObj.handle); clearInterval(timerObj.handle); } catch {}

                if (!isInterval) {
                    const elapsed = performance.now() - timerObj.startedAt;
                    timerObj.remaining = Math.max(0, timerObj.ms - elapsed);
                }
            },

            resume: () => {
                if (timerObj.running) return;
                timerObj.running = true;
                timerObj.startedAt = performance.now();

                timerObj.handle = isInterval
                    ? createHandle()
                    : TimerWorker.startTimeout(timerObj.remaining, fn);
            },

            stop: () => {
                timerObj.running = false;
                try { timerObj.handle.stop?.(); } catch {}
                try { clearTimeout(timerObj.handle); clearInterval(timerObj.handle); } catch {}
            }
        };

        this._timers.push(timerObj);
        return timerObj;
    }

    setTimeout(fn, ms) { return this.setTimer('timeout', fn, ms); }
    setInterval(fn, ms) { return this.setTimer('interval', fn, ms); }

    clearTimer(timerObj) {
        if (!timerObj) return;
        timerObj.stop();
        this._timers = this._timers.filter(t => t !== timerObj);
    }

    clearTimeout(t) { this.clearTimer(t); }
    clearInterval(t) { this.clearTimer(t); }

    // ---------------------------------------------------------------------
    // OBSERVER TRACKING
    // ---------------------------------------------------------------------

    trackObserver(observer) {
        this._observers.push(observer);
        return observer;
    }

    // ---------------------------------------------------------------------
    // DESTROY
    // ---------------------------------------------------------------------

    destroy() {
        // Remove namespaced events
        for (const $el of this._trackedElements)
            try { $el.off(this._ns); } catch {}

        // Stop timers
        for (const t of this._timers)
            try { t.stop(); } catch {}

        // Disconnect observers
        for (const obs of this._observers)
            try { obs.disconnect(); } catch {}

        // Stop DOM observer
        try { this._domObserver.disconnect(); } catch {}

        // Cleanup
        this._trackedElements = null;
        this._timers = null;
        this._observers = null;
        this._domObserver = null;
    }
}