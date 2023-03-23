import java.net.Socket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class Client {
    public static void main(String[] args) throws Exception {
        InetAddress host = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        Socket s = new Socket(host, port);

        // DataInputStream din = new DataInputStream(s.getInputStream());
        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

        System.out.println("Tager IP: " + s.getInetAddress() + " Target Port: " + s.getPort());
        System.out.println("Local IP: " + s.getLocalAddress() + " Local Port: " + s.getLocalPort());

        // try {
        // TimeUnit.SECONDS.sleep(10);
        // } catch (InterruptedException e) {
        // System.out.println(e);
        // }

        // dout.writeUTF("HELO");
        // System.out.println("SENT: HELO");

        // String str = din.readUTF();
        // System.out.println("RECEIVED: " + str);

        // dout.writeUTF("BYE");
        // System.out.println("SENT: BYE");

        // str = din.readUTF();
        // System.out.println("RECEIVED: " + str);

        // din.close();
        // dout.close();

        write(dout, "HELO");

        String str = in.readLine();
        System.out.println("RCVD: " + str);

        String username = System.getProperty("user.name");

        write(dout, "AUTH " + username);

        str = in.readLine();
        System.out.println("RCVD: " + str);

        write(dout, "REDY");

        while (true) {
            str = in.readLine();
            System.out.println("RCVD: " + str);
            String[] parameters = str.split(" ");

            if (parameters[0].equals("NONE")) {
                break;
            } else if (!parameters[0].equals("JOBN")) {
                write(dout, "REDY");
                continue;
            }

            String jobID = parameters[2];

            write(dout, "GETS All");

            str = in.readLine();
            System.out.println("RCVD: " + str);

            write(dout, "OK");

            int servers = Integer.parseInt(str.split(" ")[1]);
            String largestServerType = "NONE";
            int largestServerID = -1;
            int largestCore = -1;

            for (int i = 0; i < servers; i++) {
                str = in.readLine();
                System.out.println("RCVD: " + str);
                parameters = str.split(" ");
                if (largestServerType == "NONE" || largestCore < Integer.parseInt(parameters[4])) {
                    largestServerType = parameters[0];
                    largestServerID = Integer.parseInt(parameters[1]);
                    largestCore = Integer.parseInt(parameters[4]);
                }
            }

            write(dout, "OK");
            str = in.readLine();
            System.out.println("RCVD: " + str);

            write(dout, "SCHD " + jobID + " " + largestServerType + " " + largestServerID);
            // str = in.readLine();
            // System.out.println("RCVD: " + str);
            // write(dout, "REDY");
        }

        write(dout, "QUIT");

        str = in.readLine();
        System.out.println("RCVD: " + str);

        dout.close();
        in.close();
        s.close();
    }

    public static void write(DataOutputStream dout, String mes) throws Exception {
        dout.write((mes + "\n").getBytes());
        dout.flush();
        System.out.println("SENT: " + mes);
    }
}
