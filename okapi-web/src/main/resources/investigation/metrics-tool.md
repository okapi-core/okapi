<tool>
<tool-name>get_metrics_tool</tool-name>
<tool-desc>
This tool can be used to fetch metrics from one of many metric paths.
This tool is common abstraction on top of existing stacks such as DataDog and Mimir.
It accepts the following parameters: 

- `startTime`
- `endTime`
- `query`
- `resolution`
-

Here's a description of what each parameter means:

`startTime` and `endTime`. These parameters are linux epoch times. These specify an interval within which we'll search
the logs.

`resolution`. This specifies the resolution of the data points returned. It is specified in milliseconds.
`query`. This is a query that will be run against the metrics-store.
The metrics-store is specified in <metrics-store></metrics-store> tags.
You'll be provided with what data source this query is for.
It will be one of:

- DataDog
- Mimir
- Prometheus

The data-source to query grammar is as follows:

- For DataDog, the query language is DDQL
- For Loki and Prometheus, the query language is PromQL

You are allowed to construct any readonly query.
If the source is not set please return an error response as follows:

```json
{
  "action_type": "report_error",
  "message": "metrics-store is unspecified."
}
```

</tool-desc>

</tool>