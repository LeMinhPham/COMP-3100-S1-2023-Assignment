# COMP 3100 S1 2023 Project

This is the repository for the individual project in COMP3100 Session 1 2023. The program follows [ds-sim](https://github.com/distsys-MQ/ds-sim) simulation protocol and and employs a variety of scheduling algorithm such as Largest-Round-Robin, All-To-Largest, First-Capable, etc.

---

# Stage 1

This stage implements a plain/vanilla version of client-side simulator that acts as a simple job dispatcher. The scheduling algorithm used in this stage is Largest-Round-Robin, which sends each job to a server of the largest type in a round-robin fashion.

# Stage 2

This stage implements four new scheduling algorithms: Least-Waiting-Time, Improved-First-Fit, Improved-Best-Fit, and Improved-Worst-Fit. Tests using the demo test suit provided in iLearn suggests that "core scheduling" (scheduling based on the number of cores) is more effective in optimizing the turnaround time when there is no job backfilling or preemption.

# Run a simulation

You can run the simulation by going to the test folder and run the following commands in command line:

1. Run server `$ ./ds-server -c [CONFIG FILE] -n [OPTION]...`
2. Run client `$ java Client [SCHEDULING ALGORITHM]`

If there is no input scheduling algorithm, the program will use the ATL algorithm by default.

# Compile source files

To compile the source files, go to the src folder and run the following command in command line:

`$ javac *.java` or `$ javac Client.java`
