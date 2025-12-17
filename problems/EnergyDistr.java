/* Author: Yi Xiang
 * Many-Objective Knapsack Problems
 */



package jmetal.problems;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.ArrayRealSolutionType;
import jmetal.metaheuristics.singleObjective.differentialEvolution.AdaptiveDE_CostDistr;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.Fast_CostDistr;
import jmetal.metaheuristics.trilevel.UpperLevelCostDistr_AdaptiveDE;
import jmetal.metaheuristics.trilevel.UpperLevelCostDistr_Fast;
import jmetal.util.JMException;
import jmetal.util.wrapper.XReal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.StringTokenizer;


public class EnergyDistr extends Problem {

	private static final long serialVersionUID = 1L;
    private static String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    private static String costsPath = "/Users/emine/IdeaProjects/JMETALHOME/Costs_data/";
    private static double[] producedRE;
    private static double[] inputCosts = null;

    private static double[] lowerLimitStatic;
    private static double[] upperLimitStatic;

    public static double TL_upperLimit = 0.28;
    public static int[] numOfActiveGeneratorsPerTime;
    public static double baseGenerationCosts;
    public final static int numOfGenerators = 8;
    public final static int genDataDivisor = 10;

    public static double[] Pmin;
    public static double[] Pmax;
    public static double[] c_no_load;
    public static double[] c_linear;
    public static double[] c_start_hot;
    public static double[] c_start_cold;

    public static double[] PenPool = new double[] {100, 500, 1000};
    private static int penaltyFlag = 0;
    public void setPenaltyFlag(int flag){
        penaltyFlag = flag;
    }
    private static boolean useADEforUpper;

  public EnergyDistr(CostDistr upperLevelProblem, String dataPath, String costsName, String geneName, boolean inputUseADEforUpper) throws IOException {
      this.numberOfObjectives_ = 1;
      this.numberOfVariables_ = upperLevelProblem.getNumberOfVariables();
      this.lowerLimit_ = new double[numberOfVariables_];
      lowerLimitStatic = new double[numberOfVariables_];
      /*
      for (int i=0; i<lowerLimit_.length; i++) {
          lowerLimit_[i] = -1.0;
          lowerLimitStatic[i] = -1.0;
      }
      */
      this.upperLimit_ = new double[numberOfVariables_];
      upperLimitStatic = new double[numberOfVariables_];
      for (int i=0; i<upperLimit_.length; i++) {
          upperLimit_[i] = TL_upperLimit;
          upperLimitStatic[i] = TL_upperLimit;
      }
      producedRE = CostDistr.getProducedRE();

      if (!dataPath.equals("-")) { problemPath = dataPath; costsPath = dataPath; }
      String fileName = problemPath + this.problemName_ + ".txt";
      System.out.println(fileName);
      String costsFileName = "-";
      if (!costsName.equals("-")) costsFileName = costsPath + costsName;
      String generatorsName = "-";
      if (!geneName.equals("-")) generatorsName = problemPath + geneName;

      this.solutionType_ = new ArrayRealSolutionType(this);

      ////////////////////////////////////////////////////////////////

      if (!costsFileName.equals("-")) {
          inputCosts = new double[this.numberOfVariables_];
          BufferedReader in = new BufferedReader(new FileReader(costsFileName + "_1" + ".txt" ));
          String line;

          for (int i = 0; i < this.numberOfVariables_; i++) {
              line = in.readLine();
              inputCosts[i] = Double.parseDouble(line);
          }

          in.close();
      }

      c_linear = new double[this.numberOfVariables_];
      if (!generatorsName.equals("-")) {
          numOfActiveGeneratorsPerTime = new int[this.numberOfVariables_];
          Pmin = new double[this.numberOfVariables_];
          Pmax = new double[this.numberOfVariables_];
          c_no_load = new double[this.numberOfVariables_];
          c_start_hot = new double[this.numberOfVariables_];
          c_start_cold = new double[this.numberOfVariables_];
          for (int i=0; i<this.numberOfVariables_; i++) {
              numOfActiveGeneratorsPerTime[i] = 2;
          }

          BufferedReader in = new BufferedReader(new FileReader(generatorsName + ".txt" ));
          String line;
          in.readLine();

          for (int i=0; i<numOfGenerators; i++) {
              line = in.readLine();
              StringTokenizer tokenizer = new StringTokenizer(line, " ");
              Pmin[i] = Double.parseDouble(tokenizer.nextToken()) / genDataDivisor;
              Pmax[i] = Double.parseDouble(tokenizer.nextToken()) / genDataDivisor;
              c_no_load[i] = Double.parseDouble(tokenizer.nextToken()); // genDataDivisor;
              c_linear[i] = Double.parseDouble(tokenizer.nextToken()); // genDataDivisor;
              c_start_hot[i] = Double.parseDouble(tokenizer.nextToken()); // genDataDivisor;
              c_start_cold[i] = Double.parseDouble(tokenizer.nextToken()); // genDataDivisor;
          }

          baseGenerationCosts = calculateBaseGenerationCosts(numOfActiveGeneratorsPerTime, Pmin, c_no_load, c_linear);
      } else {
          for (int i=0; i<this.numberOfVariables_; i++) {
              c_linear[i] = 0.04;
          }
      }

      useADEforUpper = inputUseADEforUpper;
  }

