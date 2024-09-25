package live.supeer.MetropolisBlueMap;

import de.bluecolored.bluemap.api.markers.ShapeMarker;
import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import java.util.List;

public class MarkerCreator {
    public static ShapeMarker createShapeMarker(String name, Vector3d position, List<Vector2d> vector2ds, String detail, float y) {
        Shape shape = Shape.builder().addPoints(vector2ds).build();
        Color lineColor = new Color(72,202,2,1.0f);
        Color fillColor = new Color(69,135,33,0.3f);

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
                .depthTestEnabled(false)
                .minDistance(10)
                .maxDistance(10000000)
                .build();
    }
}