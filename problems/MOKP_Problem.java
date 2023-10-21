/* Author: Yi Xiang
 * Many-Objective Knapsack Problems
 */



package jmetal.problems;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.Variable;
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.util.JMException;
import jmetal.util.wrapper.XReal;


public class MOKP_Problem extends Problem {

	private static final long serialVersionUID = 1L;
    private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    private String userPreferencePath = "/Users/emine/IdeaProjects/JMETALHOME/Userpreference_data/"; // The path of the files
    public static String fileName; //
    public static String userPreferencefileName; //
    private int numberOfItems;
    private int numberOfUsers;
    private double[] w; // weight of items
    private boolean [][][] pref; // preferences of users: user x time x device
    private boolean [] pref_vector; // preferences of users: user x time x device
    private XReal costOfUsage ; // capacity of each  knapsack .
    private int[] requestedDevicesPerUser;
    private double[] nadirObjectiveValue;

  public MOKP_Problem(String problemName,String userPreferenceName) {
	  this.setMaxmized_(false); // this problem is not to be maximized
	  this.problemName_ = problemName;
      this.numberOfVariables_ = 1;

      fileName = problemPath + this.problemName_ + ".txt";
      userPreferencefileName = userPreferencePath + userPreferenceName + ".txt";
      System.out.println(fileName);

      //fills up numberOfItems, p, w, sackCapacity
      //simply read the input textfile
      this.loadProblem(fileName, userPreferencefileName);
      this.solutionType_ = new MOKP_BinarySolution(this);

  }  // 

