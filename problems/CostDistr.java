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
import jmetal.qualityIndicator.InvertedGenerationalDistance;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.C_Metric;
import jmetal.util.JMException;
import jmetal.util.Ranking;
import jmetal.util.Utils;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XReal;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
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

    private static double best_upper_level_result = Double.MAX_VALUE;
    private static QualityIndicator indicators;
    private static Comparator comparator;

    private static int wins_0_hyp = 0;
    private static int wins_1_hyp = 0;
    private static int wins_2_hyp = 0;
    private static int wins_3_hyp = 0;
    private static int wins_4_hyp = 0;
    private static int wins_5_hyp = 0;
    private static int wins_6_hyp = 0;
    private static int wins_7_hyp = 0;
    private static int wins_0_spr = 0;
    private static int wins_1_spr = 0;
    private static int wins_2_spr = 0;
    private static int wins_3_spr = 0;
    private static int wins_4_spr = 0;
    private static int wins_5_spr = 0;
    private static int wins_6_spr = 0;
    private static int wins_7_spr = 0;
    private static int wins_0_nds = 0;
    private static int wins_1_nds = 0;
    private static int wins_2_nds = 0;
    private static int wins_3_nds = 0;
    private static int wins_4_nds = 0;
    private static int wins_5_nds = 0;
    private static int wins_6_nds = 0;
    private static int wins_7_nds = 0;
    private static int wins_0_time = 0;
    private static int wins_1_time = 0;
    private static int wins_2_time = 0;
    private static int wins_3_time = 0;
    private static int wins_4_time = 0;
    private static int wins_5_time = 0;
    private static int wins_6_time = 0;
    private static int wins_7_time = 0;
    private static int wins_0_cmetric = 0;
    private static int wins_1_cmetric = 0;
    private static int wins_2_cmetric = 0;
    private static int wins_3_cmetric = 0;
    private static int wins_4_cmetric = 0;
    private static int wins_5_cmetric = 0;
    private static int wins_6_cmetric = 0;
    private static int wins_7_cmetric = 0;

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

    private static double avg_0_hyp = 0;
    private static double avg_1_hyp = 0;
    private static double avg_2_hyp = 0;
    private static double avg_3_hyp = 0;
    private static double avg_4_hyp = 0;
    private static double avg_5_hyp = 0;
    private static double avg_6_hyp = 0;
    private static double avg_7_hyp = 0;
    private static double avg_0_spr = 0;
    private static double avg_1_spr = 0;
    private static double avg_2_spr = 0;
    private static double avg_3_spr = 0;
    private static double avg_4_spr = 0;
    private static double avg_5_spr = 0;
    private static double avg_6_spr = 0;
    private static double avg_7_spr = 0;
    private static double avg_0_nds = 0;
    private static double avg_1_nds = 0;
    private static double avg_2_nds = 0;
    private static double avg_3_nds = 0;
    private static double avg_4_nds = 0;
    private static double avg_5_nds = 0;
    private static double avg_6_nds = 0;
    private static double avg_7_nds = 0;
    private static double avg_0_time = 0;
    private static double avg_1_time = 0;
    private static double avg_2_time = 0;
    private static double avg_3_time = 0;
    private static double avg_4_time = 0;
    private static double avg_5_time = 0;
    private static double avg_6_time = 0;
    private static double avg_7_time = 0;
    private static double avg_0_cmetric = 0;
    private static double avg_1_cmetric = 0;
    private static double avg_2_cmetric = 0;
    private static double avg_3_cmetric = 0;
    private static double avg_4_cmetric = 0;
    private static double avg_5_cmetric = 0;
    private static double avg_6_cmetric = 0;
    private static double avg_7_cmetric = 0;

    private static FileWriter hypWriter_0;
    private static FileWriter hypWriter_1;
    private static FileWriter hypWriter_2;
    private static FileWriter hypWriter_3;
    private static FileWriter hypWriter_4;
    private static FileWriter hypWriter_5;
    private static FileWriter hypWriter_6;
    private static FileWriter hypWriter_7;
    private static FileWriter sprWriter_0;
    private static FileWriter sprWriter_1;
    private static FileWriter sprWriter_2;
    private static FileWriter sprWriter_3;
    private static FileWriter sprWriter_4;
    private static FileWriter sprWriter_5;
    private static FileWriter sprWriter_6;
    private static FileWriter sprWriter_7;
    private static FileWriter ndsWriter_0;
    private static FileWriter ndsWriter_1;
    private static FileWriter ndsWriter_2;
    private static FileWriter ndsWriter_3;
    private static FileWriter ndsWriter_4;
    private static FileWriter ndsWriter_5;
    private static FileWriter ndsWriter_6;
    private static FileWriter ndsWriter_7;
    private static FileWriter timWriter_0;
    private static FileWriter timWriter_1;
    private static FileWriter timWriter_2;
    private static FileWriter timWriter_3;
    private static FileWriter timWriter_4;
    private static FileWriter timWriter_5;
    private static FileWriter timWriter_6;
    private static FileWriter timWriter_7;
    private static FileWriter cmeWriter_0;
    private static FileWriter cmeWriter_1;
    private static FileWriter cmeWriter_2;
    private static FileWriter cmeWriter_3;
    private static FileWriter cmeWriter_4;
    private static FileWriter cmeWriter_5;
    private static FileWriter cmeWriter_6;
    private static FileWriter cmeWriter_7;


    private static boolean createStatistics = true;
    private static boolean writeParetosToFile = true;
    private static String writeParetoPath = "LowerLevelParetoVisual/Texperiments/";
    private static int execution = 1;
    private static int ULpopSize = 100;
    private static int execPrint = 10;

  public CostDistr(String renewableFileName, MOKP_Problem lowerLevelProblem, String lowerLevelAlgorithmName, String costsName, String dataPath, String paretoFileName) {
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

      String paretoName = "OPTIMAL_PARETO";
      if (!paretoFileName.equals("-")) { paretoName = paretoFileName; }
      indicators = new QualityIndicator(this.lowerLevelProblem,
              problemPath + paretoName) ;
      
      if (this.lowerLevelProblem.isMaxmized())
          comparator = new ObjectiveComparator(0, false) ; // Single objective comparator
      else comparator = new ObjectiveComparator(0, true) ; // Single objective comparator

      if (createStatistics) {
          try {
              hypWriter_0 = new FileWriter("LowerLevelParetoVisual/hyp0.txt");
              hypWriter_1 = new FileWriter("LowerLevelParetoVisual/hyp1.txt");
              hypWriter_2 = new FileWriter("LowerLevelParetoVisual/hyp2.txt");
              hypWriter_3 = new FileWriter("LowerLevelParetoVisual/hyp3.txt");
              hypWriter_4 = new FileWriter("LowerLevelParetoVisual/hyp4.txt");
              hypWriter_5 = new FileWriter("LowerLevelParetoVisual/hyp5.txt");
              hypWriter_6 = new FileWriter("LowerLevelParetoVisual/hyp6.txt");
              hypWriter_7 = new FileWriter("LowerLevelParetoVisual/hyp7.txt");

              sprWriter_0 = new FileWriter("LowerLevelParetoVisual/spr0.txt");
              sprWriter_1 = new FileWriter("LowerLevelParetoVisual/spr1.txt");
              sprWriter_2 = new FileWriter("LowerLevelParetoVisual/spr2.txt");
              sprWriter_3 = new FileWriter("LowerLevelParetoVisual/spr3.txt");
              sprWriter_4 = new FileWriter("LowerLevelParetoVisual/spr4.txt");
              sprWriter_5 = new FileWriter("LowerLevelParetoVisual/spr5.txt");
              sprWriter_6 = new FileWriter("LowerLevelParetoVisual/spr6.txt");
              sprWriter_7 = new FileWriter("LowerLevelParetoVisual/spr7.txt");

              ndsWriter_0 = new FileWriter("LowerLevelParetoVisual/nds0.txt");
              ndsWriter_1 = new FileWriter("LowerLevelParetoVisual/nds1.txt");
              ndsWriter_2 = new FileWriter("LowerLevelParetoVisual/nds2.txt");
              ndsWriter_3 = new FileWriter("LowerLevelParetoVisual/nds3.txt");
              ndsWriter_4 = new FileWriter("LowerLevelParetoVisual/nds4.txt");
              ndsWriter_5 = new FileWriter("LowerLevelParetoVisual/nds5.txt");
              ndsWriter_6 = new FileWriter("LowerLevelParetoVisual/nds6.txt");
              ndsWriter_7 = new FileWriter("LowerLevelParetoVisual/nds7.txt");

              timWriter_0 = new FileWriter("LowerLevelParetoVisual/tim0.txt");
              timWriter_1 = new FileWriter("LowerLevelParetoVisual/tim1.txt");
              timWriter_2 = new FileWriter("LowerLevelParetoVisual/tim2.txt");
              timWriter_3 = new FileWriter("LowerLevelParetoVisual/tim3.txt");
              timWriter_4 = new FileWriter("LowerLevelParetoVisual/tim4.txt");
              timWriter_5 = new FileWriter("LowerLevelParetoVisual/tim5.txt");
              timWriter_6 = new FileWriter("LowerLevelParetoVisual/tim6.txt");
              timWriter_7 = new FileWriter("LowerLevelParetoVisual/tim7.txt");

              cmeWriter_0 = new FileWriter("LowerLevelParetoVisual/cme0.txt");
              cmeWriter_1 = new FileWriter("LowerLevelParetoVisual/cme1.txt");
              cmeWriter_2 = new FileWriter("LowerLevelParetoVisual/cme2.txt");
              cmeWriter_3 = new FileWriter("LowerLevelParetoVisual/cme3.txt");
              cmeWriter_4 = new FileWriter("LowerLevelParetoVisual/cme4.txt");
              cmeWriter_5 = new FileWriter("LowerLevelParetoVisual/cme5.txt");
              cmeWriter_6 = new FileWriter("LowerLevelParetoVisual/cme6.txt");
              cmeWriter_7 = new FileWriter("LowerLevelParetoVisual/cme7.txt");
          } catch (IOException e) {
              e.printStackTrace();
          }
      }

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
        int execType = solution.getExecType();
        long estimatedTime = 0;

        try {
            if (this.lowerLevelAlgorithmName.equals("MOEAD")) {
                long initTime = System.currentTimeMillis();
                lowerLevelSolutions = LowerLevelMOKP_MOEAD.evaluate(costs, solution);
                estimatedTime = System.currentTimeMillis() - initTime;
            } else lowerLevelSolutions = LowerLevelMOKP_NSGAII.evaluate(costs, solution);
        } catch (ClassNotFoundException e) {
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

        for (int s = 0; s < lsize; s++) {
            Solution lowerLevelSol = lowerLevelSolutions.get(s);

            // do upper-level evaluation = finding deviation from available RE
            //double result = upperLevel_evaluate_distance_from_produced(spentEnergy);
            double[] energySpent = lowerLevelSol.getSpentEnergy();
            double result = upperLevel_evaluate_XOR_distance_plus_weight(energySpent, costs);
            lowerLevelSol.setSelfConsumption(result);
            //double selfConsumption = upperLevel_evaluate_XOR_distance(energySpent);
            if (result < best_self) {
                best_self = result;
                best_solution_index = s;
            }
            double desirability = Math.abs(target_desirability[0] - lowerLevelSol.getLambda()[0]);
            if (desirability < best_desirability) {
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

        SolutionSet pareto1 = null;
        SolutionSet pareto2 = null;
        SolutionSet pareto3 = null;
        SolutionSet pareto4 = null;

        if (solution.isMarked()) {

            MOEAD algo = (MOEAD)LowerLevelMOKP_MOEAD.algorithm;
            int popSize = LowerLevelMOKP_MOEAD.popSize;

            //create temporary external that is sorted by objective
            SolutionSet newExternal = new SolutionSet(lsize);
            for (int i=0; i<lsize; i++)
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

            pareto1 = new SolutionSet(popSize);
            pareto2 = new SolutionSet(popSize);
            pareto3 = new SolutionSet(popSize);
            pareto4 = new SolutionSet(popSize);

            /*
            pareto1.add(bestSelfSol);
            pareto2.add(bestSelfSol);
            pareto3.add(bestSelfSol);
            pareto4.add(bestSelfSol);

            pareto1.add(bestDesSol);
            pareto2.add(bestDesSol);
            pareto3.add(bestDesSol);
            pareto4.add(bestDesSol);



            for (int s = 0; s < lsize; s++) {
                Solution lowerLevelSol = lowerLevelSolutions.get(s);
                double DIM1 = lowerLevelSol.getSelfConsumption();
                double DIM2_norm = Utils.AchievementScalarizationTcheby(lowerLevelSol, bestDesSol, target_desirability, nadirObjectiveValue);

                //Use below to find solutions of 2D decision-making space
                if (DIM1 < worst_self &&
                        DIM2_norm < worst_des) {
                    pareto1.add(lowerLevelSol);
                    pareto2.add(lowerLevelSol);
                    pareto3.add(lowerLevelSol);
                    pareto4.add(lowerLevelSol);
                }
            }

             */

            for (i = 0; i < popSize; i = i + 25) {
                Solution lowerLevelSol = archive.get(i);
                pareto1.add(lowerLevelSol);
            }
            for (i = 0; i < popSize; i = i + 12) {
                Solution lowerLevelSol = archive.get(i);
                pareto2.add(lowerLevelSol);
            }
            for (i = 0; i < popSize; i = i + 5) {
                Solution lowerLevelSol = archive.get(i);
                pareto3.add(lowerLevelSol);
            }
            for (i = 0; i < popSize; i = i + 2) {
                Solution lowerLevelSol = archive.get(i);
                pareto4.add(lowerLevelSol);
            }

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
            int nothing = 0;
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

        String writeParetoPathFull = writeParetoPath + (execution) + "_FUN_";
        if (solution.isMarked()) {
            solution.setLL_ND_pop(pareto1);
            solution.setLL_Special_pop(pareto2);
            solution.setLL_Reverse_pop(pareto3);
            solution.setLL_Random_pop(pareto4);
        } else if (execType == 0 && solution.isMarked() == false){

            if (writeParetosToFile)
                lowerLevelSolutions.printObjectivesToFile(writeParetoPathFull + execType);

            if (createStatistics) {
                best_hyp = -100;
                best_hyp_ind = -1;
                best_spr = 100;
                best_spr_ind = -1;
                best_nds = -1;
                best_nds_ind = -1;
                best_time = 100000;
                best_time_ind = -1;
                best_cmetric = -1;
                best_cmetric_ind = -1;
                try {
                    double hypervolume = indicators.getHypervolume(lowerLevelSolutions);
                    hypWriter_0.write(hypervolume + "\n");
                    avg_0_hyp += hypervolume;
                    if (hypervolume > best_hyp) {
                        best_hyp = hypervolume;
                        best_hyp_ind = execType;
                    }
                    double spread = indicators.getSpread(lowerLevelSolutions);
                    sprWriter_0.write(spread + "\n");
                    avg_0_spr += spread;
                    if (spread < best_spr) {
                        best_spr = spread;
                        best_spr_ind = execType;
                    }
                    int nds = lsize;
                    ndsWriter_0.write(nds + "\n");
                    avg_0_nds += nds;
                    if (nds < best_nds) {
                        best_nds = nds;
                        best_nds_ind = execType;
                    }
                    timWriter_0.write(estimatedTime + "\n");
                    avg_0_time += estimatedTime;
                    if (estimatedTime < best_time) {
                        best_time = estimatedTime;
                        best_time_ind = execType;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (solution.isMarked() == false){

            //time to compare lowerLevelSolutionss
            /*
            SolutionSet referencePop = solution.getReferencePop();
            double[][] trueFront = new double[referencePop.size()][2];
            for (int i=0; i<referencePop.size(); i++){
                Solution sol = referencePop.get(i);
                for (int j=0; j<2; j++){
                    trueFront[i][j] = sol.getObjective(j);
                }
            }
            double[][] solutionFront = new double[lsize][2];
            for (int i=0; i<lsize; i++){
                Solution sol = lowerLevelSolutions.get(i);
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

            if (writeParetosToFile)
                lowerLevelSolutions.printObjectivesToFile(writeParetoPathFull + execType);

            if (createStatistics) {
                double hypervolume = indicators.getHypervolume(lowerLevelSolutions);
                if (hypervolume > best_hyp) {
                    best_hyp = hypervolume;
                    best_hyp_ind = execType;
                }
                double spread = indicators.getSpread(lowerLevelSolutions);
                if (spread < best_spr) {
                    best_spr = spread;
                    best_spr_ind = execType;
                }
                int nds = lsize;
                if (nds > best_nds) {
                    best_nds = nds;
                    best_nds_ind = execType;
                }
                if (estimatedTime < best_time) {
                    best_time = estimatedTime;
                    best_time_ind = execType;
                }

                int enemy = -1;
                if (execType >=4 && execType <=7)
                    enemy = execType - 4;

                double cMetric = 0;
                double cMetricReverse = 0;
                if (enemy != -1) {
                    C_Metric epf = new C_Metric(writeParetoPathFull + execType,
                            writeParetoPathFull + enemy, 2);
                    cMetric = (float) epf.num_of_dominated_B / (float) epf.nds_B;

                    C_Metric epfReverse = new C_Metric(writeParetoPathFull + enemy,
                            writeParetoPathFull + execType, 2);
                    cMetricReverse = (float) epfReverse.num_of_dominated_B / (float) epfReverse.nds_B;
                }

                try {
                    switch (execType) {
                        case 1:
                            avg_1_hyp += hypervolume;
                            avg_1_spr += spread;
                            avg_1_nds += nds;
                            avg_1_time += estimatedTime;
                            hypWriter_1.write(hypervolume + "\n");
                            sprWriter_1.write(spread + "\n");
                            ndsWriter_1.write(nds + "\n");
                            timWriter_1.write(estimatedTime + "\n");
                            break;
                        case 2:
                            avg_2_hyp += hypervolume;
                            avg_2_spr += spread;
                            avg_2_nds += nds;
                            avg_2_time += estimatedTime;
                            hypWriter_2.write(hypervolume + "\n");
                            sprWriter_2.write(spread + "\n");
                            ndsWriter_2.write(nds + "\n");
                            timWriter_2.write(estimatedTime + "\n");
                            break;
                        case 3:
                            avg_3_hyp += hypervolume;
                            avg_3_spr += spread;
                            avg_3_nds += nds;
                            avg_3_time += estimatedTime;
                            hypWriter_3.write(hypervolume + "\n");
                            sprWriter_3.write(spread + "\n");
                            ndsWriter_3.write(nds + "\n");
                            timWriter_3.write(estimatedTime + "\n");
                            break;
                        case 4:
                            avg_4_hyp += hypervolume;
                            avg_4_spr += spread;
                            avg_4_nds += nds;
                            avg_4_time += estimatedTime;
                            avg_4_cmetric += cMetric;
                            avg_0_cmetric += cMetricReverse;
                            hypWriter_4.write(hypervolume + "\n");
                            sprWriter_4.write(spread + "\n");
                            ndsWriter_4.write(nds + "\n");
                            timWriter_4.write(estimatedTime + "\n");
                            cmeWriter_4.write(cMetric + "\n");
                            cmeWriter_0.write(cMetricReverse + "\n");
                            break;
                        case 5:
                            avg_5_hyp += hypervolume;
                            avg_5_spr += spread;
                            avg_5_nds += nds;
                            avg_5_time += estimatedTime;
                            avg_5_cmetric += cMetric;
                            avg_1_cmetric += cMetricReverse;
                            hypWriter_5.write(hypervolume + "\n");
                            sprWriter_5.write(spread + "\n");
                            ndsWriter_5.write(nds + "\n");
                            timWriter_5.write(estimatedTime + "\n");
                            cmeWriter_5.write(cMetric + "\n");
                            cmeWriter_1.write(cMetricReverse + "\n");
                            break;
                        case 6:
                            avg_6_hyp += hypervolume;
                            avg_6_spr += spread;
                            avg_6_nds += nds;
                            avg_6_time += estimatedTime;
                            avg_6_cmetric += cMetric;
                            avg_2_cmetric += cMetricReverse;
                            hypWriter_6.write(hypervolume + "\n");
                            sprWriter_6.write(spread + "\n");
                            ndsWriter_6.write(nds + "\n");
                            timWriter_6.write(estimatedTime + "\n");
                            cmeWriter_6.write(cMetric + "\n");
                            cmeWriter_2.write(cMetricReverse + "\n");
                            break;
                        case 7:
                            avg_7_hyp += hypervolume;
                            avg_7_spr += spread;
                            avg_7_nds += nds;
                            avg_7_time += estimatedTime;
                            avg_7_cmetric += cMetric;
                            avg_3_cmetric += cMetricReverse;
                            hypWriter_7.write(hypervolume + "\n");
                            sprWriter_7.write(spread + "\n");
                            ndsWriter_7.write(nds + "\n");
                            timWriter_7.write(estimatedTime + "\n");
                            cmeWriter_7.write(cMetric + "\n");
                            cmeWriter_3.write(cMetricReverse + "\n");
                            break;
                        default:
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //now find winner of this iteration
                if (execType == 7) {
                    switch (best_hyp_ind) {
                        case 0:
                            wins_0_hyp++;
                            break;
                        case 1:
                            wins_1_hyp++;
                            break;
                        case 2:
                            wins_2_hyp++;
                            break;
                        case 3:
                            wins_3_hyp++;
                            break;
                        case 4:
                            wins_4_hyp++;
                            break;
                        case 5:
                            wins_5_hyp++;
                            break;
                        case 6:
                            wins_6_hyp++;
                            break;
                        case 7:
                            wins_7_hyp++;
                            break;
                        default:
                            break;
                    }
                    switch (best_spr_ind) {
                        case 0:
                            wins_0_spr++;
                            break;
                        case 1:
                            wins_1_spr++;
                            break;
                        case 2:
                            wins_2_spr++;
                            break;
                        case 3:
                            wins_3_spr++;
                            break;
                        case 4:
                            wins_4_spr++;
                            break;
                        case 5:
                            wins_5_spr++;
                            break;
                        case 6:
                            wins_6_spr++;
                            break;
                        case 7:
                            wins_7_spr++;
                            break;
                        default:
                            break;
                    }
                    switch (best_nds_ind) {
                        case 0:
                            wins_0_nds++;
                            break;
                        case 1:
                            wins_1_nds++;
                            break;
                        case 2:
                            wins_2_nds++;
                            break;
                        case 3:
                            wins_3_nds++;
                            break;
                        case 4:
                            wins_4_nds++;
                            break;
                        case 5:
                            wins_5_nds++;
                            break;
                        case 6:
                            wins_6_nds++;
                            break;
                        case 7:
                            wins_7_nds++;
                            break;
                        default:
                            break;
                    }
                    switch (best_time_ind) {
                        case 0:
                            wins_0_time++;
                            break;
                        case 1:
                            wins_1_time++;
                            break;
                        case 2:
                            wins_2_time++;
                            break;
                        case 3:
                            wins_3_time++;
                            break;
                        case 4:
                            wins_4_time++;
                            break;
                        case 5:
                            wins_5_time++;
                            break;
                        case 6:
                            wins_6_time++;
                            break;
                        case 7:
                            wins_7_time++;
                            break;
                        default:
                            break;
                    }
                    switch (best_cmetric_ind) {
                        case 0:
                            wins_0_cmetric++;
                            break;
                        case 1:
                            wins_1_cmetric++;
                            break;
                        case 2:
                            wins_2_cmetric++;
                            break;
                        case 3:
                            wins_3_cmetric++;
                            break;
                        case 4:
                            wins_4_cmetric++;
                            break;
                        case 5:
                            wins_5_cmetric++;
                            break;
                        case 6:
                            wins_6_cmetric++;
                            break;
                        case 7:
                            wins_7_cmetric++;
                            break;
                        default:
                            break;
                    }
                    if (execution % execPrint == 0) {
                        System.out.println("WIN Hyp:" + wins_0_hyp + " " + wins_1_hyp + " " + wins_2_hyp + " " + wins_3_hyp + " " + wins_4_hyp + " " + wins_5_hyp + " " + wins_6_hyp + " " + wins_7_hyp);
                        System.out.println("WIN Spr:" + wins_0_spr + " " + wins_1_spr + " " + wins_2_spr + " " + wins_3_spr + " " + wins_4_spr + " " + wins_5_spr + " " + wins_6_spr + " " + wins_7_spr);
                        System.out.println("WIN Nds:" + wins_0_nds + " " + wins_1_nds + " " + wins_2_nds + " " + wins_3_nds + " " + wins_4_nds + " " + wins_5_nds + " " + wins_6_nds + " " + wins_7_nds);
                        System.out.println("WIN Tim:" + wins_0_time + " " + wins_1_time + " " + wins_2_time + " " + wins_3_time + " " + wins_4_time + " " + wins_5_time + " " + wins_6_time +  " " + wins_7_time);
                        System.out.println("WIN Cme:" + wins_0_cmetric + " " + wins_1_cmetric + " " + wins_2_cmetric + " " + wins_3_cmetric + " " + wins_4_cmetric + " " + wins_5_cmetric + " " + wins_6_cmetric + " " + wins_7_cmetric);

                        System.out.println("AVG Hyp:" + avg_0_hyp / execution + " " + avg_1_hyp / execution + " " + avg_2_hyp / execution + " " + avg_3_hyp / execution + " " + avg_4_hyp / execution + " " + avg_5_hyp / execution + " " + avg_6_hyp / execution + " " + avg_7_hyp / execution);
                        System.out.println("AVG Spr:" + avg_0_spr / execution + " " + avg_1_spr / execution + " " + avg_2_spr / execution + " " + avg_3_spr / execution + " " + avg_4_spr / execution + " " + avg_5_spr / execution + " " + avg_6_spr / execution + " " + avg_7_spr / execution);
                        System.out.println("AVG Nds:" + avg_0_nds / execution + " " + avg_1_nds / execution + " " + avg_2_nds / execution + " " + avg_3_nds / execution + " " + avg_4_nds / execution + " " + avg_5_nds / execution + " " + avg_6_nds / execution + " " + avg_7_nds / execution);
                        System.out.println("AVG Tim:" + avg_0_time / execution + " " + avg_1_time / execution + " " + avg_2_time / execution + " " + avg_3_time / execution + " " + avg_4_time / execution + " " + avg_5_time / execution + " " + avg_6_time / execution + " " + avg_7_time / execution);
                        System.out.println("AVG Cme:" + avg_0_cmetric / execution + " " + avg_1_cmetric / execution + " " + avg_2_cmetric / execution + " " + avg_3_cmetric / execution + " " + avg_4_cmetric / execution + " " + avg_5_cmetric / execution + " " + avg_6_cmetric / execution + " " + avg_7_cmetric / execution);
                    }
                    if (execution == ULpopSize) {
                        try {
                            hypWriter_0.close();
                            hypWriter_1.close();
                            hypWriter_2.close();
                            hypWriter_3.close();
                            hypWriter_4.close();
                            hypWriter_5.close();
                            hypWriter_6.close();
                            hypWriter_7.close();

                            sprWriter_0.close();
                            sprWriter_1.close();
                            sprWriter_2.close();
                            sprWriter_3.close();
                            sprWriter_4.close();
                            sprWriter_5.close();
                            sprWriter_6.close();
                            sprWriter_7.close();

                            ndsWriter_0.close();
                            ndsWriter_1.close();
                            ndsWriter_2.close();
                            ndsWriter_3.close();
                            ndsWriter_4.close();
                            ndsWriter_5.close();
                            ndsWriter_6.close();
                            ndsWriter_7.close();

                            timWriter_0.close();
                            timWriter_1.close();
                            timWriter_2.close();
                            timWriter_3.close();
                            timWriter_4.close();
                            timWriter_5.close();
                            timWriter_6.close();
                            timWriter_7.close();

                            cmeWriter_0.close();
                            cmeWriter_1.close();
                            cmeWriter_2.close();
                            cmeWriter_3.close();
                            cmeWriter_4.close();
                            cmeWriter_5.close();
                            cmeWriter_6.close();
                            cmeWriter_7.close();

                            //hyp
                            double std_0_hyp = 0; double[] hyp0;
                            double std_1_hyp = 0; double[] hyp1;
                            double std_2_hyp = 0; double[] hyp2;
                            double std_3_hyp = 0; double[] hyp3;
                            double std_4_hyp = 0; double[] hyp4;
                            double std_5_hyp = 0; double[] hyp5;
                            double std_6_hyp = 0; double[] hyp6;
                            double std_7_hyp = 0; double[] hyp7;
                            try {
                                hyp0 = Utils.readFileIntoArray("LowerLevelParetoVisual/hyp0.txt");
                                std_0_hyp = Utils.calculateStandardDeviation(hyp0);
                                hyp1 = Utils.readFileIntoArray("LowerLevelParetoVisual/hyp1.txt");
                                std_1_hyp = Utils.calculateStandardDeviation(hyp1);
                                hyp2 = Utils.readFileIntoArray("LowerLevelParetoVisual/hyp2.txt");
                                std_2_hyp = Utils.calculateStandardDeviation(hyp2);
                                hyp3 = Utils.readFileIntoArray("LowerLevelParetoVisual/hyp3.txt");
                                std_3_hyp = Utils.calculateStandardDeviation(hyp3);
                                hyp4 = Utils.readFileIntoArray("LowerLevelParetoVisual/hyp4.txt");
                                std_4_hyp = Utils.calculateStandardDeviation(hyp4);
                                hyp5 = Utils.readFileIntoArray("LowerLevelParetoVisual/hyp5.txt");
                                std_5_hyp = Utils.calculateStandardDeviation(hyp5);
                                hyp6 = Utils.readFileIntoArray("LowerLevelParetoVisual/hyp6.txt");
                                std_6_hyp = Utils.calculateStandardDeviation(hyp6);
                                hyp7 = Utils.readFileIntoArray("LowerLevelParetoVisual/hyp7.txt");
                                std_7_hyp = Utils.calculateStandardDeviation(hyp7);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            System.out.println("STD Hyp:" + std_0_hyp + " " + std_1_hyp + " " + std_2_hyp + " " + std_3_hyp + " " + std_4_hyp + " " + std_5_hyp + " " + std_6_hyp + " " + std_7_hyp);

                            //spr
                            double std_0_spr = 0; double[] spr0;
                            double std_1_spr = 0; double[] spr1;
                            double std_2_spr = 0; double[] spr2;
                            double std_3_spr = 0; double[] spr3;
                            double std_4_spr = 0; double[] spr4;
                            double std_5_spr = 0; double[] spr5;
                            double std_6_spr = 0; double[] spr6;
                            double std_7_spr = 0; double[] spr7;
                            try {
                                spr0 = Utils.readFileIntoArray("LowerLevelParetoVisual/spr0.txt");
                                std_0_spr = Utils.calculateStandardDeviation(spr0);
                                spr1 = Utils.readFileIntoArray("LowerLevelParetoVisual/spr1.txt");
                                std_1_spr = Utils.calculateStandardDeviation(spr1);
                                spr2 = Utils.readFileIntoArray("LowerLevelParetoVisual/spr2.txt");
                                std_2_spr = Utils.calculateStandardDeviation(spr2);
                                spr3 = Utils.readFileIntoArray("LowerLevelParetoVisual/spr3.txt");
                                std_3_spr = Utils.calculateStandardDeviation(spr3);
                                spr4 = Utils.readFileIntoArray("LowerLevelParetoVisual/spr4.txt");
                                std_4_spr = Utils.calculateStandardDeviation(spr4);
                                spr5 = Utils.readFileIntoArray("LowerLevelParetoVisual/spr5.txt");
                                std_5_spr = Utils.calculateStandardDeviation(spr5);
                                spr6 = Utils.readFileIntoArray("LowerLevelParetoVisual/spr6.txt");
                                std_6_spr = Utils.calculateStandardDeviation(spr6);
                                spr7 = Utils.readFileIntoArray("LowerLevelParetoVisual/spr7.txt");
                                std_7_spr = Utils.calculateStandardDeviation(spr7);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            System.out.println("STD Spr:" + std_0_spr + " " + std_1_spr + " " + std_2_spr + " " + std_3_spr + " " + std_4_spr + " " + std_5_spr + " " + std_6_spr + " " + std_7_spr);

                            //nds
                            double std_0_nds = 0; double[] nds0;
                            double std_1_nds = 0; double[] nds1;
                            double std_2_nds = 0; double[] nds2;
                            double std_3_nds = 0; double[] nds3;
                            double std_4_nds = 0; double[] nds4;
                            double std_5_nds = 0; double[] nds5;
                            double std_6_nds = 0; double[] nds6;
                            double std_7_nds = 0; double[] nds7;
                            try {
                                nds0 = Utils.readFileIntoArray("LowerLevelParetoVisual/nds0.txt");
                                std_0_nds = Utils.calculateStandardDeviation(nds0);
                                nds1 = Utils.readFileIntoArray("LowerLevelParetoVisual/nds1.txt");
                                std_1_nds = Utils.calculateStandardDeviation(nds1);
                                nds2 = Utils.readFileIntoArray("LowerLevelParetoVisual/nds2.txt");
                                std_2_nds = Utils.calculateStandardDeviation(nds2);
                                nds3 = Utils.readFileIntoArray("LowerLevelParetoVisual/nds3.txt");
                                std_3_nds = Utils.calculateStandardDeviation(nds3);
                                nds4 = Utils.readFileIntoArray("LowerLevelParetoVisual/nds4.txt");
                                std_4_nds = Utils.calculateStandardDeviation(nds4);
                                nds5 = Utils.readFileIntoArray("LowerLevelParetoVisual/nds5.txt");
                                std_5_nds = Utils.calculateStandardDeviation(nds5);
                                nds6 = Utils.readFileIntoArray("LowerLevelParetoVisual/nds6.txt");
                                std_6_nds = Utils.calculateStandardDeviation(nds6);
                                nds7 = Utils.readFileIntoArray("LowerLevelParetoVisual/nds7.txt");
                                std_7_nds = Utils.calculateStandardDeviation(nds7);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            System.out.println("STD Nds:" + std_0_nds + " " + std_1_nds + " " + std_2_nds + " " + std_3_nds + " " + std_4_nds + " " + std_5_nds + " " + std_6_nds + " " + std_7_nds);

                            //tim
                            double std_0_tim = 0; double[] tim0;
                            double std_1_tim = 0; double[] tim1;
                            double std_2_tim = 0; double[] tim2;
                            double std_3_tim = 0; double[] tim3;
                            double std_4_tim = 0; double[] tim4;
                            double std_5_tim = 0; double[] tim5;
                            double std_6_tim = 0; double[] tim6;
                            double std_7_tim = 0; double[] tim7;
                            try {
                                tim0 = Utils.readFileIntoArray("LowerLevelParetoVisual/tim0.txt");
                                std_0_tim = Utils.calculateStandardDeviation(tim0);
                                tim1 = Utils.readFileIntoArray("LowerLevelParetoVisual/tim1.txt");
                                std_1_tim = Utils.calculateStandardDeviation(tim1);
                                tim2 = Utils.readFileIntoArray("LowerLevelParetoVisual/tim2.txt");
                                std_2_tim = Utils.calculateStandardDeviation(tim2);
                                tim3 = Utils.readFileIntoArray("LowerLevelParetoVisual/tim3.txt");
                                std_3_tim = Utils.calculateStandardDeviation(tim3);
                                tim4 = Utils.readFileIntoArray("LowerLevelParetoVisual/tim4.txt");
                                std_4_tim = Utils.calculateStandardDeviation(tim4);
                                tim5 = Utils.readFileIntoArray("LowerLevelParetoVisual/tim5.txt");
                                std_5_tim = Utils.calculateStandardDeviation(tim5);
                                tim6 = Utils.readFileIntoArray("LowerLevelParetoVisual/tim6.txt");
                                std_6_tim = Utils.calculateStandardDeviation(tim6);
                                tim7 = Utils.readFileIntoArray("LowerLevelParetoVisual/tim7.txt");
                                std_7_tim = Utils.calculateStandardDeviation(tim7);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            System.out.println("STD Tim:" + std_0_tim + " " + std_1_tim + " " + std_2_tim + " " + std_3_tim + " " + std_4_tim + " " + std_5_tim + " " + std_6_tim + " " + std_7_tim);

                            //cme
                            double std_0_cme = 0; double[] cme0;
                            double std_1_cme = 0; double[] cme1;
                            double std_2_cme = 0; double[] cme2;
                            double std_3_cme = 0; double[] cme3;
                            double std_4_cme = 0; double[] cme4;
                            double std_5_cme = 0; double[] cme5;
                            double std_6_cme = 0; double[] cme6;
                            double std_7_cme = 0; double[] cme7;
                            try {
                                cme0 = Utils.readFileIntoArray("LowerLevelParetoVisual/cme0.txt");
                                std_0_cme = Utils.calculateStandardDeviation(cme0);
                                cme1 = Utils.readFileIntoArray("LowerLevelParetoVisual/cme1.txt");
                                std_1_cme = Utils.calculateStandardDeviation(cme1);
                                cme2 = Utils.readFileIntoArray("LowerLevelParetoVisual/cme2.txt");
                                std_2_cme = Utils.calculateStandardDeviation(cme2);
                                cme3 = Utils.readFileIntoArray("LowerLevelParetoVisual/cme3.txt");
                                std_3_cme = Utils.calculateStandardDeviation(cme3);
                                cme4 = Utils.readFileIntoArray("LowerLevelParetoVisual/cme4.txt");
                                std_4_cme = Utils.calculateStandardDeviation(cme4);
                                cme5 = Utils.readFileIntoArray("LowerLevelParetoVisual/cme5.txt");
                                std_5_cme = Utils.calculateStandardDeviation(cme5);
                                cme6 = Utils.readFileIntoArray("LowerLevelParetoVisual/cme6.txt");
                                std_6_cme = Utils.calculateStandardDeviation(cme6);
                                cme7 = Utils.readFileIntoArray("LowerLevelParetoVisual/cme7.txt");
                                std_7_cme = Utils.calculateStandardDeviation(cme7);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            System.out.println("STD Cme:" + std_0_cme + " " + std_1_cme + " " + std_2_cme + " " + std_3_cme + " " + std_4_cme + " " + std_5_cme + " " + std_6_cme + " " + std_7_cme);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    execution++;
                }
            }
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



