package live.supeer.MetropolisBlueMap;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CityInfo {
    public String name;
    public int id;

    public CityInfo(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public CityInfo fetchCityInfo(int cityId, HikariDataSource dataSource) {
        String query = "SELECT * FROM mp_cities WHERE cityId = ?";
        CityInfo cityInfo = null;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, cityId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    cityInfo = new CityInfo(resultSet.getString("cityName"), resultSet.getInt("cityId"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cityInfo;
    }
}
