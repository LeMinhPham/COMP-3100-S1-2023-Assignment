import java.net.Socket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class Client {

    // Fields to communicate with ds-server
    protected DataOutputStream dout;
    protected BufferedReader bin;
    protected Socket socket;

    // The latest message received from ds-server
    protected String[] latestMessage;

    // Information of the largest server type
    protected String largestServerType;
    protected int largestServerTypeCount;

    // ID of the server to schedule the next job to
    protected int currentServerID;

    /**
     * Creates a client and connects it to ds-server
     */
    public Client() throws Exception {
        socket = new Socket(InetAddress.getByName("127.0.0.1"), 50000);
        dout = new DataOutputStream(socket.getOutputStream());
        bin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Starts the simulation
     */
    public void start() {

    }

    /**
     * Sends a message to ds-server
     * 
     * @param message the message to be sent
     */
    protected void send(String message) {

    }

    /**
     * Receives a message from ds-server, splits it with space character
     * as the delimiter, and stores the result in lastestMessage
     */
    protected void receive() {

    }

    /**
     * Finds the largest server type and the number of servers of that type
     */
    protected void findLargestServerType() {

    }

    public static void main(String[] args) throws Exception {
        Client dsClient = new Client();
        dsClient.start();
    }
}
