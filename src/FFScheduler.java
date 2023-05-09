/**
 * This class implements the <b> First Fit </b> scheduling algorithm.
 */
public class FFScheduler extends Scheduler {
    public FFScheduler() throws Exception {
        super();
    }

    /**
     * Schedules jobs based on the <b> First Fit </b> algorithm
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

            // Information of the first readily available server
            String firstReadilyAvailableServerType = "";
            String firstReadilyAvailableServerID = "";

            // Information of the first sufficient server regardless of availability
            String firstSufficientServerType = "";
            String firstSufficientServerID = "";

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

                if (firstSufficientServerType.equals("")) {
                    // First sufficient server
                    firstSufficientServerType = latestMessage[0];
                    firstSufficientServerID = latestMessage[1];
                }

                if (availableCore >= requiredCore
                        && availableMemory >= requiredMemory
                        && availableDisk >= requiredDisk) {
                    // First readily available server
                    if (firstReadilyAvailableServerType.equals("")) {
                        firstReadilyAvailableServerType = latestMessage[0];
                        firstReadilyAvailableServerID = latestMessage[1];
                    }
                }
            }

            // Finish receiving server records
            send("OK");
            receive();

            // Schedule job
            if (!firstReadilyAvailableServerType.equals("")) {
                send("SCHD " + jobID + " " + firstReadilyAvailableServerType + " " + firstReadilyAvailableServerID);
            } else {
                send("SCHD " + jobID + " " + firstSufficientServerType + " " + firstSufficientServerID);
            }
            receive();
        } while (!latestMessage[0].equals("NONE"));

        // End communication
        send("QUIT");
        receive();
        close();
    }
}
