package com.safenavi.backend.graphbuild;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "graph-build")
public record GraphBuildProperties(String pbfPath) {
}
