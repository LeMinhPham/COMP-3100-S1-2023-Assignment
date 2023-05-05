import java.net.InetAddress;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * This class is an abstract class for the scheduling algorithm classes used in
 * the simulation
 */
public abstract class Scheduler {
    // Fields to communicate with ds-server
    protected Socket socket;
    protected DataOutputStream dout; // for sending messages
    protected BufferedReader bin; // for receiving messages

    // The latest message received from ds-server
    // This is an array containing keywords split from the message
    // E.g: If latest message received is "JOBN 2142 12 750 4 250 800"
    // then latestMessage = {"JOBN", "2142", "12", "750", "4", "250", "800"}
    protected String[] latestMessage;

    public Scheduler() throws Exception {
        socket = new Socket(InetAddress.getByName("127.0.0.1"), 50000);
        dout = new DataOutputStream(socket.getOutputStream());
        bin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Schedules jobs sent from ds-server
     */
    public void schedule() throws Exception {
        close();
    }

    /**
     * Closes all communication channels
     */
    protected void close() throws Exception {
        socket.close();
        dout.close();
        bin.close();
    }

    /**
     * Receives a message from ds-server, splits it with space character
     * as the delimiter, and stores the result in latestMessage
     */
    protected void receive() throws Exception {
        String str = bin.readLine();
        latestMessage = str.split(" ");
        // System.out.println("RCVD: " + str);
    }

    /**
     * Sends a message to ds-server
     * 
     * @param message the message to be sent
     */
    protected void send(String message) throws Exception {
        dout.write((message + "\n").getBytes());
        dout.flush();
        // System.out.println("SENT: " + message);
    }
}
