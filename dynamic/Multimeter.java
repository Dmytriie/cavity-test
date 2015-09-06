import java.io.*;
import java.net.*;

class Multimeter {
  Socket socketMM;
  InputStreamReader inMM;
  OutputStreamWriter outMM;

  Multimeter() {
    try {
      socketMM = new Socket("192.168.254.4", 5025);
      inMM = new InputStreamReader(socketMM.getInputStream());
      outMM = new OutputStreamWriter(socketMM.getOutputStream());
    } catch (IOException e) {
      System.err.println("Error! Couldn't establish connection to the multimeter.");
      System.exit(1);
    }

    Send("*RST;*WAI;*CLS"); // reset everything
    Send("CONF:TEMP THER"); // set sensor to thermistor
    Send("TEMP:NPLC 10"); // set average cycle

    Send("*WAI;SYST:ERR?");
    if (!Receive().equals("+0,\"No error\"")) {
      System.err.println("Error! Couldn't preset the multimeter.");
      System.exit(1);
    }
    
    System.out.println("the multimeter is ready");
  }

  void Send(String cmd) {
    try {
      outMM.write(cmd + "\n");
      outMM.flush();
    } catch (IOException e) {
      System.err.println("Error! Couldn't send commands to the multimeter.");
      System.exit(1);
    }
  }

  String Receive() {
    int c;
    StringBuilder sb = new StringBuilder();

    try {
      while ((c = inMM.read()) != '\n') {
        sb.append((char) c);
      }
    } catch (IOException e) {
      System.err.println("Error! Couldn't read feedback from the multimeter.");
      System.exit(1);
    }

    return sb.toString();
  }

  double FetchData() {
    Send("*WAI;READ?");
    return Double.parseDouble(Receive());
  }

  void CleanUp() {
    try {
      inMM.close();
      outMM.close();
      socketMM.close();
    } catch (IOException e) {
      System.err.println("Error! Couldn't disconnect with the multimeter safely.");
      System.exit(1);
    }
  }
}
