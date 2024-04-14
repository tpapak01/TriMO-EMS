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
import jmetal.operators.localSearch.CostsLocalSearch;
import jmetal.operators.localSearch.DissatisfactionLocalSearch;
import jmetal.operators.localSearch.LocalSearch;
import jmetal.operators.localSearch.MutationLocalSearch;
import jmetal.operators.mutation.CostsMutation;
import jmetal.operators.mutation.DissatisfactionMutation;
import jmetal.operators.mutation.Mutation;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.Ranking;
import jmetal.util.wrapper.XReal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;


public class CostDistr extends Problem {

	private static final long serialVersionUID = 1L;
    private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user - bilevel/"; // The path of the files
    private static String costsPath = "/Users/emine/IdeaProjects/JMETALHOME/Costs_data/";

    private MOKP_Problem lowerLevelProblem;
    private String lowerLevelAlgorithmName;
    private double[] producedRE;
    private double[] inputCosts = null;
    private double totalProducedRE;
    private LocalSearch improvementOperatorD;
    private LocalSearch improvementOperatorC;

    public static int execution = 0;
    private static double best_upper_level_result = Double.MAX_VALUE;
    private static int fileID = 1;
    private static int UL_evaluations = 0;


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

      //now initialize local search operator D
      HashMap parametersD = new HashMap() ;
      parametersD.put("repeats", 1) ;
      parametersD.put("problem",this.lowerLevelProblem) ;
      parametersD.put("algorithm",this.lowerLevelAlgorithmName) ;
      Mutation mutationD = new DissatisfactionMutation(parametersD);
      parametersD.put("improvementRounds", 1);
      parametersD.put("cooldownRounds", 80) ;
      parametersD.put("problem",this.lowerLevelProblem);
      parametersD.put("mutation", mutationD) ;
      improvementOperatorD = new DissatisfactionLocalSearch(parametersD);