    public double[] getInputCosts(){
        return inputCosts;
    }

	@Override
	public void evaluate(Solution solution) throws JMException {

        SolutionSet upperLevelSolutions = null;
        XReal costOfBuying = new XReal(solution);

        CostDistr problemCostDistr;
        if (useADEforUpper){
            UpperLevelCostDistr_AdaptiveDE ul_wrapper_ade = solution.getUl_wrapper_ade();
            if (ul_wrapper_ade.getPopulation() != null) {
                upperLevelSolutions = ul_wrapper_ade.getPopulation();
                System.out.println("Thread " + ul_wrapper_ade.getId() + " completed");
            } else {
                AdaptiveDE_CostDistr alg_fast = (AdaptiveDE_CostDistr) ul_wrapper_ade.getAlg_fast();
                upperLevelSolutions = alg_fast.getPopulation();
            }
            int id = ul_wrapper_ade.getId();
            System.out.println(id + ":");
            problemCostDistr = ul_wrapper_ade.getProblemCostDistr();
        } else {
            UpperLevelCostDistr_Fast ul_wrapper = solution.getUl_wrapper();
            if (ul_wrapper.getPopulation() != null) {
                upperLevelSolutions = ul_wrapper.getPopulation();
                System.out.println("Thread " + ul_wrapper.getId() + " completed");
            } else {
                Fast_CostDistr alg_fast = (Fast_CostDistr) ul_wrapper.getAlg_fast();
                upperLevelSolutions = alg_fast.getPopulation();
            }
            int id = ul_wrapper.getId();
            System.out.println(id + ":");
            problemCostDistr = ul_wrapper.getProblemCostDistr();
        }

        Solution best = upperLevelSolutions.get(0);
        System.out.println(
                Arrays.toString(problemCostDistr.getCostOfBuying().getArray())
        );
        System.out.println(
                Arrays.toString(problemCostDistr.getLL_wrapper().getProblemMOKP().getCostOfUsage().getArray())
        );
        System.out.println("UL OBJ: " + best.getUpperLevelObj());
        System.out.println("UL OBJ (Profit): " + best.getObjective(0));

        int u_size = upperLevelSolutions.size();
        //double[] demand = upperLevelProblem.getLowerLevelProblem().getRequestedEnergy();
        double[] spentEnergy = best.getSpentEnergy();
        solution.setSpentEnergy(spentEnergy);

        double[] difference = new double[24];
        DecimalFormat df = new DecimalFormat("#.##");
        for (int i=0; i<24; i++) {
            difference[i] = producedRE[i] - spentEnergy[i];
            difference[i] = Double.parseDouble(df.format(difference[i]));
        }
        System.out.println(
                Arrays.toString(difference)
        );

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

        // set constraints
        double contraint = setConstraint(producedRE, spentEnergy, costOfBuying, solution);
        solution.setReimbuPenalty(contraint);
        System.out.println("REIMBU: " + solution.getReimbu());
        System.out.println("ALLOWED REIMBU: " + (solution.getReimbu() - solution.getReimbuPenalty()));

        // TL objective value and self-consumption
        double result = topLevel_evaluate_objective(producedRE, spentEnergy, costOfBuying, contraint);
        solution.setObjective(0, result);
        double selfConsumption = calculateSelfConsDeviation(producedRE, spentEnergy);
        solution.setSelfConsumption(selfConsumption);
        System.out.println("SELF: " + selfConsumption);
        System.out.println();

        // UL objective value and profit
        Variable[] vars = best.getDecisionVariables();
        solution.setUpperLevelVars(vars[0]);
        solution.setUpperLevelObj(best.getObjective(0));
        solution.setProfit(best.getObjective(0));

        // LL objective value
        solution.setLowerLevelVars(best.getLowerLevelVars());
        solution.setLowerLevelObj(best.getLowerLevelObj());
        // LL platform-only
        solution.setLL_Pareto_pop(best.getLL_Pareto_pop());
        //solution.setDeviceToPreferenceMapping(chosenUpperLevelSol.getDeviceToPreferenceMapping());
        //solution.setReverseDeviceToPreferenceMapping(chosenUpperLevelSol.getReverseDeviceToPreferenceMapping());

        SolutionSet transferPop = new SolutionSet(u_size * 3 / 4);
        for (int i = 0; i < transferPop.getCapacity(); i++) {
            transferPop.add(upperLevelSolutions.get(i)) ;
        }
        solution.setUL_Transfer_pop(transferPop);


	} // evaluate

