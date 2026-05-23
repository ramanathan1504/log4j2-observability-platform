package com.observability.filesearch;

import java.nio.file.Path;

public record RankedDocument(Path path, int score) {}
