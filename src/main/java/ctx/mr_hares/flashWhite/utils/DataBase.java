package ctx.mr_hares.flashWhite.utils;

import ctx.mr_hares.flashWhite.FlashWhite;
import net.md_5.bungee.api.ChatColor;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DataBase {
    private Connection connection;
    private static final DateTimeFormatter DEFAULT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void initializeDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + FlashWhite.getInstance().getDataFolder() +
                    "/saves.db");

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(String.format("CREATE TABLE IF NOT EXISTS %s (" +
                        "uuid TEXT NOT NULL," +
                        "nick TEXT NOT NULL," +
                        "moder TEXT NOT NULL," +
                        "time TEXT NOT NULL)", "user_list"));
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(String.format("CREATE TABLE IF NOT EXISTS %s (" +
                        "channel_id TEXT NOT NULL," +
                        "user_id TEXT NOT NULL," +
                        "nick TEXT NOT NULL)", "ticket_list"));
            }
            FlashWhite.sendConsole("(FlashWhite) Выполнено подключение к базе данных");
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().warning(ChatColor.RED + "Ошибка подключения к базе данных");
        }
    }

    public void addPlayer(String UUID, String nick, String moder) {
        String sql = "INSERT INTO user_list (uuid, nick, moder, time) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID);
            pstmt.setString(2, nick.toLowerCase());
            pstmt.setString(3, moder);
            pstmt.setString(4, "unlimit");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
        }
    }

    public void createTicket(String channel_id, String user_id, String nick) {
        String sql = "INSERT INTO ticket_list (channel_id, user_id, nick) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, channel_id);
            pstmt.setString(2, user_id);
            pstmt.setString(3, nick);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
        }
    }

    public void addTempPlayer(String UUID, String nick, String moder, Integer days, Integer hours, Integer minutes,
                              Integer seconds) {
        String sql = "INSERT INTO user_list (uuid, nick, moder, time) VALUES (?, ?, ?, ?)";

        LocalDateTime currentDate = LocalDateTime.now();
        currentDate = currentDate.plusDays(days).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        String dateString = DEFAULT_FORMATTER.format(currentDate);

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID);
            pstmt.setString(2, nick.toLowerCase());
            pstmt.setString(3, moder);
            pstmt.setString(4, dateString);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
        }
    }

    public boolean isWhite(UUID UUID) {
        String sql = "SELECT time FROM user_list WHERE uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                if (Objects.equals(rs.getString("time"), "unlimit")) { return true; }
                else {
                    if (getRemainingTime(rs.getString("time")) == null) {
                        removePlayer(UUID);
                        return false;
                    }
                    else {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
            return false;
        }
    }

    public void SetUUID(String nick, UUID UUID) {
        String sql = "UPDATE user_list SET uuid = ? WHERE nick = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.toString());
            pstmt.setString(2, nick.toLowerCase());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
        }
    }

    public boolean isWhite(String nick) {
        String sql = "SELECT time FROM user_list WHERE nick = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nick.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                if (Objects.equals(rs.getString("time"), "unlimit")) { return true; }
                else {
                    if (getRemainingTime(rs.getString("time")) == null) {
                        removePlayer(nick);
                        return false;
                    }
                    else {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
            return false;
        }
    }

    public long[] getTime(UUID UUID) {
        String sql = "SELECT time FROM user_list WHERE uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                if (Objects.equals(rs.getString("time"), "unlimit")) { return new long[]{0, 0, 0, 0}; }
                else {
                    return getRemainingTime(rs.getString("time"));
                }
            }
            return null;
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
            return null;
        }
    }

    public UUID getUUID(String nick) {
        String sql = "SELECT uuid FROM user_list WHERE nick = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nick.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                if (Objects.equals(rs.getString("uuid"), "not")) { return null; }
                return UUID.fromString(rs.getString("uuid"));
            }
            return null;
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
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
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
        }

        return list;
    }

    public void removePlayer(UUID UUID) {
        String sql = "DELETE FROM user_list WHERE uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
        }
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

    public void removeTicket(String channel_id) {
        String sql = "DELETE FROM ticket_list WHERE channel_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, channel_id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
        }
    }

    public void removePlayer(String nick) {
        String sql = "DELETE FROM user_list WHERE nick = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nick.toLowerCase());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            FlashWhite.getInstance().getLogger().info(e.toString());
        }
    }

    public static long[] getRemainingTime(String dateString) {
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
