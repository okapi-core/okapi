package org.okapi.data.dto;

import java.time.Instant;
import lombok.*;

@Data
@Builder
public class DashboardDto {

  public enum DASHBOARD_STATUS {
    ACTIVE, INACTIVE
  }
  private String dashboardId;
  private String orgId;
  private String creator;
  private String lastEditor;
  private Instant created;
  private Instant updatedTime;
  private String dashboardNote;
  private String dashboardTitle;
  private DASHBOARD_STATUS dashboardStatus;
  private Long version;
  String bucket;
  String prefix;
}