  public void loadProblem(String problemFileName, String userPreferencefileName) {
      try {
          BufferedReader in = new BufferedReader(new FileReader(problemFileName));
          String line;

          // Read number of items
          line = in.readLine();
          numberOfItems = Integer.parseInt(line);

          //Read number of buckets
          line = in.readLine();
          this.numberOfConstraints_ = Integer.parseInt(line);

          this.numberOfObjectives_ = 2;
          nadirObjectiveValue = new double[this.numberOfObjectives_];
          nadirObjectiveValue[1] = Double.MIN_VALUE;

          w = new double[numberOfItems];

          for (int i = 0; i < numberOfItems; i++) {
              // Read weight for the j-th item
              line = in.readLine();
              w[i] = Double.parseDouble(line);
          }

          in.close();
      } catch (IOException e){
          System.out.println("Error reading MOKP problemFile: " + e.getMessage());
      }

      try {
          BufferedReader in = new BufferedReader(new FileReader(userPreferencefileName));

          String line = in.readLine();
          numberOfUsers = Integer.parseInt(line);
          in.readLine();

          nadirObjectiveValue[0] = numberOfUsers;
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
                      }
                      count++;
                  }
                  in.read();
                  in.read();
                  System.out.println();
              }

              in.readLine();

              if (requestedEnergyPerUser[u] > nadirObjectiveValue[1])
                  nadirObjectiveValue[1] = requestedEnergyPerUser[u];

          } //u

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

    public void setCostOfUsage(XReal y) {
        costOfUsage = y;
    }

    public int getNumberOfUsers(){
      return numberOfUsers;
    }

    public int getNumberOfItems(){
        return numberOfItems;
    }

    public int getNumberOfConstraints(){
        return this.numberOfConstraints_;
    }

    public double[] getWeightOfItems(){
        return w;
    }

    public boolean[][][] getUserPreferences(){
      return pref;
    }

    public boolean[] getUserPreferenceVector(){
        return pref_vector;
    }

    public double[] getNadirObjectiveValue(){
        return nadirObjectiveValue;
    }

    public void repair(Solution solution){
        Variable[] vars = solution.getDecisionVariables();
        Binary bin = (Binary) vars[0];
        int numOfBits = bin.getNumberOfBits();

        int[] covered = new int[numberOfUsers * this.numberOfConstraints_ * numberOfItems];
        Arrays.fill(covered, -1);
        int[] coveredReverse = new int[numberOfUsers * this.numberOfConstraints_ * numberOfItems];
        Arrays.fill(coveredReverse, -1);

        ////// loop 1
        for (int j = 0; j < numOfBits; j++) { // for each user
            if (pref_vector[j] && bin.getIth(j)) {
                covered[j] = j;
                coveredReverse[j] = j;
            }
        }

        ////// loop 2
        for (int u = 0; u < numberOfUsers; u++) { // for each user

            int userIndex = u * this.numberOfConstraints_;
            int l = 0;
            for (int i = userIndex; i < userIndex + this.numberOfConstraints_; i++) { // for each objective
                int itemIndex = i * numberOfItems;
                for (int j = itemIndex; j < itemIndex + numberOfItems; j++) { // for each bit
                    if (pref_vector[j] == false) {
                        if (bin.getIth(j)){

                            //find the satisfied slot, if it exists. If not, turn device OFF
                            int misplacement = 1;
                            int selected = -1;
                            int behind = l-1;
                            int front = l+1;
                            while (behind >= 0 || front < this.numberOfConstraints_){
                                if (behind >= 0) {
                                    int new_position = j-misplacement*numberOfItems;
                                    boolean pref_behind = pref_vector[new_position];
                                    if (pref_behind && coveredReverse[new_position] == -1) {
                                        selected = new_position;
                                        break;
                                    }
                                    behind--;
                                }
                                if (front < this.numberOfConstraints_) {
                                    int new_position = j+misplacement*numberOfItems;
                                    boolean pref_front = pref_vector[new_position];
                                    if (pref_front && coveredReverse[new_position] == -1) {
                                        selected = new_position;
                                        break;
                                    }
                                    front++;
                                }
                                misplacement++;
                            }
                            if (selected != -1) {
                                //now that we found someone to cover, add satisfaction and secure that
                                //position so that noone else claims it in the future
                                covered[j] = selected;
                                coveredReverse[selected] = j;
                            } else {
                                //no slot found means ON-device does not belong here
                                bin.setIth(j, false);
                            }
                        } //end of active device
                    } //end of preference check
                } // for j
                l++;
            } // for i
        } //for u

        solution.setDeviceToPreferenceMapping(covered);
        solution.setReverseDeviceToPreferenceMapping(coveredReverse);
    }

    @Override
	public void evaluate(Solution solution) throws JMException {

        // restore if extra data no longer filled
		//Variable[] vars = solution.getDecisionVariables();
        //Binary bin = (Binary) vars[0];

        double result;
        result = percentage_dissatisfaction_evaluate(solution);
        solution.setObjective(0, result);
        result = highest_cost_evaluate(solution);
        solution.setObjective(1, result);
        
	} // evaluate

    public int simple_user_pref_evaluate(Binary bin) {
        int dissatisfaction = 0;
        int numOfBits = bin.getNumberOfBits();

        for (int j = 0; j < numOfBits; j++) { // for each user
            if (bin.getIth(j) != pref_vector[j]) {
                dissatisfaction++;
            }
        }

        return dissatisfaction;
    }

    public double sum_of_cost_evaluate(Binary bin) throws JMException {
        double sum = 0;

        for (int u = 0; u < numberOfUsers; u++) { // for each user

            int userIndex = u * this.numberOfConstraints_;

            int l = 0;
            for (int i = userIndex; i < userIndex + this.numberOfConstraints_; i++) { // for each objective

                int startingIndex = i * numberOfItems;

                int k = 0;
                for (int j = startingIndex; j < startingIndex + numberOfItems; j++) { // for each bit
                    if (bin.getIth(j) == true) {
                        sum = sum + w[k] * costOfUsage.getValue(l);
                    }
                    k++;
                } // for j
                l++;
            } // for i

        } // for u

        return sum;
    }

    public int simple_highest_dissatisfaction_evaluate(Binary bin) {
        int highest_dissatisfaction = 0;

        for (int u = 0; u < numberOfUsers; u++) { // for each user
            int dissatisfaction = 0;

            int userIndex = u * this.numberOfConstraints_;

            int l = 0;
            for (int i = userIndex; i < userIndex + this.numberOfConstraints_; i++) { // for each objective

                int itemIndex = i * numberOfItems;

                int k = 0;
                for (int j = itemIndex; j < itemIndex + numberOfItems; j++) { // for each bit
                    if (bin.getIth(j) != pref[u][l][k]) {
                        dissatisfaction++;
                    }
                    k++;
                } // for j
                l++;
            } // for i

            if (dissatisfaction > highest_dissatisfaction){
                highest_dissatisfaction = dissatisfaction;
            }

        } //for u

        return highest_dissatisfaction;
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
                        sum = sum + w[k] * costOfUsage.getValue(l);
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

    public double complex_highest_dissatisfaction_evaluate(Binary bin) {
        double highest_dissatisfaction = 0;
        //boolean[] used = new boolean[bin.getNumberOfBits()];

        for (int u = 0; u < numberOfUsers; u++) { // for each user
            double dissatisfaction = 0;

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
                            double dissatisfaction_amount = 1.0;

                            /*
                            //check first neighbours
                            if (l-1 >=0 && !pref[u][l-1][k] && bin.getIth(j-numberOfItems) && !used[j-numberOfItems]){
                                dissatisfaction_amount = 0.5;
                                used[j-numberOfItems] = true;
                            } else if (l+1 < this.numberOfConstraints_ && !pref[u][l+1][k] && bin.getIth(j+numberOfItems) && !used[j+numberOfItems]){
                                dissatisfaction_amount = 0.5;
                                used[j+numberOfItems] = true;
                            }
                            //check second neighbours
                            else if (l-2 >=0 && !pref[u][l-2][k] && bin.getIth(j-2*numberOfItems) && !used[j-2*numberOfItems]){
                                dissatisfaction_amount = 0.75;
                                used[j-2*numberOfItems] = true;
                            } else if (l+2 < this.numberOfConstraints_ && !pref[u][l+2][k] && bin.getIth(j+2*numberOfItems) && !used[j+2*numberOfItems]){
                                dissatisfaction_amount = 0.75;
                                used[j+2*numberOfItems] = true;
                            }

                             */

                            dissatisfaction += dissatisfaction_amount;
                        }
                    }
                    k++;
                } // for j
                l++;
            } // for i

            if (dissatisfaction > highest_dissatisfaction){
                highest_dissatisfaction = dissatisfaction;
            }

        } //for u

        return highest_dissatisfaction;
    }

    public double percentage_dissatisfaction_evaluate(Solution solution) {
        double total_dissatisfaction = 0;

        //remove if filling up extra data is no longer needed
        Variable[] vars = solution.getDecisionVariables();
        Binary bin = (Binary) vars[0];
        double[] dissatisfactionPerUser = new double[numberOfUsers];
        int[] covered = solution.getDeviceToPreferenceMapping();

        for (int u = 0; u < numberOfUsers; u++) { // for each user
            double satisfaction_nominator = 0;

            int userIndex = u * this.numberOfConstraints_;

            int l = 0;
            for (int i = userIndex; i < userIndex + this.numberOfConstraints_; i++) { // for each objective
                int itemIndex = i * numberOfItems;
                int k = 0;
                for (int j = itemIndex; j < itemIndex + numberOfItems; j++) { // for each bit
                    if (pref[u][l][k]) {
                        if (bin.getIth(j)){
                            satisfaction_nominator++;
                        }
                        //not preferred
                    } else {
                        if (bin.getIth(j)){
                            int misplacement = Math.abs(covered[j] - j) / numberOfItems;
                            satisfaction_nominator += Math.pow(0.5, misplacement);
                        }
                    }
                    k++;
                } // for j
                l++;
            } // for i

            int dissatisfaction_denominator = requestedDevicesPerUser[u];
            double user_dissatisfaction = 1.0 - (satisfaction_nominator / (double) dissatisfaction_denominator);
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

}



