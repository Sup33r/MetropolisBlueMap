package live.supeer.MetropolisBlueMap;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import com.technicjelle.BMUtils.BMNative.BMNLogger;
import com.technicjelle.BMUtils.BMNative.BMNMetadata;
import com.technicjelle.UpdateChecker;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.common.api.BlueMapWorldImpl;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MetropolisBlueMap implements Runnable {
	private BMNLogger logger;
	private UpdateChecker updateChecker;
	private @Nullable Config config;
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
		updateChecker = new UpdateChecker("TechnicJelle", addonID, addonVersion);
		updateChecker.checkAsync();
		BlueMapAPI.onEnable(onEnableListener);
		BlueMapAPI.onDisable(onDisableListener);
	}

	final private Consumer<BlueMapAPI> onEnableListener = api -> {
		updateChecker.getUpdateMessage().ifPresent(logger::logWarning);

		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl("jdbc:mysql://localhost:3306/metropolis");
		hikariConfig.setUsername("bluemap");
		hikariConfig.setPassword("c6Ot^9Z98f!B#mbpCc&oNMZVMpzdYG");
		hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

		dataSource = new HikariDataSource(hikariConfig);

		try {
			config = Config.load(api);
		} catch (IOException e) {
			config = null;
			throw new RuntimeException(e);
		}

		for (BlueMapWorld world : api.getWorlds()) {
			BlueMapWorldImpl worldImpl = (BlueMapWorldImpl) world;
			for (BlueMapMap map : world.getMaps()) {
				map.getMarkerSets().clear();
				createMarkersForCity(map, 1);
			}
		}
	};

	final private Consumer<BlueMapAPI> onDisableListener = api -> {
		if (dataSource != null) {
			dataSource.close();
		}
		if (config == null) return;
		logger.logInfo("Goodbye, " + config.getWorld() + "!");
	};

	private void createMarkersForCity(BlueMapMap map, int cityId) {
		CityChunk cityChunk = new CityChunk(0, 0); // Dummy initialization
		List<CityChunk> chunks = cityChunk.fetchCityChunks(cityId, dataSource);
		List<Set<CityChunk>> groups = cityChunk.groupChunks(chunks);

		MarkerSet markerSet = MarkerSet.builder()
				.label("City " + cityId + " Markers")
				.toggleable(true)
				.defaultHidden(false)
				.build();

		for (Set<CityChunk> group : groups) {
			logger.logInfo("Group of size " + group.size());

			List<Vector2d> shape = cityChunk.generatePolygonFromChunks(group);
			logger.logInfo("Shape size: " + shape.size());
			for (Vector2d point : shape) {
				logger.logInfo("Shape position: " + point.getX() + ", " + point.getY());
			}

			if (!shape.isEmpty()) {
				Vector3d position = new Vector3d(shape.get(0).getX(), 64, shape.get(0).getY());
				ShapeMarker marker = MarkerCreator.createShapeMarker(
						"City " + cityId + " Group",
						position,
						shape,
						"Group of city chunks",
						64
				);
				markerSet.getMarkers().put("City " + cityId + " Group", marker);
				logger.logInfo("Added group marker");
			}
		}

		logger.logInfo("Added all markers");
		map.getMarkerSets().put("City " + cityId + " Markers", markerSet);
	}
}
