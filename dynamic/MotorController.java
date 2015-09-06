import java.io.*;
import java.net.*;

class MotorController {
  final int factor = 320; // 1 mm <=> 320 micro steps
  static int xSpeed = 16000; // 50 mm/s
  static int zSpeed = 16000; // 50 mm/s
  Socket socketMC;
  InputStreamReader inMC;
  OutputStreamWriter outMC;

  MotorController() {
    try {
      socketMC = new Socket("192.168.254.254", 2001);
      inMC = new InputStreamReader(socketMC.getInputStream());
      outMC = new OutputStreamWriter(socketMC.getOutputStream());
    } catch (IOException e) {
      System.err.println("Error! Couldn't establish connection to the motor controller.");
      System.exit(1);
    }

    Send("@03"); // initialize both axes 
    if (!Receive().equals("0")) {
      System.err.println("Error! Couldn't initialize both axises.");
      System.exit(1);
    }

    Send("@0N3"); // Set current position as reference point
    if (!Receive().equals("0")) {
      System.err.println("Error! Couldn't set current position as reference point.");
      System.exit(1);
    }

    System.out.println("the motor controller is ready");
  }

  void Send(String cmd) {
    try {
      outMC.write(cmd + "\r");
      outMC.flush();
    } catch (IOException e) {
      System.err.println("Error! Couldn't send commands to the motor controller.");
      System.exit(1);
    }
  }

  String Receive() {
    int c;
    StringBuilder sb = new StringBuilder();

    try {
      while (!inMC.ready()) ; // waiting for being ready
      while (inMC.ready()) {
        c = inMC.read();
        sb.append((char) c);
      }
    } catch (IOException e) {
      System.err.println("Error! Couldn't read feedback from the motor controller.");
      System.exit(1);
    }

    return sb.toString();
  }

  void Move(double x, double z) {
    int posX = (int) (x * factor);
    int posZ = (int) (z * factor);
    Send("@0M " + posX + ", " + xSpeed + ", " + posZ + ", " + zSpeed);
    if (!Receive().equals("0")) {
      System.err.println("Error! Couldn't move the cavity.");
      System.exit(1);
    }
  }

  void CleanUp() {
    Send("@0M 0, " + xSpeed + ", 0, " + zSpeed); // back to origin
    if (!Receive().equals("0")) {
      System.err.println("Error! Couldn't finalize the cavity position.");
      System.exit(1);
    }
    try {
      inMC.close();
      outMC.close();
      socketMC.close();
    } catch (IOException e) {
      System.err.println("Error! Couldn't disconnect with the motor controller safely.");
      System.exit(1);
    }
  }
}
