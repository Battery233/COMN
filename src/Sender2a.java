/* Chenghao Ye s1786987 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Sender2a {

    // define size here
    private static final int DATA_SIZE = 1024;
    private static final int PACkET_SIZE = DATA_SIZE + 5;   // head size = 5
    private static final int ACK_PACkET_SIZE = 4;   // ack size
    private static int base = 0;
    private static int sequence = 0;
    private static int speed;

    public static void main(String[] args) {
        try {
            transport(InetAddress.getByName(args[0]), Integer.valueOf(args[1]), args[2], Integer.valueOf(args[3]), Integer.valueOf(args[4]));
        } catch (UnknownHostException ignored) {
        }
    }

    private static void transport(InetAddress RemoteHost, int Port, String Filename, int timeout, int windowSize) {
        int i;
        boolean speedPrinted = false;

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

            long timeStart = System.currentTimeMillis();
            int expectedAck;
            int eofCounter = 0;

            while (base < (int) number) {
                sequence = base;
                while (sequence < base + windowSize && sequence < number) {
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
                        for (i = 0; i < file.length() - DATA_SIZE * sequence; i++) {
                            packet[i + 5] = fileBytes[DATA_SIZE * sequence + i];
                        }
                        eofCounter++;
                        if (!speedPrinted) {
                            speedPrinted = true;
                            // output total retransmission numbers and speed (KB/s)
                            speed = (int) ((file.length() / 1024.0) / ((System.currentTimeMillis() - timeStart) / 1000.0));
                            System.out.println(speed);
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
                    sequence++;
                }
                System.out.println("Packet sent! From base:" + base + " to sequence = " + (sequence - 1));

                if (eofCounter == 10)
                    base = (int) number;

                int wrongAckCounter = 0;
                int windowEnd = base + windowSize < (int) number ? base + windowSize : (int) number;
                expectedAck = base;

                while (expectedAck < windowEnd) {
                    byte[] ackData = new byte[ACK_PACkET_SIZE];
                    DatagramPacket received = new DatagramPacket(ackData, ACK_PACkET_SIZE);
                    try {
                        socket.receive(received);
                        ackData = received.getData();
                        int ack = (ackData[2] & 0xff) << 8 | (ackData[3] & 0xff);
                        System.out.println("ACK got! " + ack + " expected ack = " + expectedAck);
                        if (ack != expectedAck) {
                            base = ack + 1;
                        } else {
                            base++;
                            expectedAck++;
                        }
                    } catch (IOException e) {
                        System.out.println("ACK time out at packet: " + base);
                        break;
                    }
                }
            }

            socket.close();

        } catch (Exception ignored) {
        }
    }
}