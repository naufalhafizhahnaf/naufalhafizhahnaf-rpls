package ihsse.dao;

import ihsse.db.DataAccessException;
import ihsse.db.Database;
import ihsse.model.VideoTutorial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO untuk tabel video_tutorial.
 */
public class VideoDao {

    public List<VideoTutorial> findAll() {
        String sql = "SELECT id, judul, topik, durasi_detik, url "
                + "FROM video_tutorial ORDER BY id";
        List<VideoTutorial> daftar = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                daftar.add(new VideoTutorial(
                        resultSet.getString("id"),
                        resultSet.getString("judul"),
                        resultSet.getString("topik"),
                        resultSet.getInt("durasi_detik"),
                        resultSet.getString("url")));
            }
            return daftar;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal mengambil video tutorial: "
                    + e.getMessage(), e);
        }
    }

    public void insert(VideoTutorial video) {
        String sql = "INSERT INTO video_tutorial (id, judul, topik, durasi_detik, url) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, video.getId());
            statement.setString(2, video.getJudul());
            statement.setString(3, video.getTopik());
            statement.setInt(4, video.getDurasiDetik());
            statement.setString(5, video.getUrl());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menyimpan video tutorial: "
                    + e.getMessage(), e);
        }
    }

    public boolean delete(String id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM video_tutorial WHERE id = ?")) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghapus video tutorial: "
                    + e.getMessage(), e);
        }
    }

    public int count() {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM video_tutorial");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("Gagal menghitung video tutorial: "
                    + e.getMessage(), e);
        }
    }
}