      //now initialize local search operator C
      HashMap parametersC = new HashMap() ;
      parametersC.put("repeats", 1) ;
      parametersC.put("problem",this.lowerLevelProblem) ;
      parametersC.put("algorithm",this.lowerLevelAlgorithmName) ;
      Mutation mutationC = new CostsMutation(parametersC);
      parametersC.put("improvementRounds", 1);
      parametersC.put("cooldownRounds", 80) ;
      parametersC.put("problem",this.lowerLevelProblem);
      parametersC.put("mutation", mutationC) ;
      improvementOperatorC = new CostsLocalSearch(parametersC);

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
                lowerLevelSolutions = LowerLevelMOKP_MOEAD.evaluate(costs);
            else lowerLevelSolutions = LowerLevelMOKP_NSGAII.evaluate(costs);
        } catch (ClassNotFoundException e){
            System.out.println("Exception at LowerLevelMOKP.evaluate: " + e.getMessage());
        }

        SolutionSet improvedLowerLevelSolutions = new SolutionSet(lowerLevelSolutions.size());
        if (this.lowerLevelAlgorithmName.equals("MOEAD")) {
            for (int s = 0; s < lowerLevelSolutions.size(); s++) {
                Solution lowerLevelSol = lowerLevelSolutions.get(s);
                double[] lambda = lowerLevelSol.getLambda();
                Solution newSol = null;
                /*
                //Approach 1: Choose 1 of 2 heuristics based on lambda
                if (lambda[0] >= lambda[1]) {
                    newSol = (Solution) improvementOperatorD.execute(lowerLevelSol);
                } else {
                    newSol = (Solution) improvementOperatorC.execute(lowerLevelSol);
                }
                 */

                //Approach 2: Apply both heuristics sequentially
                /*
                newSol = (Solution) improvementOperatorD.execute(lowerLevelSol);
                newSol = (Solution) improvementOperatorC.execute(newSol);
                 */

                //Approach 3: Choose 1 of 3 heuristics based on probabilities and thresholds
                double rand = PseudoRandom.randDouble();
                if (lambda[0] >= lambda[1]){
                    //prioritize dissatisfaction
                    if (rand <= lambda[0]){
                        newSol = (Solution) improvementOperatorD.execute(lowerLevelSol);
                    } else {
                        newSol = (Solution) improvementOperatorD.execute(lowerLevelSol);
                        newSol = (Solution) improvementOperatorC.execute(newSol);
                    }
                } else {
                    //prioritize costs
                    if (rand <= lambda[1]){
                        newSol = (Solution) improvementOperatorC.execute(lowerLevelSol);
                    } else {
                        newSol = (Solution) improvementOperatorD.execute(lowerLevelSol);
                        newSol = (Solution) improvementOperatorC.execute(newSol);
                    }

                }

                newSol.setImprovedByLocalSearch(true);
                improvedLowerLevelSolutions.add(newSol);
            }
        } else {
            for (int s = 0; s < lowerLevelSolutions.size(); s++) {
                Solution lowerLevelSol = lowerLevelSolutions.get(s);
                Solution newSol = null;
                double rndSel = PseudoRandom.randDouble();

                //Approach 1: Choose 1 of 2 heuristics probabilistically
                /*
                if (rndSel < 0.5) {
                    newSol = (Solution) improvementOperatorD.execute(lowerLevelSol);
                } else {
                    newSol = (Solution) improvementOperatorC.execute(lowerLevelSol);
                }
                 */

                //Approach 2: Choose 1 of 3 heuristics probabilistically
                if (rndSel < 0.333) {
                    newSol = (Solution) improvementOperatorD.execute(lowerLevelSol);
                } else if (rndSel > 0.666) {
                    newSol = (Solution) improvementOperatorC.execute(lowerLevelSol);
                } else {
                    newSol = (Solution) improvementOperatorD.execute(lowerLevelSol);
                    newSol = (Solution) improvementOperatorC.execute(newSol);
                }

                newSol.setImprovedByLocalSearch(true);
                improvedLowerLevelSolutions.add(newSol);
            }
        }

        double best_result = Double.MAX_VALUE;
        int best_solution_index = -1;


        SolutionSet allSolutions;
        if (improvedLowerLevelSolutions.size() > 0) {
            allSolutions = lowerLevelSolutions.union(improvedLowerLevelSolutions);
            //Ranking toGetOnlyParetoOptimals = new Ranking(improvedLowerLevelSolutions);
            //SolutionSet paretoOptimalImprovedLL = toGetOnlyParetoOptimals.getSubfront(0);
            //allSolutions = lowerLevelSolutions.union(paretoOptimalImprovedLL);
        } else allSolutions = lowerLevelSolutions;

        //only keep the Pareto front of the solutions
        Ranking toGetOnlyParetoOptimals = new Ranking(allSolutions);
        SolutionSet paretoOptimal = toGetOnlyParetoOptimals.getSubfront(0);
        allSolutions = paretoOptimal;

        int improvedSurvivors = 0;
        //find best LL solution for the upper level
        for (int s=0; s<allSolutions.size(); s++) {
            Solution lowerLevelSol = allSolutions.get(s);

            if (lowerLevelSol.getImprovedByLocalSearch())
                improvedSurvivors++;

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
        Solution chosenlowerLevelSol = allSolutions.get(best_solution_index);
        Variable[] vars = chosenlowerLevelSol.getDecisionVariables();
        Binary bin = (Binary) vars[0];
        double[] energySpent = chosenlowerLevelSol.getSpentEnergy();

        //fill up upper-level solution with winner-LL-solution data
        solution.setSpentEnergy(energySpent);
        solution.setLowerLevelVars(bin);
        solution.setLowerLevelObj(new double[] {chosenlowerLevelSol.getObjective(0), chosenlowerLevelSol.getObjective(1)});
        solution.setDissatisfactionPerUser(chosenlowerLevelSol.getDissatisfactionPerUser());
        solution.setImprovedByLocalSearch(chosenlowerLevelSol.getImprovedByLocalSearch());
        solution.setEnergyAllocatedPerUser(chosenlowerLevelSol.getEnergyAllocatedPerUser());
        double deviation = calculateEnergyDeviationFromProduced(energySpent);
        solution.setEnergyDeviationFromProduced(deviation);
        double nonREpaid = calculateNonREPaid(energySpent, costs);
        solution.setNonREpaid(nonREpaid);
        solution.setDeviceToPreferenceMapping(chosenlowerLevelSol.getDeviceToPreferenceMapping());
	    solution.setReverseDeviceToPreferenceMapping(chosenlowerLevelSol.getReverseDeviceToPreferenceMapping());

        boolean improved_won = true;
        if (solution.getImprovedByLocalSearch() == false)
            improved_won = false;
        else {
            int a = 2;
        }

        if (best_upper_level_result > best_result){
            best_upper_level_result = best_result;
            int count = 0;
            if (improved_won) {
                int[] covered = solution.getDeviceToPreferenceMapping();
                for (int i = 0; i < covered.length; i++) {
                    if (covered[i] != -1 && covered[i] != i) {
                        count++;
                    }
                }
            }
            System.out.println("Improved Survivors: " + improvedSurvivors);
            System.out.println((improved_won?"WON":"LOS") + " " + best_upper_level_result + " " + count);

            /*
            SolutionSet chosenSolutionSet = new SolutionSet(1);
            chosenSolutionSet.add(chosenlowerLevelSol);
            if (this.lowerLevelAlgorithmName.equals("MOEAD"))
                chosenSolutionSet.printObjectivesToFile("LowerLevelParetoVisual/Misplacement/WithoutLocalSearch/" + (fileID) + "_CHOSEN");
            else chosenSolutionSet.printObjectivesToFile("LowerLevelParetoVisualNSGAII/Misplacement/WithoutLocalSearch/" + (fileID) + "_CHOSEN");
            //----------
            SolutionSet improvedSet = new SolutionSet(allSolutions.size());
            SolutionSet originalSet = new SolutionSet(allSolutions.size());
            for (int s=0; s<allSolutions.size(); s++){
                Solution sol = allSolutions.get(s);
                if (sol.getImprovedByLocalSearch())
                    improvedSet.add(sol);
                else
                    originalSet.add(sol);
            }
            if (this.lowerLevelAlgorithmName.equals("MOEAD")) {
                improvedSet.printObjectivesToFile("LowerLevelParetoVisual/Misplacement/WithLocalSearchD/" + (fileID) + "_FUN");
                originalSet.printObjectivesToFile("LowerLevelParetoVisual/Misplacement/WithoutLocalSearch/" + (fileID) + "_FUN");
            } else {
                improvedSet.printObjectivesToFile("LowerLevelParetoVisualNSGAII/Misplacement/WithLocalSearchD/" + (fileID) + "_FUN");
                originalSet.printObjectivesToFile("LowerLevelParetoVisualNSGAII/Misplacement/WithoutLocalSearch/" + (fileID) + "_FUN");
            }

            fileID++;
            
             */

        }


        //PRINT RESULTS
        //System.out.println(best_result);
        //System.out.println("Improved won?: " + improved_won);
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



