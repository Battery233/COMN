/* Chenghao Ye s1786987 */

import java.io.File;
import java.io.FileInputStream;
import java.net.*;

public class Sender2a {

    // define size here
    private static final int DATA_SIZE = 1024;
    private static final int PACkET_SIZE = DATA_SIZE + 5;   // head size = 5
    private static final int ACK_PACkET_SIZE = 4;   // ack size
    private static int sequence;
    private static long number;
    private static DatagramSocket socket;

    public static void main(String[] args) {
//        System.out.println("RemoteHost: " + args[0] + " Port: " + args[1] + " Filename: " + args[2] + " Timeout: " + args[3]);
        try {
            transport(InetAddress.getByName(args[0]), Integer.valueOf(args[1]), args[2], Integer.valueOf(args[3]), Integer.valueOf(args[4]));
        } catch (UnknownHostException e) {
//            System.out.println("Unknown host: " + args[0]);
        }
    }

    private static void transport(InetAddress RemoteHost, int Port, String Filename, int timeout, int windowSize) {
        int totalResend = 0;
        int speed = 0;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
            File file = new File(Filename);

            // calculate the number of packets needed
            number = file.length() / DATA_SIZE;
            if (file.length() % DATA_SIZE != 0) {
                number++;
            }
//            System.out.println("File size: " + file.length() + " packets number: " + number);

            // read file
            byte[] fileBytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(fileBytes);
            fis.close();

            int resendCounter = 0;
            int eofCounter = 0;

            long time = System.currentTimeMillis();

            int i;

            for (sequence = 0; sequence < number; sequence++) {
                byte[] packet;
                if (sequence < number - 1) {
                    packet = new byte[PACkET_SIZE];
                    // eof flag
                    packet[4] = 0;
                    for (i = 0; i < DATA_SIZE; i++) {
                        packet[i + 5] = fileBytes[DATA_SIZE * sequence + i];
                    }
                } else {
                    packet = new byte[(int) (file.length() % DATA_SIZE) + 5];
                    //eof flag
                    packet[4] = 1;

                    //last packet, time to calculate retransmission times and speed
                    time = System.currentTimeMillis() - time;
                    totalResend = resendCounter;
                    speed = (int) ((file.length() / 1024.0) / (time / 1000.0));

                    for (i = 0; i < file.length() - DATA_SIZE * sequence; i++) {
                        packet[i + 5] = fileBytes[DATA_SIZE * sequence + i];
                    }
                }
                // two header value
                packet[0] = 0;
                packet[1] = 0;
                //sequence number
                packet[2] = (byte) (sequence >> 8);
                packet[3] = (byte) sequence;

                // first send the packet
                socket.send(new DatagramPacket(packet, packet.length, RemoteHost, Port));

                //packet sent, wait for ack
                boolean sendSuccess = false;
                boolean resend;
                byte[] ackData = new byte[ACK_PACkET_SIZE];
                DatagramPacket received = new DatagramPacket(ackData, ACK_PACkET_SIZE);
                while (!sendSuccess) {
                    resend = false;
                    try {
                        socket.receive(received);
                        ackData = received.getData();
                        int ack = (ackData[2] & 0xff) << 8 | (ackData[3] & 0xff);
//                        System.out.println("***Ack number got*** " + ack);
                        if (ack != sequence) {
                            // received the wrong ack, do nothing, continue to wait for the right ack
                            continue;
                        } else {
                            sendSuccess = true;
                        }
                    } catch (SocketTimeoutException e) {
                        resend = true;
//                        System.out.println("ACK time out for sequence number " + sequence);

                        //resend for the last packet will timeout after 10 times
                        if (sequence == number - 1) {
                            eofCounter++;
                            if (eofCounter == 10)
                                sendSuccess = true;
                        }
                    }

                    if (resend) {
                        resendCounter++;
                        socket.send(new DatagramPacket(packet, packet.length, RemoteHost, Port));
                    }
                }
//                System.out.println("Packet No." + sequence + " sent! size = " + packet.length);

            }
            socket.close();
//            System.out.println("Total resent: " + resendCounter);
        } catch (Exception e) {
//            e.printStackTrace();
        }
        // output total retransmission numbers and speed (KB/s)
        System.out.println(totalResend + " " + speed);
    }
}