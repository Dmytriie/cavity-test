/* This program is intended to measure s-parameters of a cavity when it stays still. Possible application could be investigation of temperature interference on the resonant frequency drift.
 * 
 * @author X. Chen
 *
 * All Copyleft Reserved!
 */

import java.io.*;
import java.util.*;
import java.text.*;

public class StaticTest {
    static int nRep = 1;
    static String timestamp = "1970-01-01 00:00:00";

    public static void main(String[] args) {
        try {
            Parser.Parse(args);
        } catch (ArgumentException e) {
            printHelp();
            System.exit(1);
        }
        RecordSlowControl(23, true);

        VectorNetworkAnalyzer rohde = new VectorNetworkAnalyzer(); // s-parameter measurement
        Multimeter agilent = new Multimeter(); // temperature monitoring
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar rightNow = Calendar.getInstance();
        String begin = dateFormat.format(rightNow.getTime());
        rightNow.add(Calendar.SECOND, 10*nRep);
        String end = dateFormat.format(rightNow.getTime());
        System.out.println("measurement begins at " + begin);
        System.out.println("It will approximately end around " + end);

        for (int i = 0; i < nRep; i++) {
            System.out.println(String.format("%05d", nRep-i-1));
            rightNow = Calendar.getInstance();
            timestamp = dateFormat.format(rightNow.getTime());
            RecordSlowControl(agilent.FetchData(), false);
            RecordTrace(i, rohde.FetchData());
        }
        rohde.CleanUp();
        agilent.CleanUp();
    }

    static void printHelp() {
        System.err.println("+-----------+\n|   USAGE   |\n+-----------+");
        System.err.println("The program measures s-parameters of a cavity and logs ambient temperature" +
                "\n\npossible options are:" +
                "\n-c\tcenter frequency in MHz" +
                "\n-s\tspan in kHz" +
                "\n-r\trepetition of measurements" +
                "\n-n\tnumber of trace points" +
                "\n-t\ttype of measurement, select from {S11, S21, S12, S22}" +
                "\n\ndefault setting is equivalent to:" +
                "\njava -jar StaticTest.jar -c 682 -s 150 -r 1 -n 801 -t S21");
    }

    static void RecordSlowControl(double temperature, boolean initial) {
        FileWriter f = null;
        try {
            if (initial)
                f = new FileWriter("slow_control.dat"); // write to a new file
            else
                f = new FileWriter("slow_control.dat", true); // append to the existing file
        } catch (IOException e) {
            System.err.println("Error! Couldn't create file `slow_control.dat'.");
            System.exit(1);
        } 

        if (initial) {
            try {
                f.write("# number of repetitions                    " + nRep + '\n' +
                        "# number of trace points                   " + VectorNetworkAnalyzer.point + '\n' +
                        "# center frequency [MHz]                   " + VectorNetworkAnalyzer.center + '\n' +
                        "# span [kHz]                               " + VectorNetworkAnalyzer.span + '\n' +
                        "# power [dBm]                              " + VectorNetworkAnalyzer.power + '\n' +
                        "# number of acquisitions for average       " + VectorNetworkAnalyzer.average + '\n' +
                        "# bandwidth of intermediate filter [kHz]   " + VectorNetworkAnalyzer.bandwidth + '\n' +
                        "# type of measurement                      " + VectorNetworkAnalyzer.measurement + '\n');
            } catch (IOException e) {
                System.err.println("Error! Couldn't write to file `slow_control.dat'.");
                System.exit(1);
            }
        } else {
            try {
                f.write(timestamp + "\t\t" + temperature + '\n');
            } catch (IOException e) {
                System.err.println("Error! Couldn't write to file `slow_control.dat'.");
                System.exit(1);
            }
        }

        try {
            f.close();
        } catch (IOException e) {
            System.err.println("Error! Couldn't close file `slow_control.dat' properly.");
            System.exit(1);
        }
    }

    static void RecordTrace(int fileNum, String[] data) {
        FileWriter f = null;
        String fname = "";
        try {
            fname = String.format("%05d.dat", fileNum);
            f = new FileWriter(fname);
        } catch (IOException e) {
            System.err.println("Error! Couldn't create file `" + fname + "'.");
            System.exit(1);
        }

        try {
            for (int i = 0; i < data.length/2; i++)
                f.write(data[2*i] + "\t\t" + data[2*i+1] + '\n'); // real, imaginary
        } catch (IOException e) {
            System.err.println("Error! Couldn't write to file `" + fname + "'.");
            System.exit(1);
        }

        try {
            f.close();
        } catch (IOException e) {
            System.err.println("Error! Couldn't close file `" + fname + "' properly.");
            System.exit(1);
        }
    }
}
