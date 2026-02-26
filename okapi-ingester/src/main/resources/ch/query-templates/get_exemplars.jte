@param org.okapi.metrics.ch.template.ChGetExemplarTemplate data

select * from ${data.getFqTable()}
where ts_nanos <= ${data.getTsNanosEnd()}
AND ts_nanos >= ${data.getTsNanosStart()}
AND metric_name = '${data.getMetricName()}'
AND tags = ${data.asMap(data.getTags())}
