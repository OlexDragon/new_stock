export default class TableValidator {
    #$container;
    #rows = [];
    #hasError = false;
    #prevInput = undefined;
    #direction = undefined;

    constructor($container) {
        this.#$container = $container;
    }

    // Extract numeric input/output from a row
    #readRow(row) {

        const inputText = row.querySelector('.input').textContent;
        const outputText = row.querySelector('.output').textContent;

        const input = Number(inputText);
        const output = Number(outputText);

        return { input, output };
    }

    // Validate monotonic direction and duplicates
    #validateInput(input) {

		if (this.#prevInput === undefined) {
			this.#prevInput = input;
			return;
		}

		// Duplicate check
		if (this.#prevInput === input) {
		    this.#hasError = true;
		    return;
		}

		// Determine direction (true = decreasing, false = increasing)
		const currentDirection = this.#prevInput > input;

		if (this.#direction !== undefined) {
		    if (this.#direction !== currentDirection) {
		        this.#hasError = true;
		        return;
		    }
		} else {
		    this.#direction = currentDirection;
		}

    }

    // Main validation method
    validate() {
        this.#rows = [];
        this.#hasError = false;
        this.#prevInput = undefined;
        this.#direction = undefined;

        const $children = this.#$container.children();

        $children.each((_, row) => {
            if (this.#hasError) return;

            const { input, output } = this.#readRow(row);

            if (Number.isNaN(input) || Number.isNaN(output)) {
                this.#hasError = true;
                return;
            }

            this.#validateInput(input);

            if (!this.#hasError) {
                this.#rows.push({ input, output, comment: null });
            }
        });

        if (this.#hasError || this.#rows.length === 0) {
            return { ok: false, rows: [] };
        }

        return { ok: true, rows: this.#rows };
    }
}