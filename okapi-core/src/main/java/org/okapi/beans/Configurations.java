package org.okapi.beans;


public class Configurations {
    // values
    public static final String VAL_REGION = "${aws.region}";
    public static final String VAL_CLUSTER_ID = "${clusterId}";
    public static final String VAL_ADMISSION_WINDOW_HRS = "${admissionWindowHrs}";
    public static final String VAL_WRITE_BACK_WIN_MILLIS = "${writeBack.millis}";
    public static final String VAL_PROMQL_EVAL_THREADS = "${promQl.evalThreads}";

    // beans
    public static final String BEAN_ROCKS_MESSAGE_BOX = "rocksMessageBox";
    public static final String BEAN_SERIES_SUPPLIER = "seriesSupplier";
    public static final String BEAN_SHARED_EXECUTOR = "sharedExecutor";
    public static final String BEAN_PROMQL_SERIALIZER = "promQlSerializer";


}
