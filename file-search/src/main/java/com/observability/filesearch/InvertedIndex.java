package com.observability.filesearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InvertedIndex {

  private final Map<String, Map<Path, Integer>> index = new HashMap<>();

  public void indexDirectoryBfs(Path root) throws IOException {
    Deque<Path> queue = new ArrayDeque<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      Path current = queue.removeFirst();
      if (Files.isDirectory(current)) {
        try (var stream = Files.list(current)) {
          stream.forEach(queue::addLast);
        }
      } else if (Files.isRegularFile(current) && current.toString().endsWith(".java")) {
        indexFile(current);
      }
    }
  }

  public void indexFile(Path file) throws IOException {
    String content = Files.readString(file).toLowerCase(Locale.ROOT);
    String[] tokens = content.split("[^a-z0-9_]+");
    for (String token : tokens) {
      if (token.isBlank()) {
        continue;
      }
      Map<Path, Integer> postings = index.computeIfAbsent(token, ignored -> new HashMap<>());
      postings.merge(file, 1, Integer::sum);
    }
  }

  public List<RankedDocument> search(String term, int limit) {
    String key = term.toLowerCase(Locale.ROOT);
    Map<Path, Integer> postings = index.getOrDefault(key, Map.of());

    List<RankedDocument> ranked = new ArrayList<>();
    for (Map.Entry<Path, Integer> entry : postings.entrySet()) {
      ranked.add(new RankedDocument(entry.getKey(), entry.getValue()));
    }

    ranked.sort(Comparator.comparingInt(RankedDocument::score).reversed());
    if (ranked.size() > limit) {
      return ranked.subList(0, limit);
    }
    return ranked;
  }

  public int tokenCount() {
    return index.size();
  }
}
