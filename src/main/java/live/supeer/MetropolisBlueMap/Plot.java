package live.supeer.MetropolisBlueMap;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import com.zaxxer.hikari.HikariDataSource;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Plot {
    public int plotId;
    public int cityId;
    public String cityName;
    public String plotName;
    public String plotOwnerUUID;
    public List<Vector2d> plotPoints; // List of (x, z) coordinates
    public int plotYMin;
    public int plotYMax;

    public Plot(int plotId, int cityId, String cityName, String plotName, String plotOwnerUUID,
                List<Vector2d> plotPoints, int plotYMin, int plotYMax) {
        this.plotId = plotId;
        this.cityId = cityId;
        this.cityName = cityName;
        this.plotName = plotName;
        this.plotOwnerUUID = plotOwnerUUID;
        this.plotPoints = plotPoints;
        this.plotYMin = plotYMin;
        this.plotYMax = plotYMax;
    }
}
