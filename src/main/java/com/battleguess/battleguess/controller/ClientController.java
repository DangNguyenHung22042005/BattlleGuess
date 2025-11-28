package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.Client;
import com.battleguess.battleguess.database.RoomCodeGenerator;
import com.battleguess.battleguess.enum_to_manage_string.MessageType;
import com.battleguess.battleguess.model.PlayerState;
import com.battleguess.battleguess.network.Packet;
import com.battleguess.battleguess.network.request.*;
import com.battleguess.battleguess.network.response.*;
import com.battleguess.battleguess.model.RoomInfo;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.util.*;

public class ClientController {
    @FXML private VBox gamePane;
    @FXML private VBox leftNavPane;
    @FXML private VBox videoWindowContainer;
    @FXML private VBox chatWindowContainer;
    @FXML private Label lblPlayerName;
    @FXML private Label lblPlayerID;
    @FXML private Label lblRoomName;
    @FXML private Label lblRoomCode;
    @FXML private Label lblPlayerScore;
    @FXML private Label lblInRoomName;
    @FXML private Label lblInRoomID;
    @FXML private Label lblInRoomScore;
    @FXML private Button btnCloseRoom;
    @FXML private Button btnExitRoom;
    @FXML private Button joinByCodeButton;
    @FXML private Button reloadPlayerListButton;
    @FXML private Button reloadJoinedRoomsButton;
    @FXML private Button sendPuzzleButton;
    @FXML private Button sendGuessButton;
    @FXML private Button sendChatButton;
    @FXML private Button chatToggleButton;
    @FXML private Button closeChatButton;
    @FXML private Button toggleSelfCameraButton;
    @FXML private Button videoToggleButton;
    @FXML private Button micToggleButton;
    @FXML private Button toggleSelfMicButton;
    @FXML private Button closeVideoButton;
    @FXML private Button btnLogout;
    @FXML private TextField puzzleAnswerField;
    @FXML private TextField joinByCodeField;
    @FXML private TextField chatInputField;
    @FXML private BorderPane inRoomPane;
    @FXML private BorderPane canvasHostPane;
    @FXML private BorderPane canvasAndGuessArea;
    @FXML private ListView<RoomInfo> createdRoomsListView;
    @FXML private ListView<RoomInfo> joinedRoomsListView;
    @FXML private ListView<PlayerState> playerListView;
    @FXML private ListView<Node> chatMessagesListView;
    @FXML private Circle chatNotificationDot;
    @FXML private Circle videoNotificationDot;
    @FXML private FlowPane emojiFlowPane;
    @FXML private FlowPane videoGridPane;
    @FXML private ScrollPane emojiScrollPane;

    private CanvasController activeCanvasController;
    private ObservableList<RoomInfo> createdRoomsList = FXCollections.observableArrayList();
    private ObservableList<RoomInfo> joinedRoomsList = FXCollections.observableArrayList();
    private ObservableList<PlayerState> playerList = FXCollections.observableArrayList();
    private Map<Integer, VideoTile> videoTiles = new HashMap<>();
    private Map<Integer, SourceDataLine> audioPlaybackLines = new HashMap<>();
    private Task<Void> webcamTask;
    private Task<Void> audioSendTask;
    private List<Integer> activeRoomIDs = new ArrayList<>();
    private static AudioFormat AUDIO_FORMAT;
    private Webcam myWebcam;
    private Client client;
    private String playerName;
    private int playerID;
    private int playerScore;
    private int currentRoomID = -1;
    private static final int UDP_PACKET_TYPE_VIDEO = 1;
    private static final int UDP_PACKET_TYPE_AUDIO = 2;
    private boolean isOwnerOfCurrentRoom = false;
    private boolean emojisInitialized = false;
    private boolean isChatWindowOpen = false;
    private boolean isVideoWindowOpen = false;
    private boolean isMyCameraOn = false;
    private boolean isMyMicOn = false;
    private static final String[] EMOJIS = {
            "😀", "😂", "😍", "👍", "🤔", "😭", "🙏", "🔥", "🎉",
            "💯", "✅", "❌", "😱", "😎", "🤢", "😴", "👋"
    };

    @FXML
    private void initialize() {
        createdRoomsListView.setItems(createdRoomsList);
        joinedRoomsListView.setItems(joinedRoomsList);

        joinByCodeButton.setOnAction(e -> handleJoinByCode());
        reloadPlayerListButton.setOnAction(e -> handleReloadPlayerList());
        reloadJoinedRoomsButton.setOnAction(e -> loadJoinedRooms());

        createdRoomsListView.setCellFactory(param -> new CreatedRoomCell());
        joinedRoomsListView.setCellFactory(param -> new JoinedRoomCell());

        playerListView.setItems(playerList);
        playerListView.setCellFactory(param -> new PlayerListCell());

        chatInputField.setOnAction(e -> sendChatMessage());
        sendChatButton.setOnAction(e -> sendChatMessage());

        chatToggleButton.setOnAction(e -> showChatWindow(true));
        closeChatButton.setOnAction(e -> showChatWindow(false));

        videoToggleButton.setOnAction(e -> showVideoWindow(true));
        closeVideoButton.setOnAction(e -> showVideoWindow(false));
        toggleSelfCameraButton.setOnAction(e -> toggleMyCamera());

        micToggleButton.setOnAction(e -> showVideoWindow(true));
        toggleSelfMicButton.setOnAction(e -> toggleMyMic());
    }

