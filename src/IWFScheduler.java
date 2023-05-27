import java.util.ArrayList;
import java.util.TreeMap;

/**
 * This class implements the <b> Improved Worst Fit </b> scheduling algorithm,
 * which employs <b> Worst Fit </b> scheduling algorithm but is aware that
 * backfilling is not allowed in ds-sim.
 */

public class IWFScheduler extends Scheduler {
    // Information of the current job
    String jobID = "";
    int requiredCore = 0;
    int requiredMemory = 0;
    int requiredDisk = 0;

    // Information of the worst readily available server
    protected String worstReadilyAvailableServerType = "";
    protected String worstReadilyAvailableServerID = "";
    protected int worstReadilyAvailableServerCore = 0;

    // Information of the worst sufficient server regardless of availability
    protected String worstSufficientServerType = "";
    protected String worstSufficientServerID = "";
    protected int worstSufficientServerCore = 0;

    // Because booting servers do not show the scorrect number of available
    // resources after GETS Capable call, we need to use LSTJ for them.
    // Therefore, we need to store the available servers and check them later
    protected ArrayList<String[]> availableServers;

    // Initial resources of the servers
    protected TreeMap<String, Integer[]> serverInitialResources;

    public IWFScheduler() throws Exception {
        super();
        availableServers = new ArrayList<>();
        serverInitialResources = new TreeMap<>();
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

            // Scan job information
            jobID = latestMessage[2];
            requiredCore = Integer.parseInt(latestMessage[4]);
            requiredMemory = Integer.parseInt(latestMessage[5]);
            requiredDisk = Integer.parseInt(latestMessage[6]);

            // Get capable server records
            send("GETS Capable " + requiredCore + " " + requiredMemory + " " + requiredDisk);
            receive();
            send("OK");

            // Traverse all server records
            int serverCount = Integer.parseInt(latestMessage[1]);
            for (int i = 0; i < serverCount; i++) {
                receive();

                // Current resources of the server
                int availableCore = Integer.parseInt(latestMessage[4]);
                int availableMemory = Integer.parseInt(latestMessage[5]);
                int availableDisk = Integer.parseInt(latestMessage[6]);

                // GETS Capable is not guaranteed to return the intial cores of the servers at
                // every iteration, so we have to store them before assigning these servers any
                // job.
                if (!serverInitialResources.containsKey(latestMessage[0])) {
                    serverInitialResources.put(latestMessage[0],
                            new Integer[] { availableCore, availableMemory, availableDisk });
                }

                // Find the worst sufficient server based on its initial cores
                int serverInitialCores = serverInitialResources.get(latestMessage[0])[0];
                if (serverInitialCores > worstSufficientServerCore) {
                    worstSufficientServerType = latestMessage[0];
                    worstSufficientServerID = latestMessage[1];
                    worstSufficientServerCore = serverInitialCores;
                }

                // Store available servers for later check
                if (availableCore >= requiredCore
                        && availableMemory >= requiredMemory
                        && availableDisk >= requiredDisk) {
                    availableServers.add(latestMessage);
                }
            }

            // Finish receiving server records
            send("OK");
            receive();

            // Find worst-fit readily available server
            findWorstFitServer();

            // Schedule job
            if (!worstReadilyAvailableServerType.equals("")) {
                // If there is a readily available server, schedule to it
                send("SCHD " + jobID + " " + worstReadilyAvailableServerType + " " + worstReadilyAvailableServerID);
            } else {
                // If there is no readily available, schedule to the worst sufficient server
                send("SCHD " + jobID + " " + worstSufficientServerType + " " + worstSufficientServerID);
            }
            receive();
            reset();
        } while (!latestMessage[0].equals("NONE"));

        // End communication
        send("QUIT");
        receive();
        close();
    }

    /**
     * Finds the worst fit server in readily available servers,
     * i.e. <code> availableServers </code>
     */
    protected void findWorstFitServer() throws Exception {
        for (String[] server : availableServers) {
            String serverType = server[0];
            String serverID = server[1];
            String serverState = server[2];

            int availableCore;
            int availableMemory;
            int availableDisk;

            if (serverState.equals("booting")) {
                // If the server is booting, recalculate its resources by subtracting the
                // required resources of waiting jobs from the server's initial resources
                Integer[] initialResources = serverInitialResources.get(serverType);
                availableCore = initialResources[0];
                availableMemory = initialResources[1];
                availableDisk = initialResources[2];

                // Traverse job records
                send("LSTJ " + serverType + " " + serverID);
                receive();
                send("OK");

                int jobCount = Integer.parseInt(latestMessage[1]);
                for (int i = 0; i < jobCount; i++) {
                    receive();
                    int _requiredCore = Integer.parseInt(latestMessage[5]);
                    int _requiredMemory = Integer.parseInt(latestMessage[6]);
                    int _requiredDisk = Integer.parseInt(latestMessage[7]);

                    availableCore -= _requiredCore;
                    availableMemory -= _requiredMemory;
                    availableDisk -= _requiredDisk;
                }

                // Finish receiving job records
                send("OK");
                receive();
            } else {
                // Else use the stored values
                availableCore = Integer.parseInt(server[4]);
                availableMemory = Integer.parseInt(server[5]);
                availableDisk = Integer.parseInt(server[6]);
            }

            // Check for worst-fit
            if (availableCore >= requiredCore && availableMemory >= requiredMemory && availableDisk >= requiredDisk) {
                // Check if the server has bot running and waiting jobs at the same time, which
                // means this server cannot immediately execute the current job
                int waitingJobs = Integer.parseInt(server[7]);
                int runningJobs = Integer.parseInt(server[8]);
                if (!(waitingJobs > 0 && runningJobs > 0)) {
                    if (availableCore > worstReadilyAvailableServerCore) {
                        worstReadilyAvailableServerType = serverType;
                        worstReadilyAvailableServerID = serverID;
                        worstReadilyAvailableServerCore = availableCore;
                    }
                }
            }
        }
    }

    /**
     * Reset the scheduling information for the next job
     */
    protected void reset() {
        jobID = "";
        requiredCore = 0;
        requiredMemory = 0;
        requiredDisk = 0;
        worstReadilyAvailableServerType = "";
        worstReadilyAvailableServerID = "";
        worstReadilyAvailableServerCore = 0;
        worstSufficientServerType = "";
        worstSufficientServerID = "";
        worstSufficientServerCore = 0;
        availableServers.clear();
    }
}
