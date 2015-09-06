/* This program is intended to measure s-parameters of a cavity while it is being displaced on a grid. Possible application could be profiling the electric field inside the cavity.
 * 
 * @author X. Chen
 *
 * All Copyleft Reserved!
 */

import java.io.*;
import java.util.*;
import java.text.*;

public class DynamicTest {
    static double xInitl = -45.; // mm, initial coordinate of X in pipe system
    static double xFinal = 45.; // mm, final coordinate of X in pipe system
    static int xNum = 19; // number of measurements in X
    static double zInitl = -55.; // mm, initial coordinate of Z in pipe system
    static double zFinal = 55.; // mm, final coordinate of Z in pipe system
    static int zNum = 23; // number of measurements in Z
    static double zRef = 100.; // mm, two-sided reference coordinates of Z
    static String timestamp = "1970-01-01 00:00:00";

    public static void main(String[] args) {
        try {
            Parser.Parse(args);
        } catch (ArgumentException e) {
            printHelp();
            System.exit(1);
        }
        RecordSlowControl(23, true);

        MotorController isel = new MotorController(); // displace the cavity
        VectorNetworkAnalyzer rohde = new VectorNetworkAnalyzer(); // s-parameter measurement
        Multimeter agilent = new Multimeter(); // temperature monitoring
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar rightNow = Calendar.getInstance();
        String begin = dateFormat.format(rightNow.getTime());
        rightNow.add(Calendar.SECOND, 41*xNum*zNum);
        String end = dateFormat.format(rightNow.getTime());
        System.out.println("measurement begins at " + begin);
        System.out.println("It will approximately end around " + end);

        int i, j;
        double xPos, xMove;
        double xIncre = (xFinal - xInitl) / (xNum==1 ? 1 : xNum-1);
        double zPos, zMove, zMoveRef;
        double zIncre = (zFinal - zInitl) / (zNum==1 ? 1 : zNum-1);
        for (i = 0, xPos = xInitl; i < xNum; i++, xPos += xIncre) {
            xMove = -xPos;
            for (j = 0, zPos = zInitl; j < zNum; j++, zPos += zIncre) {
                if (zRef >= 0) {
                    zMoveRef = zPos>0 ? zRef : -zRef;
                    isel.Move(xMove, zMoveRef);
                    System.out.println(String.format("%03d, %03d, ref", i, j));
                    rightNow = Calendar.getInstance();
                    timestamp = dateFormat.format(rightNow.getTime());
                    RecordSlowControl(agilent.FetchData(), false);
                    RecordTrace(0, i, j, rohde.FetchData()); // 0 = reference
                }
                zMove = zPos;
                isel.Move(xMove, zMove);
                System.out.println(String.format("%03d, %03d, pert", i, j));
                rightNow = Calendar.getInstance();
                timestamp = dateFormat.format(rightNow.getTime());
                RecordSlowControl(agilent.FetchData(), false);
                RecordTrace(1, i, j, rohde.FetchData()); // 1 = perturbation
            }
        }
        isel.CleanUp();
        rohde.CleanUp();
        agilent.CleanUp();
    }

    static void printHelp() {
        System.err.println("+-----------+\n|   USAGE   |\n+-----------+");
        System.err.println("The program displaces a cavity to the assigned positions, then measures s-parameters of it and logs ambient temperature" +
                "\n\npossible options are:" +
                "\n-xi\tinitial X in mm (pipe reference system)" +
                "\n-xf\tfinal X in mm (origin of coordinates is in the center of aperture region)" +
                "\n-xn\tnumber of measurements in X" +
                "\n-zi\tinitial Z in mm" +
                "\n-zf\tfinal Z in mm" +
                "\n-zn\tnumber of measurements in Z" +
                "\n-r\tdistance between reference position and X-axis in mm (negative value means no reference measurement)" +
                "\n-c\tcenter frequency in MHz" +
                "\n-s\tspan in kHz" +
                "\n-n\tnumber of trace points" +
                "\n-t\ttype of measurement, select from {S11, S21, S12, S22}" +
                "\n\ndefault setting is equivalent to:" +
                "\njava -jar DynamicTest.jar -xi -45 -xf 45 -xn 19 -zi -55 -zf 55 -zn 23 -r 100 -c 682 -s 150 -n 801 -t S21");
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
                f.write("# initial coordinate of X [mm]             " + xInitl + '\n' +
                        "# final coordinate of X [mm]               " + xFinal + '\n' +
                        "# number of measurements in X              " + xNum + '\n' +
                        "# initial coordinate of Z [mm]             " + zInitl + '\n' +
                        "# final coordinate of Z [mm]               " + zFinal + '\n' +
                        "# number of measurements in Z              " + zNum + '\n' +
                        "# number of trace points                   " + VectorNetworkAnalyzer.point + '\n' +
                        "# center frequency [MHz]                   " + VectorNetworkAnalyzer.center + '\n' +
                        "# span [kHz]                               " + VectorNetworkAnalyzer.span + '\n' +
                        "# reference coordinate of Z [mm]           " + zRef + '\n' +
                        "# (2-sided, negative means no reference)   " + '\n' +
                        "# movement speed in X [mm/s]               " + MotorController.xSpeed/320. + '\n' +
                        "# movement speed in Z [mm/s]               " + MotorController.zSpeed/320. + '\n' +
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

    static void RecordTrace(int indicator, int xIndex, int zIndex, String[] data) {
        FileWriter f = null;
        String fname = "";
        try {
            fname = String.format("%d_%03d_%03d.dat", indicator, xIndex, zIndex);
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
