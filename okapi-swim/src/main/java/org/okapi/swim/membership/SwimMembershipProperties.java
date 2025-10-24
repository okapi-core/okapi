package org.okapi.swim.membership;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SwimMembershipProperties {
  /** Service name used for S3 prefixing: [serviceName]/membership/ */
  private String serviceName;

  /** S3 bucket for membership events. */
  private String membershipS3Bucket;

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getMembershipS3Bucket() {
    return membershipS3Bucket;
  }

  public void setMembershipS3Bucket(String membershipS3Bucket) {
    this.membershipS3Bucket = membershipS3Bucket;
  }
}
