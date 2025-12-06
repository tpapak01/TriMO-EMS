package jmetal.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jmetal.util.gson.*;

import java.io.*;

public class CreateOptaplannerInput_v3 {

    private static String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    private static String userPreferencePath = "/Users/emine/IdeaProjects/JMETALHOME/Userpreference_data/"; // The path of the files
    private static String costsPath = "/Users/emine/IdeaProjects/JMETALHOME/Costs_data/";

    private static String outputPath = "/Users/emine/Downloads/optaplanner-distribution-9.42.0.Final/optaplanner-distribution-9.42.0.Final/examples/sources/data/reproblem/unsolved/";

    public static void main (String[] args) throws IOException {

        String problemName = args[0];
        String problemUserPreferences = args[1];
        String problemCosts = args[2];
        String problemRenewable = args[3];
        String problemCostsBuy = args[4];

        String fileName = problemPath + problemName + ".txt";
        String userPreferenceFileName = userPreferencePath + problemUserPreferences + ".txt";
        String costsFileName = costsPath + problemCosts + ".txt";
        String renewableFileName = problemPath + problemRenewable + ".txt";
        String costsBuyFileName = costsPath + problemCostsBuy + ".txt";
        String outputFileName = outputPath + "REFIT_5_SUM_v2" + ".json";

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        Gson gson = builder.create();

        //How to fill object from json-file, will not be used now
        /*
        BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\emine\\IdeaProjects\\JMETALHOME\\src\\jmetal\\util\\gson\\input.json"));

        Input input = gson.fromJson(br, Input.class);
        String jsonString = gson.toJson(input);
        System.out.println(jsonString);

         */




        Input myclass = new Input();

        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String line;

        // Read number of items
        line = in.readLine();
        int numberOfItems = Integer.parseInt(line);
        myclass.num_of_distinct_appliances = numberOfItems;

        //Read number of buckets
        line = in.readLine();
        int numberOfConstraints_ = Integer.parseInt(line);
        myclass.costList = new costList[numberOfConstraints_];
        myclass.costBuyList = new costBuyList[numberOfConstraints_];
        myclass.timeslotList = new timeslotList[numberOfConstraints_];
        myclass.producedREList = new producedREList[numberOfConstraints_];

        //Read number of Users
        line = in.readLine();
        int numberOfUsers = Integer.parseInt(line);
        myclass.userList = new userList[numberOfUsers];
        for (int i = 0; i < numberOfUsers; i++) {
            userList ul = new userList();
            ul.id = i;
            ul.index = i;
            myclass.userList[i] = ul;
        }

        in.readLine();

        //Read weights of items
        myclass.energyList = new energyList[numberOfUsers*numberOfItems];
        for (int j = 0; j < numberOfUsers; j++) {
            for (int i = 0; i < numberOfItems; i++) {
                int index = j*numberOfItems + i;
                // Read weight for the j-th item
                line = in.readLine();
                energyList el = new energyList();
                el.id = index;
                el.index = index;
                el.value = Double.parseDouble(line);
                myclass.energyList[index] = el;
            }
            in.readLine();
        }

        in = new BufferedReader(new FileReader(renewableFileName));

        for (int i = 0; i < numberOfConstraints_; i++) {
            line = in.readLine();

            producedREList rel = new producedREList();
            rel.id = i;
            rel.index = i;
            rel.timeslot = i;
            rel.value = Double.parseDouble(line);
            myclass.producedREList[i] = rel;

            timeslotList tsl = new timeslotList();
            tsl.id = i;
            tsl.index = i;
            myclass.timeslotList[i] = tsl;
        }

        in.close();

        ////////////////////////////////////

        in = new BufferedReader(new FileReader(userPreferenceFileName));

        int totalsize = numberOfUsers * numberOfConstraints_ * numberOfItems;
        myclass.applianceList = new applianceList[totalsize];
        myclass.preferenceList = new preferenceList[totalsize];

        int count = 0;
        for (int u = 0; u < numberOfUsers; u++) {
            for (int i = 0; i < numberOfConstraints_; i++) {
                for (int j = 0; j < numberOfItems; j++) {
                    // Read number of items
                    Character r = (char) in.read();
                    int num = Integer.parseInt(r.toString());
                    applianceList al = new applianceList();
                    preferenceList pl = new preferenceList();
                    al.id = count;
                    al.user = u;
                    al.timeslot = i;
                    al.energy = u*numberOfItems + j;
                    pl.id = count;
                    pl.appliance = count;
                    if (num == 1) {
                        pl.status = true;
                    } else {
                        pl.status = false;
                    }
                    myclass.applianceList[count] = al;
                    myclass.preferenceList[count] = pl;
                    count++;

                }
                in.read();
                in.read();
                System.out.println();
            }

            in.readLine();

        } //u

        in.close();

        /////////////////////////////////////////////////

        in = new BufferedReader(new FileReader(costsFileName));

        for (int i = 0; i < numberOfConstraints_; i++) {
            line = in.readLine();

            costList cl = new costList();
            cl.id = i;
            cl.index = i;
            cl.timeslot = i;
            cl.value = Double.parseDouble(line);
            myclass.costList[i] = cl;
        }

        in.close();

        /////////////////////////////////////////////////

        in = new BufferedReader(new FileReader(costsBuyFileName));

        for (int i = 0; i < numberOfConstraints_; i++) {
            line = in.readLine();

            costBuyList cbl = new costBuyList();
            cbl.id = i;
            cbl.index = i;
            cbl.timeslot = i;
            cbl.value = Double.parseDouble(line);
            myclass.costBuyList[i] = cbl;
        }

        in.close();

        /////////////////////////////////////////////////

        /*********** extras *************/
        //status
        myclass.statusList = new statusList[2];
        myclass.statusList[0] = new statusList();
        myclass.statusList[1] = new statusList();
        myclass.statusList[1].id = 1;
        myclass.statusList[1].index = 1;

        String jsonString = gson.toJson(myclass);
        jsonString = jsonString.replace("\"score\": 0", "\"score\": null");
        System.out.println(jsonString);

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
        writer.write(jsonString);

        writer.close();


    }


}
