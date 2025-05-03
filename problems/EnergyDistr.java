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
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.Fast_CostDistr;
import jmetal.metaheuristics.trilevel.UpperLevelCostDistr_Fast;
import jmetal.util.JMException;
import jmetal.util.Utils;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XReal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;


public class EnergyDistr extends Problem {

	private static final long serialVersionUID = 1L;
    private static String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    private static double[] producedRE;
    private static double[] inputCosts = null;

    private static double[] lowerLimitStatic;
    private static double[] upperLimitStatic;

  public EnergyDistr(CostDistr upperLevelProblem, String dataPath) {
      this.numberOfObjectives_ = 1;
      this.numberOfVariables_ = upperLevelProblem.getNumberOfVariables();
      this.lowerLimit_ = new double[numberOfVariables_];
      lowerLimitStatic = new double[numberOfVariables_];
      this.upperLimit_ = new double[numberOfVariables_];
      upperLimitStatic = new double[numberOfVariables_];
      for (int i=0; i<upperLimit_.length; i++) {
          upperLimit_[i] = 1.0;
          upperLimitStatic[i] = 1.0;
      }
      producedRE = CostDistr.getProducedRE();

      if (!dataPath.equals("-")) { problemPath = dataPath; }
      String fileName = problemPath + this.problemName_ + ".txt";
      System.out.println(fileName);

      this.solutionType_ = new ArrayRealSolutionType(this);
  }  //

    public double[] getInputCosts(){
        return inputCosts;
    }

	@Override
	public void evaluate(Solution solution) throws JMException {

        SolutionSet upperLevelSolutions = null;
        XReal costOfBuying = new XReal(solution);

        UpperLevelCostDistr_Fast ul_wrapper = solution.getUl_wrapper();
        Thread thread = solution.getThread();

        if (ul_wrapper.getPopulation() != null) {
            upperLevelSolutions = ul_wrapper.getPopulation();
            System.out.println("Thread " + ul_wrapper.getId() + " completed");
        } else {
            Fast_CostDistr alg_fast = (Fast_CostDistr) ul_wrapper.getAlg_fast();
            upperLevelSolutions = alg_fast.getPopulation();
        }

        Solution best = upperLevelSolutions.get(0);

        int id = ul_wrapper.getId();
        System.out.println(id + ":");
        CostDistr problemCostDistr = ul_wrapper.getProblemCostDistr();
        System.out.println(
                Arrays.toString(problemCostDistr.getCostOfBuying().getArray())
        );
        System.out.println(
                Arrays.toString(problemCostDistr.getLL_wrapper().getProblemMOKP().getCostOfUsage().getArray())
        );
        System.out.println("TL OBJ: " + best.getUpperLevelObj());
        System.out.println("UL OBJ (Profit): " + best.getObjective(0));

        int u_size = upperLevelSolutions.size();
        //double[] demand = upperLevelProblem.getLowerLevelProblem().getRequestedEnergy();
        double[] spentEnergy = best.getSpentEnergy();
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
        System.out.println("SELF: " + selfDeviation);
        System.out.println();

        solution.setObjective(0, result);

        Variable[] vars = best.getDecisionVariables();
        solution.setUpperLevelVars(vars[0]);
        solution.setUpperLevelObj(best.getObjective(0));

        solution.setLowerLevelVars(best.getLowerLevelVars());
        solution.setLowerLevelObj(best.getLowerLevelObj());
        //solution.setDeviceToPreferenceMapping(chosenUpperLevelSol.getDeviceToPreferenceMapping());
        //solution.setReverseDeviceToPreferenceMapping(chosenUpperLevelSol.getReverseDeviceToPreferenceMapping());

        SolutionSet transferPop = new SolutionSet(u_size * 3 / 4);
        for (int i = 0; i < transferPop.getCapacity(); i++) {
            transferPop.add(upperLevelSolutions.get(i)) ;
        }
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

    public static double topLevel_evaluate_objective(double[] producedRE, double[] spentEnergy, XReal costOfBuying) throws JMException {

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
                sum += abs_difference * (1.0 + (cost - lowerLimitStatic[i]));
            else sum += abs_difference * (1.0 + (upperLimitStatic[i] - cost));
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



