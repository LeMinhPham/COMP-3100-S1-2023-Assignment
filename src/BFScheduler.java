/**
 * This class implements the <b> Best Fit </b> scheduling algorithm.
 */
public class BFScheduler extends Scheduler {
    public BFScheduler() throws Exception {
        super();
    }

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

            // Information of the best readily available server
            String bestReadilyAvailableServerType = "";
            String bestReadilyAvailableServerID = "";
            int bestReadilyAvailableServerCore = 0;

            // Information of the best sufficient server regardless of availability
            String bestSufficientServerType = "";
            String bestSufficientServerID = "";

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

                // First server in the least has the least number of cores
                // therefore it is the best-fit sufficient server
                if (i == 0) {
                    bestSufficientServerType = latestMessage[0];
                    bestSufficientServerID = latestMessage[1];
                }

                if (availableCore >= requiredCore
                        && availableMemory >= requiredMemory
                        && availableDisk >= requiredDisk) {
                    // Best readily available server
                    if (availableCore < bestReadilyAvailableServerCore || bestReadilyAvailableServerCore == 0) {
                        bestReadilyAvailableServerType = latestMessage[0];
                        bestReadilyAvailableServerID = latestMessage[1];
                        bestReadilyAvailableServerCore = availableCore;
                    }
                }
            }

            // Finish receiving server records
            send("OK");
            receive();

            // Schedule job
            if (!bestReadilyAvailableServerType.equals("")) {
                send("SCHD " + jobID + " " + bestReadilyAvailableServerType + " " + bestReadilyAvailableServerID);
            } else {
                send("SCHD " + jobID + " " + bestSufficientServerType + " " + bestSufficientServerID);
            }
            receive();
        } while (!latestMessage[0].equals("NONE"));

        // End communication
        send("QUIT");
        receive();
        close();
    }
}
