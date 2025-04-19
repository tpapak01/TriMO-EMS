/* Author: Yi Xiang
 * Many-Objective Knapsack Problems
 */



package jmetal.problems;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.ArrayRealSolutionType;
import jmetal.encodings.variable.ArrayReal;
import jmetal.encodings.variable.Binary;
import jmetal.metaheuristics.trilevel.LowerLevelMOKP_MOEAD;
import jmetal.metaheuristics.moead.MOEAD;
import jmetal.util.JMException;
import jmetal.util.RankingOnlyFirst;
import jmetal.util.Utils;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XReal;

import java.io.*;
import java.util.Comparator;


public class CostDistr extends Problem {

	private static final long serialVersionUID = 1L;
    private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    private static String costsPath = "/Users/emine/IdeaProjects/JMETALHOME/Costs_data/";

    private MOKP_Problem lowerLevelProblem;
    private String lowerLevelAlgorithmName;
    private double[] producedRE;
    private double[] inputCosts = null;
    private double totalProducedRE;
    private double ULObjectiveDesirability = 1.0;
    private boolean fixedTrust = true;

    private XReal costOfBuying ; // capacity of each  knapsack .

    private static double best_upper_level_result = Double.MAX_VALUE;
    private static Comparator comparator;

    public MOKP_Problem getLowerLevelProblem(){
        return lowerLevelProblem;
    }

    public double[] getProducedRE(){
        return producedRE;
    }

    public double[] getInputCosts(){
        return inputCosts;
    }

    public double getTotalProducedRE(){
        return totalProducedRE;
    }

    public void setCostOfBuying(XReal cost) {
        costOfBuying = cost;
    }

    public void setCostLowerLimit(XReal cost) throws JMException {
        for (int i=0; i<cost.size(); i++)
            this.lowerLimit_[i] = cost.getValue(i);
    }

  public CostDistr(String renewableFileName, MOKP_Problem lowerLevelProblem, String lowerLevelAlgorithmName, String costsName, String dataPath) {
	  this.setMaxmized_(false); // this problem is not to be maximized
	  this.problemName_ = renewableFileName;
	  this.lowerLevelAlgorithmName = lowerLevelAlgorithmName;
      this.numberOfVariables_ = lowerLevelProblem.getNumberOfConstraints();
      this.numberOfObjectives_ = 1;
      this.lowerLimit_ = new double[numberOfVariables_];
      this.upperLimit_ = new double[numberOfVariables_];
      for (int i=0; i<upperLimit_.length; i++)
          upperLimit_[i] = 1.0;
      producedRE = new double[numberOfVariables_];
      this.lowerLevelProblem = lowerLevelProblem;

      if (!dataPath.equals("-")) { problemPath = dataPath; costsPath = dataPath; }
      String fileName = problemPath + this.problemName_ + ".txt";
      System.out.println(fileName);
      String costsFileName = "-";
      if (!costsName.equals("-")) costsFileName = costsPath + costsName + ".txt";

      //fills up numberOfItems, p, w, sackCapacity
      //simply read the input textfile
      this.loadProblem(fileName, costsFileName);
      this.solutionType_ = new ArrayRealSolutionType(this);
      if (this.lowerLevelProblem.isMaxmized())
          comparator = new ObjectiveComparator(0, false) ; // Single objective comparator
      else comparator = new ObjectiveComparator(0, true) ; // Single objective comparator

  }  // 

  public void loadProblem(String renewableFileName, String costsFileName) {

      try {
          BufferedReader in = new BufferedReader(new FileReader(renewableFileName));
          String line;

          for (int i = 0; i < this.numberOfVariables_; i++) {
              line = in.readLine();
              producedRE[i] = Double.parseDouble(line);
              totalProducedRE += producedRE[i];
          }

          in.close();

          ////////////////////////////////////////////////////////////////

          if (!costsFileName.equals("-")) {
              inputCosts = new double[this.numberOfVariables_];
              in = new BufferedReader(new FileReader(costsFileName));

              for (int i = 0; i < this.numberOfVariables_; i++) {
                  line = in.readLine();
                  inputCosts[i] = Double.parseDouble(line);
              }

              in.close();
          }

      } catch (IOException e){
          System.out.println("Error reading MOKP problemFile: " + e.getMessage());
      }

  }
    public void repair(Solution solution) throws JMException {
        ArrayReal costs = ((ArrayReal)(solution.getDecisionVariables()[0]));
        for (int i=0; i<costs.getLength(); i++){
            if (costs.getValue(i) < lowerLimit_[i]) {
                costs.setValue(i, lowerLimit_[i]);
            }
        }
    }

