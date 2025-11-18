package com.battleguess.battleguess;

import com.battleguess.battleguess.enum_to_manage_string.MessageType;
import com.battleguess.battleguess.network.response.GenericResponsePayload;
import com.battleguess.battleguess.network.Packet;
import com.battleguess.battleguess.network.response.VideoFramePayload;
import javafx.application.Platform;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Packet> messageHandler;
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean isRunning = true;

    private DatagramSocket udpSocket;
    private SocketAddress serverUdpAddress; // Địa chỉ UDP của SERVER

    public Client(String host, int port, Consumer<Packet> messageHandler) throws IOException {
        this.messageHandler = messageHandler;
        this.isRunning = true;

        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            if (socket != null) {
                System.out.println("Connect successfully!");
            }

            // 2. Mở cổng UDP (lấy port ngẫu nhiên)
            udpSocket = new DatagramSocket();
            System.out.println("UDP Receiver listening on port: " + udpSocket.getLocalPort());

            // 3. Lưu địa chỉ UDP của Server (Cổng 8000)
            this.serverUdpAddress = new InetSocketAddress(host, 8000);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Connection failed: " + e.getMessage()); // Ném lỗi để LoginController bắt
        }
        startListening();
        startUdpListening(); // Bắt đầu luồng UDP
    }

    public int getUdpPort() {
        return udpSocket.getLocalPort();
    }

    public void setMessageHandler(Consumer<Packet> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void sendMessage(Packet packet) {
        sendExecutor.submit(() -> {
            try {
                synchronized (out) {
                    out.writeObject(packet);
                    out.flush();
                    out.reset();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (isRunning) {
                    e.printStackTrace();
                    Platform.runLater(() -> messageHandler.accept(new Packet(MessageType.ERROR, new GenericResponsePayload("Connection error: " + e.getMessage()))));
                }
            }
        });
    }

    public void sendUdpFrame(int playerID, int roomID, byte[] frameData) {
        if (udpSocket == null || !isRunning) return;

        // Chuẩn bị header (ID người gửi + ID phòng)
        ByteBuffer buffer = ByteBuffer.allocate(8 + frameData.length);
        buffer.putInt(playerID);
        buffer.putInt(roomID);
        buffer.put(frameData);
        byte[] data = buffer.array();

        // Gửi (không cần thread riêng vì UDP không blocking)
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, serverUdpAddress);
            udpSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (isRunning) {
                    Packet packet = (Packet) in.readObject();
                    if (!isRunning) break;
                    Platform.runLater(() -> messageHandler.accept(packet));
                }
            } catch (EOFException e) {
                if (isRunning) {
                    System.out.println("Server disconnected.");
                    Platform.runLater(() -> messageHandler.accept(new Packet(MessageType.ERROR, new GenericResponsePayload("Lost connection to server."))));
                }
            } catch (IOException | ClassNotFoundException e) {
                if (isRunning) {
                    e.printStackTrace();
                    Platform.runLater(() -> messageHandler.accept(new Packet(MessageType.ERROR, new GenericResponsePayload("Connection error: " + e.getMessage()))));
                }
            } finally {
                //
            }
        }).start();
    }

    private void startUdpListening() {
        new Thread(() -> {
            byte[] buffer = new byte[65507];
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet); // Chờ (blocking)

                    byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());

                    // --- SỬA LỖI Ở ĐÂY ---
                    // Phải kiểm tra gói tin rác (hole punch)
                    if (data.length <= 1) {
                        continue; // Bỏ qua, đây là gói dummy
                    }

                    // Đọc header (Server nhét vào)
                    ByteBuffer bb = ByteBuffer.wrap(data);
                    int senderID = bb.getInt();
                    byte[] frameData = new byte[data.length - 4];
                    bb.get(frameData);

                    VideoFramePayload payload = new VideoFramePayload(senderID, frameData);
                    Packet internalPacket = new Packet(MessageType.VIDEO_FRAME_BROADCAST, payload);

                    Platform.runLater(() ->messageHandler.accept(internalPacket));

                } catch (IOException e) {
                    if (isRunning) e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Gửi một gói UDP "rỗng" (1 byte) để "đục lỗ" (Hole Punching)
     * cho firewall/NAT của client.
     */
    public void sendDummyUdpPacket() {
        if (udpSocket == null || !isRunning) return;
        try {
            byte[] dummyData = new byte[1]; // Chỉ cần 1 byte rác
            DatagramPacket packet = new DatagramPacket(dummyData, dummyData.length, serverUdpAddress);
            udpSocket.send(packet);
            System.out.println("Dummy UDP packet sent (hole punch).");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (!isRunning) return;
        isRunning = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
            sendExecutor.shutdownNow();
            System.out.println("Client disconnected!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
