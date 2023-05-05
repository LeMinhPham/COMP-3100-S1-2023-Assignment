/**
 * This class implements the <b> Largest Round Robin </b> scheduling algorithm.
 */
public class LRRScheduler extends Scheduler {
    // Information of the largest server type
    protected String largestServerType;
    protected int largestServerTypeCount;

    // ID of the server to schedule the next job to
    protected int currentServerID;

    public LRRScheduler() throws Exception {
        super();

        // No largest server type yet
        largestServerType = "";
        largestServerTypeCount = 0;

        // First server ID to schedule the next job to is 0
        currentServerID = 0;
    }

    /**
     * Schedules jobs based on the Largest Round Robin algorithm
     */
    @Override
    public void schedule() throws Exception {
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
            if (largestServerType.equals("")) {
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
        close();
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
}
