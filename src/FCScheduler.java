/**
 * This class implements the <b> First Capable </b> scheduling algorithm.
 */
public class FCScheduler extends Scheduler {
    public FCScheduler() throws Exception {
        super();
    }

    /**
     * Schedules jobs based on the <b> First Capable </b> algorithm
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

            // Find first capable server

            String jobID = latestMessage[2];
            String serverType = "";
            String serverID = "";

            send("GETS Capable " + latestMessage[4] + " " + latestMessage[5] + " " + latestMessage[6]);
            receive();

            int capableServers = Integer.parseInt(latestMessage[1]);

            send("OK");

            for (int i = 0; i < capableServers; i++) {
                receive();
                if (i == 0) {
                    serverType = latestMessage[0];
                    serverID = latestMessage[1];
                }
            }
            send("OK");
            receive();

            // Schedule job
            send("SCHD " + jobID + " " + serverType + " " + serverID);
            receive();
        } while (!latestMessage[0].equals("NONE"));

        // End communication
        send("QUIT");
        receive();
    }
}
