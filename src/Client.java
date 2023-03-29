import java.net.Socket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class Client {

    // Fields to communicate with ds-server
    protected Socket socket;
    protected DataOutputStream dout; // for sending messages
    protected BufferedReader bin; // for receiving messages

    // The latest message received from ds-server
    // This is an array containing keywords split from the message
    // E.g: If latest message received is "JOBN 2142 12 750 4 250 800"
    // then latestMessage = {"JOBN", "2142", "12", "750", "4", "250", "800"}
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
        // Establish communication channels
        socket = new Socket(InetAddress.getByName("127.0.0.1"), 50000);
        dout = new DataOutputStream(socket.getOutputStream());
        bin = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // No largest server type yet
        largestServerType = "";
        largestServerTypeCount = 0;

        // First server ID to schedule the next job to is 0
        currentServerID = 0;
    }

    /**
     * Starts the simulation
     */
    public void start() throws Exception {
        // Start communication
        send("HELO");
        receive();
        send("AUTH " + System.getProperty("user.name"));
        receive();

        do {
            send("REDY");
            receive();

            // Ignore if receive a message other than JOBN
            if (!latestMessage[0].equals("JOBN")) {
                continue;
            }

            String jobID = latestMessage[2];

            // Find largest server type if not found already
            if (largestServerTypeCount == 0) {
                findLargestServerType();
            }

            // Schedule job and move to the next server ID for next job
            send("SCHD " + jobID + " " + largestServerType + " " + currentServerID);
            currentServerID = (currentServerID + 1) % largestServerTypeCount;
            receive();
        } while (!latestMessage[0].equals("NONE"));

        // End communication
        send("QUIT");
        receive();
    }

    /**
     * Closes all communication channels
     */
    public void close() throws Exception {
        socket.close();
        dout.close();
        bin.close();
    }

    /**
     * Sends a message to ds-server
     * 
     * @param message the message to be sent
     */
    protected void send(String message) throws Exception {
        dout.write((message + "\n").getBytes());
        dout.flush();
        System.out.println("SENT: " + message);
    }

    /**
     * Receives a message from ds-server, splits it with space character
     * as the delimiter, and stores the result in latestMessage
     */
    protected void receive() throws Exception {
        String str = bin.readLine();
        latestMessage = str.split(" ");
        System.out.println("RCVD: " + str);
    }

    /**
     * Finds the largest server type and the number of servers of that type
     */
    protected void findLargestServerType() throws Exception {
        // Get server records
        send("GETS All");
        receive();
        send("OK");

        int serverCount = Integer.parseInt(latestMessage[1]);
        int curLargestCoreCount = 0;
        // Iterate through all records to find the server having most CPU cores
        for (int i = 0; i < serverCount; i++) {
            receive();

            if (curLargestCoreCount < Integer.parseInt(latestMessage[4])) {
                largestServerType = latestMessage[0];
                largestServerTypeCount = 1;
                curLargestCoreCount = Integer.parseInt(latestMessage[4]);
            } else if (largestServerType.equals(latestMessage[0])) {
                largestServerTypeCount++;
            }
        }

        // Finish receiving server records
        send("OK");
        receive();
    }

    public static void main(String[] args) throws Exception {
        Client dsClient = new Client();
        dsClient.start();
        dsClient.close();
    }
}