	@Override
	public void evaluate(Solution solution) throws JMException {

        SolutionSet lowerLevelSolutions = null;
        XReal costs = new XReal(solution);

        try {
            if (this.lowerLevelAlgorithmName.equals("MOEAD"))
                lowerLevelSolutions = LowerLevelMOKP_MOEAD.evaluate(costs, solution);
        } catch (ClassNotFoundException e){
            System.out.println("Exception at LowerLevelMOKP.evaluate: " + e.getMessage());
        }

        int lsize = lowerLevelSolutions.size();

        //Identify optimistic,pessimistic and LL-desirable solution
        double best_self = Double.MAX_VALUE;
        int optimistic_index = -1;
        double pessimistic_self = -Double.MAX_VALUE;
        int pessimistic_index = -1;
        double[] target_desirability = new double[this.lowerLevelProblem.getNumberOfObjectives()];
        target_desirability[0] = this.lowerLevelProblem.getObjectiveDesirability();
        target_desirability[1] = 1 - target_desirability[0];
        //double[] nadirObjectiveValue = this.lowerLevelProblem.getNadirObjectiveValue();
        double best_desirability = 100;
        int best_desirability_index = -1;

        if (lsize == 0){
            int a = 0;
        }

        for (int s=0; s<lsize; s++) {
            Solution lowerLevelSol = lowerLevelSolutions.get(s);

            // do upper-level evaluation = finding deviation from available RE
            double[] energySpent = lowerLevelSol.getSpentEnergy();
            //XReal costOfBuying = this.costOfBuying;
            double result = upperLevel_evaluate_profit(energySpent, costs);
            //double result = upperLevel_evaluate_XOR_distance_plus_weight(energySpent, costs);
            lowerLevelSol.setSelfConsumption(result);
            if (result < best_self){
                best_self = result;
                optimistic_index = s;
            }
            if (result > pessimistic_self){
                pessimistic_self = result;
                pessimistic_index = s;
            }

            ////register Deviation from Produced - used for Platform only
            //double deviation = calculateEnergyDeviationFromProduced(energySpent);
            //lowerLevelSol.setEnergyDeviationFromProduced(deviation);

            // LL-DECISION-MAKING: identity solution that is closest to the desired.
            // 0.0: Poor: Full dissatisfaction, zero costs
            // 1.0: Rich: Zero dissatisfaction, full costs
            double desirability;
            if (lowerLevelSol.getLambda() == null){
                double position = s / (double) lsize;
                desirability = Math.abs(target_desirability[0] - position);
            } else
                desirability = Math.abs(target_desirability[0] - lowerLevelSol.getLambda()[0]);
            if (desirability < best_desirability){
                best_desirability = desirability;
                best_desirability_index = s;
            }
        }

        // LL-DECISION-MAKING: Create limits "worst_self" and "worst_des" to later identify set of best solutions in 2D space
        Solution optimisticSol = lowerLevelSolutions.get(optimistic_index);
        Solution pessimisticSol = lowerLevelSolutions.get(pessimistic_index);
        Solution bestDesSol = lowerLevelSolutions.get(best_desirability_index);
        //how much is the S of the best-DESIRABILITY-solution?
        double worst_self = bestDesSol.getSelfConsumption();
        //how much is the DESIRABILITY of the best-S-solution?
        double worst_des = Utils.ASF(optimisticSol, bestDesSol, target_desirability);

        SolutionSet transferPareto = null;
        SolutionSet decisionPareto = new SolutionSet(lsize);
        if (this.lowerLevelAlgorithmName.equals("MOEAD")) {

            MOEAD algo = (MOEAD) LowerLevelMOKP_MOEAD.algorithm;
            int popSize = LowerLevelMOKP_MOEAD.popSize;
            transferPareto = new SolutionSet(popSize);

            //create temporary external that is sorted by objective
            SolutionSet newExternal = new SolutionSet(lsize);
            for (int i = 0; i < lsize; i++)
                newExternal.add(lowerLevelSolutions.get(i));
            newExternal.sort(comparator);

            double[][] lambda = algo.getLambda_();

            //create archive from temporary external
            SolutionSet archive = new SolutionSet(popSize);
            int exIdx = 0;
            Solution externalToAdd = newExternal.get(exIdx);

            Solution newSol = new Solution(externalToAdd, lambda[0]);
            archive.add(newSol);

            Solution currentArchiveSol = newSol;
            exIdx++;
            externalToAdd = newExternal.get(exIdx);
            int i = 1;
            while (archive.size() < popSize){
                double f1 = algo.fitnessFunction(currentArchiveSol, lambda[i]);
                double f2 = algo.fitnessFunction(externalToAdd, lambda[i]);
                // if f2 smaller than f1, f2 (externalToAdd) is better
                // so select that solution x for TP consideration (for the same λ) in the next round
                // and move to the next solution from the EP
                // almost certainly, x will be added to the TP in the very next round...
                if (f2 < f1 || (f2 == f1 && exIdx < lsize)) {
                    currentArchiveSol = externalToAdd;
                    exIdx++;
                    if (exIdx < lsize)
                        externalToAdd = newExternal.get(exIdx);
                } else {
                    //copy same solution as before (but next λ) to the next TP position
                    newSol = new Solution(currentArchiveSol, lambda[i]);
                    archive.add(newSol);
                    i++;
                }
            }

            // LL-DECISION-MAKING: 2D DECISION MAKING SPACE
            for (int s = 0; s < lsize; s++) {
                Solution lowerLevelSol = lowerLevelSolutions.get(s);
                double DIM1 = lowerLevelSol.getSelfConsumption();
                double DIM2_norm = Utils.ASF(lowerLevelSol, bestDesSol, target_desirability);

                //Use below to find solutions of 2D decision-making space
                if (DIM1 <= worst_self &&
                        DIM2_norm <= worst_des) {
                    decisionPareto.add(lowerLevelSol);
                }
            }

            // TRANSFER: d = 5: The standard case in the paper
            int d = 5;
            for (i = 0; i < popSize; i = i + d) {
                Solution lowerLevelSol = archive.get(i);
                transferPareto.add(lowerLevelSol);
            }
        }

        // find best solution given UL preferences (optimistic OR LL-desirable OR in between)
        // 0.0: Full care for residents
        // 1.0: Full care for self-consumption S
        Solution chosenlowerLevelSol = null;
        if (ULObjectiveDesirability == 1.0) {
            // OPTIMISTIC APPROACH
            solution.setObjective(0, best_self);
            chosenlowerLevelSol = optimisticSol;
        } else if (ULObjectiveDesirability == 0.0){
            // RESIDENT-AWARE APPROACH
            solution.setObjective(0, worst_self);
            chosenlowerLevelSol = bestDesSol;
        } else if (ULObjectiveDesirability == -1.0) {
            // PESSIMISTIC APPROACH
            solution.setObjective(0, pessimistic_self);
            chosenlowerLevelSol = pessimisticSol;
        } else {

            int best_overall_index = -1;
            // FIXED COOP APPROACH
            if (fixedTrust) {
                if (solution.getUL_Optimism() == -1)
                    solution.setUL_Optimism(ULObjectiveDesirability);
                double q = solution.getUL_Optimism();
                best_overall_index = bestFromDecisionPareto(decisionPareto, q,
                        worst_self, best_self, bestDesSol, target_desirability, optimisticSol);
            }
            // DYNAMIC COOP APPROACH
            else {

                //if not an offspring (inheriting q), then start at q = 0
                if (solution.getUL_Optimism() == -1)
                    solution.setUL_Optimism(0.0);

                double difference = 0.01;
                boolean skipOther = false;

                //----------------------------------------------------

                {
                    double q = solution.getUL_Optimism();
                    // retrieve best solution given q, then find UL and LL score
                    int previousBestIndex = bestFromDecisionPareto(decisionPareto, q,
                            worst_self, best_self, bestDesSol, target_desirability, optimisticSol);
                    double previousDIM1 = get_DIM1_norm(worst_self, best_self, decisionPareto.get(previousBestIndex));
                    double previousDIM2 = Utils.AchievementScalarizationTcheby(decisionPareto.get(previousBestIndex), bestDesSol, target_desirability, optimisticSol);

                    double q_left = q + difference;
                    // keep moving q up until new best solution (leftIndex) is found
                    int leftIndex = -1;
                    do {
                        if (q_left >= 1) {
                            q_left = 1.0;
                            break;
                        }
                        leftIndex = bestFromDecisionPareto(decisionPareto, q_left,
                                worst_self, best_self, bestDesSol, target_desirability, optimisticSol);
                        q_left = q_left + difference;
                    } while (leftIndex == previousBestIndex);

                    // if new solution was found (q was not already at 1+)...
                    if (leftIndex != -1) {
                        // get UL and LL score of new solution
                        double leftDIM1 = get_DIM1_norm(worst_self, best_self, decisionPareto.get(leftIndex));
                        double leftDIM2 = Utils.AchievementScalarizationTcheby(decisionPareto.get(leftIndex), bestDesSol, target_desirability, optimisticSol);

                        // find UL gain and LL loss
                        double leftDiffDIM1 = previousDIM1 - leftDIM1;
                        double leftDiffDIM2 = previousDIM2 - leftDIM2;

                        if (leftDiffDIM2 > 0) {
                            int impossible = 1; // you can't move q up and have a better LL score than before
                        }

                        // fairness principle: LL loss < UL gain
                        // if fairness principle holds, you won't go the other direction next (q down)
                        if (Math.abs(leftDiffDIM2) < leftDiffDIM1)
                            skipOther = true;

                        // while fairness principle holds or while moving q has not resulted in a new best solution
                        while (Math.abs(leftDiffDIM2) < leftDiffDIM1 || leftIndex == previousBestIndex) {
                            // set currently best solution as the previous "new best"
                            previousDIM1 = leftDIM1;
                            previousDIM2 = leftDIM2;
                            previousBestIndex = leftIndex;

                            // save current q for extraction later (to set it)
                            q = q_left;
                            // and quit if q goes above 1 (the saved q is definitely below <= 1)
                            q_left = q_left + difference;
                            if (q_left > 1) {
                                break;
                            }

                            // get new best solution (might be same as before) based on new q
                            leftIndex = bestFromDecisionPareto(decisionPareto, q_left,
                                    worst_self, best_self, bestDesSol, target_desirability, optimisticSol);
                            // get UL and LL score
                            leftDIM1 = get_DIM1_norm(worst_self, best_self, decisionPareto.get(leftIndex));
                            leftDIM2 = Utils.AchievementScalarizationTcheby(decisionPareto.get(leftIndex), bestDesSol, target_desirability, optimisticSol);

                            // find UL gain and LL loss
                            leftDiffDIM1 = previousDIM1 - leftDIM1;
                            leftDiffDIM2 = previousDIM2 - leftDIM2;
                        }

                        // if you won't go the other direction next, set q of examined solution
                        if (skipOther)
                            solution.setUL_Optimism(q);
                    }
                }

                //----------------------------------------------------------------------------------

                // if the previous checks decided you WILL go the other direction (q down)...
                if (skipOther == false)
                {
                    double q = solution.getUL_Optimism();
                    // retrieve best solution given q, then find UL and LL score
                    int previousBestIndex = bestFromDecisionPareto(decisionPareto, q,
                            worst_self, best_self, bestDesSol, target_desirability, optimisticSol);
                    double previousDIM1 = get_DIM1_norm(worst_self, best_self, decisionPareto.get(previousBestIndex));
                    double previousDIM2 = Utils.AchievementScalarizationTcheby(decisionPareto.get(previousBestIndex), bestDesSol, target_desirability, optimisticSol);

                    double q_right = q - difference;
                    int rightIndex = -1;
                    // keep moving q down until new best solution (rightIndex) is found
                    do {
                        if (q_right <= 0) {
                            q_right = 0.0;
                            break;
                        }
                        rightIndex = bestFromDecisionPareto(decisionPareto, q_right,
                                worst_self, best_self, bestDesSol, target_desirability, optimisticSol);
                        q_right = q_right - difference;
                    } while (rightIndex == previousBestIndex);

                    // if new solution was found (q was not already at 0-)...
                    if (rightIndex != -1) {
                        // get UL and LL score of new solution
                        double rightDIM1 = get_DIM1_norm(worst_self, best_self, decisionPareto.get(rightIndex));
                        double rightDIM2 = Utils.AchievementScalarizationTcheby(decisionPareto.get(rightIndex), bestDesSol, target_desirability, optimisticSol);

                        // find UL loss and LL gain
                        double rightDiffDIM1 = previousDIM1 - rightDIM1;
                        double rightDiffDIM2 = previousDIM2 - rightDIM2;

                        if (rightDiffDIM1 > 0) {
                            int impossible = 1; // you can't move q down and have a better UL score than before
                        }

                        // fairness principle: LL gain > UL loss
                        // while fairness principle holds or while moving q has not resulted in a new best solution
                        while (Math.abs(rightDiffDIM1) < rightDiffDIM2 || rightIndex == previousBestIndex) {
                            // set currently best solution as the previous "new best"
                            previousDIM1 = rightDIM1;
                            previousDIM2 = rightDIM2;
                            previousBestIndex = rightIndex;

                            // save current q for extraction later (to set it)
                            q = q_right;
                            // and quit if q goes below 0 (the saved q is definitely above >= 0)
                            q_right = q_right - difference;
                            if (q_right < 0) {
                                break;
                            }
                            // get new best solution (might be same as before) based on new q
                            rightIndex = bestFromDecisionPareto(decisionPareto, q_right,
                                    worst_self, best_self, bestDesSol, target_desirability, optimisticSol);
                            // get UL and LL score
                            rightDIM1 = get_DIM1_norm(worst_self, best_self, decisionPareto.get(rightIndex));
                            rightDIM2 = Utils.AchievementScalarizationTcheby(decisionPareto.get(rightIndex), bestDesSol, target_desirability, optimisticSol);

                            // find UL loss and LL gain
                            rightDiffDIM1 = previousDIM1 - rightDIM1;
                            rightDiffDIM2 = previousDIM2 - rightDIM2;
                        }
                        // set q of examined solution
                        solution.setUL_Optimism(q);
                    }
                }

                //----------------------------------------------------------------------------------


                //if (q < 1)
                //    System.out.println("-----q: " + q);
                //solution.setUL_Optimism(q);

                // UL-DECISION-MAKING: find best solution from 2D space given UL preferences (non-optimistic and non-LL-desirable)
                best_overall_index = bestFromDecisionPareto(decisionPareto, solution.getUL_Optimism(),
                        worst_self, best_self, bestDesSol, target_desirability, optimisticSol);
            }

            chosenlowerLevelSol = decisionPareto.get(best_overall_index);
            solution.setObjective(0, chosenlowerLevelSol.getSelfConsumption());
        }

        Variable[] vars = chosenlowerLevelSol.getDecisionVariables();
        Binary bin = (Binary) vars[0];

        double[] energySpent = chosenlowerLevelSol.getSpentEnergy();
        solution.setSpentEnergy(energySpent);
        solution.setLowerLevelVars(bin);
        solution.setLowerLevelObj(new double[] {chosenlowerLevelSol.getObjective(0), chosenlowerLevelSol.getObjective(1)});
        //solution.setDissatisfactionPerUser(chosenlowerLevelSol.getDissatisfactionPerUser());
        //solution.setEnergyAllocatedPerUser(chosenlowerLevelSol.getEnergyAllocatedPerUser());
        //double deviation = calculateEnergyDeviationFromProduced(energySpent);
        //solution.setEnergyDeviationFromProduced(deviation);

        //double[] deviationArray = calculateEnergyDeviationFromProducedArray(energySpent);
        //solution.setEnergyDeviationFromProducedArray(deviationArray);

        //double nonREpaid = calculateNonREPaid(energySpent, costs);
        //solution.setNonREpaid(nonREpaid);
        //solution.setDeviceToPreferenceMapping(chosenlowerLevelSol.getDeviceToPreferenceMapping());
        //solution.setReverseDeviceToPreferenceMapping(chosenlowerLevelSol.getReverseDeviceToPreferenceMapping());
        solution.setLL_Transfer_pop(transferPareto);
        //platform only
        //solution.setLL_Pareto_pop(lowerLevelSolutions);

        if (best_upper_level_result > best_self) {
            best_upper_level_result = best_self;

            System.out.println(best_upper_level_result);

            /*
            SolutionSet chosenSolutionSet = new SolutionSet(1);
            chosenSolutionSet.add(chosenlowerLevelSol);
            chosenSolutionSet.printObjectivesToFile("LowerLevelParetoVisual/Misplacement/" + (fileID) + "_CHOSEN");

	        SolutionSet upperLevelSet = new SolutionSet(1);
            upperLevelSet.add(solution);
            upperLevelSet.printObjectivesToFile("LowerLevelParetoVisual/Misplacement/" + (fileID) + "_SELF");

            Ranking finalRanking = new Ranking(lowerLevelSolutions);
            SolutionSet finalParetoFront = finalRanking.getSubfront(0);
            finalParetoFront.printObjectivesToFile("LowerLevelParetoVisual/Misplacement/" + (fileID) + "_FUN"); //check
            fileID++;
            
             */

        }

        //System.out.println(best_result);
        //System.out.println(solution.getDecisionVariables()[0]);

	} // evaluate

