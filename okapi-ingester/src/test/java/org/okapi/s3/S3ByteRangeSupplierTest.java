package org.okapi.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.okapi.byterange.S3ByteRangeSupplier;
import org.okapi.testutils.OkapiTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ByteRangeSupplierTest {

  String bucket = "okapi-bytes-supplier-test-bucket";

  @BeforeAll
  static void setup() {
    var s3Client = OkapiTestUtils.getLocalstackS3Client();
    s3Client.createBucket(b -> b.bucket("okapi-bytes-supplier-test-bucket"));
  }

  @Test
  void testByteRangeSupplier() {
    var localStackClient = OkapiTestUtils.getLocalstackS3Client();
    var contents = "Hello, this is a test object for S3ByteRangeSupplier.";
    localStackClient.putObject(
        PutObjectRequest.builder().bucket(bucket).key("object-key").build(),
        RequestBody.fromBytes(contents.getBytes()));

    var s3ByteRangeSupplier =
        new S3ByteRangeSupplier(
            bucket, "object-key", localStackClient, new S3ByteRangeCache(1024L * 1024));

    var range = s3ByteRangeSupplier.getBytes(7, 20);
    var rangeStr = new String(range);
    assertEquals(contents.length(), s3ByteRangeSupplier.getEnd());
    assertEquals(20, range.length);
    assertEquals("this is a test objec", rangeStr);
  }

  @Test
  void testByteRangeSupplierCachesIfPossible() {
    var localStackClient = OkapiTestUtils.getLocalstackS3Client();
    var contents = "Hello, this is a test object for S3ByteRangeSupplier.";
    localStackClient.putObject(
        PutObjectRequest.builder().bucket(bucket).key("object-key").build(),
        RequestBody.fromBytes(contents.getBytes()));
    var cache = mock(S3ByteRangeCache.class);
    var s3ByteRangeSupplier =
        new S3ByteRangeSupplier(bucket, "object-key", localStackClient, cache);
    var range1 = s3ByteRangeSupplier.getBytes(0, 10);
    verify(cache, times(1))
        .getCachedRange(eq(bucket), eq("object-key"), eq(0L), eq(10L), anyString());
    verify(cache, times(1))
        .possiblyCache(eq(bucket), eq("object-key"), eq(0L), eq(10L), any(), anyString());
  }

  @Test
  void testByteRangeReturnsCachedContentWhenAvailable() {
    var s3Client = mock(S3Client.class);
    var cache = mock(S3ByteRangeCache.class);
    var mockHead = mock(HeadObjectResponse.class, RETURNS_DEEP_STUBS);
    when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(mockHead);
    when(mockHead.eTag()).thenReturn("mock-etag");
    when(mockHead.contentLength()).thenReturn(100L);
    when(cache.getCachedRange(eq(bucket), eq("object-key-2"), eq(0L), eq(10L), anyString()))
        .thenReturn(java.util.Optional.of("cached-data".getBytes()));
    var s3ByteRangeSupplier = new S3ByteRangeSupplier(bucket, "object-key-2", s3Client, cache);
    var fetched = s3ByteRangeSupplier.getBytes(0, 10);
    var fetchedStr = new String(fetched);
    assertEquals("cached-data", fetchedStr);
  }
}
