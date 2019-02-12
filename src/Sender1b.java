/* Chenghao Ye s1786987 */

import java.io.File;
import java.io.FileInputStream;
import java.net.*;

public class Sender1b {

    // define size here
    private static final int DATA_SIZE = 1024;
    private static final int PACkET_SIZE = DATA_SIZE + 5;   // head size = 5
    private static final int ACK_PACkET_SIZE = 4;   // ack size
//    private static final int SLEEP_TIME = 10;

    public static void main(String[] args) {
        System.out.println("RemoteHost: " + args[0] + " Port: " + args[1] + " Filename: " + args[2] + " Timeout: " + args[3]);
        try {
            transport(InetAddress.getByName(args[0]), Integer.valueOf(args[1]), args[2], Integer.valueOf(args[3]));
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + args[0]);
        }
    }

    private static void transport(InetAddress RemoteHost, int Port, String Filename, int timeout) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
            File file = new File(Filename);

            // calculate the number of packets needed
            long number = file.length() / DATA_SIZE;
            if (file.length() % DATA_SIZE != 0) {
                number++;
            }
            System.out.println("File size: " + file.length() + " packets number: " + number);

            // read file
            byte[] fileBytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(fileBytes);
            fis.close();

            int sequence = 0;
            int resendCounter = 0;
            int eofCounter = 0;

            int i, j;
            for (i = 0; i < number; i++) {
                byte[] packet;
                if (i < number - 1) {
                    packet = new byte[PACkET_SIZE];
                    // eof flag
                    packet[4] = 0;
                    for (j = 0; j < DATA_SIZE; j++) {
                        packet[j + 5] = fileBytes[DATA_SIZE * i + j];
                    }
                } else {
                    packet = new byte[(int) (file.length() % DATA_SIZE) + 5];
                    //eof flag
                    packet[4] = 1;
                    for (j = 0; j < file.length() - DATA_SIZE * i; j++) {
                        packet[j + 5] = fileBytes[DATA_SIZE * i + j];
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
                        System.out.println("***Ack number got*** " + ack);
                        if (ack != sequence) {
                            resend = true;
                            System.out.println("Packet No." + sequence + " resent! resendCounter = " + resendCounter);
                        } else {
                            sendSuccess = true;
                        }
                    } catch (SocketTimeoutException e) {
                        resend = true;
                        System.out.println("ACK time out for sequence number " + sequence);

                        //resend for last command will timeout after send 5 times
                        if (i == number - 1) {
                            eofCounter++;
                        }

                        if (eofCounter == 10)
                            sendSuccess = true;
                    }
                    if (resend) {
                        resendCounter++;
                        socket.send(new DatagramPacket(packet, packet.length, RemoteHost, Port));
                    }
                }
                System.out.println("Packet No." + sequence + " sent! size = " + packet.length);
                sequence++;
                // todo : clean old comments
                // sleep time
//                sleep(SLEEP_TIME);
            }
            socket.close();
            System.out.println("Total resent: " + resendCounter);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}