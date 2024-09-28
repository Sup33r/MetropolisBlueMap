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
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class MetropolisBlueMap implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(MetropolisBlueMap.class);
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
				createPlotMarkers(map);
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
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(query);
			 ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				int cityId = resultSet.getInt("cityId");
				String cityName = resultSet.getString("cityName");
				MarkerSet markerSet = MarkerSet.builder()
						.label(cityName)
						.toggleable(true)
						.defaultHidden(false)
						.build();
				List<Marker> markers = getCityMarkers(cityId);
				int i = 1;
				for (Marker marker : markers) {
					markerSet.put(cityName + " #" +i, marker);
					i++;
				}
				map.getMarkerSets().put("city-" + cityId, markerSet);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private List<Marker> getCityMarkers(int cityId) {
		CityChunk cityChunk = new CityChunk(0, 0);
		List<CityChunk> claimedChunks = cityChunk.fetchCityChunks(cityId, dataSource);
		List<Set<CityChunk>> groups = cityChunk.groupChunks(claimedChunks);
		List<Marker> markers = new ArrayList<>();
		CityInfo cityInfo = new CityInfo("", 0);
		cityInfo = cityInfo.fetchCityInfo(cityId, dataSource);

		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;

		for (CityChunk chunk : claimedChunks) {
			if (chunk.x < minX) minX = chunk.x;
			if (chunk.x > maxX) maxX = chunk.x;
			if (chunk.z < minZ) minZ = chunk.z;
			if (chunk.z > maxZ) maxZ = chunk.z;
		}

		minX -= 1;
		maxX += 1;
		minZ -= 1;
		maxZ += 1;

		List<CityChunk> allChunks = new ArrayList<>();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				allChunks.add(new CityChunk(x, z));
			}
		}

		for (Set<CityChunk> group : groups) {
			List<Vector2d> shape = cityChunk.generatePolygonFromChunks(group);
			List<Shape> holes = cityChunk.generateHoleShapes(group, allChunks);
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

	private void createPlotMarkers(BlueMapMap map) {
		List<Plot> plots = fetchPlots();

		// Group plots by city
		Map<Integer, List<Plot>> plotsByCity = new HashMap<>();
		for (Plot plot : plots) {
			plotsByCity.computeIfAbsent(plot.cityId, k -> new ArrayList<>()).add(plot);
		}

		for (Map.Entry<Integer, List<Plot>> entry : plotsByCity.entrySet()) {
			int cityId = entry.getKey();
			List<Plot> cityPlots = entry.getValue();

			// Create or get the marker set for the city
			String markerSetId = "city-" + cityId;
			MarkerSet markerSet = map.getMarkerSets().get(markerSetId);
			if (markerSet == null) {
				markerSet = MarkerSet.builder()
						.label(cityPlots.get(0).cityName)
						.toggleable(true)
						.defaultHidden(false)
						.build();
				map.getMarkerSets().put(markerSetId, markerSet);
			}

			for (Plot plot : cityPlots) {
				if (plot.plotPoints.isEmpty()) continue;

				// Build the shape
				Shape shape = Shape.builder().addPoints(plot.plotPoints).build();

				// Position is the first point
				Vector2d firstPoint = plot.plotPoints.get(0);
				Vector3d position = new Vector3d(firstPoint.getX(), plot.plotYMin, firstPoint.getY());

				// Prepare the detail (tooltip) text
				String detail = "Tomt: " + plot.plotName + "<br/>" +
						"Ägare: " + (plot.plotOwnerUUID != null ? plot.plotOwnerUUID : "ingen");

				// Create the extrude marker
				ExtrudeMarker extrudeMarker = MarkerCreator.createExtrudeMarker(
						plot.plotName,
						position,
						shape,
						detail,
						plot.plotYMin,
						plot.plotYMax,
						new Color(0, 0, 255, 0.5f),
						new Color(0, 0, 255, 0.1f)
				);
				String markerId = "plot-" + plot.plotId;
				markerSet.put(markerId, extrudeMarker);
			}
		}
	}

	private List<Plot> fetchPlots() {
		List<Plot> plots = new ArrayList<>();
		//select mp_plots where plotCenter starts with world
		String query = "SELECT * FROM mp_plots WHERE plotCenter LIKE 'world%'";

		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(query);
			 ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				int plotId = resultSet.getInt("plotId");
				int cityId = resultSet.getInt("cityId");
				String cityName = resultSet.getString("cityName");
				String plotName = resultSet.getString("plotName");
				String plotOwnerUUID = resultSet.getString("plotOwnerUUID");
				String plotPointsStr = resultSet.getString("plotPoints");
				int plotYMin = resultSet.getInt("plotYMin");
				int plotYMax = resultSet.getInt("plotYMax");

				List<Vector2d> plotPoints = parsePlotPoints(plotPointsStr);

				Plot plot = new Plot(plotId, cityId, cityName, plotName, plotOwnerUUID,
						plotPoints, plotYMin, plotYMax);

				plots.add(plot);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return plots;
	}

	private List<Vector2d> parsePlotPoints(String plotPointsStr) {
		List<Vector2d> plotPoints = new ArrayList<>();
		String[] tokens = plotPointsStr.trim().split("\\s+");
		for (int i = 0; i < tokens.length; i += 2) {
			try {
				double x = Double.parseDouble(tokens[i]);
				double z = Double.parseDouble(tokens[i + 1]);

				x += 0.5;
				z += 0.5;
				plotPoints.add(new Vector2d(x, z));
			} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
		return plotPoints;
	}

}
