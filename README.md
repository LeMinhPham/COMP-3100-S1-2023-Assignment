# COMP 3100 S1 2023 Project

This is the repository for stage 1 of the project in COMP3100 Session 1 2023. Client.java is a plain/vanilla version of client-side simulator that acts as a simple job dispatcher. It follows [ds-sim](https://github.com/distsys-MQ/ds-sim) simulation protocol and uses Largest-Round-Robin scheduling algorithm, which sends each job to a server of the largest type in a round-robin fashion.

---

# Run a simulation

You can run the simulation by going to the src/test folder and run the following commands in command line:

1. Run server `$ ./ds-server -c [CONFIG FILE] -n [OPTION]...`
2. Run client `$ java Client 127.0.0.1 50000`
