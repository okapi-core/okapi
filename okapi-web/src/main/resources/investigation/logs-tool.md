<tool>
<tool-name>get_logs_tool</tool-name>
<tool-desc>
This tool can be used to find out logs from different log-streams.
This tool is common abstraction on top of existing stacks such as DataDog and Loki.
It accepts the following parameters: 

- `startTime`
- `endTime`
- `query`

Here's a description of what each parameter means:

`startTime` and `endTime`. These parameters are linux epoch times. These specify an interval within which we'll search
the logs.
`query`. This is a query that will be run against the log-store.
The log-store is specified in <logs-store></logs-store> tags.
You'll be provided with what data source this query is for.
It will be one of:

- DataDog
- Loki

The data-source to query grammar is as follows:

- For DataDog, these are DataDog query language or DDQL queries
- For Loki, these are LogQL queries.

You are allowed to construct any readonly query.
If the source is not set please return an error response as follows:

```json
{
  "action_type": "report_error",
  "message": "log-store is unspecified."
}
```

</tool-desc>
</tool>