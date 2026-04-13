export default class IrtChart {

    static colors = [ 'blue', 'red', 'green', 'orange', 'purple', 'cyan', 'magenta', 'yellow' ];
    static backgroundColors = [
        'rgba(0,0,255,1.0)', 'rgba(255,0,0,1.0)', 'rgba(0,255,0,1.0)', 'rgba(255,165,0,1.0)',
        'rgba(128,0,128,1.0)', 'rgba(0,255,255,1.0)', 'rgba(255,0,255,1.0)', 'rgba(255,255,0,1.0)'
    ];
    static borderColors = [
        'rgba(0,0,255,0.1)', 'rgba(255,0,0,0.1)', 'rgba(0,255,0,0.1)', 'rgba(255,165,0,0.1)',
        'rgba(128,0,128,0.1)', 'rgba(0,255,255,0.1)', 'rgba(255,0,255,0.1)', 'rgba(255,255,0,0.1)'
    ];

    #xLabels = [];
    #yValues = [];
    #oldX = null;
    #oldY = null;
    #chart = null;
    #root = null;
    #labels = [];

    constructor(root, labels) {
        if (!root || !labels?.length)
            throw new Error('Root element and labels are required to initialize the chart.');

        this.#root = root.length ? root[0] : root;
        this.#labels = Array.isArray(labels) ? labels : [labels];

        this.#initChart();
    }

    #initChart() {
        this.#chart = new Chart(this.#root, {
            type: "line",
            data: {
                labels: this.#xLabels,
                datasets: this.#getDataSets()
            },
            options: {
                responsive: true,
                interaction: {
                    mode: 'index',
                    intersect: false,
                },
                stacked: false,
                plugins: {
                    title: {
                        display: true,
                        text: this.#getChartTitle()
                    }
                },
                scales: this.#getScales()
            }
        });
    }

    #getDataSets() {
        const datasets = [];

        this.#labels.forEach((label, i) => {
            this.#yValues[i] = [];

            datasets.push({
                label: label || `Value #${i + 1}`,
                fill: false,
                tension: 0,
                backgroundColor: IrtChart.backgroundColors[i],
                borderColor: IrtChart.borderColors[i],
                data: this.#yValues[i],
                yAxisID: 'y' + (i === 0 ? '' : i + 1),
            });
        });

        return datasets;
    }

    #getScales() {
        const scales = {};

        this.#labels.forEach((_, i) => {
            scales['y' + (i === 0 ? '' : i + 1)] = {
                ticks: {
                    color: IrtChart.colors[i],
                },
                type: 'linear',
                display: true,
                position: i === 0 ? 'left' : 'right',
            };
        });

        return scales;
    }

    #getChartTitle() {
        const serialNumber = $('#serialNumber').text();
        const description = $('#unit_description').children('h4').text();
        return `${serialNumber} - ${description}`;
    }

    get data() {
        return {
            x: [...this.#xLabels],
            y: this.#yValues.map(arr => [...arr])
        };
    }

    get length() {
        return this.#xLabels.length;
    }

    appendPoint(xVal, yVals) {
        if (xVal !== undefined){
            this.#xLabels.push(xVal);

        	if (yVals !== undefined) {
            	if (!Array.isArray(yVals)) yVals = [yVals];

				this.#yValues.forEach((arr, i) => {
					const yVal = yVals[i];
					arr.push(yVal !== undefined ? yVal : null);
				});
				if(yVals !== undefined)
					this.update();
			}
		}
    }

	replacePoint(index, xVal, yVals) {
		if (index < 0 || index >= this.#xLabels.length) {
			console.warn('Index out of bounds');
			return;
		}
		if(!Array.isArray(yVals)) yVals = [yVals];
		
        this.#xLabels[index] = xVal;
		this.#yValues.forEach((arr, i) => {
			const yVal = yVals[i];
			arr[index] = yVal !== undefined ? yVal : null;
		});
		this.update();
	}
		
		
	removePoint(index) {
		if (index < 0 || index >= this.#xLabels.length) {
			console.warn('Index out of bounds');
			return;
		}
		
        this.#xLabels.splice(index, 1);
		this.#yValues.forEach(arr => arr.splice(index, 1));
		this.update();
	}
    clear() {
        this.#xLabels.length = 0;
        this.#yValues.forEach(arr => arr.length = 0);
    }

    update() {
        this.#chart?.update();
    }

    optimize(percent = 2.5) {
        if (this.#yValues.length !== 1) {
            console.warn('The chart must have only 1 line');
            return;
        }

        const tolerance = percent / 100;
        const yValue = this.#yValues[0];

        this.#oldX = [...this.#xLabels];
        this.#oldY = [...yValue];

        this.clear();

        // Keep first point
        this.#xLabels.push(this.#oldX[0]);
        yValue.push(this.#oldY[0]);

        for (let i = 1; i < this.#oldX.length - 1; i++) {
            const x1 = this.#xLabels[this.#xLabels.length - 1];
            const y1 = yValue[yValue.length - 1];

            const x2 = this.#oldX[i];
            const y2 = this.#oldY[i];

            if (y2 <= y1) continue;

            const x3 = this.#oldX[i + 1];
            const y3 = this.#oldY[i + 1];

            const t = (x2 - x1) / (x3 - x1);
            const expectedY = y1 + t * (y3 - y1);

            const deviation = Math.abs(y2 - expectedY);
            const allowed = Math.abs(y3 - y1) * tolerance;

            if (deviation <= allowed) continue;

            this.#xLabels.push(x2);
            yValue.push(y2);
        }

        const lastX = this.#oldX[this.#oldX.length - 1];
        const lastY = this.#oldY[this.#oldY.length - 1];

        if (lastY > yValue[yValue.length - 1]) {
            this.#xLabels.push(lastX);
            yValue.push(lastY);
        }

        this.update();
    }

    reset() {
        if (!this.#oldX || !this.#oldY) return;

        this.clear();
        this.#xLabels.push(...this.#oldX);
        this.#yValues[0].push(...this.#oldY);
        this.update();
    }

    destroy() {
        if (this.#chart) {
            this.#chart.destroy();
            this.#chart = null;
        }

        this.#xLabels = null;
        this.#yValues = null;
        this.#oldX = null;
        this.#oldY = null;
        this.#root = null;

        console.log('IrtChart instance destroyed');
    }
}