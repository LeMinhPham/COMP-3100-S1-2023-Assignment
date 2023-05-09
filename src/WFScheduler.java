import java.util.TreeMap;

/**
 * This class implements the <b> Worst Fit </b> scheduling algorithm.
 */
public class WFScheduler extends Scheduler {
    protected TreeMap<String, Integer> serverInitialCores;

    public WFScheduler() throws Exception {
        super();
        serverInitialCores = new TreeMap<>();
    }

    /**
     * Schedules jobs based on the <b> Worst Fit </b> algorithm
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

            // Information of the current job
            String jobID = latestMessage[2];
            int requiredCore = Integer.parseInt(latestMessage[4]);
            int requiredMemory = Integer.parseInt(latestMessage[5]);
            int requiredDisk = Integer.parseInt(latestMessage[6]);

            // Information of the worst readily available server
            String worstReadilyAvailableServerType = "";
            String worstReadilyAvailableServerID = "";
            int worstReadilyAvailableServerCore = 0;

            // Information of the worst sufficient server regardless of availability
            String worstSufficientServerType = "";
            String worstSufficientServerID = "";
            int worstSufficientServerCore = 0;

            // Get capable server records
            send("GETS Capable " + requiredCore + " " + requiredMemory + " " + requiredDisk);
            receive();
            send("OK");

            // Traverse all server records to find the first readily available server
            int serverCount = Integer.parseInt(latestMessage[1]);
            for (int i = 0; i < serverCount; i++) {
                receive();

                // Current resources of the server
                int availableCore = Integer.parseInt(latestMessage[4]);
                int availableMemory = Integer.parseInt(latestMessage[5]);
                int availableDisk = Integer.parseInt(latestMessage[6]);

                // GETS Capable is not guaranteed to return the intial cores of the servers at
                // every iteration, so we have to store them before assigning these servers any
                // job
                if (!serverInitialCores.containsKey(latestMessage[0])) {
                    serverInitialCores.put(latestMessage[0], availableCore);
                }

                if (serverInitialCores.get(latestMessage[0]) > worstSufficientServerCore) {
                    worstSufficientServerType = latestMessage[0];
                    worstSufficientServerID = latestMessage[1];
                    worstSufficientServerCore = serverInitialCores.get(latestMessage[0]);
                }

                if (availableCore >= requiredCore
                        && availableMemory >= requiredMemory
                        && availableDisk >= requiredDisk) {
                    // Best readily available server
                    if (availableCore > worstReadilyAvailableServerCore) {
                        worstReadilyAvailableServerType = latestMessage[0];
                        worstReadilyAvailableServerID = latestMessage[1];
                        worstReadilyAvailableServerCore = availableCore;
                    }
                }
            }

            // Finish receiving server records
            send("OK");
            receive();

            // Schedule job
            if (!worstReadilyAvailableServerType.equals("")) {
                send("SCHD " + jobID + " " + worstReadilyAvailableServerType + " " + worstReadilyAvailableServerID);
            } else {
                send("SCHD " + jobID + " " + worstSufficientServerType + " " + worstSufficientServerID);
            }
            receive();
        } while (!latestMessage[0].equals("NONE"));

        // End communication
        send("QUIT");
        receive();
        close();
    }
}
