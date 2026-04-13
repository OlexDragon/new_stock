class TimerWorker {
    static safe(cb, label) {
        try { cb(); } catch (e) { console.error(label, e); }
    }

    static fallback(kind, cb, ms) {
        const isPoll = kind === 'poll';
        const id = (isPoll ? setInterval : setTimeout)(
            () => TimerWorker.safe(cb, `${kind} callback`),
            ms
        );

        return {
            type: 'fallback',
            id,
            stop() {
                try { (isPoll ? clearInterval : clearTimeout)(id); } catch {}
            }
        };
    }

    static worker(kind, cb, ms) {
        const event = kind === 'poll' ? 'tick' : 'timeout';
        const code = kind === 'poll'
            ? `setInterval(()=>postMessage('${event}'),${ms})`
            : `setTimeout(()=>postMessage('${event}'),${ms})`;

        const url = URL.createObjectURL(new Blob([code], { type: 'application/javascript' }));

        try {
            const worker = new Worker(url);
            URL.revokeObjectURL(url);

            worker.onmessage = e => {
                if (e.data === event) {
                    TimerWorker.safe(cb, `${kind} callback`);
                    if (kind === 'timeout') worker.terminate();
                }
            };

            worker.onerror = err => console.error(`${kind} worker error`, err);

            return {
                type: 'worker',
                worker,
                stop() {
                    try { worker.terminate(); } catch {}
                }
            };
        } catch {
            URL.revokeObjectURL(url);
            return TimerWorker.fallback(kind, cb, ms);
        }
    }

    static startPoll(cb, ms) {
        if (typeof cb !== 'function')
            throw new TypeError('startPoll: callback must be a function');

        return typeof Worker === 'undefined'
            ? TimerWorker.fallback('poll', cb, ms)
            : TimerWorker.worker('poll', cb, ms);
    }

    static startTimeout(cb, ms) {        if (typeof cb !== 'function')
            throw new TypeError('startTimeout: callback must be a function');

        return typeof Worker === 'undefined'
            ? TimerWorker.fallback('timeout', cb, ms)
            : TimerWorker.worker('timeout', cb, ms);
    }
}

// Optional global exposure
try {
    if (typeof window !== 'undefined') {
        window.timerWorker = TimerWorker;
    }
} catch {}

export default TimerWorker;