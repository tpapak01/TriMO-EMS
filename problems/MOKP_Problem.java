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
    private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data/"; // The path of the files
    private String userPreferencePath = "/Users/emine/IdeaProjects/JMETALHOME/Userpreference_data/"; // The path of the files
    public static String fileName; //
    public static String userPreferencefileName; //
    private int numberOfItems;
    private int [][] p; // profit of items
    private int [][] w; // weight of items
    private boolean [][] pref; // preferece of user: time x device
    private double[] sackCapacity ; // capacity of each  knapsack .
	

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
      this.solutionType_ = new MOKP_BinarySolution(this, numberOfItems,p,w, sackCapacity);

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

          this.numberOfObjectives_ = 1;

          sackCapacity = new double[this.numberOfConstraints_];

          p = new int[this.numberOfConstraints_][numberOfItems];
          w = new int[this.numberOfConstraints_][numberOfItems];
          pref = new boolean[this.numberOfConstraints_][numberOfItems];

          for (int i = 0; i < this.numberOfConstraints_; i++) {
              //reads line mentioning capacity of bucket, whether existent or 1+e155
              line = in.readLine();

              if (i < this.numberOfConstraints_) {
                  sackCapacity[i] = Double.parseDouble(line);
//    		  System.out.println(capacity[i]);	  
              } else {
//    		  System.out.println(line);	  
              }
              int sumProfit = 0;

              for (int j = 0; j < numberOfItems; j++) {
                  // Read weight for the j-th item
                  line = in.readLine();
                  w[i][j] = Integer.parseInt(line);
//    		  System.out.println(w[i][j]);	

                  // Read profit for the j-th item
                  line = in.readLine();
                  p[i][j] = Integer.parseInt(line);
                  sumProfit = sumProfit + p[i][j];
//    		  System.out.println(p[i][j]);	    		  
              }

//    	  System.out.println("sumProfit = " + sumProfit);	
          }

          in.close();
      } catch (IOException e){
          System.out.println("Error reading MOKP problemFile: " + e.getMessage());
      }


      try {
          BufferedReader in = new BufferedReader(new FileReader(userPreferencefileName));

          for (int i = 0; i < this.numberOfConstraints_; i++) {
              for (int j = 0; j < numberOfItems; j++) {
                  // Read number of items
                  Character r = (char) in.read();
                  int num = Integer.parseInt(r.toString());
                  if (num == 1)
                    pref[i][j]=true;
              }
              in.read();
              in.read();
              System.out.println();
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

        int result = simple_user_pref_evaluate(bin);
        //int result = sum_of_profit_evaluate(bin);

        solution.setObjective(0, result);
        
	} // evaluate

    public int simple_user_pref_evaluate(Binary bin) {
        int dissatisfaction = 0;
        for (int i = 0; i < this.numberOfConstraints_; i++) { // for each objective

            int startingIndex = i * numberOfItems;

            int k=0;
            for(int j = startingIndex; j < startingIndex+numberOfItems; j++) { // for each bit
                if (bin.getIth(j) != pref[i][k]) {
                    dissatisfaction++;
                }
                k++;
            } // for j

        } // for i

        return dissatisfaction;
    }

    public int sum_of_profit_evaluate(Binary bin) throws JMException {
        int sum = 0;
        for (int i = 0; i < this.numberOfConstraints_; i++) { // for each objective

            int startingIndex = i * numberOfItems;

            int k = 0;
            for (int j = startingIndex; j < startingIndex + numberOfItems; j++) { // for each bit
                if (bin.getIth(j) == true) {
                    sum = sum + p[i][k];
                }
                k++;
            } // for j

        } // for i
        return sum;
    }
       
      // Evaluates the constraint overhead of a solution
      @Override
	  public void evaluateConstraints(Solution solution) throws JMException {
		Variable[] vars = solution.getDecisionVariables();
	    Binary bin = (Binary) vars[0];
	    
	    int numberViolate = 0;
	    double CV = 0.0;
	    
	    for (int i = 0; i < this.numberOfConstraints_;i++) {

            int startingIndex = i * numberOfItems;

            int sumWeight = 0;
            int k = 0;
            for(int j = startingIndex; j < startingIndex+numberOfItems; j++) { // for each bit
                  if (bin.getIth(j) == true) {
                      sumWeight = sumWeight +  w[i][k];
                  }
                  k++;
            }
	    	  
            if (sumWeight > sackCapacity[i])  {
                  numberViolate++;
                  CV = CV + (sumWeight - sackCapacity[i]);
            }
	    	  
	    } // for i
	    	    	 
	    solution.setNumberOfViolatedConstraint(numberViolate);    
	    solution.setOverallConstraintViolation(CV);
	  } // evaluateConstraints   
}



