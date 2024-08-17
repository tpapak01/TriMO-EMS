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
import jmetal.qualityIndicator.InvertedGenerationalDistance;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.C_Metric;
import jmetal.util.JMException;
import jmetal.util.Ranking;
import jmetal.util.Utils;
import jmetal.util.wrapper.XReal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;


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

    private static double best_upper_level_result = Double.MAX_VALUE;
    private static int fileID = 1;
    private static int UL_evaluations = 0;
    private static QualityIndicator indicators;

    private static int wins_0_hyp = 0;
    private static int wins_1_hyp = 0;
    private static int wins_2_hyp = 0;
    private static int wins_3_hyp = 0;
    private static int wins_4_hyp = 0;
    private static int wins_0_spr = 0;
    private static int wins_1_spr = 0;
    private static int wins_2_spr = 0;
    private static int wins_3_spr = 0;
    private static int wins_4_spr = 0;
    private static int wins_0_nds = 0;
    private static int wins_1_nds = 0;
    private static int wins_2_nds = 0;
    private static int wins_3_nds = 0;
    private static int wins_4_nds = 0;
    private static int wins_0_time = 0;
    private static int wins_1_time = 0;
    private static int wins_2_time = 0;
    private static int wins_3_time = 0;
    private static int wins_4_time = 0;
    private static int wins_0_cmetric = 0;
    private static int wins_1_cmetric = 0;
    private static int wins_2_cmetric = 0;
    private static int wins_3_cmetric = 0;
    private static int wins_4_cmetric = 0;

    private static double best_hyp;
    private static int best_hyp_ind;
    private static double best_spr;
    private static int best_spr_ind;
    private static int best_nds;
    private static int best_nds_ind;
    private static long best_time;
    private static int best_time_ind;
    private static double best_cmetric;
    private static int best_cmetric_ind;


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
      indicators = new QualityIndicator(this.lowerLevelProblem, "OPTIMAL_PARETO") ;

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

    int execution = 0;

	@Override
	public void evaluate(Solution solution) throws JMException {

        SolutionSet lowerLevelSolutions = null;
        XReal costs = new XReal(solution);
        int execType = solution.getExecType();
        long estimatedTime = 0;

        try {
            if (this.lowerLevelAlgorithmName.equals("MOEAD")) {
                long initTime = System.currentTimeMillis();
                lowerLevelSolutions = LowerLevelMOKP_MOEAD.evaluate(costs, solution);
                estimatedTime = System.currentTimeMillis() - initTime;
            }
            else lowerLevelSolutions = LowerLevelMOKP_NSGAII.evaluate(costs, solution);
        } catch (ClassNotFoundException e){
            System.out.println("Exception at LowerLevelMOKP.evaluate: " + e.getMessage());
        }

        //Identify optimistic and pessimistic solution
        double best_self = Double.MAX_VALUE;
        int best_solution_index = -1;
        double[] target_desirability = new double[this.lowerLevelProblem.getNumberOfObjectives()];
        target_desirability[0] = this.lowerLevelProblem.getObjectiveDesirability();
        target_desirability[1] = 1 - target_desirability[0];
        double[] nadirObjectiveValue = this.lowerLevelProblem.getNadirObjectiveValue();
        double best_desirability = 100;
        int best_desirability_index = -1;

        for (int s=0; s<lowerLevelSolutions.size(); s++) {
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
            double desirability = Math.abs(target_desirability[0] - lowerLevelSol.getLambda()[0]);
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

        SolutionSet specialPareto = new SolutionSet(lowerLevelSolutions.size());
        SolutionSet randomPareto = new SolutionSet(lowerLevelSolutions.size());
        SolutionSet reversePareto = new SolutionSet(lowerLevelSolutions.size());

        specialPareto.add(bestSelfSol); randomPareto.add(bestSelfSol);
        specialPareto.add(bestDesSol); randomPareto.add(bestDesSol);
        int counter = 0;
        for (int s=0; s<lowerLevelSolutions.size(); s++) {
            Solution lowerLevelSol = lowerLevelSolutions.get(s);
            double DIM1 = lowerLevelSol.getSelfConsumption();
            double DIM2_norm = Utils.AchievementScalarizationTcheby(lowerLevelSol, bestDesSol, target_desirability, nadirObjectiveValue);

            //Use below to find solutions of 2D decision-making space
            if (DIM1 < worst_self &&
                    DIM2_norm < worst_des) {
                specialPareto.add(lowerLevelSol);
                counter++;
            } else reversePareto.add(lowerLevelSol);
        }

        Integer[] arr = new Integer[lowerLevelSolutions.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i;
        }
        Collections.shuffle(Arrays.asList(arr));
        for (int k=0; k<counter; k++) {
            if (arr[k] != best_solution_index && arr[k] != best_desirability_index) {
                Solution lowerLevelSol = lowerLevelSolutions.get(arr[k]);
                randomPareto.add(lowerLevelSol);
            } else counter++;
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

        if (solution.isMarked()) {
            solution.setLL_ND_pop(lowerLevelSolutions);
            solution.setLL_Special_pop(specialPareto);
            solution.setLL_Reverse_pop(reversePareto);
            solution.setLL_Random_pop(randomPareto);
        } else if (execType == 0 && solution.isMarked() == false){
            if (solution.getReferencePop() != null){
                int problem = 1111;
            } else {
                execution++;
                solution.setReferencePop(specialPareto);

                best_hyp = -100; best_hyp_ind = -1;
                best_spr = 100; best_spr_ind = -1;
                best_nds = -1; best_nds_ind = -1;
                best_time = 10000; best_time_ind = -1;
                double hypervolume = indicators.getHypervolume(specialPareto);
                if (hypervolume > best_hyp) {
                    best_hyp = hypervolume;
                    best_hyp_ind = execType;
                }
                double spread = indicators.getSpread(specialPareto);
                if (spread < best_spr) {
                    best_spr = spread;
                    best_spr_ind = execType;
                }
                int nds = specialPareto.size();
                if (nds < best_nds) {
                    best_nds = nds;
                    best_nds_ind = execType;
                }
                if (estimatedTime < best_time){
                    best_time = estimatedTime;
                    best_time_ind = execType;
                }
                //System.out.println("val:" + hypervolume);
                //specialPareto.printObjectivesToFile("LowerLevelParetoEvolution/" + execution + "_FUN_" + execType);
            }
        } else if (solution.isMarked() == false){

            //time to compare specialParetos
            /*
            SolutionSet referencePop = solution.getReferencePop();
            double[][] trueFront = new double[referencePop.size()][2];
            for (int i=0; i<referencePop.size(); i++){
                Solution sol = referencePop.get(i);
                for (int j=0; j<2; j++){
                    trueFront[i][j] = sol.getObjective(j);
                }
            }
            double[][] solutionFront = new double[specialPareto.size()][2];
            for (int i=0; i<specialPareto.size(); i++){
                Solution sol = specialPareto.get(i);
                for (int j=0; j<2; j++){
                    solutionFront[i][j] = sol.getObjective(j);
                }
            }

            InvertedGenerationalDistance qualityIndicator = new InvertedGenerationalDistance();
            double value = qualityIndicator.invertedGenerationalDistance(
                    solutionFront,
                    trueFront,
                    2);
            System.out.println("Type:" + execType + ", val:" + value);

             */
            double hypervolume = indicators.getHypervolume(specialPareto);
            if (hypervolume > best_hyp) {
                best_hyp = hypervolume;
                best_hyp_ind = execType;
            }
            double spread = indicators.getSpread(specialPareto);
            if (spread < best_spr) {
                best_spr = spread;
                best_spr_ind = execType;
            }
            int nds = specialPareto.size();
            if (nds > best_nds) {
                best_nds = nds;
                best_nds_ind = execType;
            }
            if (estimatedTime < best_time){
                best_time = estimatedTime;
                best_time_ind = execType;
            }
            specialPareto.printObjectivesToFile("LowerLevelParetoEvolution/FUN_1");
            solution.getReferencePop().printObjectivesToFile("LowerLevelParetoEvolution/FUN_2");
            C_Metric epf = new C_Metric("LowerLevelParetoEvolution/FUN_1",
                    "LowerLevelParetoEvolution/FUN_2", 2);
            double cMetric = (float) epf.num_of_dominated_B / (float) epf.nds_B;
            File file1 = new File("LowerLevelParetoEvolution/FUN_1"); file1.delete();
            File file2 = new File("LowerLevelParetoEvolution/FUN_2"); file2.delete();
            if (cMetric > best_cmetric){
                best_cmetric = cMetric;
                best_cmetric_ind = execType;
            }
            if (execType == 4){
                switch(best_hyp_ind){
                    case 0: wins_0_hyp++; break;
                    case 1: wins_1_hyp++; break;
                    case 2: wins_2_hyp++; break;
                    case 3: wins_3_hyp++; break;
                    case 4: wins_4_hyp++; break;
                    default: break;
                }
                switch(best_spr_ind){
                    case 0: wins_0_spr++; break;
                    case 1: wins_1_spr++; break;
                    case 2: wins_2_spr++; break;
                    case 3: wins_3_spr++; break;
                    case 4: wins_4_spr++; break;
                    default: break;
                }
                switch(best_nds_ind){
                    case 0: wins_0_nds++; break;
                    case 1: wins_1_nds++; break;
                    case 2: wins_2_nds++; break;
                    case 3: wins_3_nds++; break;
                    case 4: wins_4_nds++; break;
                    default: break;
                }
                switch(best_time_ind){
                    case 0: wins_0_time++; break;
                    case 1: wins_1_time++; break;
                    case 2: wins_2_time++; break;
                    case 3: wins_3_time++; break;
                    case 4: wins_4_time++; break;
                    default: break;
                }
                switch(best_cmetric_ind){
                    case 0: wins_0_cmetric++; break;
                    case 1: wins_1_cmetric++; break;
                    case 2: wins_2_cmetric++; break;
                    case 3: wins_3_cmetric++; break;
                    case 4: wins_4_cmetric++; break;
                    default: break;
                }
                System.out.println("Hyp:" + wins_0_hyp + " " + wins_1_hyp + " " + wins_2_hyp + " " + wins_3_hyp + " " + wins_4_hyp);
                System.out.println("Spr:" + wins_0_spr + " " + wins_1_spr + " " + wins_2_spr + " " + wins_3_spr + " " + wins_4_spr);
                System.out.println("Nds:" + wins_0_nds + " " + wins_1_nds + " " + wins_2_nds + " " + wins_3_nds + " " + wins_4_nds);
                System.out.println("Tim:" + wins_0_time + " " + wins_1_time + " " + wins_2_time + " " + wins_3_time + " " + wins_4_time);
                System.out.println("Cme:" + wins_0_cmetric + " " + wins_1_cmetric + " " + wins_2_cmetric + " " + wins_3_cmetric + " " + wins_4_cmetric);
            }
            //System.out.println("val:" + hypervolume);
            //specialPareto.printObjectivesToFile("LowerLevelParetoEvolution/" + execution + "_FUN_" + execType);
        }

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

        /*
        if (UL_evaluations % 100 == 0) {
            SolutionSet chosenSolutionSet = new SolutionSet(1);
            chosenSolutionSet.add(chosenlowerLevelSol);
            chosenSolutionSet.printObjectivesToFile("LowerLevelParetoVisual/" + (fileID) + "_CHOSEN");

            Ranking finalRanking = new Ranking(lowerLevelSolutions);
            SolutionSet finalParetoFront = finalRanking.getSubfront(0);
            finalParetoFront.printObjectivesToFile("LowerLevelParetoVisual/" + (fileID) + "_FUN"); //check
            fileID++;
        }
        UL_evaluations++;
        
         */


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



