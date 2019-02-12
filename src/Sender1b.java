/* Chenghao Ye s1786987 */

import java.io.File;
import java.io.FileInputStream;
import java.net.*;

import static java.lang.Thread.sleep;

public class Sender1b {

    // define size here
    private static final int DATA_SIZE = 1024;
    private static final int PACkET_SIZE = DATA_SIZE + 5;   // head size = 5
    private static final int SLEEP_TIME = 10;

    public static void main(String[] args) {
        System.out.println("RemoteHost: " + args[0] + " Port: " + args[1] + " Filename: " + args[2]);
        try {
            transport(InetAddress.getByName(args[0]), Integer.valueOf(args[1]), args[2]);
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + args[0]);
        }
    }

    private static void transport(InetAddress RemoteHost, int Port, String Filename) {
        try {
            DatagramSocket socket = new DatagramSocket();
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
                packet[2] = (byte) (i >> 8);
                packet[3] = (byte) i;
                socket.send(new DatagramPacket(packet, packet.length, RemoteHost, Port));
                System.out.println("Packet No." + i + " sent! size = " + packet.length);
                // sleep time
                sleep(SLEEP_TIME);
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}