    public static double calculateSelfConsDeviation(double[] producedRE, double[] spentEnergy){

        double sum = 0;
        for (int i=0; i<producedRE.length; i++) {
            double freeEnergyLeft = producedRE[i] - spentEnergy[i];
            if (freeEnergyLeft > 0)
                sum += freeEnergyLeft;
        }
        return sum;
    }

    public static double setConstraint(double[] producedRE, double[] spentEnergy, XReal costOfBuying, Solution solution) throws JMException {
        //double realTimeGenerationCosts = calculateRealTimeGenerationCosts(numOfActiveGeneratorsPerTime, Pmin, Pmax, c_linear, producedRE, spentEnergy);
        //double requiredGenerationPayment = baseGenerationCosts + realTimeGenerationCosts;
        double penalty = 0;
        double allowedReimbursement = 0;
        double requiredGenerationPayment = calculateMCPRequiredCosts(numOfActiveGeneratorsPerTime, c_linear, producedRE, spentEnergy);
        double income = calculateIncome(producedRE, spentEnergy, costOfBuying);
        if (income < requiredGenerationPayment)
            penalty = requiredGenerationPayment - income;
        else
            allowedReimbursement = income - requiredGenerationPayment;
        double reimbursement = calculateReimbursement(producedRE, spentEnergy, costOfBuying);
        if (reimbursement - allowedReimbursement > 0)
            penalty = penalty + (reimbursement - allowedReimbursement);

        solution.setReimbu(reimbursement);
        return penalty;
    }

