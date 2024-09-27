package live.supeer.MetropolisBlueMap;

import com.flowpowered.math.vector.Vector2d;
import com.zaxxer.hikari.HikariDataSource;
import de.bluecolored.bluemap.api.math.Shape;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CityChunk {
    public int x;
    public int z;

    public CityChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getRealX() {
        return x * 16;
    }

    public int getRealZ() {
        return z * 16;
    }

    public List<CityChunk> fetchCityChunks(int cityId, HikariDataSource dataSource) {
        List<CityChunk> chunks = new ArrayList<>();
        String query = "SELECT * FROM mp_claims WHERE world = ? AND cityId = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, "world");
            statement.setInt(2, cityId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    chunks.add(new CityChunk(resultSet.getInt("xPosition"), resultSet.getInt("zPosition")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return chunks;
    }

    public List<Set<CityChunk>> groupChunks(List<CityChunk> chunks) {
        List<Set<CityChunk>> groups = new ArrayList<>();
        Set<CityChunk> visited = new HashSet<>();

        for (CityChunk chunk : chunks) {
            if (!visited.contains(chunk)) {
                Set<CityChunk> group = new HashSet<>();
                exploreGroup(chunk, chunks, group, visited);
                groups.add(group);
            }
        }
        return groups;
    }

    private void exploreGroup(CityChunk chunk, List<CityChunk> chunks, Set<CityChunk> group, Set<CityChunk> visited) {
        if (visited.contains(chunk)) return;
        visited.add(chunk);
        group.add(chunk);

        for (CityChunk neighbor : getNeighbors(chunk, chunks)) {
            exploreGroup(neighbor, chunks, group, visited);
        }
    }

    private List<CityChunk> getNeighbors(CityChunk chunk, List<CityChunk> chunks) {
        List<CityChunk> neighbors = new ArrayList<>();
        for (CityChunk c : chunks) {
            if (Math.abs(c.x - chunk.x) <= 1 && Math.abs(c.z - chunk.z) <= 1 && !(c.x == chunk.x && c.z == chunk.z)) {
                neighbors.add(c);
            }
        }
        return neighbors;
    }

    public List<Shape> generateHoleShapes(Set<CityChunk> group, List<CityChunk> allChunks) {
        List<Shape> holeShapes = new ArrayList<>();
        List<Set<CityChunk>> holes = identifyHoles(group, allChunks);

        for (Set<CityChunk> holeGroup : holes) {
            List<Vector2d> holeBoundary = generatePolygonFromChunks(holeGroup);
            if (!holeBoundary.isEmpty()) {
                Shape holeShape = Shape.builder().addPoints(holeBoundary).build();
                holeShapes.add(holeShape);
            }
        }
        return holeShapes;
    }

    public List<CityChunk> getEdgeChunks(Set<CityChunk> group) {
        List<CityChunk> edges = new ArrayList<>();
        for (CityChunk chunk : group) {
            boolean isEdge = false;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    CityChunk neighbor = new CityChunk(chunk.x + dx, chunk.z + dz);
                    if (!group.contains(neighbor)) {
                        isEdge = true;
                        break;
                    }
                }
                if (isEdge) break;
            }
            if (isEdge) {
                edges.add(chunk);
            }
        }
        return edges;
    }

    public List<CityChunk> traceBoundary(Set<CityChunk> group) {
        List<CityChunk> boundary = new ArrayList<>();

        // Find a starting edge chunk
        CityChunk startChunk = null;
        for (CityChunk chunk : group) {
            if (isEdgeChunk(chunk, group)) {
                startChunk = chunk;
                break;
            }
        }

        if (startChunk == null) {
            return boundary; // No edge chunks found
        }

        CityChunk currentChunk = startChunk;
        CityChunk previousChunk = null;
        Set<CityChunk> visited = new HashSet<>();

        do {
            boundary.add(currentChunk);
            visited.add(currentChunk);
            CityChunk nextChunk = getNextBoundaryChunk(currentChunk, previousChunk, group);
            if (nextChunk == null || visited.contains(nextChunk)) break;
            previousChunk = currentChunk;
            currentChunk = nextChunk;
        } while (!currentChunk.equals(startChunk));

        return boundary;
    }


    private CityChunk getNextBoundaryChunk(CityChunk current, CityChunk previous, Set<CityChunk> group) {
        // Define the 8 possible directions (N, NE, E, SE, S, SW, W, NW)
        int[][] directions = {
                {0, -1}, {1, -1}, {1, 0}, {1, 1},
                {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
        };

        // Starting direction
        int startDir = 0;

        // If we have a previous chunk, set the starting direction accordingly
        if (previous != null) {
            int dx = current.x - previous.x;
            int dz = current.z - previous.z;
            for (int i = 0; i < directions.length; i++) {
                if (directions[i][0] == dx && directions[i][1] == dz) {
                    startDir = (i + 6) % 8; // Turn back two directions
                    break;
                }
            }
        }

        // Check the neighboring chunks in order
        for (int i = 0; i < directions.length; i++) {
            int dir = (startDir + i) % 8;
            int nx = current.x + directions[dir][0];
            int nz = current.z + directions[dir][1];
            CityChunk neighbor = new CityChunk(nx, nz);

            if (group.contains(neighbor) && isEdgeChunk(neighbor, group)) {
                return neighbor;
            }
        }

        // No next chunk found
        return null;
    }

    private boolean isEdgeChunk(CityChunk chunk, Set<CityChunk> group) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                CityChunk neighbor = new CityChunk(chunk.x + dx, chunk.z + dz);
                if (!group.contains(neighbor)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CityChunk)) return false;
        CityChunk cityChunk = (CityChunk) o;
        return x == cityChunk.x && z == cityChunk.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    public List<Vector2d> generatePolygonFromChunks(Set<CityChunk> group) {
        List<Vector2d> polygon = new ArrayList<>();

        // Create a set of all the edges (sides) of the chunks
        Set<Edge> edges = new HashSet<>();
        for (CityChunk chunk : group) {
            int x = chunk.getRealX();
            int z = chunk.getRealZ();

            // Define the four edges of the chunk
            Edge top = new Edge(new Vector2d(x, z), new Vector2d(x + 16, z));
            Edge right = new Edge(new Vector2d(x + 16, z), new Vector2d(x + 16, z + 16));
            Edge bottom = new Edge(new Vector2d(x + 16, z + 16), new Vector2d(x, z + 16));
            Edge left = new Edge(new Vector2d(x, z + 16), new Vector2d(x, z));

            // Add edges to the set; if the edge already exists, remove it (interior edge)
            if (!edges.add(top)) edges.remove(top);
            if (!edges.add(right)) edges.remove(right);
            if (!edges.add(bottom)) edges.remove(bottom);
            if (!edges.add(left)) edges.remove(left);
        }

        // Now, edges set contains only the outer edges. We need to order them to form a polygon.
        // Start from any edge and walk through connected edges
        if (edges.isEmpty()) return polygon;

        List<Edge> orderedEdges = orderEdges(edges);
        for (Edge edge : orderedEdges) {
            polygon.add(edge.start);
        }
        // Add the start point at the end to close the polygon
        polygon.add(orderedEdges.get(0).start);

        return polygon;
    }

    private List<Edge> orderEdges(Set<Edge> edges) {
        List<Edge> orderedEdges = new ArrayList<>();
        Map<Vector2d, Edge> edgeMap = new HashMap<>();

        // Map edges by their start point
        for (Edge edge : edges) {
            edgeMap.put(edge.start, edge);
        }

        // Start from an arbitrary edge
        Edge currentEdge = edges.iterator().next();
        orderedEdges.add(currentEdge);
        edgeMap.remove(currentEdge.start);

        while (edgeMap.size() > 0) {
            Edge nextEdge = edgeMap.get(currentEdge.end);
            if (nextEdge == null) {
                // The polygon is not closed; break to avoid infinite loop
                break;
            }
            orderedEdges.add(nextEdge);
            edgeMap.remove(nextEdge.start);
            currentEdge = nextEdge;
        }

        return orderedEdges;
    }

    private List<Set<CityChunk>> identifyHoles(Set<CityChunk> group, List<CityChunk> allChunks) {
        Set<CityChunk> unclaimedChunks = new HashSet<>(allChunks);
        unclaimedChunks.removeAll(group);

        List<Set<CityChunk>> holes = new ArrayList<>();
        Set<CityChunk> visited = new HashSet<>();

        for (CityChunk chunk : unclaimedChunks) {
            if (!visited.contains(chunk) && isSurrounded(chunk, group, allChunks)) {
                Set<CityChunk> hole = new HashSet<>();
                exploreHole(chunk, unclaimedChunks, hole, visited, group, allChunks);
                holes.add(hole);
            }
        }

        return holes;
    }

    private boolean isSurrounded(CityChunk chunk, Set<CityChunk> group, List<CityChunk> allChunks) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                CityChunk neighbor = new CityChunk(chunk.x + dx, chunk.z + dz);
                if (!group.contains(neighbor)) {
                    if (allChunks.contains(neighbor)) {
                        // Neighbor is unclaimed and not part of the group
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void exploreHole(CityChunk chunk, Set<CityChunk> unclaimedChunks, Set<CityChunk> hole, Set<CityChunk> visited, Set<CityChunk> group, List<CityChunk> allChunks) {
        if (visited.contains(chunk)) return;
        visited.add(chunk);
        hole.add(chunk);

        for (CityChunk neighbor : getNeighbors(chunk, new ArrayList<>(unclaimedChunks))) {
            if (!group.contains(neighbor) && isSurrounded(neighbor, group, allChunks)) {
                exploreHole(neighbor, unclaimedChunks, hole, visited, group, allChunks);
            }
        }
    }
}
