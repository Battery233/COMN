/* Chenghao Ye s1786987 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver2a {

    // define size here
    private static final int DATA_SIZE = 1024;
    private static final int PACkET_SIZE = DATA_SIZE + 5;   // head size = 5
    private static final int ACK_PACkET_SIZE = 4;   // ack size

    public static void main(String[] args) {
//        System.out.println("Port: " + args[0] + " Filename: " + args[1]);
        receiver(Integer.parseInt(args[0]), args[1]);
    }

    private static void receiver(int Port, String fileName) {
        try {
            // get tools ready
            DatagramSocket socket = new DatagramSocket(Port);
            FileOutputStream writeFile = new FileOutputStream(new File(fileName));
            int currentSequence = 0;

            boolean eof = false;    // flag for the last packet
            boolean missing = false;
            while (!eof || missing) {
                byte[] data = new byte[PACkET_SIZE];
                DatagramPacket received = new DatagramPacket(data, PACkET_SIZE);
                socket.receive(received);
                data = received.getData();
                int length = received.getLength();  // real length of the packet size
                int sequence = (data[2] & 0xff) << 8 | (data[3] & 0xff);    // get sequence num from bytes
                eof = 1 == (data[4] & 0xff);    // get eof flag

                if (currentSequence != sequence) {
                    sendACK(socket, received.getAddress(), received.getPort(), currentSequence - 1);
//                    if (currentSequence > sequence) {
//                        System.out.println("Duplicate packet: want: " + currentSequence + ", got " + sequence);
//                    } else {
//                        System.out.println("Missing packet: want: " + currentSequence + ", got " + sequence);
//                    }
                    missing = true;
                    eof = false;
//                     send ACK and wait for next packet
                } else {
                    sendACK(socket, received.getAddress(), received.getPort(), currentSequence);
                    currentSequence++;
                    missing = false;
                    if (!eof) {
                        writeFile.write(data, PACkET_SIZE - DATA_SIZE, DATA_SIZE);
//                        System.out.println("Packet No." + sequence + " got! Length = " + length);
                    } else {
//                        System.out.println("Last sequence number is: " + sequence + " length = " + length);
                        writeFile.write(data, PACkET_SIZE - DATA_SIZE, length - (PACkET_SIZE - DATA_SIZE));
                        socket.close();
                        writeFile.close();
                    }
                }
            }
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    private static void sendACK(DatagramSocket socket, InetAddress RemoteHost, int Port, int ack) throws IOException {
        byte[] packet = new byte[ACK_PACkET_SIZE];
        packet[0] = 0;
        packet[1] = 0;
        //sequence number
        packet[2] = (byte) (ack >> 8);
        packet[3] = (byte) ack;
        socket.send(new DatagramPacket(packet, packet.length, RemoteHost, Port));
    }
}