    public static double topLevel_evaluate_objective(double[] producedRE, double[] spentEnergy, XReal costOfBuying,
                  double penalty) throws JMException {

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
            double freeEnergyLeft = producedRE[i] - spentEnergy[i];
            double cost = costOfBuying.getValue(i);
            if (freeEnergyLeft > 0) {
                if (penalty > 0)
                    sum += ( freeEnergyLeft * (1.0 + (cost - lowerLimitStatic[i])))
                            + ( penalty * (cost - lowerLimitStatic[i]) ); //lower price to reduce compensation
                else
                    sum += freeEnergyLeft * (1.0 + (upperLimitStatic[i] - cost)); //raise price if there is RE left
            }
            else {
                if (penalty > 0)
                    sum += (upperLimitStatic[i] - cost) * penalty; // raise price to accommodate for compensation and income costs
                else
                    sum += cost - lowerLimitStatic[i]; // lower price if you spent all RE
            }
        }

        return sum; // + (reimbuPenalty + incomePenalty) * PenPool[penaltyFlag];
    }

    private static double calculateBaseGenerationCosts(int[] numOfActiveGeneratorsPerTime, double[] Pmin, double[] c_no_load, double[] c_linear) {
        double sum = 0;
        for (int i=0; i<numOfActiveGeneratorsPerTime.length; i++){
            for (int j=0; j<numOfActiveGeneratorsPerTime[i]; j++) {
                sum += c_no_load[j] + (Pmin[j] * c_linear[j]);
            }
        }
        return sum;
    }

    private static double calculateRealTimeGenerationCosts(int[] numOfActiveGeneratorsPerTime, double[] Pmin, double[] Pmax, double[] c_linear, double[] producedRE, double[] spentEnergy) {
      double minCosts = 0;
        for (int i=0; i<numOfActiveGeneratorsPerTime.length; i++){
            double gridEnergy = spentEnergy[i] - producedRE[i];
            int lastGenIndex = numOfActiveGeneratorsPerTime[i] - 1;
            // part 1 - calculate the non-base costs for minRequiredIncome
            if (gridEnergy > 0) {
                for (int j=0; j<=lastGenIndex; j++) {
                    double nonBaseEnergyToPay = gridEnergy - Pmin[j];
                    if (Pmax[j] - gridEnergy >= 0) {
                        if (nonBaseEnergyToPay > 0)
                            minCosts += nonBaseEnergyToPay * c_linear[j];
                        break;
                    } else {
                        minCosts += (Pmax[j] - Pmin[j]) * c_linear[j];
                        gridEnergy = gridEnergy - Pmax[j];
                    }
                }
            }
        }
        return minCosts;
    }

    private static double calculateMCPRequiredCosts(int[] numOfActiveGeneratorsPerTime, double[] c_linear, double[] producedRE, double[] spentEnergy) {
        double minCosts = 0;
        /*
        for (int i=0; i<numOfActiveGeneratorsPerTime.length; i++){
            double gridEnergy = spentEnergy[i] - producedRE[i];
            int lastGenIndex = numOfActiveGeneratorsPerTime[i] - 1;
            if (gridEnergy > 0) {
                minCosts += gridEnergy * c_linear[lastGenIndex];
            }
        }
        */
        for (int i=0; i<c_linear.length; i++){
            double gridEnergy = spentEnergy[i] - producedRE[i];
            if (gridEnergy > 0) {
                minCosts += gridEnergy * c_linear[i];
            }
        }
        return minCosts;
    }

    private static double calculateReimbursement(double[] producedRE, double[] spentEnergy, XReal costOfBuying) throws JMException {
        double sum = 0;
        for (int i=0; i<producedRE.length; i++){
            double freeEnergy = producedRE[i];
            if (spentEnergy[i] < producedRE[i]) {
                freeEnergy = spentEnergy[i];
            }
            sum += freeEnergy * costOfBuying.getValue(i);
        }
        return sum;
    }

    private static double calculateIncome(double[] producedRE, double[] spentEnergy, XReal costOfBuying) throws JMException {
        double sum = 0;
        for (int i=0; i<producedRE.length; i++){
            double gridEnergy = spentEnergy[i] - producedRE[i];
            if (gridEnergy > 0) {
                sum += gridEnergy * costOfBuying.getValue(i);
            }
        }
        return sum;
    }

}



