public class Client {
    // Scheduler algorithm
    Scheduler scheduler;

    /**
     * Creates a client with a specific scheduler algorithm
     */
    public Client(Scheduler _scheduler) throws Exception {
        scheduler = _scheduler;
    }

    /**
     * Starts the simulation
     */
    public void start() throws Exception {
        scheduler.schedule();
    }

    /**
     * Change scheduler algorithm
     * 
     * @param newScheduler new algorithm to be used
     */
    public void setScheduler(Scheduler newScheduler) {
        scheduler = newScheduler;
    }

    public static void main(String[] args) throws Exception {
        Client dsClient;
        Scheduler scheduler;

        if (args[0].equalsIgnoreCase("lrr")) {
            scheduler = new LRRScheduler();
        } else if (args[0].equalsIgnoreCase("fc")) {
            scheduler = new FCScheduler();
        } else if (args[0].equalsIgnoreCase("ff")) {
            scheduler = new FFScheduler();
        } else if (args[0].equalsIgnoreCase("bf")) {
            scheduler = new BFScheduler();
        } else if (args[0].equalsIgnoreCase("wf")) {
            scheduler = new WFScheduler();
        } else if (args[0].equalsIgnoreCase("lwt")) {
            scheduler = new LWTScheduler();
        } else if (args[0].equalsIgnoreCase("iff")) {
            scheduler = new IFFScheduler();
        } else if (args[0].equalsIgnoreCase("ibf")) {
            scheduler = new IBFScheduler();
        } else if (args[0].equalsIgnoreCase("iwf")) {
            scheduler = new IWFScheduler();
        } else {
            scheduler = new ATLScheduler();
        }
        dsClient = new Client(scheduler);
        dsClient.start();
    }
}