    @FXML
    private void handleShowCreateRoom() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tạo phòng mới");
        dialog.setHeaderText("Nhập tên phòng của bạn:");
        dialog.setContentText("Tên phòng:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(roomName -> {
            if (roomName.trim().isEmpty()) {
                showAlert("Lỗi", "Tên phòng không được để trống.");
                return;
            }

            String roomCode = RoomCodeGenerator.generateUniqueRoomCode();

            CreateRoomRequestPayload createRoomRequestPayload = new CreateRoomRequestPayload(this.playerID, roomName, roomCode);
            client.sendMessage(new Packet(MessageType.CREATE_ROOM_REQUEST, createRoomRequestPayload));
        });
    }

    @FXML
    private void handleCloseRoom() {
        PlayerIDAndRoomIDPayload playerIDAndRoomIDPayload = new PlayerIDAndRoomIDPayload(playerID, currentRoomID);
        client.sendMessage(new Packet(MessageType.CLOSE_ROOM_REQUEST, playerIDAndRoomIDPayload));
        showLobbyView();
    }

    @FXML
    private void handleExitRoom() {
        PlayerIDAndRoomIDPayload playerIDAndRoomIDPayload = new PlayerIDAndRoomIDPayload(playerID, currentRoomID);
        client.sendMessage(new Packet(MessageType.EXIT_ROOM_SESSION_REQUEST, playerIDAndRoomIDPayload));
        showLobbyView();
        loadJoinedRooms();
    }

    @FXML
    private void handleJoinByCode() {
        String roomCode = joinByCodeField.getText().trim().toUpperCase();
        if (roomCode.isEmpty()) {
            showAlert("Gia nhập", "Vui lòng nhập mã phòng.");
            return;
        }
        JoinByCodeRequestPayload payload = new JoinByCodeRequestPayload(playerID, playerName, roomCode);
        client.sendMessage(new Packet(MessageType.JOIN_BY_CODE_REQUEST, payload));
        showAlert("Đang chờ", "Đã gửi yêu cầu. Vui lòng chờ chủ phòng chấp nhận.");
    }

    @FXML
    private void handleReloadPlayerList() {
        if (currentRoomID != -1) {
            PlayerIDAndRoomIDPayload payload = new PlayerIDAndRoomIDPayload(playerID, currentRoomID);
            client.sendMessage(new Packet(MessageType.GET_ROOM_STATE_REQUEST, payload));
        }
    }

    @FXML
    private void handleSendPuzzle() {
        if (!isOwnerOfCurrentRoom || activeCanvasController == null) {
            showAlert("Lỗi", "Bạn không có quyền vẽ.");
            return;
        }

        String answer = puzzleAnswerField.getText().trim();

        if (activeCanvasController.isCanvasBlank()) {
            showAlert("Thiếu thông tin", "Bạn chưa vẽ câu đố!");
            return;
        }
        if (answer.isEmpty()) {
            showAlert("Thiếu thông tin", "Bạn chưa nhập đáp án!");
            return;
        }

        try {
            WritableImage img = activeCanvasController.getSnapshot();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", baos);
            byte[] imageBytes = baos.toByteArray();

            SendPuzzleRequestPayload payload = new SendPuzzleRequestPayload(
                    playerID, currentRoomID, imageBytes, answer
            );
            client.sendMessage(new Packet(MessageType.SEND_PUZZLE_REQUEST, payload));

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể gửi hình ảnh.");
        }
    }

    @FXML
    private void handleSendGuess() {
        String guess = puzzleAnswerField.getText().trim();
        if (guess.isEmpty()) {
            showAlert("Thiếu thông tin", "Bạn chưa nhập dự đoán!");
            return;
        }

        SendGuessRequestPayload payload = new SendGuessRequestPayload(
                playerID, playerName, currentRoomID, guess
        );
        client.sendMessage(new Packet(MessageType.SEND_GUESS_REQUEST, payload));

        puzzleAnswerField.clear(); // Xóa sau khi gửi
    }

    @FXML
    private void handleEmojiButtonToggle() {
        // 1. Khởi tạo (chỉ 1 lần)
        if (!emojisInitialized) {
            emojiFlowPane.getChildren().clear();
            for (String emojiString : EMOJIS) {

                // 1. Tạo một Text node (nó sẽ tự render màu)
                Text emojiText = new Text(emojiString);
                emojiText.setStyle("-fx-font-size: 18px;"); // Style cho kích cỡ emoji

                // 2. Tạo một Button rỗng (không có text)
                Button emojiBtn = new Button();

                // 3. Đặt Text node vào bên trong Button
                emojiBtn.setGraphic(emojiText);

                // 4. Style cho Button (chỉ background, không đụng đến màu chữ)
                emojiBtn.setStyle("-fx-background-color: transparent; -fx-padding: 2;");

                // 5. Thêm hiệu ứng Hover cho đẹp
                emojiBtn.setOnMouseEntered(e -> emojiBtn.setStyle("-fx-background-color: #555; -fx-background-radius: 5; -fx-padding: 2;"));
                emojiBtn.setOnMouseExited(e -> emojiBtn.setStyle("-fx-background-color: transparent; -fx-padding: 2;"));

                // Khi bấm vào nút emoji
                emojiBtn.setOnAction(e -> {
                    chatInputField.appendText(emojiString); // Thêm emoji vào text
                });
                emojiFlowPane.getChildren().add(emojiBtn);
            }
            emojisInitialized = true;
        }

        // 2. Bật/Tắt (Toggle)
        boolean isVisible = emojiScrollPane.isVisible();
        emojiScrollPane.setVisible(!isVisible);
        emojiScrollPane.setManaged(!isVisible);
    }

    @FXML
    private void handleLogout() {
        // 1. Ngắt kết nối
        gracefulShutdown();

        // 2. Quay về màn hình Login
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleguess/battleguess/view/login-view.fxml"));
            javafx.scene.Parent root = loader.load();

            // Lấy Stage hiện tại
            javafx.stage.Stage stage = (javafx.stage.Stage) btnLogout.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root, 400, 400));
            stage.setTitle("🎨 BattleGuess Login");
            stage.centerOnScreen();

            // Xóa sự kiện Close cũ (để tránh lỗi)
            stage.setOnCloseRequest(null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isUserInRoom() {
        return this.currentRoomID != -1;
    }

    public List<Integer> getActiveRoomIDs() {
        return this.activeRoomIDs;
    }

    public ClientController() {
        AUDIO_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                8000.0f, // 8kHz
                16,      // 16 bit
                1,       // Mono
                2,       // 2 bytes/frame
                8000.0f, // 8k frames/giây
                false);  // Little-endian
    }

    public void initData(int playerID, String username, int score, Client connectedClient) {
        this.playerID = playerID;
        this.playerName = username;
        this.client = connectedClient;
        this.playerScore = score;
        this.client.setMessageHandler(this::handleServerMessage);

        lblPlayerName.setText("Tên: " + username);
        lblPlayerID.setText("ID: " + playerID);
        lblPlayerScore.setText("Điểm: " + score);

        gamePane.setVisible(true);
        gamePane.setManaged(true);
        leftNavPane.setVisible(true);
        leftNavPane.setManaged(true);

        loadMyRooms();
        loadJoinedRooms();
        loadActiveRoomIDs();

        RegisterUdpPayload payload = new RegisterUdpPayload(playerID, client.getUdpPort());
        client.sendMessage(new Packet(MessageType.REGISTER_UDP_PORT_REQUEST, payload));

        client.sendDummyUdpPacket();
    }

    public void handleServerMessage(Packet packet) {
        switch (packet.getType()) {
            case GET_MY_ROOMS_RESPONSE:
                ListRoomInfoPayload getMyRoomsResponsePayload = (ListRoomInfoPayload) packet.getData();
                createdRoomsList.setAll(getMyRoomsResponsePayload.getListRoomInfo());
                break;

            case ROOM_NOW_ACTIVE:
                RoomIDPayload activePayload = (RoomIDPayload) packet.getData();
                if (!activeRoomIDs.contains(activePayload.getRoomID())) {
                    activeRoomIDs.add(activePayload.getRoomID());
                }
                createdRoomsListView.refresh();
                joinedRoomsListView.refresh();
                break;

            case ROOM_NOW_INACTIVE:
                RoomIDPayload inactivePayload = (RoomIDPayload) packet.getData();
                activeRoomIDs.remove(Integer.valueOf(inactivePayload.getRoomID()));
                createdRoomsListView.refresh();
                joinedRoomsListView.refresh();
                break;

            case CREATE_ROOM_SUCCESS:
                CreateRoomSuccessPayload createRoomSuccessPayload = (CreateRoomSuccessPayload) packet.getData();
                RoomInfo newRoom = createRoomSuccessPayload.getRoomInfo();
                createdRoomsList.add(newRoom);
                showAlert("Thành công", "Đã tạo phòng: " + newRoom.getRoomName());
                break;

            case CREATE_ROOM_FAILED:
                GenericResponsePayload createRoomFailedPayload = (GenericResponsePayload) packet.getData();
                showAlert("Lỗi", createRoomFailedPayload.getMessage());
                break;

            case GET_JOINED_ROOMS_RESPONSE:
                ListRoomInfoPayload getJoinedRoomsResponsePayload = (ListRoomInfoPayload) packet.getData();
                joinedRoomsList.setAll(getJoinedRoomsResponsePayload.getListRoomInfo());
                break;

            case DELETE_ROOM_SUCCESS:
                PlayerIDAndRoomIDPayload deleteRoomSuccessPayload = (PlayerIDAndRoomIDPayload) packet.getData();
                showAlert("Thành công", "Đã xóa phòng.");
                createdRoomsList.removeIf(room -> room.getRoomID() == deleteRoomSuccessPayload.getRoomID());
                break;

            case DELETE_ROOM_FAILED:
                GenericResponsePayload deleteRoomFailedPayload = (GenericResponsePayload) packet.getData();
                showAlert("Lỗi", deleteRoomFailedPayload.getMessage());
                break;

            case LEAVE_ROOM_SUCCESS:
                PlayerIDAndRoomIDPayload leaveRoomSuccessPayload = (PlayerIDAndRoomIDPayload) packet.getData();
                showAlert("Thành công", "Đã rời phòng.");
                joinedRoomsList.removeIf(room -> room.getRoomID() == leaveRoomSuccessPayload.getRoomID());
                break;

            case LEAVE_ROOM_FAILED:
                GenericResponsePayload leaveRoomFailedPayload = (GenericResponsePayload) packet.getData();
                showAlert("Lỗi", leaveRoomFailedPayload.getMessage());
                break;

            case ROOM_OPEN_SUCCESS:
            {
                RoomStateUpdatePayload roomStateUpdatePayload = (RoomStateUpdatePayload) packet.getData();
                RoomInfo currentRoomInfo = roomStateUpdatePayload.getRoomInfo();
                if (currentRoomInfo == null) {
                    showAlert("Lỗi", "Không nhận được thông tin phòng.");
                    return;
                }
                boolean isOwner = (this.playerID == findOwnerID(roomStateUpdatePayload.getPlayerStates()));

                playerList.setAll(roomStateUpdatePayload.getPlayerStates()); // Cập nhật danh sách

                for (PlayerState p : roomStateUpdatePayload.getPlayerStates()) {
                    if (p.getPlayerID() == this.playerID) {
                        updateMyScore(p.getScore());
                        break;
                    }
                }

                if (activeCanvasController != null) {
                    activeCanvasController.showTools(isOwner);
                    activeCanvasController.setDrawingEnabled(isOwner);
                }

                showInRoomView(currentRoomInfo, isOwner);
                break;
            }

            case ROOM_JOIN_SUCCESS:
            {
                RoomJoinSuccessPayload roomJoinSuccessPayload = (RoomJoinSuccessPayload) packet.getData();
                RoomInfo currentRoomInfo = roomJoinSuccessPayload.getRoomInfo();
                if (currentRoomInfo == null) {
                    showAlert("Lỗi", "Không nhận được thông tin phòng.");
                    return;
                }
                boolean isOwner = (this.playerID == findOwnerID(roomJoinSuccessPayload.getPlayerList()));

                playerList.setAll(roomJoinSuccessPayload.getPlayerList());

                for (PlayerState p : roomJoinSuccessPayload.getPlayerList()) {
                    if (p.getPlayerID() == this.playerID) {
                        updateMyScore(p.getScore());
                        break;
                    }
                }

                if (activeCanvasController != null) {
                    activeCanvasController.showTools(isOwner);
                    activeCanvasController.setDrawingEnabled(isOwner);
                }

                showInRoomView(currentRoomInfo, isOwner);
                break;
            }

            case ROOM_STATE_UPDATE: // Ai đó vào/ra/offline
                RoomStateUpdatePayload roomStateUpdatePayload = (RoomStateUpdatePayload) packet.getData();
                playerList.setAll(roomStateUpdatePayload.getPlayerStates());

                for (PlayerState p : roomStateUpdatePayload.getPlayerStates()) {
                    if (p.getPlayerID() == this.playerID) {
                        updateMyScore(p.getScore());
                        break;
                    }
                }

                break;

            case YOU_WERE_KICKED:
                GenericResponsePayload youWereKickedPayload = (GenericResponsePayload) packet.getData();
                showAlert("Thông báo", youWereKickedPayload.getMessage());
                showLobbyView();
                loadMyRooms();
                loadJoinedRooms();
                break;

            case KICK_PLAYER_FAILED:
                GenericResponsePayload kickPlayerFailedPayload = (GenericResponsePayload) packet.getData();
                showAlert("Error", kickPlayerFailedPayload.getMessage());
                break;

            case ROOM_CLOSED_BY_OWNER:
                GenericResponsePayload roomClosedByOwnerPayload = (GenericResponsePayload) packet.getData();
                showAlert("Thông báo", roomClosedByOwnerPayload.getMessage());
                showLobbyView();
                loadMyRooms();
                loadJoinedRooms();
                break;

            case GET_ACTIVE_ROOM_IDS_RESPONSE:
                ListRoomIDPayload getActiveRoomIDSResponse = (ListRoomIDPayload) packet.getData();
                this.activeRoomIDs = getActiveRoomIDSResponse.getListRoomIDs();
                createdRoomsListView.refresh();
                joinedRoomsListView.refresh();
                break;

            case INCOMING_JOIN_REQUEST: // (Dành cho chủ phòng)
                InComingJoinRequestPayload inComingJoinPayload = (InComingJoinRequestPayload) packet.getData();
                handleIncomingJoinRequest(inComingJoinPayload);
                break;

            case JOIN_BY_CODE_FAILED:
                GenericResponsePayload joinByCodeFailedPayload = (GenericResponsePayload) packet.getData();
                showAlert("Thất bại", joinByCodeFailedPayload.getMessage());
                break;

            case JOIN_DENIED:
                GenericResponsePayload joinDeniedPayload = (GenericResponsePayload) packet.getData();
                showAlert("Bị từ chối", joinDeniedPayload.getMessage());
                break;

            case PUZZLE_BROADCAST: // (Dành cho người đoán)
                if (activeCanvasController != null) {
                    PuzzleBroadcastPayload payload = (PuzzleBroadcastPayload) packet.getData();
                    activeCanvasController.loadPuzzleImage(payload.getImageData());

                    // Kích hoạt UI cho người đoán
                    puzzleAnswerField.setDisable(false);
                    sendGuessButton.setDisable(false); // Bật nút "Gửi Đoán"
                }
                break;

            case ANSWER_CORRECT_BROADCAST: // (Dành cho mọi người)
                AnswerCorrectBroadcastPayload payload = (AnswerCorrectBroadcastPayload) packet.getData();

                showAlert("Đoán Đúng!", "Người chơi '" + payload.getWinnerName() +
                        "' đã đoán thành công! Đáp án là: " + payload.getCorrectAnswer());

                if (activeCanvasController != null) {
                    activeCanvasController.forceClearDrawing();
                }

                if (isOwnerOfCurrentRoom) {
                    // Chủ phòng: Sẵn sàng vẽ
                    puzzleAnswerField.setDisable(false);
                    sendPuzzleButton.setDisable(false);
                    activeCanvasController.setDrawingEnabled(true);
                    showAlert("Thông báo", "Câu đố đã được giải! Hãy vẽ câu tiếp theo.");
                } else {
                    // Người đoán: Chờ câu đố
                    puzzleAnswerField.clear();
                    puzzleAnswerField.setDisable(true);
                    sendGuessButton.setDisable(true); // Tắt nút "Gửi Đoán"
                }
                break;

            case ANSWER_WRONG_BROADCAST:
                showAlert("Đoán sai", "Đáp án sai rồi bro ơi!");
                break;

            case CHAT_MESSAGE_BROADCAST:
                ChatMessageBroadcastPayload chatMessageBroadcastPayload = (ChatMessageBroadcastPayload) packet.getData();
                addChatMessage(chatMessageBroadcastPayload.getSenderID(), chatMessageBroadcastPayload.getSenderName(), chatMessageBroadcastPayload.getMessageContent());

                if (!isChatWindowOpen && chatMessageBroadcastPayload.getSenderID() != this.playerID) {
                    chatNotificationDot.setVisible(true);
                    chatNotificationDot.setManaged(true);
                }
                break;

            case PLAYER_UDP_LIST_UPDATE:
                PlayerUdpListPayload udpListPayload = (PlayerUdpListPayload) packet.getData();
                break;

            case PLAYER_CAMERA_STATUS_UPDATE:
                PlayerCameraStatusPayload statusPayload = (PlayerCameraStatusPayload) packet.getData();
                if (statusPayload.getPlayerID() != this.playerID) {
                    updateVideoFeed(statusPayload.getPlayerID(), null, statusPayload.isCameraOn());
                    if (statusPayload.isCameraOn() && !isVideoWindowOpen) {
                        videoNotificationDot.setVisible(true);
                        videoNotificationDot.setManaged(true); // Sửa lỗi: Phải set cả 2
                    }
                }
                break;

            case VIDEO_FRAME_BROADCAST: // (Từ luồng UDP)
                VideoFramePayload framePayload = (VideoFramePayload) packet.getData();
                Image receivedImage = new Image(new ByteArrayInputStream(framePayload.getFrameData()));
                updateVideoFeed(framePayload.getPlayerID(), receivedImage, true);
                break;

            case AUDIO_FRAME_BROADCAST: // (Từ luồng UDP)
                AudioFramePayload audioPayload = (AudioFramePayload) packet.getData();
                playAudioData(audioPayload.getSenderID(), audioPayload.getAudioData());
                break;

            case PLAYER_MIC_STATUS_UPDATE:
                PlayerMicStatusPayload micStatus = (PlayerMicStatusPayload) packet.getData();
                if (micStatus.getPlayerID() != this.playerID) {
                    // Nếu người khác tắt mic, ta phải DỪNG loa của họ
                    if (!micStatus.isMicOn()) {
                        SourceDataLine speaker = audioPlaybackLines.remove(micStatus.getPlayerID());
                        if (speaker != null) {
                            speaker.stop();
                            speaker.close();
                        }
                    }
                }
                break;

            case FORCE_REFRESH_DATA:
                Platform.runLater(() -> {
                    loadMyRooms();
                    loadJoinedRooms();
                    if (currentRoomID != -1) {
                        handleReloadPlayerList();
                    }
                });
                break;

            case ADMIN_CHAT_BROADCAST:
                AdminChatBroadcastPayload adminChatPayload = (AdminChatBroadcastPayload) packet.getData();
                addSystemMessage(adminChatPayload.getMessageContent());

                if (!isChatWindowOpen) {
                    chatNotificationDot.setVisible(true);
                    chatNotificationDot.setManaged(true);
                }
                break;

            case ERROR:
                GenericResponsePayload errorPayload = (GenericResponsePayload) packet.getData();
                showAlert("Error", errorPayload.getMessage());
                break;
        }
    }

    private void loadMyRooms() {
        if (client == null) return;
        PlayerIDPayload playerIDPayload = new PlayerIDPayload(this.playerID);
        client.sendMessage(new Packet(MessageType.GET_MY_ROOMS_REQUEST, playerIDPayload));
    }

    private void loadJoinedRooms() {
        if (client == null) return;
        PlayerIDPayload playerIDPayload = new PlayerIDPayload(this.playerID);
        client.sendMessage(new Packet(MessageType.GET_JOINED_ROOMS_REQUEST, playerIDPayload));
    }

    private void loadActiveRoomIDs() {
        if (client == null) return;
        PlayerIDPayload playerIDPayload = new PlayerIDPayload(this.playerID);
        client.sendMessage(new Packet(MessageType.GET_ACTIVE_ROOM_IDS_REQUEST, playerIDPayload));
    }

    private void showLobbyView() {
        leftNavPane.setVisible(true);
        leftNavPane.setManaged(true);
        gamePane.setVisible(true);
        gamePane.setManaged(true);
        inRoomPane.setVisible(false);
        inRoomPane.setManaged(false);

        currentRoomID = -1;
        isOwnerOfCurrentRoom = false;

        updateMyScore(this.playerScore);

        // DỌN DẸP
        canvasHostPane.setCenter(null);
        activeCanvasController = null;
        showChatWindow(false);
        chatMessagesListView.getItems().clear();
        chatNotificationDot.setVisible(false);

        // DỌN DẸP VIDEO
        if (isMyCameraOn) {
            toggleMyCamera(); // Tự động tắt cam
        }
        showVideoWindow(false); // Đóng cửa sổ
        videoGridPane.getChildren().clear(); // Xóa lưới
        videoTiles.clear(); // Xóa map
        videoToggleButton.getParent().setVisible(true); // Hiện lại icon bar
    }

    private void showInRoomView(RoomInfo room, boolean isOwner) {
        leftNavPane.setVisible(false);
        leftNavPane.setManaged(false);
        gamePane.setVisible(false);
        gamePane.setManaged(false);
        inRoomPane.setVisible(true);
        inRoomPane.setManaged(true);

        lblInRoomName.setText(playerName);
        lblInRoomID.setText("(ID: " + playerID + ")");
        lblInRoomScore.setText(playerScore + " pts");

        currentRoomID = room.getRoomID();
        isOwnerOfCurrentRoom = isOwner;
        lblRoomName.setText(room.getRoomName());
        lblRoomCode.setText("(Code: " + room.getRoomCode() + ")");
        btnCloseRoom.setVisible(isOwner);
        btnCloseRoom.setManaged(isOwner);
        btnExitRoom.setVisible(!isOwner);
        btnExitRoom.setManaged(!isOwner);

        if (activeCanvasController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleguess/battleguess/view/canvas-view.fxml"));
                Node canvasView = loader.load();
                activeCanvasController = loader.getController();
                canvasHostPane.setCenter(canvasView);

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Lỗi nghiêm trọng", "Không thể tải component canvas-view.fxml");
            }
        }

        if (activeCanvasController != null) {
            activeCanvasController.setDrawingEnabled(isOwner);
            activeCanvasController.showTools(isOwner);
        }

        canvasAndGuessArea.setVisible(true);
        canvasAndGuessArea.setManaged(true);
        videoWindowContainer.setVisible(false);
        videoWindowContainer.setManaged(false);
        videoToggleButton.getParent().setVisible(true);

        showChatWindow(false);

        if (isOwner) {
            // Chủ phòng: Sẵn sàng vẽ
            puzzleAnswerField.setPromptText("Nhập đáp án...");
            puzzleAnswerField.setDisable(false);

            sendPuzzleButton.setVisible(true);
            sendPuzzleButton.setManaged(true);
            sendPuzzleButton.setDisable(false);

            sendGuessButton.setVisible(false);
            sendGuessButton.setManaged(false);

        } else {
            // Người đoán: Chờ câu đố
            puzzleAnswerField.setPromptText("Nhập dự đoán...");
            puzzleAnswerField.setDisable(true); // Tắt cho đến khi có câu đố

            sendPuzzleButton.setVisible(false);
            sendPuzzleButton.setManaged(false);

            sendGuessButton.setVisible(true);
            sendGuessButton.setManaged(true);
            sendGuessButton.setDisable(true);
        }
    }

    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showChatWindow(boolean show) {
        isChatWindowOpen = show;

        chatWindowContainer.setVisible(show);
        chatWindowContainer.setManaged(show);

//        chatIconBar.setVisible(!show);
//        chatIconBar.setManaged(!show);

        if (show && isVideoWindowOpen) {
            showVideoWindow(false);
        }

        if (show) {
            chatNotificationDot.setVisible(false);
            chatNotificationDot.setManaged(false);
        }
    }

    private void showVideoWindow(boolean show) {
        isVideoWindowOpen = show;

        // Bật/Tắt cửa sổ Video (nằm ở trung tâm)
        videoWindowContainer.setVisible(show);
        videoWindowContainer.setManaged(show);

        // Bật/Tắt cửa sổ Canvas (nằm ở trung tâm)
        canvasAndGuessArea.setVisible(!show);
        canvasAndGuessArea.setManaged(!show);

        // Ẩn cửa sổ Chat nếu nó đang mở
        if (show && isChatWindowOpen) {
            showChatWindow(false);
        }

        if (show) {
            videoNotificationDot.setVisible(false);
            videoNotificationDot.setManaged(false);
        }
    }

    private void updateMyScore(int newScore) {
        this.playerScore = newScore;
        Platform.runLater(() -> {
            if (lblPlayerScore != null) {
                lblPlayerScore.setText("Điểm: " + newScore); // Cập nhật ở Sảnh
            }
            if (lblInRoomScore != null) {
                lblInRoomScore.setText(newScore + " pts"); // Cập nhật ở Phòng
            }
        });
    }

    private int findOwnerID(List<PlayerState> states) {
        for(PlayerState p : states) if(p.isOwner()) return p.getPlayerID();
        return -1;
    }

    private void toggleMyCamera() {
        if (isMyCameraOn) {
            // --- TẮT CAMERA ---
            isMyCameraOn = false;

            toggleSelfCameraButton.setText("Đang tắt...");
            toggleSelfCameraButton.setDisable(true);

            CameraStatusUpdatePayload payload = new CameraStatusUpdatePayload(playerID, currentRoomID, false);
            client.sendMessage(new Packet(MessageType.CAMERA_STATUS_UPDATE, payload));

            Task<Void> closeTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    if (webcamTask != null) webcamTask.cancel(true);
                    if (myWebcam != null && myWebcam.isOpen()) {
                        myWebcam.close();
                        System.out.println("Webcam closed.");
                    }
                    return null;
                }
            };
            closeTask.setOnSucceeded(e -> {
                toggleSelfCameraButton.setText("Bật Camera");
                toggleSelfCameraButton.getStyleClass().remove("danger");
                toggleSelfCameraButton.setDisable(false);

                // --- FIX LỖI "ĐỨNG HÌNH" (Problem 1) ---
                Platform.runLater(() -> updateVideoFeed(playerID, null, false));
            });
            new Thread(closeTask).start();

        } else {
            // --- BẬT CAMERA ---
            isMyCameraOn = true;
            toggleSelfCameraButton.setText("Đang mở...");
            toggleSelfCameraButton.setDisable(true);

            CameraStatusUpdatePayload payload = new CameraStatusUpdatePayload(playerID, currentRoomID, true);
            client.sendMessage(new Packet(MessageType.CAMERA_STATUS_UPDATE, payload));

            startWebcamTask();
        }
    }

    private void toggleMyMic() {
        isMyMicOn = !isMyMicOn;

        // 1. Gửi lệnh TCP
        MicStatusUpdatePayload payload = new MicStatusUpdatePayload(playerID, currentRoomID, isMyMicOn);
        client.sendMessage(new Packet(MessageType.MIC_STATUS_UPDATE, payload));

        if (isMyMicOn) {
            // --- BẬT MIC ---
            toggleSelfMicButton.setText("Tắt Mic");
            toggleSelfMicButton.getStyleClass().add("danger");
            startAudioSendTask(); // Bắt đầu luồng gửi âm thanh
        } else {
            // --- TẮT MIC ---
            toggleSelfMicButton.setText("Bật Mic");
            toggleSelfMicButton.getStyleClass().remove("danger");
            if (audioSendTask != null) {
                audioSendTask.cancel(true); // Dừng luồng
                audioSendTask = null;
            }
        }
    }

    private void startWebcamTask() {
        if (webcamTask != null) webcamTask.cancel(true);

        webcamTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. KHỞI TẠO WEBCAM (TRONG LUỒNG NỀN)
                if (myWebcam == null) {
                    System.out.println("Finding webcam...");
                    myWebcam = Webcam.getDefault();
                    if (myWebcam == null) {
                        Platform.runLater(() -> showAlert("Lỗi Camera", "Không tìm thấy webcam."));
                        throw new IllegalStateException("No webcam found");
                    }
                    myWebcam.setViewSize(WebcamResolution.QQVGA.getSize());
                }

                // 2. MỞ WEBCAM
                if (!myWebcam.isOpen()) {
                    System.out.println("Opening webcam...");
                    myWebcam.open();
                }

                // 3. CẬP NHẬT UI (BÁO LÀ ĐÃ MỞ)
                Platform.runLater(() -> {
                    toggleSelfCameraButton.setText("Tắt Camera");
                    toggleSelfCameraButton.getStyleClass().add("danger");
                    toggleSelfCameraButton.setDisable(false);
                });

                // 4. BẮT ĐẦU VÒNG LẶP GỬI
                while (!isCancelled()) {
                    BufferedImage awtImage = myWebcam.getImage();
                    if (awtImage == null) continue;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(awtImage, "JPG", baos);
                    byte[] frameData = baos.toByteArray();

                    client.sendUdpData(UDP_PACKET_TYPE_VIDEO, playerID, currentRoomID, frameData);

                    Image fxImage = new Image(new ByteArrayInputStream(frameData));
                    Platform.runLater(() -> updateVideoFeed(playerID, fxImage, true));

                    Thread.sleep(50); // 10 FPS
                }
                return null;
            }
        };

        webcamTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showAlert("Lỗi Camera", "Không thể khởi động webcam.");
                if (isMyCameraOn) {
                    toggleMyCamera(); // Tự động reset
                }
            });
        });

        new Thread(webcamTask).start();
    }

    private void startAudioSendTask() {
        if (audioSendTask != null) audioSendTask.cancel(true);

        audioSendTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                TargetDataLine microphone;
                try {
                    microphone = AudioSystem.getTargetDataLine(AUDIO_FORMAT);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
                    microphone = (TargetDataLine) AudioSystem.getLine(info);
                    microphone.open(AUDIO_FORMAT);
                } catch (LineUnavailableException e) {
                    Platform.runLater(() -> showAlert("Lỗi Mic", "Không thể mở micro."));
                    return null;
                }

                microphone.start();
                byte[] buffer = new byte[1024]; // Gói 1024 byte

                while (!isCancelled()) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        // Gửi âm thanh bằng UDP
                        client.sendUdpData(UDP_PACKET_TYPE_AUDIO, playerID, currentRoomID, buffer);
                    }
                }

                // Dọn dẹp
                microphone.stop();
                microphone.close();
                return null;
            }
        };

        audioSendTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                if (isMyMicOn) toggleMyMic(); // Tự reset nếu lỗi
            });
        });

        new Thread(audioSendTask).start();
    }

    private void updateVideoFeed(int feedPlayerID, Image image, boolean isCameraOn) {
        VideoTile tile = videoTiles.get(feedPlayerID);

        if (tile == null) {
            // Lấy tên từ danh sách PlayerState
            String name = playerList.stream()
                    .filter(p -> p.getPlayerID() == feedPlayerID)
                    .map(PlayerState::getUsername)
                    .findFirst()
                    .orElse("Player " + feedPlayerID);

            tile = new VideoTile(name); // Tạo Tile mới
            videoTiles.put(feedPlayerID, tile);

            VideoTile finalTile = tile;
            Platform.runLater(() -> videoGridPane.getChildren().add(finalTile));
        }

        // Cập nhật ảnh (hoặc tắt)
        if (isCameraOn) {
            tile.updateImage(image);
        } else {
            tile.setCameraOff(); // <-- FIX LỖI "ĐỨNG HÌNH"
        }
    }

    private void playAudioData(int senderID, byte[] audioData) {
        try {
            SourceDataLine speaker = audioPlaybackLines.get(senderID);

            // Nếu là người nói mới, tạo Loa mới cho họ
            if (speaker == null) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
                speaker = (SourceDataLine) AudioSystem.getLine(info);
                speaker.open(AUDIO_FORMAT);
                speaker.start();
                audioPlaybackLines.put(senderID, speaker);
            }

            // Ghi (phát) âm thanh ra loa
            speaker.write(audioData, 0, audioData.length);

            // --- LOGIC HIGHLIGHT  ---
            VideoTile tile = videoTiles.get(senderID);
            if (tile != null) {
                tile.setSpeaking(true); // Bật sáng
            }

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void sendChatMessage() {
        String message = chatInputField.getText().trim();
        if (message.isEmpty() || currentRoomID == -1) {
            return;
        }

        SendChatMessageRequestPayload payload = new SendChatMessageRequestPayload(playerID, currentRoomID, message);

        client.sendMessage(new Packet(MessageType.SEND_CHAT_MESSAGE_REQUEST, payload));
        chatInputField.clear();
    }

    private void addChatMessage(int senderID, String senderName, String messageContent) {
        Text senderText = new Text(senderName + "\n");
        senderText.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        Text messageText = new Text(messageContent);
        messageText.setStyle("-fx-font-size: 14px;");

        TextFlow textFlow = new TextFlow(senderText, messageText);

        HBox messageBox = new HBox(textFlow);

        if (senderID == this.playerID) {
            // TIN CỦA MÌNH: Màu xanh, căn phải
            senderText.setFill(Color.web("#d1eaff"));
            messageText.setFill(Color.WHITE);
            textFlow.setStyle("-fx-background-color: #0084ff; -fx-padding: 8px; -fx-background-radius: 15px;");
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            // TIN CỦA NGƯỜI KHÁC: Màu xám, căn trái
            senderText.setFill(Color.web("#b0b3b8"));
            messageText.setFill(Color.BLACK);
            textFlow.setStyle("-fx-background-color: #e4e6eb; -fx-padding: 8px; -fx-background-radius: 15px;");
            messageBox.setAlignment(Pos.CENTER_LEFT);
        }

        chatMessagesListView.getItems().add(messageBox);

        // Tự động cuộn xuống
        chatMessagesListView.scrollTo(chatMessagesListView.getItems().size() - 1);
    }

    public void gracefulShutdown() {
        if (client != null) {
            client.disconnect();
        }
    }

    private void handleIncomingJoinRequest(InComingJoinRequestPayload payload) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Yêu cầu gia nhập");
        alert.setHeaderText("Người chơi '" + payload.getJoinerName() + "' muốn vào phòng của bạn.");
        alert.setContentText("Bạn có đồng ý không?");

        Optional<ButtonType> result = alert.showAndWait();
        boolean approved = (result.isPresent() && result.get() == ButtonType.OK);

        JoinRequestResponsePayload joinRequestResponsePayload = new JoinRequestResponsePayload(payload.getJoinerID(), payload.getRoomInfo(), approved, payload.getJoinerName());
        client.sendMessage(new Packet(MessageType.JOIN_REQUEST_RESPONSE, joinRequestResponsePayload));
    }

    private void addSystemMessage(String messageContent) {
        Text senderText = new Text("HỆ THỐNG ADMIN\n");
        senderText.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-fill: #ffeb3b;");

        Text messageText = new Text(messageContent);
        messageText.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-fill: white;");

        TextFlow textFlow = new TextFlow(senderText, messageText);
        textFlow.setStyle("-fx-background-color: #c0392b; -fx-padding: 10px; -fx-background-radius: 10px; -fx-border-color: #f1c40f; -fx-border-width: 2px; -fx-border-radius: 10px;");

        HBox messageBox = new HBox(textFlow);
        messageBox.setAlignment(Pos.CENTER_LEFT);

        chatMessagesListView.getItems().add(messageBox);
        chatMessagesListView.scrollTo(chatMessagesListView.getItems().size() - 1);
    }

    private class CreatedRoomCell extends ListCell<RoomInfo> {
        private HBox hbox = new HBox(5);
        private Label label = new Label();
        private Button openButton = new Button("▶ Mở"); // THÊM NÚT NÀY
        private Button deleteButton = new Button("❌");
        private Region spacer = new Region();

        public CreatedRoomCell() {
            super();
            openButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 10px;");
            deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-size: 10px;");

            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().addAll(label, spacer, openButton, deleteButton);
            hbox.setAlignment(Pos.CENTER_LEFT);

            // GỬI YÊU CẦU MỞ PHÒNG
            openButton.setOnAction(event -> {
                RoomInfo room = getItem();
                PlayerIDAndRoomIDPayload playerIDAndRoomIDPayload = new PlayerIDAndRoomIDPayload(playerID, room.getRoomID());
                client.sendMessage(new Packet(MessageType.OPEN_ROOM_REQUEST, playerIDAndRoomIDPayload));
            });

            deleteButton.setOnAction(event -> {
                RoomInfo room = getItem();
                // Hiển thị xác nhận
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Xác nhận xóa");
                alert.setHeaderText("Bạn có chắc chắn muốn xóa phòng: " + room.getRoomName() + "?");
                alert.setContentText("Hành động này không thể hoàn tác.");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    PlayerIDAndRoomIDPayload playerIDAndRoomIDPayload = new PlayerIDAndRoomIDPayload(playerID, room.getRoomID());
                    client.sendMessage(new Packet(MessageType.DELETE_ROOM_REQUEST, playerIDAndRoomIDPayload));
                }
            });
        }

        @Override
        protected void updateItem(RoomInfo item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle(null); // Xóa style
            } else {
                label.setText(item.toString());
                setGraphic(hbox);

                // TÔ MÀU NẾU ACTIVE
                if (getActiveRoomIDs().contains(item.getRoomID())) {
                    setStyle("-fx-border-color: #2ecc71; -fx-border-width: 2;");
                } else {
                    setStyle(null);
                }
            }
        }
    }

    private class JoinedRoomCell extends ListCell<RoomInfo> {
        private HBox hbox = new HBox();
        private Label label = new Label();
        private Button leaveButton = new Button("Rời");
        private Button joinButton = new Button("Tham gia");
        private Region spacer = new Region();

        public JoinedRoomCell() {
            super();
            leaveButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px;");
            joinButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 10px;");

            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().addAll(label, spacer, joinButton, leaveButton); // Thêm joinButton nếu cần
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setSpacing(5);

            joinButton.setOnAction(event -> {
                RoomInfo room = getItem();
                PlayerIDAndRoomIDPayload playerIDAndRoomIDPayload = new PlayerIDAndRoomIDPayload(playerID, room.getRoomID());
                client.sendMessage(new Packet(MessageType.JOIN_ROOM_SESSION_REQUEST, playerIDAndRoomIDPayload));
            });

            leaveButton.setOnAction(event -> {
                RoomInfo room = getItem();
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Xác nhận rời phòng");
                alert.setHeaderText("Bạn có chắc chắn muốn rời phòng: " + room.getRoomName() + "?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    PlayerIDAndRoomIDPayload playerIDAndRoomIDPayload = new PlayerIDAndRoomIDPayload(playerID, room.getRoomID());
                    client.sendMessage(new Packet(MessageType.LEAVE_ROOM_REQUEST, playerIDAndRoomIDPayload));
                }
            });
        }

        @Override
        protected void updateItem(RoomInfo item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle(null);
            } else {
                label.setText(item.toString());
                setGraphic(hbox);

                // TÔ MÀU NẾU ACTIVE
                if (getActiveRoomIDs().contains(item.getRoomID())) {
                    setStyle("-fx-border-color: #2ecc71; -fx-border-width: 2;");
                    joinButton.setDisable(false); // Cho phép Tái gia nhập
                } else {
                    setStyle(null);
                    joinButton.setDisable(true); // Không cho gia nhập phòng offline
                }
            }
        }
    }

    private class PlayerListCell extends ListCell<PlayerState> {
        private HBox hbox = new HBox(5);
        private Label label = new Label();
        private Button kickButton = new Button("Kick");
        private Region spacer = new Region();

        public PlayerListCell() {
            super();
            kickButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px;");
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().addAll(label, spacer, kickButton);
            hbox.setAlignment(Pos.CENTER_LEFT);

            kickButton.setOnAction(event -> {
                PlayerState playerToKick = getItem();
                KickPlayerRequestPayload kickPlayerRequestPayload = new KickPlayerRequestPayload(playerID, currentRoomID, playerToKick.getPlayerID());
                client.sendMessage(new Packet(MessageType.KICK_PLAYER_REQUEST, kickPlayerRequestPayload));
            });
        }

        @Override
        protected void updateItem(PlayerState item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                label.setText(item.toString());
                if (item.isOnline()) {
                    label.setTextFill(Color.GREEN);
                } else {
                    label.setTextFill(Color.GRAY);
                }

                if (isOwnerOfCurrentRoom && item.getPlayerID() != playerID) {
                    kickButton.setVisible(true);
                    kickButton.setManaged(true);
                } else {
                    kickButton.setVisible(false);
                    kickButton.setManaged(false);
                }

                setGraphic(hbox);
            }
        }
    }

    private class VideoTile extends VBox {
        private ImageView imageView;
        private Label nameLabel;
        private ObjectProperty<Image> imageProperty;
        private StackPane stack;
        private PauseTransition speakingTimer;

        public VideoTile(String playerName) {
            super();
            this.setAlignment(Pos.CENTER);
            // Style cho cái "khung" của video
            this.setStyle("-fx-background-color: #222; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #555;");

            // 1. Tạo ô hiển thị ảnh
            imageView = new ImageView();
            imageView.setFitWidth(240); // Kích thước ô video
            imageView.setFitHeight(180);
            imageView.setPreserveRatio(false);

            // 2. Dùng Property để dễ dàng cập nhật ảnh
            imageProperty = new SimpleObjectProperty<>();
            imageView.imageProperty().bind(imageProperty);

            // 3. Tạo tên (hiển thị đè lên video)
            nameLabel = new Label(playerName);
            nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5; -fx-background-color: rgba(0,0,0,0.5);");

            // 4. Dùng StackPane
            stack = new StackPane(imageView, nameLabel);
            StackPane.setAlignment(nameLabel, Pos.BOTTOM_LEFT);
            this.getChildren().add(stack);

            // 5. Khởi tạo Timer (200ms)
            speakingTimer = new PauseTransition(Duration.millis(200));
            speakingTimer.setOnFinished(e -> {
                // Khi timer kết thúc, xóa viền
                this.setStyle("-fx-background-color: #222; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #555;");
            });
        }

        public void updateImage(Image image) {
            Platform.runLater(() -> imageProperty.set(image));
        }

        public void setCameraOff() {
            // Set về null (rỗng)
            Platform.runLater(() -> imageProperty.set(null));
        }

        public void setSpeaking(boolean isSpeaking) {
            Platform.runLater(() -> {
                if (isSpeaking) {
                    // Đặt style viền xanh
                    this.setStyle("-fx-background-color: #222; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #2ecc71; -fx-border-width: 3;");
                    // Reset timer
                    speakingTimer.playFromStart();
                }
            });
        }
    }
}
