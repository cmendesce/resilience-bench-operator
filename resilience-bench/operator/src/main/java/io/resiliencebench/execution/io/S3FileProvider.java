package io.resiliencebench.execution.io;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@Component("s3FileProvider")
public class S3FileProvider implements FileProvider {

  private final static Logger logger = LoggerFactory.getLogger(S3FileProvider.class);

  private final AmazonS3 s3Client;
  private final String bucketName;

  public S3FileProvider(@Value("${AWS_BUCKET_NAME:none}") String bucketName, AmazonS3 s3Client) {
    this.bucketName = bucketName; // TODO abstrair para CRD Benchmark
    this.s3Client = s3Client;
  }

  @Override
  public void writeToFile(String resultFile, String content) {
    try {
      byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
      InputStream inputStream = new ByteArrayInputStream(contentBytes);
      var metadata = new ObjectMetadata();
      metadata.setContentLength(contentBytes.length);
      var putObjectRequest = new PutObjectRequest(bucketName, resultFile, inputStream, metadata);
      s3Client.putObject(putObjectRequest);
      logger.info("File {} uploaded to bucket {}.", resultFile, bucketName);
    } catch (Exception e) {
      logger.warn("Error writing file {}. {}", resultFile, e.getMessage());
    }
  }

  @Override
  public Optional<String> getFileAsString(String resultFile) {
    try {
      InputStream inputStream = s3Client.getObject(new GetObjectRequest(bucketName, resultFile)).getObjectContent();
      String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      return of(content);
    } catch (Exception e) {
      logger.warn("Error reading file {}. {}", resultFile, e.getMessage());
      return empty();
    }
  }
}
