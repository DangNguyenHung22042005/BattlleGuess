package com.battleguess.battleguess.database;

import java.sql.*;

import com.battleguess.battleguess.model.AdminRoomRow;
import com.battleguess.battleguess.model.PlayerState;
import com.battleguess.battleguess.model.RoomInfo;
import com.battleguess.battleguess.service.ServerLogger;

import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlserver://HUNG\\SQLEXPRESS:1433;databaseName=BATTLEGUESS;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASSWORD = "123456789";

    private Connection connection;

    public DatabaseManager() throws SQLException {
        connect();
    }

    private void connect() throws SQLException {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
        System.out.println("✅ Connected to SQL Server successfully!");
        ServerLogger.success("✅ Connected to SQL Server successfully!");
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- PLAYER METHODS ---

    /**
     * Kiểm tra xem tên người chơi đã tồn tại trong database hay chưa.
     * @param username Tên người chơi cần kiểm tra.
     * @return true nếu tồn tại, false nếu không.
     * @throws SQLException
     */
    public boolean playerExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM Players WHERE Username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Nếu rs.next() là true, nghĩa là tìm thấy 1 bản ghi
            }
        }
    }

    /**
     * Thêm mới một player (Đăng ký).
     * @param username Tên người chơi.
     * @param hashedPassword Mật khẩu (nên được hash trước khi đưa vào đây).
     * @return true nếu đăng ký thành công, false nếu tên người chơi đã tồn tại.
     * @throws SQLException
     */
    public boolean addPlayer(String username, String hashedPassword) throws SQLException {
        if (playerExists(username)) {
            return false;
        }

        String sql = "INSERT INTO Players (Username, Password) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashedPassword);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Xác thực thông tin player (Đăng nhập).
     * @param username Tên người chơi.
     * @param plainTextPassword Mật khẩu người dùng nhập (chưa hash).
     * @return PlayerID nếu đăng nhập thành công, -1 nếu thất bại (sai tên hoặc sai mật khẩu).
     * @throws SQLException
     */
    public int validatePlayer(String username, String plainTextPassword) throws SQLException {
        String sql = "SELECT PlayerID, Password FROM Players WHERE Username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Tìm thấy user, kiểm tra mật khẩu
                    String storedHashedPassword = rs.getString("Password");

                    // Dùng BCrypt để so khớp
                    if (PasswordUtils.checkPassword(plainTextPassword, storedHashedPassword)) {
                        return rs.getInt("PlayerID"); // Đăng nhập thành công, trả về ID
                    }
                }
                return -1; // Sai tên đăng nhập hoặc mật khẩu
            }
        }
    }

    /**
     * Cập nhật mật khẩu mới cho người dùng.
     * @param username Tên đăng nhập
     * @param newHashedPassword Mật khẩu mới (đã hash)
     * @return true nếu cập nhật thành công (user tồn tại), false nếu không.
     */
    public boolean updatePassword(String username, String newHashedPassword) throws SQLException {
        String sql = "UPDATE Players SET Password = ? WHERE Username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setString(2, username);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0; // Trả về true nếu có dòng được cập nhật
        }
    }

    /**
     * Lấy danh sách các phòng do một người chơi cụ thể sở hữu.
     * @param ownerPlayerID ID của người chủ phòng.
     * @return Danh sách các đối tượng RoomInfo.
     * @throws SQLException
     */
    public List<RoomInfo> getRoomsByOwner(int ownerPlayerID) throws SQLException {
        List<RoomInfo> rooms = new ArrayList<>();
        String sql = "SELECT RoomID, RoomName, RoomCode FROM Rooms WHERE OwnerPlayerID = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, ownerPlayerID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(new RoomInfo(
                            rs.getInt("RoomID"),
                            rs.getString("RoomName"),
                            rs.getString("RoomCode")
                    ));
                }
            }
        }
        return rooms;
    }

    // --- ROOM METHODS ---

    /**
     * Thêm mới một phòng chơi (Tạo phòng).
     * @param roomName Tên phòng.
     * @param ownerPlayerID ID của người chủ phòng.
     * @param roomCode Mã tham gia phòng (do bạn tự tạo, ví dụ 6 ký tự).
     * @return RoomID mới được tạo nếu thành công, -1 nếu thất bại.
     * @throws SQLException
     */
    public int addRoom(String roomName, int ownerPlayerID, String roomCode) throws SQLException {
        String sql = "INSERT INTO Rooms (RoomName, OwnerPlayerID, RoomCode) OUTPUT INSERTED.RoomID VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roomName);
            ps.setInt(2, ownerPlayerID);
            ps.setString(3, roomCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1); // Trả về RoomID vừa được tạo
                }
            }
        }
        return -1; // Lỗi không tạo được phòng
    }

    /**
     * Xoá một phòng chơi (Chỉ chủ phòng mới có quyền).
     * Cần phải xoá các bản ghi trong RoomMemberships trước.
     * @param roomID ID phòng cần xoá.
     * @param ownerPlayerID ID của người đang thực hiện hành động (phải là chủ phòng).
     * @return true nếu xoá thành công, false nếu không.
     * @throws SQLException
     */
    public boolean deleteRoom(int roomID, int ownerPlayerID) throws SQLException {
        // Bắt đầu một Transaction để đảm bảo cả 2 lệnh xoá đều thành công
        connection.setAutoCommit(false);

        try {
            // Bước 1: Xoá tất cả thành viên khỏi phòng
            String sqlDeleteMembers = "DELETE FROM RoomMemberships WHERE RoomID = ?";
            try (PreparedStatement psMembers = connection.prepareStatement(sqlDeleteMembers)) {
                psMembers.setInt(1, roomID);
                psMembers.executeUpdate();
            }

            // Bước 2: Xoá phòng, VÀ kiểm tra xem người xoá có phải là chủ phòng không
            String sqlDeleteRoom = "DELETE FROM Rooms WHERE RoomID = ? AND OwnerPlayerID = ?";
            try (PreparedStatement psRoom = connection.prepareStatement(sqlDeleteRoom)) {
                psRoom.setInt(1, roomID);
                psRoom.setInt(2, ownerPlayerID);
                int rowsAffected = psRoom.executeUpdate();

                if (rowsAffected > 0) {
                    connection.commit(); // Hoàn tất transaction
                    return true;
                } else {
                    connection.rollback(); // Huỷ bỏ, do không phải chủ phòng hoặc phòng không tồn tại
                    return false;
                }
            }
        } catch (SQLException e) {
            connection.rollback(); // Huỷ bỏ nếu có lỗi
            throw e; // Ném lỗi ra ngoài
        } finally {
            connection.setAutoCommit(true); // Trả về chế độ auto-commit
        }
    }

    // --- ROOM MEMBERSHIP METHODS ---

    /**
     * Kiểm tra xem người chơi đã là thành viên của phòng chưa.
     * @param playerID ID người chơi.
     * @param roomID ID phòng.
     * @return true nếu đã là thành viên, false nếu chưa.
     * @throws SQLException
     */
    public boolean isPlayerInRoom(int playerID, int roomID) throws SQLException {
        String sql = "SELECT 1 FROM RoomMemberships WHERE PlayerID = ? AND RoomID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerID);
            ps.setInt(2, roomID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Thêm một người chơi vào phòng (Join phòng / Tạo phòng).
     * @param playerID ID người chơi.
     * @param roomID ID phòng.
     * @return true nếu thêm thành công (hoặc đã ở trong phòng), false nếu thất bại.
     * @throws SQLException
     */
    public boolean addPlayerToRoom(int playerID, int roomID) throws SQLException {
        if (isPlayerInRoom(playerID, roomID)) {
            return true; // Người chơi đã ở trong phòng
        }

        String sql = "INSERT INTO RoomMemberships (PlayerID, RoomID) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerID);
            ps.setInt(2, roomID);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Xoá một người chơi khỏi phòng (Kick / Leave phòng).
     * @param playerID ID người chơi.
     * @param roomID ID phòng.
     * @return true nếu xoá thành công, false nếu không.
     * @throws SQLException
     */
    public boolean removePlayerFromRoom(int playerID, int roomID) throws SQLException {
        String sql = "DELETE FROM RoomMemberships WHERE PlayerID = ? AND RoomID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerID);
            ps.setInt(2, roomID);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Lấy danh sách các phòng mà người chơi đã tham gia (nhưng không sở hữu).
     * @param playerID ID của người chơi.
     * @return Danh sách các đối tượng RoomInfo.
     * @throws SQLException
     */
    public List<RoomInfo> getJoinedRooms(int playerID) throws SQLException {
        List<RoomInfo> rooms = new ArrayList<>();

        // Query JOIN 2 bảng, loại trừ phòng do chính mình sở hữu
        String sql = "SELECT r.RoomID, r.RoomName, r.RoomCode " +
                "FROM Rooms r " +
                "JOIN RoomMemberships m ON r.RoomID = m.RoomID " +
                "WHERE m.PlayerID = ? AND r.OwnerPlayerID != ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerID); // m.PlayerID
            ps.setInt(2, playerID); // r.OwnerPlayerID

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(new RoomInfo(
                            rs.getInt("RoomID"),
                            rs.getString("RoomName"),
                            rs.getString("RoomCode")
                    ));
                }
            }
        }
        return rooms;
    }

    /**
     * Lấy danh sách TẤT CẢ PlayerID và Username của thành viên trong phòng.
     * @param roomID ID phòng
     * @param ownerPlayerID ID của chủ phòng
     * @return Danh sách PlayerState (chỉ có ID, Username, IsOwner)
     * @throws SQLException
     */
    public List<PlayerState> getRoomMembersBasicInfo(int roomID, int ownerPlayerID) throws SQLException {
        List<PlayerState> members = new ArrayList<>();

        String sql = "SELECT p.PlayerID, p.Username, p.TotalScore FROM Players p " +
                "JOIN RoomMemberships m ON p.PlayerID = m.PlayerID " +
                "WHERE m.RoomID = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int currentID = rs.getInt("PlayerID");
                    boolean isOwner = (currentID == ownerPlayerID);
                    members.add(new PlayerState(
                            currentID,
                            rs.getString("Username"),
                            rs.getInt("TotalScore"),
                            isOwner,
                            false // Mặc định là Offline khi mới load
                    ));
                }
            }
        }
        return members;
    }

    /**
     * Lấy OwnerID của một phòng.
     * @param roomID ID phòng
     * @return OwnerPlayerID, hoặc -1 nếu không tìm thấy.
     * @throws SQLException
     */
    public int getRoomOwner(int roomID) throws SQLException {
        String sql = "SELECT OwnerPlayerID FROM Rooms WHERE RoomID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("OwnerPlayerID");
                }
            }
        }
        return -1;
    }

    /**
     * Tìm kiếm các phòng đang hoạt động theo tên,
     * VÀ loại trừ các phòng người chơi đã là thành viên.
     * @param roomNameQuery Tên phòng (có thể dùng LIKE)
     * @param playerID ID của người tìm kiếm (để loại trừ)
     * @param activeRoomIDs Danh sách ID các phòng đang mở (từ Server)
     * @return Danh sách RoomInfo
     * @throws SQLException
     */
    public List<RoomInfo> searchActiveRooms(String roomNameQuery, int playerID, List<Integer> activeRoomIDs) throws SQLException {
        List<RoomInfo> rooms = new ArrayList<>();
        if (activeRoomIDs == null || activeRoomIDs.isEmpty()) {
            return rooms; // Không có phòng nào active
        }

        // Tạo chuỗi "IN (id1, id2, ...)"
        String inClause = String.join(",", activeRoomIDs.stream().map(String::valueOf).toArray(String[]::new));

        // Câu query phức tạp:
        // 1. Tìm phòng theo tên (LIKE)
        // 2. Chỉ tìm trong các phòng đang active (IN (...))
        // 3. Loại trừ phòng do mình sở hữu (OwnerPlayerID != ?)
        // 4. Loại trừ phòng mình đã tham gia (LEFT JOIN ... WHERE m.MembershipID IS NULL)
        String sql = "SELECT r.RoomID, r.RoomName, r.RoomCode " +
                "FROM Rooms r " +
                "LEFT JOIN RoomMemberships m ON r.RoomID = m.RoomID AND m.PlayerID = ? " +
                "WHERE r.RoomName LIKE ? " +
                "AND r.RoomID IN (" + inClause + ") " +
                "AND r.OwnerPlayerID != ? " +
                "AND m.MembershipID IS NULL";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerID);           // m.PlayerID
            ps.setString(2, "%" + roomNameQuery + "%"); // r.RoomName LIKE
            ps.setInt(3, playerID);           // r.OwnerPlayerID

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(new RoomInfo(
                            rs.getInt("RoomID"),
                            rs.getString("RoomName"),
                            rs.getString("RoomCode")
                    ));
                }
            }
        }
        return rooms;
    }

    /**
     * Lấy RoomCode của một phòng.
     * @param roomID ID phòng
     * @return RoomCode, hoặc null nếu không tìm thấy.
     * @throws SQLException
     */
    public String getRoomCode(int roomID) throws SQLException {
        String sql = "SELECT RoomCode FROM Rooms WHERE RoomID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("RoomCode");
                }
            }
        }
        return null;
    }

    /**
     * Lấy thông tin cơ bản của 1 phòng (RoomInfo).
     * @param roomID ID phòng
     * @return RoomInfo object, hoặc null nếu không tìm thấy.
     * @throws SQLException
     */
    public RoomInfo getRoomInfo(int roomID) throws SQLException {
        String sql = "SELECT RoomName, RoomCode FROM Rooms WHERE RoomID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new RoomInfo(
                            roomID,
                            rs.getString("RoomName"),
                            rs.getString("RoomCode")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Lấy điểm số của một người chơi.
     * @param playerID ID người chơi
     * @return TotalScore, hoặc 0 nếu không tìm thấy.
     * @throws SQLException
     */
    public int getPlayerScore(int playerID) throws SQLException {
        String sql = "SELECT TotalScore FROM Players WHERE PlayerID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("TotalScore");
                }
            }
        }
        return 0; // Mặc định
    }

    /**
     * Cộng thêm điểm cho người chơi.
     * @param playerID ID người chơi
     * @param scoreToAdd Số điểm (ví dụ: 10)
     * @return Điểm số MỚI (TotalScore)
     * @throws SQLException
     */
    public int addScoreToPlayer(int playerID, int scoreToAdd) throws SQLException {
        // Lấy điểm hiện tại
        String getScoreSql = "SELECT TotalScore FROM Players WHERE PlayerID = ?";
        int currentScore = 0;
        try (PreparedStatement ps = connection.prepareStatement(getScoreSql)) {
            ps.setInt(1, playerID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentScore = rs.getInt("TotalScore");
                }
            }
        }

        int newScore = currentScore + scoreToAdd;

        // Cập nhật điểm mới
        String updateScoreSql = "UPDATE Players SET TotalScore = ? WHERE PlayerID = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateScoreSql)) {
            ps.setInt(1, newScore);
            ps.setInt(2, playerID);
            ps.executeUpdate();
        }

        return newScore;
    }

    /**
     * Lấy danh sách tất cả người chơi (cho Admin).
     */
    public List<PlayerState> getAllPlayers() throws SQLException {
        List<PlayerState> players = new ArrayList<>();
        String sql = "SELECT PlayerID, Username, TotalScore FROM Players";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Mặc định offline và not owner, AdminController sẽ cập nhật lại sau
                players.add(new PlayerState(
                        rs.getInt("PlayerID"),
                        rs.getString("Username"),
                        rs.getInt("TotalScore"),
                        false,
                        false
                ));
            }
        }
        return players;
    }

    /**
     * Xóa người chơi và toàn bộ dữ liệu liên quan (Cascade).
     * 1. Xóa thành viên trong các phòng do người này tạo.
     * 2. Xóa các phòng do người này tạo.
     * 3. Xóa người này khỏi các phòng họ đang tham gia.
     * 4. Xóa tài khoản người này.
     */
    public boolean deletePlayerFully(int playerID) throws SQLException {
        connection.setAutoCommit(false); // Bắt đầu Transaction
        try {
            // 1. Xóa membership của TẤT CẢ các phòng do người này sở hữu
            // (Vì khi xóa phòng, membership của người khác trong phòng đó cũng phải bay màu)
            String sqlDeleteMembersOfOwnedRooms =
                    "DELETE FROM RoomMemberships WHERE RoomID IN (SELECT RoomID FROM Rooms WHERE OwnerPlayerID = ?)";
            try (PreparedStatement ps = connection.prepareStatement(sqlDeleteMembersOfOwnedRooms)) {
                ps.setInt(1, playerID);
                ps.executeUpdate();
            }

            // 2. Xóa các phòng do người này sở hữu
            String sqlDeleteOwnedRooms = "DELETE FROM Rooms WHERE OwnerPlayerID = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlDeleteOwnedRooms)) {
                ps.setInt(1, playerID);
                ps.executeUpdate();
            }

            // 3. Xóa membership của chính người này (ở các phòng khác)
            String sqlDeleteSelfMembership = "DELETE FROM RoomMemberships WHERE PlayerID = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlDeleteSelfMembership)) {
                ps.setInt(1, playerID);
                ps.executeUpdate();
            }

            // 4. Cuối cùng: Xóa tài khoản
            String sqlDeletePlayer = "DELETE FROM Players WHERE PlayerID = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlDeletePlayer)) {
                ps.setInt(1, playerID);
                int rows = ps.executeUpdate();

                connection.commit(); // Thành công thì lưu
                return rows > 0;
            }
        } catch (SQLException e) {
            connection.rollback(); // Lỗi thì quay lại
            e.printStackTrace();
            return false;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Lấy danh sách ID các phòng do người chơi này sở hữu.
     * (Dùng để Server đóng active room trước khi xóa).
     */
    public List<Integer> getRoomIDsOwnedBy(int playerID) throws SQLException {
        List<Integer> roomIDs = new ArrayList<>();
        String sql = "SELECT RoomID FROM Rooms WHERE OwnerPlayerID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roomIDs.add(rs.getInt("RoomID"));
                }
            }
        }
        return roomIDs;
    }

    /**
     * Lấy danh sách tất cả phòng chơi cho Admin.
     * Kèm theo Tên chủ phòng và Số lượng thành viên.
     */
    public List<AdminRoomRow> getAllRoomsForAdmin() throws SQLException {
        List<AdminRoomRow> rooms = new ArrayList<>();
        // Query kết hợp (JOIN) để lấy tên chủ phòng và đếm số thành viên
        String sql = "SELECT r.RoomID, r.RoomName, r.RoomCode, p.Username as OwnerName, " +
                "(SELECT COUNT(*) FROM RoomMemberships WHERE RoomID = r.RoomID) as MemberCount " +
                "FROM Rooms r " +
                "JOIN Players p ON r.OwnerPlayerID = p.PlayerID";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Status sẽ được Server cập nhật sau (Open/Closed)
                rooms.add(new AdminRoomRow(
                        rs.getInt("RoomID"),
                        rs.getString("RoomName"),
                        rs.getString("OwnerName"),
                        "Closed", // Mặc định
                        rs.getInt("MemberCount"),
                        rs.getString("RoomCode")
                ));
            }
        }
        return rooms;
    }

    /**
     * Xóa phòng và toàn bộ membership (Cascade).
     */
    public boolean deleteRoomFully(int roomID) throws SQLException {
        connection.setAutoCommit(false);
        try {
            // 1. Xóa Membership trước
            String sqlDeleteMembers = "DELETE FROM RoomMemberships WHERE RoomID = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlDeleteMembers)) {
                ps.setInt(1, roomID);
                ps.executeUpdate();
            }

            // 2. Xóa Phòng
            String sqlDeleteRoom = "DELETE FROM Rooms WHERE RoomID = ?";
            try (PreparedStatement ps = connection.prepareStatement(sqlDeleteRoom)) {
                ps.setInt(1, roomID);
                int rows = ps.executeUpdate();

                connection.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            return false;
        } finally {
            connection.setAutoCommit(true);
        }
    }
}