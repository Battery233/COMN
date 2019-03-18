/* Chenghao Ye s1786987 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class Receiver2b {

    // define size here
    private static final int DATA_SIZE = 1024;
    private static final int PACkET_SIZE = DATA_SIZE + 5;   // head size = 5
    private static final int ACK_PACkET_SIZE = 4;   // ack size

    public static void main(String[] args) {
        receiver(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]));
    }

    private static void receiver(int Port, String fileName, int windowSize) {
        try {
            // get tools ready
            DatagramSocket socket = new DatagramSocket(Port);
            FileOutputStream writeFile = new FileOutputStream(new File(fileName));
            int lastSequence = 2147483647;
            int base = 0;
            HashMap<Integer, byte[]> map = new HashMap<Integer, byte[]>();
            boolean eof;    // flag for the last packet

            while (base < lastSequence) {
                byte[] data = new byte[PACkET_SIZE];
                DatagramPacket received = new DatagramPacket(data, PACkET_SIZE);
                socket.receive(received);
                data = received.getData();
                int length = received.getLength();  // real length of the packet size
                int sequence = (data[2] & 0xff) << 8 | (data[3] & 0xff);    // get sequence num from bytes
                eof = 1 == (data[4] & 0xff);    // get eof flag
                System.out.println("Get packet: " + sequence + " length = " + length);

                if (eof) {
                    lastSequence = sequence;
                }

                if (sequence >= base && sequence < base + windowSize) {
                    sendACK(socket, received.getAddress(), received.getPort(), sequence);
                    System.out.println("Sent ack " + sequence);
                    if (!map.containsKey(sequence))
                        map.put(sequence, data);
                    if (sequence == base) {
                        while (map.containsKey(base)) {
                            if (base != lastSequence) {
                                writeFile.write(map.get(base), PACkET_SIZE - DATA_SIZE, DATA_SIZE);
                                map.remove(base);
                                System.out.println("Packet No." + base + " written!");
                            } else {
                                System.out.println("Last sequence number is: " + sequence + " length = " + length);
                                writeFile.write(map.get(base), PACkET_SIZE - DATA_SIZE, length - (PACkET_SIZE - DATA_SIZE));
                                socket.close();
                                writeFile.close();
                                break;
                            }
                            base++;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
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
