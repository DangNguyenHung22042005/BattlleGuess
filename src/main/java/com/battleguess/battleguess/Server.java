package com.battleguess.battleguess;

import com.battleguess.battleguess.database.DatabaseManager;
import com.battleguess.battleguess.database.PasswordUtils;
import com.battleguess.battleguess.enum_to_manage_string.MessageType;
import com.battleguess.battleguess.model.PlayerState;
import com.battleguess.battleguess.model.RoomInfo;
import com.battleguess.battleguess.model.RoomSession;
import com.battleguess.battleguess.network.request.*;
import com.battleguess.battleguess.network.response.*;
import com.battleguess.battleguess.network.Packet;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server implements Runnable {
    private ServerSocket serverSocket;
    private Thread thread;
    private boolean running;
    private int port;
    private DatabaseManager db;
    private Map<Integer, RoomSession> activeRooms = new ConcurrentHashMap<>();
    private Map<ObjectOutputStream, Integer> clientToPlayerID = new ConcurrentHashMap<>();
    private Map<Integer, Integer> playerIDToRoomID = new ConcurrentHashMap<>();
    private Map<Integer, ObjectOutputStream> pendingJoins = new ConcurrentHashMap<>();

    private DatagramSocket udpSocket;
    private final int UDP_PORT = 8000; // Cổng UDP cố định của Server
    private Map<SocketAddress, Integer> udpAddrToPlayerID = new ConcurrentHashMap<>();
    private Map<Integer, SocketAddress> playerUdpAddressStore = new ConcurrentHashMap<>();

    public Server(int port) throws IOException {
        this.port = port;
        this.running = false;
        try {
            this.db = new DatabaseManager();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IOException("Failed to connect to Database.");
        }
        createServerSocket(); // Mở TCP

        // --- MỞ CỔNG UDP ---
        try {
            this.udpSocket = new DatagramSocket(UDP_PORT);
            System.out.println("UDP Reflector started on port " + UDP_PORT);
            startUdpReflector(); // Bắt đầu luồng UDP
        } catch (Exception e) {
            throw new IOException("Failed to start UDP socket: " + e.getMessage());
        }
    }

    private void createServerSocket() throws IOException {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server created on port " + port);
        } catch (IOException e) {
            throw new IOException("Port " + port + " is already in use or invalid.");
        }
    }

    public void startServer() {
        if (!running) {
            running = true;
            try {
                if (serverSocket == null || serverSocket.isClosed()) {
                    createServerSocket();
                }
                thread = new Thread(this);
                thread.start();
                System.out.println("Server started on port " + port);
            } catch (IOException e) {
                running = false;
                throw new RuntimeException("Failed to start server on port " + port + ": " + e.getMessage());
            }
        }
    }

    public void stopServer() {
        if (running) {
            running = false;
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                if (thread != null) {
                    thread.interrupt();
                }
                System.out.println("Server stopped on port " + port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                if (clientSocket != null) {
                    System.out.println("Client connected on port " + port);
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                if (!running) {

                    break;
                }
                e.printStackTrace();
            }
        }
    }

    private void startUdpReflector() {
        Thread udpThread = new Thread(() -> {
            byte[] buffer = new byte[65507];
            while (running) {
                try {
                    DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(udpPacket);

                    SocketAddress senderAddress = udpPacket.getSocketAddress();
                    byte[] data = Arrays.copyOf(udpPacket.getData(), udpPacket.getLength());

                    // --- SỬA LỖI Ở ĐÂY ---
                    // Kiểm tra gói tin rác (hole punch)
                    if (data.length <= 1) {
                        System.out.println("Dummy packet received from " + senderAddress + ". Ignoring.");
                        continue; // Bỏ qua, không phản xạ
                    }

                    ByteBuffer bb = ByteBuffer.wrap(data);
                    int senderID = bb.getInt();
                    int roomID = bb.getInt();

                    // --- THÊM LOGIC "HỌC" ĐỊA CHỈ (QUAN TRỌNG) ---
                    // "Học" địa chỉ UDP của client từ gói tin đầu tiên
                    if (!playerUdpAddressStore.containsKey(senderID)) {
                        playerUdpAddressStore.put(senderID, senderAddress);
                    }
                    // Cũng cập nhật nó vào session
                    RoomSession session = activeRooms.get(roomID);
                    if (session != null) {
                        session.registerUdpAddress(senderID, senderAddress);
                    } else {
                        continue; // Bỏ qua nếu phòng không active
                    }
                    // --- HẾT PHẦN THÊM ---

                    byte[] frameData = new byte[data.length - 8];
                    bb.get(frameData);

                    // ... (Phần logic broadcast/reflect còn lại giữ nguyên)
                    ByteBuffer broadcastBuffer = ByteBuffer.allocate(4 + frameData.length);
                    broadcastBuffer.putInt(senderID);
                    broadcastBuffer.put(frameData);
                    byte[] broadcastData = broadcastBuffer.array();

                    for (Map.Entry<Integer, SocketAddress> entry : session.getUdpAddresses().entrySet()) {
                        if (entry.getKey() != senderID) {
                            DatagramPacket sendPacket = new DatagramPacket(
                                    broadcastData,
                                    broadcastData.length,
                                    entry.getValue()
                            );
                            udpSocket.send(sendPacket);
                        }
                    }

                } catch (IOException e) {
                    if (running) e.printStackTrace();
                }
            }
        });
        udpThread.setDaemon(true);
        udpThread.start();
    }

    private void handleClient(Socket clientSocket) {
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        int currentPlayerID = -1;

        try {
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());

            while (running) {
                Packet packet = (Packet) in.readObject();
                MessageType type = packet.getType();

                switch (type) {
                    case LOGIN_REQUEST:
                        LoginRequestPayload loginPayload = (LoginRequestPayload) packet.getData();
                        int pID = handleLoginRequest(loginPayload, out);
                        if (pID != -1) {
                            currentPlayerID = pID;
                            clientToPlayerID.put(out, currentPlayerID);
                        }
                        break;

                    case REGISTER_REQUEST:
                        LoginRequestPayload regPayload = (LoginRequestPayload) packet.getData();
                        handleRegisterRequest(regPayload, out);
                        break;

                    case GET_MY_ROOMS_REQUEST:
                        PlayerIDPayload getMyRoomsPayload = (PlayerIDPayload) packet.getData();
                        handleGetMyRooms(getMyRoomsPayload, out);
                        break;

                    case CREATE_ROOM_REQUEST:
                        CreateRoomRequestPayload createRoomRequestPayload = (CreateRoomRequestPayload) packet.getData();
                        handleCreateRoomRequest(createRoomRequestPayload, out);
                        break;

                    case GET_JOINED_ROOMS_REQUEST:
                        PlayerIDPayload getJoinedRoomsPayload = (PlayerIDPayload) packet.getData();
                        handleGetJoinedRooms(getJoinedRoomsPayload, out);
                        break;

                    case DELETE_ROOM_REQUEST:
                        PlayerIDAndRoomIDPayload deleteRoomRequestPayload = (PlayerIDAndRoomIDPayload) packet.getData();
                        handleDeleteRoomRequest(deleteRoomRequestPayload, out);
                        break;

                    case LEAVE_ROOM_REQUEST:
                        PlayerIDAndRoomIDPayload leaveRoomRequestPayload = (PlayerIDAndRoomIDPayload) packet.getData();
                        handleLeaveRoomRequest(leaveRoomRequestPayload, out);
                        break;

                    case OPEN_ROOM_REQUEST:
                        PlayerIDAndRoomIDPayload openRoomRequestPayload = (PlayerIDAndRoomIDPayload) packet.getData();
                        handleOpenRoom(openRoomRequestPayload, out);
                        break;

                    case JOIN_ROOM_SESSION_REQUEST:
                        PlayerIDAndRoomIDPayload joinRoomSessionRequestPayload = (PlayerIDAndRoomIDPayload) packet.getData();
                        handleJoinSession(joinRoomSessionRequestPayload, out);
                        break;

                    case JOIN_BY_CODE_REQUEST:
                        JoinByCodeRequestPayload joinByCodePayload = (JoinByCodeRequestPayload) packet.getData();
                        handleJoinByCodeRequest(joinByCodePayload, out);
                        break;

                    case EXIT_ROOM_SESSION_REQUEST:
                        PlayerIDAndRoomIDPayload exitRoomSessionRequestPayload = (PlayerIDAndRoomIDPayload) packet.getData();
                        handleExitSession(exitRoomSessionRequestPayload.getPlayerID(), exitRoomSessionRequestPayload.getRoomID());
                        break;

                    case CLOSE_ROOM_REQUEST:
                        PlayerIDAndRoomIDPayload closeRoomRequestPayload = (PlayerIDAndRoomIDPayload) packet.getData();
                        handleCloseRoom(closeRoomRequestPayload.getRoomID(), closeRoomRequestPayload.getPlayerID());
                        break;

                    case KICK_PLAYER_REQUEST:
                        KickPlayerRequestPayload kickPlayerRequestPayload = (KickPlayerRequestPayload) packet.getData();
                        handleKickPlayer(kickPlayerRequestPayload, out);
                        break;

                    case GET_ACTIVE_ROOM_IDS_REQUEST:
                        handleGetActiveRoomIDs(out);
                        break;

                    case JOIN_REQUEST_RESPONSE:
                        JoinRequestResponsePayload joinRequestResponsePayload = (JoinRequestResponsePayload) packet.getData();
                        handleJoinRequestResponse(joinRequestResponsePayload);
                        break;

                    case GET_ROOM_STATE_REQUEST:
                        PlayerIDAndRoomIDPayload reloadPayload = (PlayerIDAndRoomIDPayload) packet.getData();
                        handleReloadRequest(reloadPayload, out);
                        break;

                    case SEND_PUZZLE_REQUEST:
                        SendPuzzleRequestPayload puzzlePayload = (SendPuzzleRequestPayload) packet.getData();
                        handleSendPuzzle(puzzlePayload);
                        break;

                    case SEND_GUESS_REQUEST:
                        SendGuessRequestPayload guessPayload = (SendGuessRequestPayload) packet.getData();
                        handleSendGuess(guessPayload);
                        break;

                    case SEND_CHAT_MESSAGE_REQUEST:
                        SendChatMessageRequestPayload chatPayload = (SendChatMessageRequestPayload) packet.getData();
                        handleSendChat(chatPayload);
                        break;

                    case REGISTER_UDP_PORT_REQUEST:
                        RegisterUdpPayload udpPayload = (RegisterUdpPayload) packet.getData();
                        SocketAddress tcpAddress = clientSocket.getRemoteSocketAddress();
                        handleRegisterUdp(udpPayload, tcpAddress, out);
                        break;

                    case CAMERA_STATUS_UPDATE:
                        CameraStatusUpdatePayload camPayload = (CameraStatusUpdatePayload) packet.getData();
                        handleCameraStatusUpdate(camPayload);
                        break;
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected.");
            // --- LOGIC DETECT OFFLINE QUAN TRỌNG ---
            handleClientDisconnect(out, currentPlayerID);
        } catch (IOException | ClassNotFoundException | SQLException e) {
            if (running) e.printStackTrace();
            handleClientDisconnect(out, currentPlayerID);
        }finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRegisterUdp(RegisterUdpPayload payload, SocketAddress tcpAddress, ObjectOutputStream out) throws IOException {
        String ipAddress = ((java.net.InetSocketAddress) tcpAddress).getAddress().getHostAddress();
        SocketAddress udpAddress = new java.net.InetSocketAddress(ipAddress, payload.getUdpPort());

        playerUdpAddressStore.put(payload.getPlayerID(), udpAddress);
        System.out.println("UDP Registered for Player " + payload.getPlayerID() + " at " + udpAddress);

        udpAddrToPlayerID.put(udpAddress, payload.getPlayerID());

        // 2. Cập nhật mọi RoomSession mà người này tham gia (nếu họ đang trong phòng)
        Integer roomID = playerIDToRoomID.get(payload.getPlayerID());
        if (roomID != null) {
            RoomSession session = activeRooms.get(roomID);
            if (session != null) {
                session.registerUdpAddress(payload.getPlayerID(), udpAddress);
                // Gửi danh sách UDP MỚI cho MỌI NGƯỜI
                PlayerUdpListPayload listPayload = new PlayerUdpListPayload(session.getUdpAddresses());
                session.broadcast(new Packet(MessageType.PLAYER_UDP_LIST_UPDATE, listPayload));
            }
        }
    }

    private void broadcastToAllClients(Packet packet) {
        // Lặp qua danh sách 'clientToPlayerID' (chứa tất cả stream)
        for (ObjectOutputStream stream : clientToPlayerID.keySet()) {
            try {
                stream.writeObject(packet);
                stream.flush();
            } catch (IOException e) {
                 e.printStackTrace();
            }
        }
    }

    private void handleSendPuzzle(SendPuzzleRequestPayload payload) throws IOException {
        RoomSession session = activeRooms.get(payload.getRoomID());
        if (session == null || payload.getPlayerID() != session.getOwnerPlayerID()) {
            return; // Phòng không tồn tại hoặc người gửi không phải chủ phòng
        }

        // 1. Lưu đáp án (dạng chữ thường)
        session.setCorrectAnswer(payload.getAnswer());

        // 2. Chuẩn bị packet hình ảnh
        PuzzleBroadcastPayload puzzleBroadcast = new PuzzleBroadcastPayload(payload.getImageData());
        Packet packet = new Packet(MessageType.PUZZLE_BROADCAST, puzzleBroadcast);

        // 3. Gửi cho tất cả người đoán (trừ chủ phòng)
        session.broadcastToGuessers(packet);
    }

    private void handleSendGuess(SendGuessRequestPayload payload) throws IOException, SQLException {
        RoomSession session = activeRooms.get(payload.getRoomID());
        if (session == null || payload.getPlayerID() == session.getOwnerPlayerID()) {
            return; // Phòng không tồn tại hoặc chủ phòng không được đoán
        }

        String correctAnswer = session.getCorrectAnswer();
        String guess = (payload.getGuess() != null) ? payload.getGuess().toLowerCase() : "";

        // So khớp (không phân biệt hoa thường)
        if (correctAnswer != null && correctAnswer.equals(guess)) {
            // ĐOÁN ĐÚNG!

            // 1. Clear đáp án để không ai đoán đúng nữa
            session.clearCorrectAnswer();

            // 2. Cộng 10 điểm vào DB và lấy điểm mới
            int newScore = db.addScoreToPlayer(payload.getPlayerID(), 10);

            // 3. Cập nhật điểm mới vào bộ nhớ Session
            session.updatePlayerScore(payload.getPlayerID(), newScore);

            // 4. Gửi thông báo ĐOÁN ĐÚNG cho MỌI NGƯỜI (cả chủ phòng)
            AnswerCorrectBroadcastPayload correctPayload = new AnswerCorrectBroadcastPayload(
                    payload.getPlayerName(), // (Cần gửi playerName trong SendGuessRequest)
                    payload.getGuess().toUpperCase() // Gửi đáp án dạng IN HOA
            );
            session.broadcast(new Packet(MessageType.ANSWER_CORRECT_BROADCAST, correctPayload));

            // 5. Gửi thông báo CẬP NHẬT TRẠNG THÁI (điểm mới)
            RoomStateUpdatePayload updatePayload = new RoomStateUpdatePayload(
                    session.getRoomInfo(),
                    session.getPlayerStates()
            );
            session.broadcast(new Packet(MessageType.ROOM_STATE_UPDATE, updatePayload));
        }

        // (Nếu đoán sai: Không làm gì cả)
    }

    private void handleSendChat(SendChatMessageRequestPayload payload) throws IOException {
        RoomSession session = activeRooms.get(payload.getRoomID());
        if (session == null) {
            return;
        }

        String senderName = session.getPlayerName(payload.getPlayerID());

        ChatMessageBroadcastPayload broadcastPayload = new ChatMessageBroadcastPayload(
                payload.getPlayerID(),
                senderName,
                payload.getMessageContent()
        );
        Packet packet = new Packet(MessageType.CHAT_MESSAGE_BROADCAST, broadcastPayload);

        session.broadcast(packet);
    }

    private void handleReloadRequest(PlayerIDAndRoomIDPayload payload, ObjectOutputStream out) throws IOException {
        RoomSession session = activeRooms.get(payload.getRoomID());
        if (session != null) {
            // Gửi lại trạng thái mới nhất cho CHỈ người yêu cầu
            RoomStateUpdatePayload roomStateUpdatePayload = new RoomStateUpdatePayload(session.getRoomInfo(), session.getPlayerStates());
            out.writeObject(new Packet(MessageType.ROOM_STATE_UPDATE, roomStateUpdatePayload));
        }
    }

    private int handleLoginRequest(LoginRequestPayload payload, ObjectOutputStream out) throws IOException {
        try {
            int playerID = db.validatePlayer(payload.getUsername(), payload.getPassword());
            if (playerID != -1) {
                out.writeObject(new Packet(MessageType.LOGIN_SUCCESS, new LoginSuccessPayload(playerID, payload.getUsername())));
                return playerID;
            } else {
                out.writeObject(new Packet(MessageType.LOGIN_FAILED, new GenericResponsePayload("Tên đăng nhập hoặc mật khẩu không đúng.")));
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.writeObject(new Packet(MessageType.LOGIN_FAILED, new GenericResponsePayload("Lỗi CSDL.")));
            return -1;
        }
    }

    private void handleRegisterRequest(LoginRequestPayload payload, ObjectOutputStream out) throws IOException {
        try {
            if (db.playerExists(payload.getUsername())) {
                out.writeObject(new Packet(MessageType.REGISTER_FAILED, new GenericResponsePayload("Tên người chơi đã tồn tại.")));
                return;
            }

            String hashedPassword = PasswordUtils.hashPassword(payload.getPassword());
            boolean success = db.addPlayer(payload.getUsername(), hashedPassword);

            if (success) {
                out.writeObject(new Packet(MessageType.REGISTER_SUCCESS, new GenericResponsePayload("Đăng ký thành công!")));
            } else {
                out.writeObject(new Packet(MessageType.REGISTER_FAILED, new GenericResponsePayload("Đăng ký thất bại.")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.writeObject(new Packet(MessageType.REGISTER_FAILED, new GenericResponsePayload("Lỗi CSDL.")));
        }
    }

    private void handleGetMyRooms(PlayerIDPayload payload, ObjectOutputStream out) throws IOException {
        try {
            List<RoomInfo> myRooms = db.getRoomsByOwner(payload.getPlayerID());
            ListRoomInfoPayload listRoomInfoPayload = new ListRoomInfoPayload(myRooms);
            out.writeObject(new Packet(MessageType.GET_MY_ROOMS_RESPONSE, listRoomInfoPayload));
        } catch (SQLException e) {
            e.printStackTrace();
            out.writeObject(new Packet(MessageType.ERROR, new GenericResponsePayload("Không thể tải danh sách phòng.")));
        }
    }

    private void handleCreateRoomRequest(CreateRoomRequestPayload payload, ObjectOutputStream out) throws IOException {
        try {
            int newRoomID = db.addRoom(payload.getRoomName(), payload.getPlayerID(), payload.getRoomCode());

            if (newRoomID != -1) {
                RoomInfo newRoomInfo = new RoomInfo(newRoomID, payload.getRoomName(), payload.getRoomCode());
                db.addPlayerToRoom(payload.getPlayerID(), newRoomID);

                CreateRoomSuccessPayload createRoomSuccessPayload = new CreateRoomSuccessPayload(newRoomInfo);
                out.writeObject(new Packet(MessageType.CREATE_ROOM_SUCCESS, createRoomSuccessPayload));
            } else {
                out.writeObject(new Packet(MessageType.CREATE_ROOM_FAILED, new GenericResponsePayload("Tạo phòng thất bại (ID = -1).")));
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601) {
                out.writeObject(new Packet(MessageType.CREATE_ROOM_FAILED, new GenericResponsePayload("Lỗi: Code phòng đã tồn tại.")));
            } else {
                e.printStackTrace();
                out.writeObject(new Packet(MessageType.CREATE_ROOM_FAILED, new GenericResponsePayload("Lỗi CSDL khi tạo phòng: " + e.getMessage())));
            }
        }
    }

    private void handleGetJoinedRooms(PlayerIDPayload payload, ObjectOutputStream out) throws IOException {
        try {
            List<RoomInfo> joinedRooms = db.getJoinedRooms(payload.getPlayerID());
            ListRoomInfoPayload listRoomInfoPayload = new ListRoomInfoPayload(joinedRooms);
            out.writeObject(new Packet(MessageType.GET_JOINED_ROOMS_RESPONSE, listRoomInfoPayload));
        } catch (SQLException e) {
            e.printStackTrace();
            out.writeObject(new Packet(MessageType.ERROR, new GenericResponsePayload("Không thể tải danh sách phòng đã tham gia.")));
        }
    }

    private void handleDeleteRoomRequest(PlayerIDAndRoomIDPayload payload, ObjectOutputStream out) throws IOException {
        try {
            boolean success = db.deleteRoom(payload.getRoomID(), payload.getPlayerID());
            if (success) {
                PlayerIDAndRoomIDPayload playerIDAndRoomIDPayload = new PlayerIDAndRoomIDPayload(payload.getPlayerID(), payload.getRoomID());
                out.writeObject(new Packet(MessageType.DELETE_ROOM_SUCCESS, playerIDAndRoomIDPayload));
            } else {
                out.writeObject(new Packet(MessageType.DELETE_ROOM_FAILED, new GenericResponsePayload("Xóa phòng thất bại (Không phải chủ phòng).")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.writeObject(new Packet(MessageType.DELETE_ROOM_FAILED, new GenericResponsePayload("Lỗi CSDL khi xóa phòng.")));
        }
    }

    private void handleLeaveRoomRequest(PlayerIDAndRoomIDPayload payload, ObjectOutputStream out) throws IOException {
        int playerID = payload.getPlayerID();
        int roomID = payload.getRoomID();

        try {
            boolean success = db.removePlayerFromRoom(payload.getPlayerID(), payload.getRoomID());

            if (success) {
                PlayerIDAndRoomIDPayload playerIDAndRoomIDPayload = new PlayerIDAndRoomIDPayload(payload.getPlayerID(), payload.getRoomID());
                out.writeObject(new Packet(MessageType.LEAVE_ROOM_SUCCESS, playerIDAndRoomIDPayload));

                RoomSession session = activeRooms.get(roomID);
                if (session != null) {
                    session.playerKick(playerID);
                    playerIDToRoomID.remove(playerID);

                    RoomStateUpdatePayload roomStateUpdatePayload = new RoomStateUpdatePayload(session.getRoomInfo(), session.getPlayerStates());
                    session.broadcast(new Packet(MessageType.ROOM_STATE_UPDATE, roomStateUpdatePayload));
                }
            } else {
                out.writeObject(new Packet(MessageType.LEAVE_ROOM_FAILED, new GenericResponsePayload("Rời phòng thất bại.")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.writeObject(new Packet(MessageType.LEAVE_ROOM_FAILED, new GenericResponsePayload("Lỗi CSDL khi rời phòng.")));
        }
    }

    private void handleOpenRoom(PlayerIDAndRoomIDPayload payload, ObjectOutputStream out) throws SQLException, IOException {
        int roomID = payload.getRoomID();
        int ownerPlayerID = payload.getPlayerID();

        RoomInfo roomInfo = db.getRoomInfo(roomID);
        if (roomInfo == null) {
            out.writeObject(new Packet(MessageType.ERROR, new GenericResponsePayload("Lỗi: Không tìm thấy thông tin phòng.")));
            return;
        }

        List<PlayerState> members = db.getRoomMembersBasicInfo(roomID, ownerPlayerID);

        RoomSession session = new RoomSession(roomInfo, ownerPlayerID, members);

        session.playerJoin(ownerPlayerID, out);

        SocketAddress udpAddr = playerUdpAddressStore.get(ownerPlayerID);
        if (udpAddr != null) {
            session.registerUdpAddress(ownerPlayerID, udpAddr);
        } else {
            System.err.println("WARN: Owner " + ownerPlayerID + " opened room but has no UDP address registered.");
        }

        activeRooms.put(roomID, session);
        playerIDToRoomID.put(ownerPlayerID, roomID);

        List<PlayerState> currentStates = session.getPlayerStates();

        RoomStateUpdatePayload roomStateUpdatePayload = new RoomStateUpdatePayload(roomInfo, currentStates);
        out.writeObject(new Packet(MessageType.ROOM_OPEN_SUCCESS, roomStateUpdatePayload));

        broadcastToAllClients(new Packet(MessageType.ROOM_NOW_ACTIVE, new RoomIDPayload(roomID)));
    }

    private void handleJoinSession(PlayerIDAndRoomIDPayload payload, ObjectOutputStream out) throws IOException {
        int roomID = payload.getRoomID();
        int playerID = payload.getPlayerID();

        RoomSession session = activeRooms.get(roomID);
        if (session == null) {
            out.writeObject(new Packet(MessageType.ERROR, new GenericResponsePayload("Phòng này chưa được mở.")));
            return;
        }

        session.playerJoin(playerID, out);
        playerIDToRoomID.put(playerID, roomID);

        SocketAddress udpAddr = playerUdpAddressStore.get(playerID);
        if (udpAddr != null) {
            session.registerUdpAddress(playerID, udpAddr);
        } else {
            System.err.println("WARN: Player " + playerID + " joined room but has no UDP address registered.");
        }

        List<PlayerState> currentStates = session.getPlayerStates();

        RoomJoinSuccessPayload joinSuccessPayload = new RoomJoinSuccessPayload(session.getRoomInfo(), currentStates);
        out.writeObject(new Packet(MessageType.ROOM_JOIN_SUCCESS, joinSuccessPayload));

        RoomStateUpdatePayload roomStateUpdatePayload = new RoomStateUpdatePayload(session.getRoomInfo(), currentStates);
        session.broadcast(new Packet(MessageType.ROOM_STATE_UPDATE, roomStateUpdatePayload));

        // 3. Gửi danh sách UDP (TCP)
        PlayerUdpListPayload listPayload = new PlayerUdpListPayload(session.getUdpAddresses());
        out.writeObject(new Packet(MessageType.PLAYER_UDP_LIST_UPDATE, listPayload));

        // 4. Cập nhật cho người cũ
        session.broadcast(new Packet(MessageType.PLAYER_UDP_LIST_UPDATE, listPayload));
    }

    private void handleCameraStatusUpdate(CameraStatusUpdatePayload payload) throws IOException {
        RoomSession session = activeRooms.get(payload.getRoomID());
        if (session == null) return;

        session.setPlayerCameraStatus(payload.getPlayerID(), payload.isCameraOn());

        PlayerCameraStatusPayload statusPayload = new PlayerCameraStatusPayload(payload.getPlayerID(), payload.isCameraOn());
        session.broadcast(new Packet(MessageType.PLAYER_CAMERA_STATUS_UPDATE, statusPayload));
    }

    private void handleExitSession(int playerID, int roomID) {
        RoomSession session = activeRooms.get(roomID);
        if (session != null) {
            session.playerLeave(playerID);
            playerIDToRoomID.remove(playerID);
            // Thông báo update
            RoomStateUpdatePayload roomStateUpdatePayload = new RoomStateUpdatePayload(session.getRoomInfo(), session.getPlayerStates());
            session.broadcast(new Packet(MessageType.ROOM_STATE_UPDATE, roomStateUpdatePayload));
        }
    }

    private void handleCloseRoom(int roomID, int playerID) {
        RoomSession session = activeRooms.get(roomID);
        if (session != null) {
            // (Nên kiểm tra playerID có phải là owner không)

            // 1. Thông báo cho mọi người là phòng đã đóng
            session.broadcast(new Packet(MessageType.ROOM_CLOSED_BY_OWNER, new GenericResponsePayload("Phòng đã bị chủ đóng.")));

            // 2. Xóa session
            activeRooms.remove(roomID);
            // Xóa map phụ
            for(Integer pID : session.getPlayerStates().stream().map(PlayerState::getPlayerID).toList()) {
                playerIDToRoomID.remove(pID);
            }

            broadcastToAllClients(new Packet(MessageType.ROOM_NOW_INACTIVE, new RoomIDPayload(roomID)));
        }
    }

    private void handleKickPlayer(KickPlayerRequestPayload payload, ObjectOutputStream out) throws SQLException, IOException {
        int roomID = payload.getCurrentRoomID();
        int targetID = payload.getTargetPlayerToKickID();
        RoomSession session = activeRooms.get(roomID);

        if (session == null) {
            out.writeObject(new Packet(MessageType.KICK_PLAYER_FAILED, new GenericResponsePayload("Phòng không hoạt động.")));
            return;
        }

        boolean success = db.removePlayerFromRoom(targetID, roomID);

        if (!success) {
            out.writeObject(new Packet(MessageType.KICK_PLAYER_FAILED, new GenericResponsePayload("Kick thất bại (Lỗi CSDL).")));
            return;
        }

        ObjectOutputStream targetStream = session.getStream(targetID);
        if (targetStream != null) {
            targetStream.writeObject(new Packet(MessageType.YOU_WERE_KICKED, new GenericResponsePayload("Bạn đã bị kick khỏi phòng.")));
        }

        session.playerKick(targetID);
        playerIDToRoomID.remove(targetID);

        RoomStateUpdatePayload roomStateUpdatePayload = new RoomStateUpdatePayload(session.getRoomInfo(), session.getPlayerStates());
        session.broadcast(new Packet(MessageType.ROOM_STATE_UPDATE, roomStateUpdatePayload));
    }

    private void handleClientDisconnect(ObjectOutputStream out, int playerID) {
        clientToPlayerID.remove(out);
        pendingJoins.remove(playerID);
        playerUdpAddressStore.remove(playerID);

        Integer roomID = playerIDToRoomID.get(playerID);
        if (roomID != null) {
            RoomSession session = activeRooms.get(roomID);
            if (session != null && session.getOwnerPlayerID() == playerID) {
                // Nếu chủ phòng rớt mạng, ĐÓNG PHÒNG
                handleCloseRoom(roomID, playerID);
            } else {
                // Người chơi thường rớt mạng
                handleExitSession(playerID, roomID);
            }
        }
    }

    private void handleGetActiveRoomIDs(ObjectOutputStream out) throws IOException {
        List<Integer> activeIDs = new ArrayList<>(activeRooms.keySet());
        ListRoomIDPayload listRoomIDPayload = new ListRoomIDPayload(activeIDs);
        out.writeObject(new Packet(MessageType.GET_ACTIVE_ROOM_IDS_RESPONSE, listRoomIDPayload));
    }

    private void handleJoinRequestResponse(JoinRequestResponsePayload payload) throws IOException, SQLException {
        int targetPlayerID = payload.getTargetPlayerID();
        RoomInfo roomInfo = payload.getRoomInFo();
        int roomID = roomInfo.getRoomID();
        boolean approved = payload.isApproved();
        String joinerName = payload.getJoinerName();

        ObjectOutputStream targetStream = pendingJoins.remove(targetPlayerID);
        if (targetStream == null) {
            return;
        }

        RoomSession session = activeRooms.get(roomID);
        if (session == null) {
            targetStream.writeObject(new Packet(MessageType.JOIN_DENIED, new GenericResponsePayload("Phòng vừa bị đóng.")));
            return;
        }

        if (approved) {
            db.addPlayerToRoom(targetPlayerID, roomID);
            int joinerScore = db.getPlayerScore(targetPlayerID);

            session.playerJoin(targetPlayerID, joinerName, joinerScore, false, targetStream);
            playerIDToRoomID.put(targetPlayerID, roomID);

            SocketAddress udpAddr = playerUdpAddressStore.get(targetPlayerID);
            if (udpAddr != null) {
                session.registerUdpAddress(targetPlayerID, udpAddr);
            } else {
                System.err.println("WARN: Player " + targetPlayerID + " was approved but has no UDP address registered.");
            }

            List<PlayerState> currentStates = session.getPlayerStates();
            RoomJoinSuccessPayload roomJoinSuccessPayload = new RoomJoinSuccessPayload(session.getRoomInfo(), currentStates);
            targetStream.writeObject(new Packet(MessageType.ROOM_JOIN_SUCCESS, roomJoinSuccessPayload));

            PlayerUdpListPayload listPayload = new PlayerUdpListPayload(session.getUdpAddresses());
            session.broadcast(new Packet(MessageType.PLAYER_UDP_LIST_UPDATE, listPayload));

            RoomStateUpdatePayload roomStateUpdatePayload = new RoomStateUpdatePayload(session.getRoomInfo(), session.getPlayerStates());
            session.broadcast(new Packet(MessageType.ROOM_STATE_UPDATE, roomStateUpdatePayload));
        } else {
            targetStream.writeObject(new Packet(MessageType.JOIN_DENIED, new GenericResponsePayload("Chủ phòng đã từ chối yêu cầu.")));
        }
    }

    private void handleJoinByCodeRequest(JoinByCodeRequestPayload payload, ObjectOutputStream out) throws IOException {
        String requestedCode = payload.getRoomCode();
        int joinerID = payload.getPlayerID();
        String joinerName = payload.getPlayerName();

        // 1. Tìm session đang active có code này
        RoomSession targetSession = null;
        for (RoomSession session : activeRooms.values()) {
            if (session.getRoomInfo().getRoomCode().equals(requestedCode)) {
                targetSession = session;
                break;
            }
        }

        // 2. Xử lý kết quả
        if (targetSession == null) {
            // Không tìm thấy phòng active
            out.writeObject(new Packet(MessageType.JOIN_BY_CODE_FAILED, new GenericResponsePayload("Không tìm thấy phòng nào đang mở với mã này.")));
            return;
        }

        // 3. Lấy stream của chủ phòng
        int ownerID = targetSession.getOwnerPlayerID();
        ObjectOutputStream ownerStream = targetSession.getStream(ownerID);

        if (ownerStream == null) {
            // Chủ phòng offline
            out.writeObject(new Packet(MessageType.JOIN_BY_CODE_FAILED, new GenericResponsePayload("Phòng này đang mở nhưng chủ phòng bị offline.")));
            return;
        }

        // 4. Lưu stream của người xin vào
        pendingJoins.put(joinerID, out);

        // 5. Gửi yêu cầu cho chủ phòng (Tái sử dụng MessageType)
        InComingJoinRequestPayload inComingJoinPayload = new InComingJoinRequestPayload(joinerID, joinerName, targetSession.getRoomInfo());
        ownerStream.writeObject(new Packet(MessageType.INCOMING_JOIN_REQUEST, inComingJoinPayload));
    }
}