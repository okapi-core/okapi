package org.okapi.abstractio;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

@AllArgsConstructor
public class NodeIdSeparatedBinFilesPrefixRegistry implements BinFilesPrefixRegistry {
  String whoAmI;
  S3Client s3Client;

  @Override
  public String getRootPrefixForLogBinFile(String base, String streamId, long hrBlock) {
    return base + "/" + streamId + "/" + hrBlock;
  }

  @Override
  public String getRootPrefixForLogBinFileForMe(
      String bucket, String base, String streamId, String partName, long hrBlock) {
    return getRootPrefixForLogBinFile(base, streamId, hrBlock)
        + "/"
        + whoAmI
        + "/"
        + partName
        + "."
        + UUID.randomUUID().toString();
  }

  @Override
  public List<String> getAllPrefixesForLogBinFile(
      String bucket, String base, String streamId, String partName, long hrBlock) {
    var prefixQuery = getRootPrefixForLogBinFile(base, streamId, hrBlock) + "/";
    var prefixes = s3Client.listObjectsV2Paginator(b -> b.bucket(bucket).prefix(prefixQuery));
    return prefixes.stream()
        .flatMap(r -> r.contents().stream())
        .map(S3Object::key)
        .filter(key -> key.startsWith(prefixQuery))
        .toList();
  }
}
