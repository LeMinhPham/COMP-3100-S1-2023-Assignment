import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * <p>
 * This class implements the <b> Least Waiting Time </b> scheduling algorithm.
 * This algorithm schedules the current job to the server which has the
 * least estimated waiting time. Waiting time is calculated based on the
 * availability of resources and order of jobs in the server's local queue. In
 * ds-sim, jobs are executed in strict order, i.e. no backfilling is allowed,
 * even if the latter jobs can be immediately executed with the current
 * resources, it still has to wait for the former jobs to be executed.
 * </p>
 * 
 * <p>
 * In case there are 2 or more server with the same estimated waiting time, the
 * server that has the least number of available cores at the time the current
 * job is estimated to be executed will be chosen.
 * </p>
 * 
 * <p>
 * If there are idle servers, the first one will be chosen as it has the least
 * number of available cores and waiting time.
 * </p>
 * 
 * <p>
 * If the least waiting time is longer than the current job's estimated run time
 * and there is at least an inactive server that has sufficient resources for
 * the job, schedule it to this server.
 * </p>
 */
public class LWTScheduler extends Scheduler {
    // Information of the current job
    String jobID = "";
    int submitTime = 0;
    int estimatedRunTime = 0;
    int requiredCore = 0;
    int requiredMemory = 0;
    int requiredDisk = 0;

    // Information of the server to schedule the current job to, i.e. best server
    protected String bestServerType = "";
    protected String bestServerID = "";

    // Because this algorithm requires examining the servers' local queue, we need
    // to use the LSTJ command. Since LSTJ cannot be used when GETS is running, we
    // need to store the server information for later check.
    protected List<String[]> capableServers;

    // Initial resources of different server types
    protected TreeMap<String, Integer[]> serverInitialResources;

    public LWTScheduler() throws Exception {
        super();
        capableServers = new ArrayList<>();
        serverInitialResources = new TreeMap<>();
    }

    /**
     * Schedules jobs based on the <b> Least Waiting Time </b> algorithm
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
            submitTime = Integer.parseInt(latestMessage[1]);
            jobID = latestMessage[2];
            estimatedRunTime = Integer.parseInt(latestMessage[3]);
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

                // Scan server information
                String serverType = latestMessage[0];
                int availableCore = Integer.parseInt(latestMessage[4]);
                int availableMemory = Integer.parseInt(latestMessage[5]);
                int availableDisk = Integer.parseInt(latestMessage[6]);

                // Store the initial resources of this server type
                if (!serverInitialResources.containsKey(serverType)) {
                    serverInitialResources.put(serverType,
                            new Integer[] { availableCore, availableMemory, availableDisk });
                }

                // Store capable servers for later check
                capableServers.add(latestMessage);
            }

            // Finish receiving server records
            send("OK");
            receive();

            // Find best server
            findBestServer();

            // Schedule job
            send("SCHD " + jobID + " " + bestServerType + " " + bestServerID);

            receive();
            reset();
        } while (!latestMessage[0].equals("NONE"));

        // End communication
        send("QUIT");
        receive();
        close();
    }

    /**
     * Find the best server in <b>capableServers</b>
     */
    protected void findBestServer() throws Exception {
        String firstInactiveServerType = "";
        String firstInactiveServerID = "";

        int minWaitingTime = Integer.MAX_VALUE;
        int bestServerCore = Integer.MAX_VALUE;

        for (String[] server : capableServers) {
            String serverType = server[0];
            String serverID = server[1];
            String state = server[2];

            List<String[]> runningJobs = new ArrayList<>();
            List<String[]> waitingJobs = new ArrayList<>();

            if (state.equals("idle")) {
                bestServerType = serverType;
                bestServerID = serverID;
                minWaitingTime = 0;
                break;
            } else if (state.equals("inactive") && firstInactiveServerType.equals("")) {
                firstInactiveServerType = serverType;
                firstInactiveServerID = serverID;
                minWaitingTime = Integer.MAX_VALUE;
                break;
            } else if (state.equals("active") || state.equals("booting")) {

                // Traverse job records
                send("LSTJ " + serverType + " " + serverID);
                receive();
                send("OK");

                int jobCount = Integer.parseInt(latestMessage[1]);
                for (int i = 0; i < jobCount; i++) {
                    receive();

                    if (latestMessage[1].equals("2")) {
                        runningJobs.add(latestMessage);
                    } else {
                        waitingJobs.add(latestMessage);
                    }
                }

                // Finish receiving job records
                send("OK");
                receive();

                List<Integer> estimates = estimateWaitingTime(runningJobs, waitingJobs, serverType, state);
                int waitingTime = estimates.get(0);
                int availableCore = estimates.get(1);

                if (waitingTime < minWaitingTime
                        || (waitingTime == minWaitingTime && availableCore < bestServerCore)) {
                    bestServerType = serverType;
                    bestServerID = serverID;
                    bestServerCore = availableCore;
                }

            }
        }

        if (minWaitingTime > 2 * estimatedRunTime && !firstInactiveServerType.equals("")) {
            bestServerType = firstInactiveServerType;
            bestServerID = firstInactiveServerID;
        }
    }

