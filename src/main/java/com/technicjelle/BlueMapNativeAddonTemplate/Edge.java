package com.technicjelle.BlueMapNativeAddonTemplate;

import com.flowpowered.math.vector.Vector2d;

public class Edge {
    public Vector2d start;
    public Vector2d end;

    public Edge(Vector2d start, Vector2d end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge edge = (Edge) o;
        // Edges are undirected; check both start-end and end-start
        return (start.equals(edge.start) && end.equals(edge.end)) ||
                (start.equals(edge.end) && end.equals(edge.start));
    }

    @Override
    public int hashCode() {
        // Ensure undirected edges have the same hash code
        return start.hashCode() + end.hashCode();
    }
}

