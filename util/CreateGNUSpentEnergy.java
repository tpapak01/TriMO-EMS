package jmetal.util;

import java.io.*;
import java.util.Locale;
import java.util.Scanner;

public class CreateGNUSpentEnergy {

    public static void main (String[] args) throws IOException {
        double value;
        String useless;
        File input = new File("SPENT");
        File input2 = new File("GNU_SPENT_TEMPLATE");
        Scanner scan = new Scanner(input).useLocale(Locale.US);;
        Scanner template_scan = new Scanner(input2);

        PrintWriter pw = new PrintWriter(new FileWriter("GNU_SPENT"));

        while (scan.hasNext()) {
            scan.useDelimiter(",");
            while (scan.hasNextDouble()) {
                value = scan.nextDouble();
                String to_put = template_scan.nextLine() + " " + String.valueOf(value) + "\n";
                System.out.println(value);
                pw.write(to_put);
            }
            scan.useDelimiter("");
            useless = scan.next();
        }

        pw.close();
    }

}
