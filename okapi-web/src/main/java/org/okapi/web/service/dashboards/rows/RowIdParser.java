package org.okapi.web.service.dashboards.rows;

import org.okapi.exceptions.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class RowIdParser {

  public RowId parse(String rowFqId) throws BadRequestException {
    String[] parts = rowFqId.split("/");
    if (parts.length != 3) {
      throw new BadRequestException(
          "Invalid Row FQ ID format. Expected format: orgId/dashboardId/rowId");
    }
    return new RowId(parts[0], parts[1], parts[2]);
  }
}
