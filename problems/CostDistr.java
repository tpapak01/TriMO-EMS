/* Author: Yi Xiang
 * Many-Objective Knapsack Problems
 */



package jmetal.problems;

import com.sun.org.apache.regexp.internal.RE;
import com.sun.xml.internal.bind.v2.TODO;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.ArrayIntSolutionType;
import jmetal.encodings.solutionType.ArrayRealSolutionType;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.variable.ArrayInt;
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.Int;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.metaheuristics.bilevel.LowerLevelMOKP;
import jmetal.util.JMException;
import jmetal.util.wrapper.XInt;
import jmetal.util.wrapper.XReal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class CostDistr extends Problem {

	private static final long serialVersionUID = 1L;
    private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    private String fileName;

    private MOKP_Problem lowerLevelProblem;
    private int[] producedRE;


  public CostDistr(String problemName, MOKP_Problem lowerLevelProblem) {
	  this.setMaxmized_(false); // this problem is not to be maximized
	  this.problemName_ = problemName;
      this.numberOfVariables_ = lowerLevelProblem.getNumberOfConstraints();
      this.numberOfObjectives_ = 1;
      this.lowerLimit_ = new double[numberOfVariables_];
      this.upperLimit_ = new double[numberOfVariables_];
      for (int i=0; i<upperLimit_.length; i++)
          upperLimit_[i] = 1.0;
      producedRE = new int[numberOfVariables_];
      this.lowerLevelProblem = lowerLevelProblem;

      fileName = problemPath + problemName + ".txt";
      System.out.println(fileName);

      //fills up numberOfItems, p, w, sackCapacity
      //simply read the input textfile
      this.loadProblem(fileName);
      this.solutionType_ = new ArrayRealSolutionType(this);

  }  // 

  public void loadProblem(String problemFileName) {

      try {
          BufferedReader in = new BufferedReader(new FileReader(problemFileName));
          String line;

          in.readLine();
          in.readLine();

          for (int i = 0; i < lowerLevelProblem.getNumberOfItems(); i++) {
              in.readLine();
          }

          for (int i = 0; i < lowerLevelProblem.getNumberOfConstraints(); i++) {
              line = in.readLine();
              producedRE[i] = Integer.parseInt(line);
          }

          in.close();
      } catch (IOException e){
          System.out.println("Error reading MOKP problemFile: " + e.getMessage());
      }

  }

    public int[] getProducedRE(){
        return producedRE;
    }
  
	@Override
	public void evaluate(Solution solution) throws JMException {

        SolutionSet lowerLevelSolutions = null;
        try {
            lowerLevelSolutions = LowerLevelMOKP.evaluate(new XReal(solution));
        } catch (Exception e){
            System.out.println("Exception at LowerLevelMOKP.evaluate: " + e.getMessage());
        }

        double best_result = Double.MAX_VALUE;
        int llConstraints = lowerLevelProblem.getNumberOfConstraints();
        int llUsers = lowerLevelProblem.getNumberOfUsers();
        int llItems = lowerLevelProblem.getNumberOfItems();
        double[] w = lowerLevelProblem.getWeightOfItems();

        for (int s=0; s<lowerLevelSolutions.size(); s++) {
            Solution lowerLevelSol = lowerLevelSolutions.get(s);

            //Calculate total energy based on solution picked
            //only take weight in account, not cost. where 1, add the weight to the total for that bucket
            double[] spentEnergy = new double[llConstraints];

            Variable[] vars = lowerLevelSol.getDecisionVariables();
            Binary bin = (Binary) vars[0];

            for (int u = 0; u < llUsers; u++) { // for each user
                int userIndex = u * llConstraints;
                int l = 0;
                for (int i = userIndex; i < userIndex + llConstraints; i++) { // for each objective
                    int itemIndex = i * llItems;
                    int k = 0;
                    for (int j = itemIndex; j < itemIndex + llItems; j++) { // for each bit
                        if (bin.getIth(j)) {
                            spentEnergy[l] += w[k];
                        }
                        k++;
                    } // for j
                    l++;
                } // for i
            } //for u

            lowerLevelSol.setSpentEnergy(spentEnergy);

            //do upper-level evaluation = finding deviation from available RE
            //double result = upperLevel_evaluate_distance_from_produced(spentEnergy);
            double result = upperLevel_evaluate_maximize_usage_of_produced(spentEnergy);
            if (result < best_result){
                best_result = result;
                solution.setSpentEnergy(spentEnergy);
                solution.setLowerLevelVars(bin);
                solution.setLowerLevelObj(new double[] {lowerLevelSol.getObjective(0), lowerLevelSol.getObjective(1)});
            }
        }

        solution.setObjective(0, best_result);
        System.out.println(best_result);

	} // evaluate

    public double upperLevel_evaluate_distance_from_produced(double[] spentEnergy) {

        double sum = 0;
        for (int i=0; i<producedRE.length; i++) {
            //if (spentEnergy[i] > producedRE[i]){
                sum += Math.abs(spentEnergy[i] - producedRE[i]);
            //}
        }

        return sum;
    }

    public double upperLevel_evaluate_maximize_usage_of_produced(double[] spentEnergy) {

        double sum = 0;
        for (int i=0; i<producedRE.length; i++) {
            double diff = producedRE[i] - spentEnergy[i];
            if (diff > 0)
                sum += diff;
        }

        return sum;
    }

}



