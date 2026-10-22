package com.safenavi.backend.graphbuild;

public record GraphBuildResult(String regionCode, String pbfPath, int waysRead, int nodesSaved, int segmentsSaved,
        int nodesDeleted, int segmentsDeleted, long elapsedMillis) {
}
