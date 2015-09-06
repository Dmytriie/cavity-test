import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

class VectorNetworkAnalyzer {
  static double center = 682.; // MHz
  static double span = 150.; // kHz
  static int point = 801;
  static double bandwidth = 1.; // kHz
  static double power = 0.; // dBm
  static int average = 10;
  static String measurement = "S21";
  Socket socketVNA;
  InputStreamReader inVNA;
  OutputStreamWriter outVNA;

  VectorNetworkAnalyzer() {
    try {
      socketVNA = new Socket("192.168.254.2", 5025);
      inVNA = new InputStreamReader(socketVNA.getInputStream());
      outVNA = new OutputStreamWriter(socketVNA.getOutputStream());
    } catch (IOException e) {
      System.err.println("Error! Couldn't establish connection to the vector network analyzer.");
      System.exit(1);
    }

    Send("@REM"); // invoke remote mode
    Send("*RST;*WAI;*CLS"); // reset everything
    Send("CALC:PAR:MEAS 'TRC1','" + measurement + "'");
    Send("INIT:CONT OFF"); // single sweep
    Send("SWE:COUN " + average);
    Send("SWE:POIN " + point);
    Send("AVER:COUN " + average);
    Send("AVER ON");
    Send("BAND " + bandwidth + "KHZ");
    Send("FREQ:CENT " + center + "MHZ");
    Send("FREQ:SPAN " + span + "KHZ");
    Send("SOUR:POW " + power);
    Send("MMEM:LOAD:CORR 1,'MOST_20150726_930_540000.cal'"); // calibration file, to be replaced in every test

    Send("*WAI;SYST:ERR:ALL?");
    if (!Receive().equals("0,\"No error\"")) {
      System.err.println("Error! Couldn't preset the vector network analyzer.");
      System.exit(1);
    }
    System.out.println("the vector network analyzer is ready");
  }

  void Send(String cmd) {
    try {
      outVNA.write(cmd + "\n");
      outVNA.flush();
    } catch (IOException e) {
      System.err.println("Error! Couldn't send commands to the vector network analyzer.");
      System.exit(1);
    }
  }

  String Receive() {
    int c;
    StringBuilder sb = new StringBuilder();

    try {
      while ((c = inVNA.read()) != '\n') {
        sb.append((char) c);
      }
    } catch (IOException e) {
      System.err.println("Error! Couldn't read feedback from the vector network analyzer.");
      System.exit(1);
    }

    return sb.toString();
  }

  String[] FetchData() {
    try {
      Thread.sleep(1000); // pause 1 second
    } catch (InterruptedException e) {
      System.err.println("Error! Couldn't settle the cavity before fetching data.");
      System.exit(1);
    }

    Send("AVER:CLE"); // clean previous frames
    Send("INIT"); // initiate a new cycle
    Send("*WAI;CALC:DATA? SDAT");
    String data = Receive();

    return data.split(",");
  }

  void CleanUp() {
    Send("@LOC"); // set to local mode
    try {
      inVNA.close();
      outVNA.close();
      socketVNA.close();
    } catch (IOException e) {
      System.err.println("Error! Couldn't disconnect with the vector network analyzer safely.");
      System.exit(1);
    }
  }
}