    /**
     * Estimate the waiting time if assign the current job to a server
     * The estimation works similarly to how ds-sim simulates events
     * 
     * @param runningJobs List of running jobs currently in the server
     * @param waitingJobs List of waiting jobs currently in the server
     * @param serverType  Type of the server
     * @param state       State of the server
     * @return A list contains 2 element: estimated waiting time and available cores
     *         at the time of allocating the current job to this server
     */
    protected List<Integer> estimateWaitingTime(
            List<String[]> runningJobs,
            List<String[]> waitingJobs,
            String serverType,
            String state) {
        Integer[] initialResources = serverInitialResources.get(serverType);
        int availableCore = initialResources[0];
        int availableMemory = initialResources[1];
        int availableDisk = initialResources[2];

        int waitingTime = 0;

        // If the server is booting, increase waiting time by bootup time
        // Otherwise, recalculate estimated remaining runtime of running jobs
        if (state.equals("booting")) {
            int bootupTime = Integer.parseInt(waitingJobs.get(0)[3]) - submitTime;
            waitingTime += bootupTime;
        } else {
            int jobCount = runningJobs.size();
            for (int i = 0; i < jobCount; i++) {
                String[] job = runningJobs.remove(0);

                // Estimate remaining runtime
                int startTime = Integer.parseInt(job[3]);
                int runTime = Integer.parseInt(job[4]);
                int remainRunTime = runTime - (submitTime - startTime);

                if (remainRunTime < 0) {
                    remainRunTime = 0;
                }

                job[4] = String.valueOf(remainRunTime);
                runningJobs.add(job);

                // Take resources from the server
                int _requiredCore = Integer.parseInt(job[5]);
                int _requiredMemory = Integer.parseInt(job[6]);
                int _requiredDisk = Integer.parseInt(job[7]);

                availableCore -= _requiredCore;
                availableDisk -= _requiredDisk;
                availableMemory -= _requiredMemory;
            }
        }

        // Simulating events similar to ds-sim
        while (true) {
            // Check if the first job in the waiting list can be executed with the current
            // resources. If it can, execute it, and check for the next job in the list.
            // Otherwise, increase the waiting time until the next running job is finished
            if (!waitingJobs.isEmpty()) {
                String[] job = waitingJobs.get(0);
                int _requiredCore = Integer.parseInt(job[5]);
                int _requiredMemory = Integer.parseInt(job[6]);
                int _requiredDisk = Integer.parseInt(job[7]);

                if (availableCore >= _requiredCore
                        && availableMemory >= _requiredMemory
                        && availableDisk >= _requiredDisk) {
                    waitingJobs.remove(0);
                    runningJobs.add(job);
                    availableCore -= _requiredCore;
                    availableMemory -= _requiredMemory;
                    availableDisk -= _requiredDisk;
                    continue;
                }
            } else {
                // If there is no waiting jobs, try execute the current job. If it can be
                // executed, the estimation is done
                if (availableCore >= requiredCore
                        && availableDisk >= requiredDisk
                        && availableMemory >= requiredMemory) {
                    availableCore -= requiredCore;
                    break;
                }
            }

            // Get the next job expected to be completed
            int minRunTime = Integer.MAX_VALUE;
            for (String[] job : runningJobs) {
                int runTime = Integer.parseInt(job[4]);

                if (runTime < minRunTime) {
                    minRunTime = runTime;
                }
            }

            waitingTime += minRunTime;
            int count = runningJobs.size();
            for (int i = 0; i < count; i++) {
                String[] job = runningJobs.remove(0);
                int runTime = Integer.parseInt(job[4]);
                int remainRunTime = runTime - minRunTime;

                if (remainRunTime == 0) {
                    int freedCore = Integer.parseInt(job[5]);
                    int freedMemory = Integer.parseInt(job[6]);
                    int freedDisk = Integer.parseInt(job[7]);
                    availableCore += freedCore;
                    availableMemory += freedMemory;
                    availableDisk += freedDisk;
                } else {
                    job[4] = String.valueOf(remainRunTime);
                    runningJobs.add(job);
                }
            }

        }

        return List.of(waitingTime, availableCore);
    }

    /**
     * Reset the scheduling information for the next job
     */
    protected void reset() {
        jobID = "";
        submitTime = 0;
        estimatedRunTime = 0;
        requiredCore = 0;
        requiredMemory = 0;
        requiredDisk = 0;

        bestServerType = "";
        bestServerID = "";
        capableServers.clear();
    }
}
