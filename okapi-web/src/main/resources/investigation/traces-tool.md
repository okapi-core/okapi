<tool>
<tool-name>get_traces_tool</tool-name>
<tool-desc>
This tool can be used to fetch metrics from one of many spans.
This tool is common abstraction on top of existing stacks such as DataDog and Tempo.
It accepts the following parameters: 
- `startTime`
- `endTime`
- `query`

Here's a description of what each parameter means:
`startTime` and `endTime`. These parameters are linux epoch times. These specify an interval within which we'll search
the logs.
`query`. This is a query that will be run against the metrics-store.
The metrics-store is specified in <traces-store></traces-store> tags.
You'll be provided with what data source this query is for.
It will be one of:

- DataDog
- Tempo

The data-source to query grammar is as follows:

- For DataDog, the query language is DDQL
- For Loki and Prometheus, the query language is TempoQL

You are allowed to construct any readonly query.
If the source is not set please return an error response as follows:

```json
{
  "action_type": "report_error",
  "message": "traces-store is unspecified."
}
```

</tool-desc>
</tool>
