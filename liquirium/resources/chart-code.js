$(function () {

    let chartDataByMarketName = window.chartData;
    let currentMarket = null;
    let chartWrapper = new ChartWrapper();

    $(window).on('hashchange', function () {
        if (selectedMarket() !== currentMarket) {
            displayChartData(selectedMarket());
        }
    });

    addButtonsForMarkets(chartDataByMarketName);
    let firstMarket = Object.keys(chartDataByMarketName)[0];
    if (!selectedMarket()) navigateToMarket(firstMarket);
    displayChartData(selectedMarket());

    function displayChartData(market) {
        currentMarket = market;
        chartWrapper.displayData(chartDataByMarketName[market]);
    }
})

function ChartWrapper() {
    const self = this;
    self.chart = window.LightweightCharts.createChart($(".chart")[0], getChartDimensions());

    const $legend = $('.chart-panel .legend');

    $(window).on('resize', function () {
        let dim = getChartDimensions();
        self.chart.resize(dim.width, dim.height);
    });

    self.chart.applyOptions({
        priceScale: {
            mode: LightweightCharts.PriceScaleMode.Logarithmic
        }
    });

    self.candlestickSeries = self.chart.addCandlestickSeries();

    // for each config there will be a data series with the same index
    self.dataSeriesConfigs = [];
    self.dataSeries = [];

    self.displayData = function (chartData) {
        // noinspection JSUnresolvedReference
        const candleData = chartData.candleData;
        // noinspection JSUnresolvedReference
        self.dataSeriesConfigs = chartData.dataSeries.map(ds => ds.config);
        self.candlestickSeries.setData(candleData);
        for (const series of self.dataSeries) {
            self.chart.removeSeries(series);
        }

        // self.configsByDataSeries = new Map();
        self.dataSeries = [];
        // noinspection JSUnresolvedReference
        chartData.dataSeries.forEach(function (ds, index) {
            const series = makeDataSeries(ds.config.appearance, index);
            series.setData(addTimeToSeriesData(ds.data, candleData));
            self.dataSeries.push(series);
        });

        self.chart.timeScale().fitContent();
    }

    function makeDataSeries(appearance, index) {
        if (appearance.type === "line") {
            // noinspection JSUnresolvedReference
            return self.chart.addLineSeries({
                color: appearance.color,
                lineWidth: appearance.lineWidth,
                priceScaleId: appearance.overlay ? 'dataSeriesScale' + index : 'right',
            });
        } else if (appearance.type === "histogram") {
            const series =
                self.chart.addHistogramSeries({
                    color: appearance.color,
                    priceFormat: {
                        type: 'volume',
                    },
                    priceScaleId: 'volume',
                });
            series.priceScale().applyOptions({
                scaleMargins: {
                    top: 0.7,
                    bottom: 0,
                },
            });
            return series;
        }
    }

    self.chart.subscribeCrosshairMove((param) => {
        if (param.time) {
            let dataSeriesHtml = '';

            self.dataSeriesConfigs.forEach(function (dsc, index) {
                const currentValue = param.seriesData.get(self.dataSeries[index]).value;
                let formattedValue = currentValue.toFixed(dsc.precision);
                let style = 'color: ' + dsc.appearance.color;
                dataSeriesHtml = dataSeriesHtml + '<div style="' + style + '">' +
                    dsc.caption + ': ' + formattedValue +
                    '</div>';
            });
            let price = null;
            if (param.seriesData.has(self.candlestickSeries)) {
                price = param.seriesData.get(self.candlestickSeries).close;
            }
            const priceString = price === null ? '-' : price.toFixed(8);
            $legend.html('<div class="price">' + priceString + '</div>' +
                dataSeriesHtml
            );
        } else {
            $legend.html('');
        }
    });
}

function selectedMarket() {
    let hash = window.location.hash;
    if (hash && hash.length > 0) return hash.substring(1);
    else return null;
}

function addButtonsForMarkets(marketDataFilesByMarket) {
    let $toolbar = $('#toolbar');
    $toolbar.find('.market-button').remove();
    for (const market of Object.keys(marketDataFilesByMarket)) {
        let $button = $('<button>').text(market)
        $button.addClass('market-button')
        $button.click(() => navigateToMarket(market));
        $toolbar.append($button);
    }
}

function navigateToMarket(market) {
    window.location.hash = market;
}

function addTimeToSeriesData(seriesData, candleData) {
    return seriesData.map((value, i) => {
        return {
            time: candleData[i].time,
            value: value,
        };
    });
}

function getChartDimensions() {
    let $body = $('body');
    return {
        width: $body.width(),
        height: $body.height() - $("#toolbar").outerHeight()
    }
}
