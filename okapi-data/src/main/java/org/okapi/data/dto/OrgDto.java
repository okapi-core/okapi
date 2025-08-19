package org.okapi.data.dto;

import com.google.common.base.Preconditions;
import java.util.Date;
import lombok.Builder;
import lombok.Getter;

@Builder
public class OrgDto {
  @Getter String orgId;
  @Getter String orgName;
  @Getter String orgCreator;
  @Getter Date created;

  public OrgDto setOrgId(String orgId) {
    Preconditions.checkNotNull(orgId, "orgId must not be null");
    this.orgId = orgId;
    return this;
  }

  public OrgDto setOrgName(String orgName) {
    this.orgName = orgName;
    return this;
  }

  public OrgDto setCreated(Date created) {
    this.created = created;
    return this;
  }

  public OrgDto setOrgCreator(String orgCreator) {
    this.orgCreator = orgCreator;
    return this;
  }

  public OrgDto(String orgId, String orgName, String orgCreator, Date created) {
    this.setOrgId(orgId);
    this.setOrgName(orgName);
    this.setCreated(created);
    this.setOrgCreator(orgCreator);
  }
}
