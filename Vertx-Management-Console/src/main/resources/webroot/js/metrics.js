	var charts = new Charts();
	var eb = new EventBus("http://" + location.host + "/managment-service/eventbus");
	eb.onopen = function() {
		eb.registerHandler("metrics", function(dashboard) {
			var x = (new Date()).getTime(); // current time
			for ( var id in dashboard) {
				if (dashboard.hasOwnProperty(id)) {
					var metrics = dashboard[id];
					for ( var metric in metrics) {
						if (metrics.hasOwnProperty(metric)) {
							var chart = charts.getChart(metric);
							var y = metrics[metric];
							var serie = chart.getSerie(id,
									function() {
										var data = [], time = (new Date())
												.getTime(), i;
										for (i = -19; i <= 0; i += 1) {
											data.push({
												x : time + i * 1000,
												y : y
											});
										}
										return data;
									});
							serie.addPoint([ x, y ], false, true);
						}
					}
				}
			}
			// Remove metrics
			charts.removeSeries(function(id) {
				if (dashboard[id] === undefined) {
					return true;
				}
				return false;
			});
			//
			charts.redraw();
		});
	};