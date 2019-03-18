/* Chenghao Ye s1786987 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
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
    private static int acked = 0;
    private static DatagramChannel channel;
    private static byte[] fileBytes;
    private static SocketAddress sa;
    private static HashMap<Integer, Boolean> map;
    private static int timeout;
    private static int windowSize;

    public static void main(String[] args) {
        try {
            transport(InetAddress.getByName(args[0]), Integer.valueOf(args[1]), args[2], Integer.valueOf(args[3]), Integer.valueOf(args[4]));
        } catch (UnknownHostException ignored) {
        }
    }

    private static void transport(InetAddress RemoteHost, int Port, String Filename, int arg3, int arg4) {
        boolean speedPrinted = false;
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
            channel = DatagramChannel.open();
            sa = new InetSocketAddress(RemoteHost, Port);
            channel.socket().bind(sa);
            int sequence;
            map = new HashMap<Integer, Boolean>();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        while (acked != number) {
                            ByteBuffer data = ByteBuffer.allocate(ACK_PACkET_SIZE * windowSize);
                            try {
                                channel.read(data);
                                System.out.println(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            byte[] ackData = data.array();
                            for (int i = 0; i < (ackData.length) / ACK_PACkET_SIZE; i++) {
                                int ack = (ackData[2] & 0xff) << 8 | (ackData[3] & 0xff);
                                map.remove(ack);
                                map.put(ack, true);
                                acked++;
                            }
                            //todo last ack lost
                        }
                    }
                }
            }).start();

            while (base < (int) number) {
                sequence = base;
                while (sequence < base + windowSize) {
                    synchronized (Sender2b.class) {
                        if (!map.containsKey(sequence)) {
                            map.put(sequence, false);
                            sendData(sequence);
                            System.out.println("Send data " + sequence);
                        }
                        sequence++;
                    }
                }
                while (map.get(base)) {
                    base++;
                    System.out.println("Base moved to " + base);
                }
            }

            channel.close();
            if (!speedPrinted)
                System.out.println(String.format("%.2f", (file.length() / 1024.0) / ((System.currentTimeMillis() - timeStart) / 1000.0)));
        } catch (Exception ignored) {
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
            packet[0] = 0;
            packet[1] = 0;
            //sequence number
            packet[2] = (byte) (sendDataSequence >> 8);
            packet[3] = (byte) sendDataSequence;
        }
        channel.send(ByteBuffer.wrap(packet), sa);

        ScheduledExecutorService scheduler
                = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new Runnable() {
            public void run() {
                if (!map.get(sendDataSequence)) {
                    try {
                        sendData(sendDataSequence);
//                        System.out.println("Packet " + sendDataSequence + " Time out! resend!");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        scheduler.schedule(task, timeout, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
    }
}