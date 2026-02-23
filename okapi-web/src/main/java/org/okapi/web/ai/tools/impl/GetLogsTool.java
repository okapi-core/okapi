package org.okapi.web.ai.tools.impl;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.okapi.web.ai.tools.GetLogsToolkit;
import org.okapi.web.ai.tools.QueryContext;
import org.okapi.web.ai.tools.params.LogQuery;

public class GetLogsTool {
  QueryContext context;
  GetLogsToolkit getLogsToolkit;

  @Tool(
"""
Use this tool to retrieve logs based on specific query parameters. Various filters can be applied to narrow down the log results.
""")
  public String getLogs(
      @P(
"""
Object with fields:
- resourcePath: object with fields:
  - pathName: name of a specific log path
  - tags: a map of tag keys to tag values (map of string to string)
- startTime: start time in milliseconds since epoch (long)
- endTime: end time in milliseconds since epoch (long)
- labelFilter: filter logs by labels (object)
- levelFilter: filter logs by levels (object)
- regexFilter: filter logs by regex patterns (object)

labelFilter object fields:
- labels: a map of label keys to label values (map of string to string). Useful for systems that only support label-based log querying.

levelFilter object fields:
- levels: a list of log levels to filter by (e.g., ERROR, WARN, INFO, DEBUG). This is an integer interpreted as the following:
    - 10: ERROR
    - 20: WARN
    - 30: INFO
    - 40: DEBUG

regexFilter object fields:
- pattern: a regex pattern (string) to filter log messages.
""")
          LogQuery logQuery)
      throws Exception {
    return getLogsToolkit.getLogs(context, logQuery).toString();
  }
}
