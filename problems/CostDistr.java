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
    //private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    //private static String costsPath = "/Users/emine/IdeaProjects/JMETALHOME/Costs_data/";
    private String problemPath = "/Users/emine/source/repos/SmartHome3/SmartHome3/data/"; // The path of the files
    private static String costsPath = "/Users/emine/source/repos/SmartHome3/SmartHome3/data/";

    private MOKP_Problem lowerLevelProblem;
    private String lowerLevelAlgorithmName;
    private double[] producedRE;
    private double[] inputCosts = null;
    private double totalProducedRE;
    private double ULObjectiveDesirability = 1.0;

    private static double best_upper_level_result = Double.MAX_VALUE;
    private static int fileID = 1;
    private static int UL_evaluations = 0;
    private static Comparator comparator;


  public CostDistr(String renewableFileName, MOKP_Problem lowerLevelProblem, String lowerLevelAlgorithmName, String costsName) {
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

      String fileName = problemPath + this.problemName_ + ".txt";
      System.out.println(fileName);
      String costsFileName = "";
      if (!costsName.equals("")) costsFileName = costsPath + costsName + ".txt";

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

          if (!costsFileName.equals("")) {
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

    public double[] getProducedRE(){
        return producedRE;
    }

    public double[] getInputCosts(){
      return inputCosts;
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
                lowerLevelSolutions = LowerLevelMOKP_MOEAD.evaluate(costs, solution);
            else lowerLevelSolutions = LowerLevelMOKP_NSGAII.evaluate(costs, solution);
        } catch (ClassNotFoundException e){
            System.out.println("Exception at LowerLevelMOKP.evaluate: " + e.getMessage());
        }

        int lsize = lowerLevelSolutions.size();

        //Identify optimistic and pessimistic solution
        double best_self = Double.MAX_VALUE;
        int best_solution_index = -1;
        double[] target_desirability = new double[this.lowerLevelProblem.getNumberOfObjectives()];
        target_desirability[0] = this.lowerLevelProblem.getObjectiveDesirability();
        target_desirability[1] = 1 - target_desirability[0];
        double[] nadirObjectiveValue = this.lowerLevelProblem.getNadirObjectiveValue();
        double best_desirability = 100;
        int best_desirability_index = -1;

        for (int s=0; s<lsize; s++) {
            Solution lowerLevelSol = lowerLevelSolutions.get(s);

            // do upper-level evaluation = finding deviation from available RE
            //double result = upperLevel_evaluate_distance_from_produced(spentEnergy);
            double[] energySpent = lowerLevelSol.getSpentEnergy();
            double result = upperLevel_evaluate_XOR_distance_plus_weight(energySpent, costs);
            lowerLevelSol.setSelfConsumption(result);
            //double selfConsumption = upperLevel_evaluate_XOR_distance(energySpent);
            if (result < best_self){
                best_self = result;
                best_solution_index = s;
            }

            ////register Deviation from Produced - used for Platform only
            double deviation = calculateEnergyDeviationFromProduced(energySpent);
            lowerLevelSol.setEnergyDeviationFromProduced(deviation);

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

        //Identify set of best solutions in 2D space using limits "worst_self" and "worst_des"
        //and add them to the special Pareto, along with the UL and LL preferred solution
        Solution bestSelfSol = lowerLevelSolutions.get(best_solution_index);
        Solution bestDesSol = lowerLevelSolutions.get(best_desirability_index);
        double worst_self = bestDesSol.getSelfConsumption();
        double worst_des = Utils.AchievementScalarizationTcheby(bestSelfSol, bestDesSol, target_desirability, nadirObjectiveValue);

        SolutionSet specialPareto;
        if (this.lowerLevelAlgorithmName.equals("MOEAD")) {

            MOEAD algo = (MOEAD) LowerLevelMOKP_MOEAD.algorithm;
            int popSize = LowerLevelMOKP_MOEAD.popSize;
            specialPareto = new SolutionSet(popSize);

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
            int i = 0;
            while (archive.size() < popSize){
                double f1 = algo.fitnessFunction(currentArchiveSol, lambda[i]);
                double f2 = algo.fitnessFunction(externalToAdd, lambda[i]);
                // if f2 smaller than f1, f2 (externalToAdd) is better
                if (f2 < f1 || (f2 == f1 && exIdx < lsize)) {
                    currentArchiveSol = externalToAdd;
                    exIdx++;
                    if (exIdx < lsize)
                        externalToAdd = newExternal.get(exIdx);
                } else {
                    newSol = new Solution(currentArchiveSol, lambda[i]);
                    archive.add(newSol);
                    i++;
                }
            }

            /*
            specialPareto.add(bestSelfSol);
            specialPareto.add(bestDesSol);

            for (int s = 0; s < lsize; s++) {
                Solution lowerLevelSol = lowerLevelSolutions.get(s);
                double DIM1 = lowerLevelSol.getSelfConsumption();
                double DIM2_norm = Utils.AchievementScalarizationTcheby(lowerLevelSol, bestDesSol, target_desirability, nadirObjectiveValue);

                //Use below to find solutions of 2D decision-making space
                if (DIM1 < worst_self &&
                        DIM2_norm < worst_des) {
                    specialPareto.add(lowerLevelSol);
                }
            }

             */

            for (i = 0; i < lsize; i = i + 5) {
                Solution lowerLevelSol = lowerLevelSolutions.get(i);
                specialPareto.add(lowerLevelSol);
            }
        } else {
            specialPareto = lowerLevelSolutions;
        }

        //find best solution given UL and LL preferences (optimistic OR pessimistic OR in between)
        Solution chosenlowerLevelSol = null;
        if (ULObjectiveDesirability == 1.0) {
            solution.setObjective(0, best_self);
            chosenlowerLevelSol = bestSelfSol;
        } else if (ULObjectiveDesirability == 0.0){
            solution.setObjective(0, worst_self);
            chosenlowerLevelSol = bestDesSol;
        } else {
            //find best solution given UL and LL preferences (non-optimistic and non-pessimistic)
            double best_overall = Double.MAX_VALUE;
            int best_overall_index = -1;
            for (int s=0; s<specialPareto.size(); s++) {
                Solution lowerLevelSol = specialPareto.get(s);
                double DIM1_norm = (lowerLevelSol.getSelfConsumption() - best_self) / (worst_self - best_self);
                double DIM2_norm = Utils.AchievementScalarizationTcheby(lowerLevelSol, bestDesSol, target_desirability, nadirObjectiveValue);

                double evaluation = (ULObjectiveDesirability * DIM1_norm) + ((1-ULObjectiveDesirability)*DIM2_norm);
                if (evaluation < best_overall){
                    best_overall = evaluation;
                    best_overall_index = s;
                }
            }
            chosenlowerLevelSol = specialPareto.get(best_overall_index);
            solution.setObjective(0, chosenlowerLevelSol.getSelfConsumption());
        }

        Variable[] vars = chosenlowerLevelSol.getDecisionVariables();
        Binary bin = (Binary) vars[0];
        double[] energySpent = chosenlowerLevelSol.getSpentEnergy();

        solution.setSpentEnergy(energySpent);
        solution.setLowerLevelVars(bin);
        solution.setLowerLevelObj(new double[] {chosenlowerLevelSol.getObjective(0), chosenlowerLevelSol.getObjective(1)});
        solution.setDissatisfactionPerUser(chosenlowerLevelSol.getDissatisfactionPerUser());
        solution.setEnergyAllocatedPerUser(chosenlowerLevelSol.getEnergyAllocatedPerUser());
        double deviation = calculateEnergyDeviationFromProduced(energySpent);
        solution.setEnergyDeviationFromProduced(deviation);
        double nonREpaid = calculateNonREPaid(energySpent, costs);
        solution.setNonREpaid(nonREpaid);
        solution.setDeviceToPreferenceMapping(chosenlowerLevelSol.getDeviceToPreferenceMapping());
        solution.setReverseDeviceToPreferenceMapping(chosenlowerLevelSol.getReverseDeviceToPreferenceMapping());
        solution.setLL_ND_pop(specialPareto);
        //platform only
        solution.setLL_Pareto_pop(lowerLevelSolutions);

        if (best_upper_level_result > best_self) {
            best_upper_level_result = best_self;
            int[] covered = solution.getDeviceToPreferenceMapping();
            int count = 0;
            for (int i=0; i<covered.length; i++){
                if (covered[i] != -1 && covered[i] != i){
                    count++;
                }
            }
            System.out.println(best_upper_level_result + " " + count);

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


        if (UL_evaluations % 100 == 0) {
            /*
            SolutionSet chosenSolutionSet = new SolutionSet(1);
            chosenSolutionSet.add(chosenlowerLevelSol);
            chosenSolutionSet.printObjectivesToFile("LowerLevelParetoVisual/" + (fileID) + "_CHOSEN");

             */

            RankingOnlyFirst finalRanking = new RankingOnlyFirst(lowerLevelSolutions);
            SolutionSet finalParetoFront = finalRanking.getSubfront(0);
            //finalParetoFront.printObjectivesToFile("LowerLevelParetoVisual/" + (fileID) + "_FUN"); //check
            finalParetoFront.printParetoToFile("C:\\Users\\emine\\source\\repos\\SmartHome3\\SmartHome3\\results\\Paretos\\PARETO_" + (fileID-1));
            fileID++;
        }
        UL_evaluations++;
        



        //System.out.println(best_result);
        //System.out.println(solution.getDecisionVariables()[0]);

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



