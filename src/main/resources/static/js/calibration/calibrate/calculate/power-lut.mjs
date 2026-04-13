export default class PowerLUT {

    #$modalBody;
    #$outField;
    #$maxPower;

    constructor($modalBody, $outField, $maxPower) {
        this.#$modalBody = $modalBody;
        this.#$outField = $outField;
        this.#$maxPower = $maxPower;
    }

    // Extract numeric values from a row
    #readRow(row) {
        return {
            input: Number(row.find('.input').text()),
            output: Number(row.find('.output').text())
        };
    }

    // Compute next input value based on last two rows
    #calculateNext() {
        const $rows = this.#$modalBody.children();
        if ($rows.length < 2) return null;

        const last = this.#readRow($rows.eq($rows.length - 1));
        const prev = this.#readRow($rows.eq($rows.length - 2));

        const diffIn = Math.abs(last.input - prev.input);
        const diffOut = last.output - prev.output;

        const factor = diffOut !== 0 ? diffIn / diffOut : 0;
		const max = Number(this.#$maxPower.val());
        const calc = (max - last.output) * factor;

        return {
            input: Math.round(last.input + calc),
            output: max
        };
    }

    // Public method to add the next row
    addNextRow(addRowCallback) {
        const next = this.#calculateNext();
        if (!next){
			console.warn('The next value cannot be calculated.');
			 return;
		}

        addRowCallback(next.input, next.output);
        this.#$outField.value = next.output;
    }
}