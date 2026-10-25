package com.safenavi.backend.graphbuild;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.collections.impl.map.mutable.primitive.LongDoubleHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap;
import org.springframework.stereotype.Component;

import com.safenavi.backend.common.geo.BoundingBox;

import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

/**
 * Reads walkable ways from an OSM PBF in two passes (ways first, then only the coordinates of
 * nodes those ways use), keeping memory to primitive maps sized by the walkable network.
 */
@Component
public class OsmPbfGraphReader {

    public record ReadResult(int waysRead, List<RawSegment> segments) {
    }

    public ReadResult read(Path pbf, BoundingBox clip) throws IOException {
        List<long[]> ways = new ArrayList<>();
        LongIntHashMap refCounts = new LongIntHashMap();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(pbf))) {
            for (EntityContainer container : new PbfIterator(in, false)) {
                if (container.getType() != EntityType.Way) {
                    continue;
                }
                OsmWay way = (OsmWay) container.getEntity();
                if (way.getNumberOfNodes() < 2 || !WalkableWayFilter.isWalkable(OsmModelUtil.getTagsAsMap(way))) {
                    continue;
                }
                long[] nodeIds = new long[way.getNumberOfNodes()];
                for (int i = 0; i < nodeIds.length; i++) {
                    nodeIds[i] = way.getNodeId(i);
                    refCounts.addToValue(nodeIds[i], 1);
                }
                ways.add(nodeIds);
            }
        }

        LongDoubleHashMap lats = new LongDoubleHashMap();
        LongDoubleHashMap lngs = new LongDoubleHashMap();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(pbf))) {
            for (EntityContainer container : new PbfIterator(in, false)) {
                if (container.getType() == EntityType.Way) {
                    break; // PBF files list all nodes before ways
                }
                if (container.getType() != EntityType.Node) {
                    continue;
                }
                OsmNode node = (OsmNode) container.getEntity();
                if (!refCounts.containsKey(node.getId())) {
                    continue;
                }
                if (clip != null && !clip.contains(node.getLatitude(), node.getLongitude())) {
                    continue;
                }
                lats.put(node.getId(), node.getLatitude());
                lngs.put(node.getId(), node.getLongitude());
            }
        }
        return new ReadResult(ways.size(), WaySplitter.split(ways, refCounts, lats, lngs));
    }
}
