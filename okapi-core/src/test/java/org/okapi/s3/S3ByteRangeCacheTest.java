package org.okapi.s3;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

public class S3ByteRangeCacheTest {

  S3Client s3;
  String bucket;

  @BeforeEach
  public void setup() {

    s3 =
        S3Client.builder()
            .endpointOverride(URI.create("http://localhost:4566")) // LocalStack endpoint
            .region(Region.US_EAST_1) // must be set, even if ignored by LocalStack
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true) // enable path-style
                    .build())
            .build();
    this.bucket = "test-bucket";
    try {
      s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
      // Bucket exists
    } catch (NoSuchBucketException e) {
      // Bucket does not exist, create it
      s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    } catch (S3Exception e) {
      if (e.statusCode() == 404) {
        // Also means bucket not found
        s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
      } else {
        throw e; // unexpected error
      }
    }
  }

  @ParameterizedTest
  @MethodSource("combinedS3BrsCases")
  public void testByteRangeScanning(
      byte[] contents, int cachedPages, long pageSize, long start, long end)
      throws IOException, ExecutionException {
    var prefix = "this/is/a/file";
    var tempFile = Files.createTempFile("prefix", "tmp");
    Files.write(tempFile, contents);

    s3.putObject(
        PutObjectRequest.builder().bucket(bucket).key(prefix).build(),
        RequestBody.fromFile(tempFile.toFile()));
    var rangeCache =
        new S3ByteRangeCache(this.s3, cachedPages, pageSize, Duration.of(10, ChronoUnit.MILLIS));
    var fetched = rangeCache.getRange(this.bucket, prefix, start, end);
    for (int i = (int) start; i < end; i++) {
      byte expected = contents[i];
      byte got = fetched[i - (int) start];
      assertEquals(expected, got, "Mismatch at byte " + i);
    }
  }

  @Test
  public void testExpiry() throws IOException, ExecutionException {
    var tempFile = Files.createTempFile("prefix", ".tmp");
    var contents = new byte[] {0x1, 0x2, 0x3, 0x4};
    Files.write(tempFile, contents);
    var prefix = "testExpiry/contents";
    var cached = new S3ByteRangeCache(this.s3, 1, 2, Duration.of(10, ChronoUnit.MILLIS));

    s3.putObject(
        PutObjectRequest.builder().bucket(bucket).key(prefix).build(),
        RequestBody.fromFile(tempFile.toFile()));
    var fetched = cached.getRange(this.bucket, prefix, 2, 3);
    assertEquals(fetched[0], 0x3);

    var updatedContents = new byte[] {0x1, 0x2, 0x4, 0x5};
    Files.write(tempFile, updatedContents);

    s3.putObject(
        PutObjectRequest.builder().bucket(bucket).key(prefix).build(),
        RequestBody.fromFile(tempFile.toFile()));
    await()
        .atMost(Duration.of(200, ChronoUnit.MILLIS))
        .until(
            () -> {
              var newFetched = cached.getRange(this.bucket, prefix, 2, 3);
              return newFetched[0] == 0x4;
            });
  }

  public static Stream<Arguments> combinedS3BrsCases() {
    return Stream.concat(testS3BrsData(), testS3BrsEdgeCases());
  }

  public static Stream<Arguments> testS3BrsData() {
    return Stream.of(
        Arguments.of(new byte[] {0x1}, 1, 1, 0L, 1L),
        Arguments.of(new byte[] {0x1, 0x2, 0x4, 0x5}, 1, 1, 0L, 1L),
        Arguments.of(new byte[] {0x1, 0x2, 0x4, 0x5}, 1, 1, 1L, 2L),
        Arguments.of(new byte[] {0x1, 0x2, 0x4, 0x5}, 1, 1, 3L, 4L),
        Arguments.of(new byte[] {0x1, 0x2, 0x4, 0x5}, 2, 2, 0L, 3L),
        Arguments.of(new byte[] {0x1, 0x2, 0x4, 0x5}, 2, 2, 1L, 3L),
        Arguments.of(new byte[] {0x1, 0x2, 0x4, 0x5}, 2, 2, 2L, 4L));
  }

  public static Stream<Arguments> testS3BrsEdgeCases() {
    byte[] fourBytes = new byte[] {0x1, 0x2, 0x3, 0x4};
    byte[] sixteenBytes = new byte[16];
    for (int i = 0; i < 16; i++) sixteenBytes[i] = (byte) i;

    byte[] sixtyFourBytes = new byte[64];
    for (int i = 0; i < 64; i++) sixtyFourBytes[i] = (byte) (i * 2);

    byte[] oneKilobyte = new byte[1024];
    for (int i = 0; i < 1024; i++) oneKilobyte[i] = (byte) (255 - (i % 256));

    return Stream.of(
        // Basic single-page ranges
        Arguments.of(fourBytes, 2, 4, 0L, 4L), // full page
        Arguments.of(fourBytes, 1, 4, 1L, 3L), // mid-section

        //  Boundary case: last byte of page
        Arguments.of(sixteenBytes, 2, 8, 7L, 8L), // last byte of page 0
        Arguments.of(sixteenBytes, 2, 8, 8L, 9L), // first byte of page 1

        // Multi-page read
        Arguments.of(sixteenBytes, 2, 4, 2L, 10L), // spans page 0–2

        // Stress LRU eviction
        Arguments.of(sixtyFourBytes, 2, 8, 0L, 40L), // uses 5 pages, 2 cached

        // Full overlap tests
        Arguments.of(oneKilobyte, 4, 256, 0L, 1024L), // full file read

        // Partial trailing page
        Arguments.of(oneKilobyte, 4, 256, 900L, 1024L), // ends inside last page

        // Very small read
        Arguments.of(oneKilobyte, 2, 128, 5L, 6L), // one byte read

        // Custom page size not power of 2
        Arguments.of(new byte[] {0xA, 0xB, 0xC, 0xD, 0xE, 0xF}, 2, 3, 2L, 5L) // page size = 3
        );
  }
}
