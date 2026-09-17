package ru.mr_hares.flashWhite.utils;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DataBase {
    private Connection connection;
    private final String player_list = "user_list";
    private static final DateTimeFormatter DEFAULT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DataBase(Plugin plugin) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() +
                "/data.db");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format("CREATE TABLE IF NOT EXISTS %s (" +
                    "nick TEXT NOT NULL," +
                    "uuid TEXT NOT NULL," +
                    "moder TEXT NOT NULL," +
                    "time TEXT NOT NULL," +
                    "discordID TEXT NOT NULL)", player_list));
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format("CREATE TABLE IF NOT EXISTS %s (" +
                    "channel_id TEXT NOT NULL," +
                    "user_id TEXT NOT NULL," +
                    "nick TEXT NOT NULL)", "ticket_list"));
        }
    }

    public void addPlayer(String nick, String moder, String discordID) {
        try (PreparedStatement stmt =
                     connection.prepareStatement(String.format("INSERT INTO %s (nick, uuid, moder, time, discordID) " +
                             "VALUES (?, ?, ?, ?, ?)", player_list))) {
            stmt.setString(1, nick);
            stmt.setString(2, "null");
            stmt.setString(3, moder);
            stmt.setString(4, "forever");
            stmt.setString(5, discordID);

            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public void addTempPlayer(String nick, String moder, String discordID, Integer seconds, Integer minutes,
                              Integer hours,
                              Integer days) {
        try (PreparedStatement stmt =
                     connection.prepareStatement(String.format("INSERT INTO %s (nick, uuid, moder, time, discordID) " +
                             "VALUES (?, ?, ?, ?, ?)", player_list))) {
            stmt.setString(1, nick);
            stmt.setString(2, "null");
            stmt.setString(3, moder);

            LocalDateTime currentDate =
                     LocalDateTime.now().plusDays(days).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);

            stmt.setString(4, DEFAULT_FORMATTER.format(currentDate));
            stmt.setString(5, discordID);

            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public void createTicket(String channel_id, String user_id, String nick) {
        String sql = "INSERT INTO ticket_list (channel_id, user_id, nick) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, channel_id);
            pstmt.setString(2, user_id);
            pstmt.setString(3, nick);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public void setUUID(String nick, UUID uuid) {
        try (PreparedStatement stmt =
                     connection.prepareStatement(String.format("UPDATE %s SET uuid = ? WHERE nick = ?", player_list))) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, nick);

            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public String[] getTicket(String channel_id) {
        String sql = "SELECT user_id, nick FROM ticket_list WHERE channel_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, channel_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new String[]{rs.getString("user_id"), rs.getString("nick")};
            }
            return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public String[] getTicket(long user_id) {
        String sql = "SELECT user_id, nick FROM ticket_list WHERE user_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(user_id));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new String[]{rs.getString("user_id"), rs.getString("nick")};
            }
            return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public List<String[]> getPlayers() {
        String sql = "SELECT uuid, nick, moder FROM user_list";
        List<String[]> list = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new String[]{rs.getString("uuid"), rs.getString("nick"), rs.getString("moder")});
            }
        } catch (SQLException ignored) {}

        return list;
    }

    public void removeTicket(String channel_id) {
        String sql = "DELETE FROM ticket_list WHERE channel_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, channel_id);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public Object[] getInfoPlayer(String nick, @Nullable UUID uuid) {
        try (PreparedStatement stmt =
                     connection.prepareStatement(String.format("SELECT * FROM %s WHERE nick = ? OR uuid = ?",
                             player_list))) {
            stmt.setString(1, nick);
            stmt.setString(2, uuid == null ? "null" : uuid.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Object[]{rs.getString("nick"), rs.getString("uuid"), rs.getString("moder"),
                        upTime(rs.getString("time")), rs.getString("discordID")};
            }
        } catch (SQLException ignored) {}

        return null;
    }

    public void removePlayer(String nick) {
        try (PreparedStatement stmt =
                     connection.prepareStatement(String.format("DELETE FROM %s WHERE nick = ?", player_list))) {
            stmt.setString(1, nick);
            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private boolean upTime(String time) {
        return getRemainingTime(time) == null;
    }

    public static long[] getRemainingTime(String dateString) {
        if (dateString.equals("forever")) return new long[]{0, 0, 0, 0};

        LocalDateTime targetDate = LocalDateTime.parse(dateString, DEFAULT_FORMATTER);
        LocalDateTime now = LocalDateTime.now();

        if (targetDate.isBefore(now)) {
            return null;
        }

        long days = ChronoUnit.DAYS.between(now, targetDate);
        long hours = ChronoUnit.HOURS.between(now, targetDate) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, targetDate) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, targetDate) % 60;

        return new long[]{days, hours, minutes, seconds};
    }
}