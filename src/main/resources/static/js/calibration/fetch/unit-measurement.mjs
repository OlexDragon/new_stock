import CachedInfo from './cached-info.mjs';
import { handleLoginIfNeeded } from '../global-login-handler.mjs';
import { unitSerialNumber } from '../../calibration.mjs';

export class UnitMeasurement extends CachedInfo {

    constructor() {
		super(CachedInfo.makeFetcher("/calibration/rest/monitorInfo", { sn: unitSerialNumber, mode: "fast" }), 500 );
    }

    async getValueAsync() {
        try {
            return await super.getValueAsync();
        } catch (err) {
            if (err.message === "not_logged_in") {
                await handleLoginIfNeeded("<!DOCTYPE html>");
                return super.getValueAsync();
            }
            throw err;
        }
    }
}

const um = new UnitMeasurement();
export default um;