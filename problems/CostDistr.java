/* Author: Yi Xiang
 * Many-Objective Knapsack Problems
 */



package jmetal.problems;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.ArrayRealSolutionType;
import jmetal.encodings.variable.Binary;
import jmetal.metaheuristics.bilevel.LowerLevelMOKP_MOEAD;
import jmetal.metaheuristics.bilevel.LowerLevelMOKP_NSGAII;
import jmetal.util.JMException;
import jmetal.util.wrapper.XReal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class CostDistr extends Problem {

	private static final long serialVersionUID = 1L;
    private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    private String fileName;

    private MOKP_Problem lowerLevelProblem;
    private String lowerLevelAlgorithmName;
    private double[] producedRE;
    private double totalProducedRE;


  public CostDistr(String problemName, MOKP_Problem lowerLevelProblem, String lowerLevelAlgorithmName) {
	  this.setMaxmized_(false); // this problem is not to be maximized
	  this.problemName_ = problemName;
	  this.lowerLevelAlgorithmName = lowerLevelAlgorithmName;
      this.numberOfVariables_ = lowerLevelProblem.getNumberOfConstraints();
      this.numberOfObjectives_ = 1;
      this.lowerLimit_ = new double[numberOfVariables_];
      this.upperLimit_ = new double[numberOfVariables_];
      for (int i=0; i<upperLimit_.length; i++)
          upperLimit_[i] = 1.0;
      producedRE = new double[numberOfVariables_];
      this.lowerLevelProblem = lowerLevelProblem;

      fileName = problemPath + this.problemName_ + ".txt";
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
              producedRE[i] = Double.parseDouble(line);
              totalProducedRE += producedRE[i];
          }

          in.close();
      } catch (IOException e){
          System.out.println("Error reading MOKP problemFile: " + e.getMessage());
      }

  }

    public double[] getProducedRE(){
        return producedRE;
    }

    public double getTotalProducedRE(){
        return totalProducedRE;
    }
  
	@Override
	public void evaluate(Solution solution) throws JMException {

        SolutionSet lowerLevelSolutions = null;
        XReal costs = new XReal(solution);
        try {
            if (this.lowerLevelAlgorithmName.equals("MOEAD"))
                lowerLevelSolutions = LowerLevelMOKP_MOEAD.evaluate(costs);
            else lowerLevelSolutions = LowerLevelMOKP_NSGAII.evaluate(costs);
        } catch (Exception e){
            System.out.println("Exception at LowerLevelMOKP.evaluate: " + e.getMessage());
        }

        int lsize = lowerLevelSolutions.size();

        double best_result = Double.MAX_VALUE;
        int best_solution_index = -1;

        for (int s=0; s<lowerLevelSolutions.size(); s++) {
            Solution lowerLevelSol = lowerLevelSolutions.get(s);

            // do upper-level evaluation = finding deviation from available RE
            //double result = upperLevel_evaluate_distance_from_produced(spentEnergy);
            double[] energySpent = lowerLevelSol.getSpentEnergy();
            double result = upperLevel_evaluate_XOR_distance_plus_weight(energySpent, costs);
            if (result < best_result){
                best_result = result;
                best_solution_index = s;
            }

        }

        solution.setObjective(0, best_result);
        // fill up extra data for analysis
        Solution lowerLevelSol = lowerLevelSolutions.get(best_solution_index);
        Variable[] vars = lowerLevelSol.getDecisionVariables();
        Binary bin = (Binary) vars[0];
        double[] energySpent = lowerLevelSol.getSpentEnergy();

        solution.setSpentEnergy(energySpent);
        solution.setLowerLevelVars(bin);
        solution.setLowerLevelObj(new double[] {lowerLevelSol.getObjective(0), lowerLevelSol.getObjective(1)});
        solution.setDissatisfactionPerUser(lowerLevelSol.getDissatisfactionPerUser());
        solution.setEnergyAllocatedPerUser(lowerLevelSol.getEnergyAllocatedPerUser());
        double deviation = calculateEnergyDeviationFromProduced(energySpent);
        solution.setEnergyDeviationFromProduced(deviation);
        double nonREpaid = calculateNonREPaid(energySpent, costs);
        solution.setNonREpaid(nonREpaid);

        System.out.println(best_result);
        System.out.println(solution.getDecisionVariables()[0]);

	} // evaluate

    public double upperLevel_evaluate_XOR_distance(double[] spentEnergy) {

        double sum = 0;
        for (int i=0; i<producedRE.length; i++) {
            sum += Math.abs(spentEnergy[i] - producedRE[i]);
        }

        return sum;
    }

    public double upperLevel_evaluate_XOR_distance_plus_weight(double[] spentEnergy, XReal costs) throws JMException {

        double sum = 0;

        for (int i=0; i<producedRE.length; i++) {
            double difference = spentEnergy[i] - producedRE[i];
            double abs_difference = Math.abs(difference);
            double cost = costs.getValue(i);
            if (difference < 0)
                sum += abs_difference * (1.0 + cost);
            else sum += abs_difference * (2.0 - cost);
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

    public double upperLevel_evaluate_distance_from_produced_complex(double[] spentEnergy) {

        double sum_extra = 0;
        double sum_less = 0;
        for (int i=0; i<producedRE.length; i++) {
            double sum = spentEnergy[i] - producedRE[i];
            if (sum < 0)
                sum_less += sum * (-1);
            else sum_extra += sum;
        }

        return 0.5 * sum_extra + 0.5 * sum_less;
    }

    ////////////////////////////////    HELPER FUNCTIONS       ////////////////////////////////////////

    public double calculateEnergyDeviationFromProduced(double[] spentEnergy) {
        double energyDeviationFromProduced = 0;
        for (int i=0; i<producedRE.length; i++) {
            double difference = Math.abs(spentEnergy[i] - producedRE[i]);
            energyDeviationFromProduced += difference;
        }
        return energyDeviationFromProduced;
    }

    public double calculateNonREPaid(double[] spentEnergy, XReal costs) throws JMException {

        double sum = 0;

        for (int i=0; i<producedRE.length; i++) {
            double difference = spentEnergy[i] - producedRE[i];
            double abs_difference = Math.abs(difference);
            double cost = costs.getValue(i);
            if (difference > 0)
                sum += abs_difference * cost;
        }

        return sum;
    }

}



