package com.observability.filesearch;

import java.io.IOException;
import java.nio.file.Path;

public class FileSearchApplication {

  public static void main(String[] args) {
    Path root = args.length > 0 ? Path.of(args[0]) : Path.of(".");
    InvertedIndex index = new InvertedIndex();

    try {
      index.indexDirectoryBfs(root);
      System.out.println("file-search scaffold is running.");
      System.out.println("Indexed unique tokens: " + index.tokenCount());

      for (RankedDocument doc : index.search("log4j2", 5)) {
        System.out.println("match: " + doc.path() + " score=" + doc.score());
      }
    } catch (IOException exception) {
      System.err.println("Indexing failed: " + exception.getMessage());
    }
  }
}
