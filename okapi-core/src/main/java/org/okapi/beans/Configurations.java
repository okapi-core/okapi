package org.okapi.beans;

public class Configurations {
  // values
  public static final String VAL_REGION = "${aws.region}";
  public static final String VAL_CLUSTER_ID = "${clusterId}";
  public static final String VAL_PROMQL_EVAL_THREADS = "${promQl.evalThreads}";
  public static final String VAL_CAS_ASYNC_THREADS = "${cas.async.threads}";
  public static final String VAL_CAS_CONTACT_PT = "${cas.contact.point}";
  public static final String VAL_CAS_CONTACT_DATACENTER = "${cas.contact.datacenter}";
  public static final String VAL_METRICS_KEY_SPACE = "${cas.metrics.keyspace}";

  // beans
  public static final String BEAN_FDB_MESSAGE_BOX = "fdbMessageBox";
  public static final String BEAN_SERIES_SUPPLIER = "seriesSupplier";
  public static final String BEAN_PROMQL_SERIALIZER = "promQlSerializer";
}
