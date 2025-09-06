package io.resiliencebench.execution.io;

import java.util.Optional;

public interface FileProvider {

  void writeToFile(String resultFile, String content);
  void writeToFile(String resultFile, String content, String contentType);

  Optional<String> getFileAsString(String resultFile);
}
