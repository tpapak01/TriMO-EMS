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
import jmetal.metaheuristics.moead.MOEAD;
import jmetal.metaheuristics.trilevel.UpperLevelCostDistr_Fast;
import jmetal.util.JMException;
import jmetal.util.Utils;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XReal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;


public class EnergyDistr extends Problem {

	private static final long serialVersionUID = 1L;
    private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    private static String costsPath = "/Users/emine/IdeaProjects/JMETALHOME/Costs_data/";

    private CostDistr upperLevelProblem;
    private double[] inputCosts = null;

  public EnergyDistr(String renewableFileName, CostDistr upperLevelProblem, String costsName, String dataPath) {
      this.numberOfObjectives_ = 1;
      this.numberOfVariables_ = upperLevelProblem.getNumberOfVariables();
      this.lowerLimit_ = new double[numberOfVariables_];
      this.upperLimit_ = new double[numberOfVariables_];
      for (int i=0; i<upperLimit_.length; i++)
          upperLimit_[i] = 1.0;
      this.upperLevelProblem = upperLevelProblem;


      if (!dataPath.equals("-")) { problemPath = dataPath; costsPath = dataPath; }
      String fileName = problemPath + this.problemName_ + ".txt";
      System.out.println(fileName);
      String costsFileName = "-";
      if (!costsName.equals("-")) costsFileName = costsPath + costsName + ".txt";

      //fills up numberOfItems, p, w, sackCapacity
      //simply read the input textfile
      this.loadProblem(fileName, costsFileName);
      this.solutionType_ = new ArrayRealSolutionType(this);

  }  // 

  public void loadProblem(String renewableFileName, String costsFileName) {

  }

    public double[] getInputCosts(){
        return inputCosts;
    }

	@Override
	public void evaluate(Solution solution) throws JMException {

        SolutionSet upperLevelSolutions = null;
        XReal costOfBuying = new XReal(solution);

        try {
            upperLevelSolutions = UpperLevelCostDistr_Fast.evaluate(costOfBuying, solution);
        } catch (ClassNotFoundException e){
            System.out.println("Exception at LowerLevelMOKP.evaluate: " + e.getMessage());
        }

        int u_size = upperLevelSolutions.size();
        double[] producedRE = upperLevelProblem.getProducedRE();
        //double[] demand = upperLevelProblem.getLowerLevelProblem().getRequestedEnergy();
        Solution chosenUpperLevelSol = upperLevelSolutions.get(0);
        double[] spentEnergy = chosenUpperLevelSol.getSpentEnergy();
        //double[] selfConsDeviation = chosenUpperLevelSol.getEnergyDeviationFromProducedArray();

        //for (int i=0; i<u_size; i++){
            //Solution upperLevelSol = upperLevelSolutions.get(i);
            //selfConsDeviation = upperLevelSol.getEnergyDeviationFromProducedArray();
            /*
            double[] prices = new double[selfConsDeviation.length];
            for (int j=0; j<selfConsDeviation.length; j++){
                double worstSelf = producedRE[j];
                if (demand[j] - producedRE[j] > worstSelf)
                    worstSelf = demand[j] - producedRE[j];
                if (worstSelf == 0) worstSelf = 0.0000001;
                double price = selfConsDeviation[j] / worstSelf;
                if (price > 1) price = 1;
                prices[i] = price;
            }
            solution.setCostOfBuyingEnergy(prices);
            */
        //}

        double result = topLevel_evaluate_objective(producedRE, spentEnergy, costOfBuying);
        double selfDeviation = calculateSelfConsDeviation(producedRE, spentEnergy);
        solution.setSelfConsumption(selfDeviation);

        solution.setObjective(0, result);

        Variable[] vars = chosenUpperLevelSol.getDecisionVariables();
        solution.setUpperLevelVars(vars[0]);
        solution.setUpperLevelObj(chosenUpperLevelSol.getObjective(0));

        solution.setLowerLevelVars(chosenUpperLevelSol.getLowerLevelVars());
        solution.setLowerLevelObj(chosenUpperLevelSol.getLowerLevelObj());
        //solution.setDeviceToPreferenceMapping(chosenUpperLevelSol.getDeviceToPreferenceMapping());
        //solution.setReverseDeviceToPreferenceMapping(chosenUpperLevelSol.getReverseDeviceToPreferenceMapping());

        SolutionSet transferPop = upperLevelSolutions;
        solution.setUL_Transfer_pop(transferPop);


	} // evaluate

    private double calculateSelfConsDeviation(double[] producedRE, double[] spentEnergy){

        double sum = 0;
        for (int i=0; i<producedRE.length; i++) {
            double difference = Math.abs(spentEnergy[i] - producedRE[i]);
            sum += difference;
        }
        return sum;
    }

    private double topLevel_evaluate_objective(double[] producedRE, double[] spentEnergy, XReal costOfBuying) throws JMException {

        double sum = 0;

        /*
        for (int t=0; t<selfConsDeviation.length; t++){
            double selfConstPerc = selfConsPercentage(selfConsDeviation[t], producedRE[t], demand[t]);
            double costPart1 = selfConstPerc * (this.upperLimit_[t] - costOfBuying.getValue(t));
            double costPart2 = (1 - selfConstPerc) * (costOfBuying.getValue(t) - this.lowerLimit_[t]);
            sum += costPart1 + costPart2;
        }

        */

        for (int i=0; i<producedRE.length; i++) {
            double difference = spentEnergy[i] - producedRE[i];
            double abs_difference = Math.abs(difference);
            double cost = costOfBuying.getValue(i);
            if (difference < 0)
                sum += abs_difference * (1.0 + (cost - lowerLimit_[i]));
            else sum += abs_difference * (1.0 + (upperLimit_[i] - cost));
        }

        return sum;
    }

    private double selfConsPercentage(double selfConsDeviation, double producedRE, double demand){
        double worstSelf = producedRE;
        if (demand - producedRE > worstSelf)
            worstSelf = demand - producedRE;
        double selfConsPerc = selfConsDeviation / worstSelf;
        return selfConsPerc;
    }

}



