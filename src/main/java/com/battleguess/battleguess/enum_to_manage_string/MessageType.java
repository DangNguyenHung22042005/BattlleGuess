package com.battleguess.battleguess.enum_to_manage_string;

public enum MessageType {
    // Yêu cầu từ Client (Login/Register)
    LOGIN_REQUEST,
    REGISTER_REQUEST,
    RESET_PASSWORD_REQUEST,

    // Phản hồi từ Server
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    REGISTER_SUCCESS,
    REGISTER_FAILED,
    RESET_PASSWORD_SUCCESS,
    RESET_PASSWORD_FAILED,

    // Yêu cầu từ Client (Quản lý phòng)
    CREATE_ROOM_REQUEST,
    GET_MY_ROOMS_REQUEST,
    GET_JOINED_ROOMS_REQUEST,
    DELETE_ROOM_REQUEST,
    LEAVE_ROOM_REQUEST,

    // --- CÁC YÊU CẦU "HIỆN DIỆN" (PRESENCE) ---
    OPEN_ROOM_REQUEST,        // (Chủ phòng) Yêu cầu "mở" phòng
    JOIN_ROOM_SESSION_REQUEST, // (Thành viên) Yêu cầu "vào" phòng đang mở
    CLOSE_ROOM_REQUEST,       // (Chủ phòng) Yêu cầu "đóng" phòng
    EXIT_ROOM_SESSION_REQUEST, // (Thành viên) Yêu cầu "thoát" khỏi phòng (về sảnh)
    KICK_PLAYER_REQUEST,      // (Chủ phòng) Yêu cầu "kick" một người
    GET_ROOM_STATE_REQUEST,   // (Client) Yêu cầu refresh danh sách người chơi

    // --- TÌM KIẾM VÀ GIA NHẬP (Cần xét duyệt) ---
    GET_ACTIVE_ROOM_IDS_REQUEST, // (Client) Yêu cầu danh sách ID các phòng đang mở
    JOIN_BY_CODE_REQUEST,        // (Client -> Server) Yêu cầu join bằng code
    INCOMING_JOIN_REQUEST,       // (Server -> Owner) Báo cho chủ phòng có người xin vào
    JOIN_REQUEST_RESPONSE,       // (Owner -> Server) Chủ phòng đồng ý/từ chối
    JOIN_DENIED,                 // (Server -> Client) Thông báo bị từ chối

    // --- PHẢN HỒI CHO LOGIC JOIN ---
    GET_ACTIVE_ROOM_IDS_RESPONSE,// (Server) Gửi danh sách ID các phòng đang mở
    JOIN_BY_CODE_FAILED,         // (Server -> Client) Báo lỗi (VD: code sai)

    // Phản hồi từ Server
    CREATE_ROOM_SUCCESS,
    CREATE_ROOM_FAILED,
    GET_MY_ROOMS_RESPONSE,
    GET_JOINED_ROOMS_RESPONSE,
    DELETE_ROOM_SUCCESS,
    DELETE_ROOM_FAILED,
    LEAVE_ROOM_SUCCESS,
    LEAVE_ROOM_FAILED,

    // --- CÁC PHẢN HỒI "HIỆN DIỆN" (PRESENCE) ---
    ROOM_OPEN_SUCCESS,        // (Gửi cho chủ phòng) OK, phòng đã mở, đây là danh sách
    ROOM_JOIN_SUCCESS,        // (Gửi cho thành viên) OK, bạn đã vào
    ROOM_STATE_UPDATE,        // (Gửi cho mọi người) Cập nhật danh sách người chơi/status
    YOU_WERE_KICKED,          // (Gửi cho người bị kick)
    KICK_PLAYER_FAILED,       // (Gửi lại cho chủ phòng)
    ROOM_CLOSED_BY_OWNER,     // (Gửi cho mọi người) Phòng đã bị chủ đóng

    ROOM_NOW_ACTIVE,          // (Broadcast) Báo cho sảnh biết 1 phòng vừa mở
    ROOM_NOW_INACTIVE,        // (Broadcast) Báo cho sảnh biết 1 phòng vừa đóng

    // --- LOGIC GAME CHÍNH ---
    SEND_PUZZLE_REQUEST,        // (Owner -> Server) Gửi câu đố + đáp án
    PUZZLE_BROADCAST,           // (Server -> Guessers) Gửi hình ảnh cho người đoán
    SEND_GUESS_REQUEST,         // (Guesser -> Server) Gửi dự đoán
    ANSWER_CORRECT_BROADCAST,   // (Server -> All) Báo có người đoán đúng
    ANSWER_WRONG_BROADCAST,

    SEND_CHAT_MESSAGE_REQUEST,      // (Client -> Server)
    CHAT_MESSAGE_BROADCAST,       // (Server -> All Clients)

    REGISTER_UDP_PORT_REQUEST,    // (Client -> Server) Báo "Tôi nghe UDP ở port 9001"
    PLAYER_UDP_LIST_UPDATE,     // (Server -> All) Gửi danh sách UDP address của mọi người
    CAMERA_STATUS_UPDATE,       // (Client -> Server) Báo tôi Bật/Tắt cam
    PLAYER_CAMERA_STATUS_UPDATE, // (Server -> All) Báo Player A Bật/Tắt cam (cho UI)

    VIDEO_FRAME_BROADCAST,      // (UDP Receiver -> ClientController) Gói tin "nội bộ"

    MIC_STATUS_UPDATE,          // (Client -> Server) Báo tôi Bật/Tắt mic
    PLAYER_MIC_STATUS_UPDATE,   // (Server -> All) Báo Player A Bật/Tắt mic (cho UI)
    AUDIO_FRAME_BROADCAST,      // (UDP Receiver -> ClientController) Gói tin "nội bộ"

    // --- LỆNH HỆ THỐNG ---
    FORCE_REFRESH_DATA, // (Server -> All Clients) Yêu cầu tải lại danh sách phòng/tham gia
    ADMIN_CHAT_BROADCAST,         // (Server -> All Clients in Room) Tin nhắn từ Admin

    ERROR;
}