    /*
     * Retrieve best solution from decision-making reaction set given the cooperation index q
     */
    private int bestFromDecisionPareto(SolutionSet decisionPareto, double q,
                                          double worst_self, double best_self, Solution bestDesSol, double[] target_desirability, Solution optimisticSol){

      double best_overall = Double.MAX_VALUE;
        int best_overall_index = -1;
        for (int s=0; s<decisionPareto.size(); s++) {
            Solution sol = decisionPareto.get(s);
            double DIM1_norm = get_DIM1_norm(worst_self, best_self, sol);
            double DIM2_norm = Utils.AchievementScalarizationTcheby(sol, bestDesSol, target_desirability, optimisticSol);
            //System.out.println("DIM: " + DIM1_norm + " " + DIM2_norm);

            double evaluation = (q * DIM1_norm) + ((1-q)*DIM2_norm);
            if (evaluation < best_overall){
                best_overall = evaluation;
                best_overall_index = s;
            }
        }
        return best_overall_index;
    }

    private double get_DIM1_norm(double worst_self, double best_self, Solution sol){
        double DIM1_denominator = worst_self - best_self;
        if (DIM1_denominator == 0)
            return 0;
        return (sol.getSelfConsumption() - best_self) / DIM1_denominator;
    }

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

    public double upperLevel_evaluate_profit_simple(double[] spentEnergy, XReal costs, XReal costOfBuying) throws JMException {

        double sum = 0;

        for (int i=0; i<producedRE.length; i++) {
            double buyingCosts = costOfBuying.getValue(i) * spentEnergy[i];
            double sellingCosts = costs.getValue(i) * spentEnergy[i];
            double loss = buyingCosts - sellingCosts;
            sum += loss;
        }

        return sum;
    }

    public double upperLevel_evaluate_profit(double[] spentEnergy, XReal costs) throws JMException {

        double sum = 0;

        for (int i=0; i<producedRE.length; i++) {
            double difference = costs.getValue(i) - lowerLimit_[i];
            if (difference < 0){
                int a = 2;
            }
            double profit = spentEnergy[i] * difference;
            sum += profit;
        }

        return -sum;
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



