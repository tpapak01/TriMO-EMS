/* Author: Yi Xiang
 * Many-Objective Knapsack Problems
 */



package jmetal.problems;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.Variable;
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.util.JMException;


public class MOKP_Problem extends Problem {

	private static final long serialVersionUID = 1L;
    private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user/"; // The path of the files
    private String userPreferencePath = "/Users/emine/IdeaProjects/JMETALHOME/Userpreference_data/"; // The path of the files
    public static String fileName; //
    public static String userPreferencefileName; //
    private int numberOfItems;
    private int numberOfUsers;
    private int[] w; // weight of items
    private boolean [][][] pref; // preferences of users: user x time x device
    private int[] costOfUsage ; // capacity of each  knapsack .
	

  public MOKP_Problem(String problemName,String userPreferenceName) {
	  this.setMaxmized_(false); // this problem is not to be maximized
	  this.problemName_ = problemName;
      this.numberOfVariables_ = 1;

      fileName = problemPath + problemName + ".txt";
      userPreferencefileName = userPreferencePath + userPreferenceName + ".txt";
      System.out.println(fileName);

      //fills up numberOfItems, p, w, sackCapacity
      //simply read the input textfile
      this.loadProblem(fileName, userPreferencefileName);
      this.solutionType_ = new MOKP_BinarySolution(this, numberOfItems, numberOfUsers, w);

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

          w = new int[numberOfItems];

          for (int i = 0; i < numberOfItems; i++) {
              // Read weight for the j-th item
              line = in.readLine();
              w[i] = Integer.parseInt(line);
          }

          costOfUsage = new int[this.numberOfConstraints_];
          for (int i = 0; i < this.numberOfConstraints_; i++) {
              line = in.readLine();
              costOfUsage[i] = Integer.parseInt(line);
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

          pref = new boolean[numberOfUsers][this.numberOfConstraints_][numberOfItems];

          for (int u = 0; u < numberOfUsers; u++) {
              for (int i = 0; i < this.numberOfConstraints_; i++) {
                  for (int j = 0; j < numberOfItems; j++) {
                      // Read number of items
                      Character r = (char) in.read();
                      int num = Integer.parseInt(r.toString());
                      if (num == 1)
                          pref[u][i][j] = true;
                  }
                  in.read();
                  in.read();
                  System.out.println();
              }

              in.readLine();

          }

          in.close();
      } catch (IOException e){
          System.out.println("Error reading MOKP problemFile: " + e.getMessage());
      }

  }
  
	@Override
	public void evaluate(Solution solution) throws JMException {
		// TODO Auto-generated method stub
		Variable[] vars = solution.getDecisionVariables();
        Binary bin = (Binary) vars[0];

        double result;
        result = complex_highest_dissatisfaction_evaluate(bin);
        solution.setObjective(0, result);
        result = highest_cost_evaluate(bin);
        solution.setObjective(1, result);
        
	} // evaluate

    public int simple_user_pref_evaluate(Binary bin) {
        int dissatisfaction = 0;

        for (int u = 0; u < numberOfUsers; u++) { // for each user

            int userIndex = u * numberOfUsers;

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

        } //for u

        return dissatisfaction;
    }

    public int sum_of_cost_evaluate(Binary bin) {
        int sum = 0;

        for (int u = 0; u < numberOfUsers; u++) { // for each user

            int userIndex = u * numberOfUsers;

            int l = 0;
            for (int i = userIndex; i < userIndex + this.numberOfConstraints_; i++) { // for each objective

                int startingIndex = i * numberOfItems;

                int k = 0;
                for (int j = startingIndex; j < startingIndex + numberOfItems; j++) { // for each bit
                    if (bin.getIth(j) == true) {
                        sum = sum + w[k] * costOfUsage[l];
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

            int userIndex = u * numberOfUsers;

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

    public int highest_cost_evaluate(Binary bin) {
        int highest_cost = 0;

        for (int u = 0; u < numberOfUsers; u++) { // for each user
            int sum = 0;

            int userIndex = u * numberOfUsers;

            int l = 0;
            for (int i = userIndex; i < userIndex + this.numberOfConstraints_; i++) { // for each objective

                int startingIndex = i * numberOfItems;

                int k = 0;
                for (int j = startingIndex; j < startingIndex + numberOfItems; j++) { // for each bit
                    if (bin.getIth(j)) {
                        sum = sum + w[k] * costOfUsage[l];
                    }
                    k++;
                } // for j
                l++;
            } // for i

            if (sum > highest_cost){
                highest_cost = sum;
            }
        } // for u

        return highest_cost;
    }

    public double complex_highest_dissatisfaction_evaluate(Binary bin) {
        double highest_dissatisfaction = 0;

        for (int u = 0; u < numberOfUsers; u++) { // for each user
            double dissatisfaction = 0;

            int userIndex = u * numberOfUsers;

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
                            //check first neighbours
                            if ((l-1 >=0 && !pref[u][l-1][k] && j-numberOfItems >=0 && bin.getIth(j-numberOfItems)) ||
                                    l+1 < this.numberOfConstraints_ && !pref[u][l+1][k] && j+numberOfItems < bin.getNumberOfBits() && bin.getIth(j+numberOfItems)){
                                dissatisfaction_amount = 0.5;
                            }
                            //check second neighbours
                            else if ((l-2 >=0 && !pref[u][l-2][k] && j-2*numberOfItems >=0 && bin.getIth(j-2*numberOfItems)) ||
                                    l+2 < this.numberOfConstraints_ && !pref[u][l+2][k] && j+2*numberOfItems < bin.getNumberOfBits() && bin.getIth(j+2*numberOfItems)){
                                dissatisfaction_amount = 0.75;
                            }
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
}



