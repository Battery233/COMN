/* Chenghao Ye s1786987 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Sender2b {

    // define size here
    private static final int DATA_SIZE = 1024;
    private static final int PACkET_SIZE = DATA_SIZE + 5;   // head size = 5
    private static final int ACK_PACkET_SIZE = 4;   // ack size
    private static int base = 0;
    private static long number;
    private static long fileLength;
    private static DatagramSocket socket;
    private static byte[] fileBytes;
    private static HashMap<Integer, Boolean> map;
    private static int timeout;
    private static InetAddress RemoteHost;
    private static int Port;
    private static int windowSize;

    public static void main(String[] args) {
        try {
            transport(InetAddress.getByName(args[0]), Integer.valueOf(args[1]), args[2], Integer.valueOf(args[3]), Integer.valueOf(args[4]));
        } catch (UnknownHostException ignored) {
        }
    }

    private static void transport(InetAddress arg1, int arg2, String Filename, int arg3, int arg4) {
        Sender2b.RemoteHost = arg1;
        Sender2b.Port = arg2;
        Sender2b.timeout = arg3;
        Sender2b.windowSize = arg4;

        try {
            File file = new File(Filename);

            // calculate the number of packets needed
            number = file.length() / DATA_SIZE;
            if (file.length() % DATA_SIZE != 0) {
                number++;
            }
//            System.out.println("File size: " + file.length() + " packets number: " + number);

            // read file
            fileLength = (int) file.length();
            fileBytes = new byte[(int) fileLength];
            FileInputStream fis = new FileInputStream(file);
            fis.read(fileBytes);
            fis.close();

            long timeStart = System.currentTimeMillis();
            int sequence;
            map = new HashMap<Integer, Boolean>();
            socket = new DatagramSocket();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        while (base != number) {
                            byte[] ackData = new byte[ACK_PACkET_SIZE];
                            DatagramPacket received = new DatagramPacket(ackData, ACK_PACkET_SIZE);
                            try {
                                socket.receive(received);
                                ackData = received.getData();
                            } catch (IOException e) {
                                // potential socket closed exception when all the ack is received
//                                stop the process when socket is closed
                                break;
                            }
                            int ack = (ackData[2] & 0xff) << 8 | (ackData[3] & 0xff);
                            map.remove(ack);
                            map.put(ack, true);
//                            System.out.println("Get ack " + ack);
                        }
                    }
                }
            }).start();

            while (base < (int) number) {
                synchronized (Sender2b.class) {
//                    System.out.println(base + " ---- " + number);
                    sequence = base;
                    while (sequence < base + windowSize && sequence < number) {
                        if (!map.containsKey(sequence)) {
                            map.put(sequence, false);
                            sendData(sequence);
                            if (sequence == number - 1)
                                System.out.println(String.format("%.2f", (file.length() / 1024.0) / ((System.currentTimeMillis() - timeStart) / 1000.0)));
//                           System.out.println("Send data " + sequence);
                        }
                        sequence++;
                    }
                    try {
                        while (map.get(base)) {
                            if (base < sequence)
                                base++;
                            else
                                break;
//                            System.out.println("Base moved to " + base);
                        }
                    } catch (NullPointerException ignored) {
                        // when base has not been sent, null exception will be thrown
                        //can safely ignore this
                    }
                }
            }
            socket.close();
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    private static void sendData(int sequence) throws IOException {
        byte[] packet;
        int i;
        final int sendDataSequence = sequence;
        if (sendDataSequence < number - 1) {
            packet = new byte[PACkET_SIZE];
            // eof flag
            packet[4] = 0;
            for (i = 0; i < DATA_SIZE; i++) {
                packet[i + 5] = fileBytes[DATA_SIZE * sendDataSequence + i];
            }
        } else {
            packet = new byte[(int) (fileLength % DATA_SIZE) + 5];
            //eof flag
            packet[4] = 1;
            for (i = 0; i < fileLength - DATA_SIZE * sendDataSequence; i++) {
                packet[i + 5] = fileBytes[DATA_SIZE * sendDataSequence + i];
            }
        }
        packet[0] = 0;
        packet[1] = 0;
        //sequence number
        packet[2] = (byte) (sendDataSequence >> 8);
        packet[3] = (byte) sendDataSequence;
//        System.out.println("inside packet sequence " + packet[2] + " " + packet[3]);
        socket.send(new DatagramPacket(packet, packet.length, RemoteHost, Port));

        ScheduledExecutorService scheduler
                = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new Runnable() {
            public void run() {
                if (!map.get(sendDataSequence)) {
                    try {
                        sendData(sendDataSequence);
//                        System.out.println("Packet " + sendDataSequence + " Time out! resend!");
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }
                }
            }
        };

        scheduler.schedule(task, timeout, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
    }
}