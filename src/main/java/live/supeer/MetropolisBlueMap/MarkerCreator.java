package live.supeer.MetropolisBlueMap;

import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.List;

public class MarkerCreator {
    public static ShapeMarker createShapeMarker(String name, Vector3d position, List<Vector2d> vector2ds, String detail, float y, Color lineColor, Color fillColor, List<Shape> holes) {
        Shape shape = Shape.builder().addPoints(vector2ds).build();
        return ShapeMarker.builder()
                .label(name)
                .position(position)
                .shape(shape, y)
                .detail(detail)
                .lineWidth(5)
                .lineColor(lineColor)
                .fillColor(fillColor)
                .sorting(0)
                .listed(true)
                .holes(holes.toArray(new Shape[0]))
                .depthTestEnabled(false)
                .minDistance(10)
                .maxDistance(2500)
                .build();
    }

    public static ExtrudeMarker createExtrudeMarker(String name, Vector3d position, Shape shape, String detail, float minY, float maxY, Color lineColor, Color fillColor) {
        return ExtrudeMarker.builder()
                .label(name)
                .position(position)
                .shape(shape, minY, maxY)
                .detail(detail)
                .lineWidth(5)
                .lineColor(lineColor)
                .fillColor(fillColor)
                .sorting(0)
                .listed(true)
                .depthTestEnabled(false)
                .minDistance(12)
                .maxDistance(2500)
                .build();
    }
}