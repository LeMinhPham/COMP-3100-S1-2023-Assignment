/**
 * This class implements the <b> All To Largest </b> scheduling algorithm.
 */
public class ATLScheduler extends Scheduler {
    // Information of the largest server type
    protected String largestServerType;

    public ATLScheduler() throws Exception {
        super();

        // No largest server type yet
        largestServerType = "";
    }

    /**
     * Schedules jobs based on the <b> All To Largest </b> algorithm
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

            // Schedule job
            send("SCHD " + jobID + " " + largestServerType + " " + 0);
            receive();
        } while (!latestMessage[0].equals("NONE"));

        // End communication
        send("QUIT");
        receive();
        close();
    }

    /**
     * Finds the largest server type
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
                curLargestCoreCount = Integer.parseInt(latestMessage[4]);
            }
        }

        // Finish receiving server records
        send("OK");
        receive();
    }
}
