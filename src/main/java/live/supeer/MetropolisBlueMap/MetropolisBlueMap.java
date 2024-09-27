package live.supeer.MetropolisBlueMap;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import com.technicjelle.BMUtils.BMNative.BMNLogger;
import com.technicjelle.BMUtils.BMNative.BMNMetadata;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MetropolisBlueMap implements Runnable {
	private BMNLogger logger;
	private HikariDataSource dataSource;

	@Override
	public void run() {
		String addonID;
		String addonVersion;
		try {
			addonID = BMNMetadata.getAddonID(this.getClass().getClassLoader());
			addonVersion = BMNMetadata.getKey(this.getClass().getClassLoader(), "version");
			logger = new BMNLogger(this.getClass().getClassLoader());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		logger.logInfo("Starting " + addonID + " " + addonVersion);
		BlueMapAPI.onEnable(onEnableListener);
		BlueMapAPI.onDisable(onDisableListener);
	}

	final private Consumer<BlueMapAPI> onEnableListener = api -> {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl("jdbc:mysql://localhost:3306/metropolis");
		hikariConfig.setUsername("bluemap");
		hikariConfig.setPassword("c6Ot^9Z98f!B#mbpCc&oNMZVMpzdYG");
		hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

		dataSource = new HikariDataSource(hikariConfig);

		for (BlueMapWorld world : api.getWorlds()) {
			for (BlueMapMap map : world.getMaps()) {
				map.getMarkerSets().clear();
				createCityMarkers(map);
			}
		}
	};

	final private Consumer<BlueMapAPI> onDisableListener = api -> {
		if (dataSource != null) {
			dataSource.close();
		}
	};

	private void createCityMarkers(BlueMapMap map) {
		String query = "SELECT * FROM mp_cities WHERE isOpen = 1";
		MarkerSet markerSet = MarkerSet.builder()
				.label("Städer")
				.toggleable(true)
				.defaultHidden(false)
				.build();
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(query);
			 ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				int cityId = resultSet.getInt("cityId");
				String cityName = resultSet.getString("cityName");
				List<Marker> markers = getCityMarkers(cityId);
				for (Marker marker : markers) {
					markerSet.put(cityName, marker);
				}
			}
			map.getMarkerSets().put("Städer", markerSet);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private List<Marker> getCityMarkers(int cityId) {
		CityChunk cityChunk = new CityChunk(0, 0);
		List<CityChunk> chunks = cityChunk.fetchCityChunks(cityId, dataSource);
		List<Set<CityChunk>> groups = cityChunk.groupChunks(chunks);
		List<Marker> markers = new ArrayList<>();
		CityInfo cityInfo = new CityInfo("", 0);
		cityInfo = cityInfo.fetchCityInfo(cityId, dataSource);
		for (Set<CityChunk> group : groups) {
			List<Vector2d> shape = cityChunk.generatePolygonFromChunks(group);
			List<Shape> holes = cityChunk.generateHoleShapes(group, chunks);
			if (!shape.isEmpty()) {
				Vector3d position = new Vector3d(shape.get(0).getX(), 64, shape.get(0).getY());
				ShapeMarker marker = MarkerCreator.createShapeMarker(
						cityInfo.name,
						position,
						shape,
						cityInfo.name,
						64,
						new Color(72, 202, 2, 1.0f),
						new Color(69, 135, 33, 0.3f),
						holes
				);
				markers.add(marker);
			}
		}
		return markers;
	}
}
