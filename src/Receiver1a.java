/* Chenghao Ye s1786987 */

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Receiver1a {

    // define size here
    private static final int DATA_SIZE = 1024;
    private static final int PACkET_SIZE = DATA_SIZE + 5;   // head size = 5

    public static void main(String[] args) {
//        System.out.println("Port: " + args[0] + " Filename: " + args[1]);
        receiver(Integer.parseInt(args[0]), args[1]);
    }

    private static void receiver(int Port, String fileName) {
        try {
            // get tools ready
            DatagramSocket socket = new DatagramSocket(Port);
            FileOutputStream writeFile = new FileOutputStream(new File(fileName));

            boolean eof = false;    // flag for the last packet
            while (!eof) {
                byte[] data = new byte[PACkET_SIZE];
                DatagramPacket received = new DatagramPacket(data, PACkET_SIZE);
                socket.receive(received);
                data = received.getData();
                int length = received.getLength();  // real length of the packet size
                int sequence = (data[2] & 0xff) << 8 | (data[3] & 0xff);    // get sequence num from bytes
                eof = 1 == (data[4] & 0xff);    // get flag
                if (!eof) {
                    writeFile.write(data, PACkET_SIZE - DATA_SIZE, DATA_SIZE);
//                    System.out.println("Packet No." + sequence + " got! Length = " + length);
                } else {
//                    System.out.println("Last sequence number is: " + sequence + " length = " + length);
                    writeFile.write(data, PACkET_SIZE - DATA_SIZE, length - (PACkET_SIZE - DATA_SIZE));
                    socket.close();
                    writeFile.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}