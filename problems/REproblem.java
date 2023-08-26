/* Author: Yi Xiang
 * Many-Objective Knapsack Problems
 */



package jmetal.problems;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.Variable;
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.encodings.variable.REproblem_BinarySolution;
import jmetal.util.JMException;
import jmetal.util.wrapper.XReal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class REproblem extends Problem {

    private static final long serialVersionUID = 1L;
    private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    private String userPreferencePath = "/Users/emine/IdeaProjects/JMETALHOME/Userpreference_data/"; // The path of the files
    public static String fileName; //
    public static String userPreferencefileName; //

    //lower level
    private int numberOfItems;
    private int numberOfUsers;
    private double[] w; // weight of items
    private boolean [][][] pref; // preferences of users: user x time x device
    private boolean [] pref_vector; // preferences of users: user x time x device
    private double[] costs ; // capacity of each  knapsack .
    private int[] requestedDevicesPerUser;

    //upper level
    private double[] producedRE;
    private double totalProducedRE;

    //extras of optimal solutions
    private ArrayList<List<Double>> itemsPerTimeslot;
    private ArrayList<List<Integer>> indexPerTimeslot;

    public int getNumberOfItems(){
        return numberOfItems;
    }

    public int getNumberOfUsers(){
        return numberOfUsers;
    }


    public REproblem(String problemName,String userPreferenceName) {
          this.setMaxmized_(false); // this problem is not to be maximized
          this.problemName_ = problemName;
          this.numberOfVariables_ = 1;
          this.numberOfObjectives_ = 1;

          fileName = problemPath + this.problemName_ + ".txt";
          userPreferencefileName = userPreferencePath + userPreferenceName + ".txt";
          System.out.println(fileName);

          //fills up numberOfItems, p, w, sackCapacity
          //simply read the input textfile
          this.loadProblem(fileName, userPreferencefileName);
          this.solutionType_ = new REproblem_BinarySolution(this);

    }  //

  public void loadProblem(String problemFileName, String userPreferencefileName) {

        //1) items, constraints, weights, producedRE
      try {
          BufferedReader in = new BufferedReader(new FileReader(problemFileName));
          String line;

          // Read number of items
          line = in.readLine();
          numberOfItems = Integer.parseInt(line);

          //Read number of buckets
          line = in.readLine();
          this.numberOfConstraints_ = Integer.parseInt(line);
          producedRE = new double[this.numberOfConstraints_];
          itemsPerTimeslot = new ArrayList<List<Double>>(this.numberOfConstraints_);
          indexPerTimeslot = new ArrayList<List<Integer>>(this.numberOfConstraints_);

          w = new double[numberOfItems];
          for (int i = 0; i < numberOfItems; i++) {
              // Read weight for the j-th item
              line = in.readLine();
              w[i] = Double.parseDouble(line);
          }

          for (int i = 0; i < this.numberOfConstraints_; i++) {
              line = in.readLine();
              producedRE[i] = Double.parseDouble(line);
              totalProducedRE += producedRE[i];
              itemsPerTimeslot.add(new LinkedList<Double>());
              indexPerTimeslot.add(new LinkedList<Integer>());
          }

          in.close();
      } catch (IOException e) {
          System.out.println("Error reading MOKP problemFile: " + e.getMessage());
      }

        //2) users, preferences, requestedDevicesPerUser
          try {
              BufferedReader in = new BufferedReader(new FileReader(userPreferencefileName));

              String line = in.readLine();
              numberOfUsers = Integer.parseInt(line);
              in.readLine();

              pref = new boolean[numberOfUsers][this.numberOfConstraints_][numberOfItems];
              pref_vector = new boolean[numberOfUsers*this.numberOfConstraints_*numberOfItems];
              //--------
              double[] requestedEnergy = new double[this.numberOfConstraints_];
              requestedDevicesPerUser = new int[numberOfUsers];
              double[] requestedEnergyPerUser = new double[numberOfUsers];
              double[][] requestedEnergyPerUserPerTime = new double[numberOfUsers][this.numberOfConstraints_];

              int count = 0;
              for (int u = 0; u < numberOfUsers; u++) {
                  for (int i = 0; i < this.numberOfConstraints_; i++) {
                      for (int j = 0; j < numberOfItems; j++) {
                          // Read number of items
                          Character r = (char) in.read();
                          int num = Integer.parseInt(r.toString());
                          if (num == 1) {
                              pref[u][i][j] = true;
                              pref_vector[count] = true;
                              requestedEnergy[i] += w[j];
                              requestedDevicesPerUser[u]++;
                              requestedEnergyPerUser[u] += w[j];
                              requestedEnergyPerUserPerTime[u][i] += w[j];
                              itemsPerTimeslot.get(i).add(w[j]);
                              indexPerTimeslot.get(i).add(count);
                          }
                          count++;
                      }
                      in.read();
                      in.read();
                      System.out.println();
                  }

                  in.readLine();

              }

              in.close();

              System.out.println("Requested Energy Per Time ");
              System.out.println(Arrays.toString(requestedEnergy));
              System.out.println("Requested Energy Per User ");
              System.out.println(Arrays.toString(requestedEnergyPerUser));
              System.out.println("Requested Energy Per User Per Time ");
              for (int i=0; i<numberOfUsers; i++)
                  System.out.println(Arrays.toString(requestedEnergyPerUserPerTime[i]));

          } catch (IOException e){
              System.out.println("Error reading MOKP problemFile: " + e.getMessage());
          }

  }
  
	@Override
	public void evaluate(Solution solution) throws JMException {

            calculateSpentEnergy(solution);
            double[] energySpent = solution.getSpentEnergy();
            double result = upperLevel_evaluate_XOR_distance(energySpent);

            solution.setObjective(0, result);
            solution.setEnergyDeviationFromProduced(result);

            // fill up extra data for analysis
            Variable[] vars = solution.getDecisionVariables();
            Binary bin = (Binary) vars[0];
            solution.setLowerLevelVars(bin);

            costs = new double[this.numberOfConstraints_];
            for (int j=0; j<this.numberOfConstraints_; j++)
                costs[j] = 0.5;

            double Dresult;
            Dresult = percentage_dissatisfaction_evaluate(solution);
            double Cresult;
            Cresult = highest_cost_evaluate(solution);
            solution.setLowerLevelObj(new double[] {Dresult, Cresult});

            double nonREpaid = calculateNonREPaid(energySpent, costs);
            solution.setNonREpaid(nonREpaid);

            System.out.println(result);
            System.out.println(solution.getDecisionVariables()[0]);

	} // evaluate

    public double upperLevel_evaluate_XOR_distance(double[] spentEnergy) {

        double sum = 0;
        for (int i=0; i<producedRE.length; i++) {
            sum += Math.abs(spentEnergy[i] - producedRE[i]);
        }

        return sum;
    }

    public double highest_cost_evaluate(Solution solution) throws JMException {
        double highest_cost = 0;

        //remove if filling up extra data is no longer needed
        Variable[] vars = solution.getDecisionVariables();
        Binary bin = (Binary) vars[0];
        double[] energyAllocatedPerUser = new double[numberOfUsers];

        for (int u = 0; u < numberOfUsers; u++) { // for each user
            double sum = 0;

            int userIndex = u * this.numberOfConstraints_;

            int l = 0;
            for (int i = userIndex; i < userIndex + this.numberOfConstraints_; i++) { // for each objective

                int startingIndex = i * numberOfItems;

                int k = 0;
                for (int j = startingIndex; j < startingIndex + numberOfItems; j++) { // for each bit
                    if (bin.getIth(j)) {
                        sum = sum + w[k] * costs[l];
                    }
                    k++;
                } // for j
                l++;
            } // for i

            if (sum > highest_cost){
                highest_cost = sum;
            }

            energyAllocatedPerUser[u] = sum;
            energyAllocatedPerUser[u] = Math.round(energyAllocatedPerUser[u]*100.0) / 100.0;

        } // for u

        solution.setEnergyAllocatedPerUser(energyAllocatedPerUser);

        return highest_cost;
    }

    public double percentage_dissatisfaction_evaluate(Solution solution) {
        double total_dissatisfaction = 0;

        //remove if filling up extra data is no longer needed
        Variable[] vars = solution.getDecisionVariables();
        Binary bin = (Binary) vars[0];
        double[] dissatisfactionPerUser = new double[numberOfUsers];

        for (int u = 0; u < numberOfUsers; u++) { // for each user
            double dissatisfaction_nominator = 0;

            int userIndex = u * this.numberOfConstraints_;

            int l = 0;
            for (int i = userIndex; i < userIndex + this.numberOfConstraints_; i++) { // for each objective

                int itemIndex = i * numberOfItems;

                int k = 0;

                for (int j = itemIndex; j < itemIndex + numberOfItems; j++) { // for each bit
                    //we only care if we actually wanted a device running at that time
                    if (pref[u][l][k]) {
                        //if it is not running, we are dissatisfied. But how much?
                        if (bin.getIth(j) == false){
                            dissatisfaction_nominator++;
                        }
                    }
                    k++;
                } // for j
                l++;
            } // for i

            int dissatisfaction_denominator = requestedDevicesPerUser[u];
            double user_dissatisfaction = dissatisfaction_nominator / (double) dissatisfaction_denominator;
            total_dissatisfaction += user_dissatisfaction;

            dissatisfactionPerUser[u] = user_dissatisfaction;
            dissatisfactionPerUser[u] = Math.round(dissatisfactionPerUser[u]*100.0) / 100.0;

        } //for u

        solution.setDissatisfactionPerUser(dissatisfactionPerUser);

        return total_dissatisfaction;
    }

    ////////////////////////////////    HELPER FUNCTIONS       ////////////////////////////////////////

    public void calculateSpentEnergy(Solution solution) {
        Variable[] vars = solution.getDecisionVariables();
        Binary bin = (Binary) vars[0];

        double[] spentEnergy = new double[this.numberOfConstraints_];
        for (int u = 0; u < numberOfUsers; u++) { // for each user
            int userIndex = u * this.numberOfConstraints_;
            int l = 0;
            for (int i = userIndex; i < userIndex + this.numberOfConstraints_; i++) { // for each objective
                int itemIndex = i * numberOfItems;
                int k = 0;
                for (int j = itemIndex; j < itemIndex + numberOfItems; j++) { // for each bit
                    if (bin.getIth(j)) {
                        spentEnergy[l] += w[k];
                    }
                    k++;
                } // for j
                spentEnergy[l] = Math.round(spentEnergy[l]*100.0) / 100.0;
                l++;
            } // for i
        } //for u

        solution.setSpentEnergy(spentEnergy);
    }

    public double calculateNonREPaid(double[] spentEnergy, double[] costs) {

        double sum = 0;

        for (int i=0; i<producedRE.length; i++) {
            double difference = spentEnergy[i] - producedRE[i];
            double abs_difference = Math.abs(difference);
            double cost = costs[i];
            if (difference > 0)
                sum += abs_difference * cost;
        }

        return sum;
    }

    public int[] produceListForTimeslot(int pos){
        List<Double> list = itemsPerTimeslot.get(pos);
        int[] list2 = new int[list.size()];
        for (int i=0; i<list.size(); i++)
            list2[i] = (int) (list.get(i).doubleValue() * 10);
        return list2;
    }

    public Integer[] getIndexListPerTimeslot(int pos){
        return indexPerTimeslot.get(pos).toArray(new Integer[0]);
    }

    public int calculateMaxPossibleSum(int[] list, int sum, int pos){
        int upper_limit_sum = (int) (producedRE[pos] * 10);
        if (upper_limit_sum > sum)
            upper_limit_sum = sum;
        boolean exists;
        while (true) {
            exists = isSubsetSum(list, list.length, upper_limit_sum);
            if (exists)
                return upper_limit_sum;
            upper_limit_sum -= 1;
        }
    }

    // A Dynamic Programming solution for subset
    // sum problem

    // Returns true if there is a subset of
    // set[] with sum equal to given sum
    static boolean isSubsetSum(int set[], int n, int sum)
    {
        // The value of subset[i][j] will be
        // true if there is a subset of
        // set[0..j-1] with sum equal to i
        boolean subset[][] = new boolean[sum + 1][n + 1];

        // If sum is 0, then answer is true
        for (int i = 0; i <= n; i++)
            subset[0][i] = true;

        // If sum is not 0 and set is empty,
        // then answer is false
        for (int i = 1; i <= sum; i++)
            subset[i][0] = false;

        // Fill the subset table in bottom
        // up manner
        for (int i = 1; i <= sum; i++) {
            for (int j = 1; j <= n; j++) {
                subset[i][j] = subset[i][j - 1];
                if (i >= set[j - 1])
                    subset[i][j]
                            = subset[i][j]
                            || subset[i - set[j - 1]][j - 1];
            }
        }

        return subset[sum][n];
    }

    // Driver code
    /*
    public static void main(String args[])
    {
        int set[] = { 3, 34, 4, 12, 5, 2 };
        int sum = 9;
        int n = set.length;
        if (isSubsetSum(set, n, sum) == true)
            System.out.println("Found a subset"
                    + " with given sum");
        else
            System.out.println("No subset with"
                    + " given sum");
    }

     */


    /* This code is contributed by Rajat Mishra */

}



