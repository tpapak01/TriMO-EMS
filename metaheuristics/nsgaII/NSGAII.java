//  NSGAII.java
//
//  Author:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jmetal.metaheuristics.nsgaII;

import jmetal.core.*;
import jmetal.encodings.variable.Binary;
import jmetal.problems.MOKP_Problem;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Distance;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.Ranking;
import jmetal.util.comparators.CrowdingComparator;
import jmetal.util.comparators.DominanceComparator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;

/** 
 *  Implementation of NSGA-II.
 *  This implementation of NSGA-II makes use of a QualityIndicator object
 *  to obtained the convergence speed of the algorithm. This version is used
 *  in the paper:
 *     A.J. Nebro, J.J. Durillo, C.A. Coello Coello, F. Luna, E. Alba 
 *     "A Study of Convergence Speed in Multi-Objective Metaheuristics." 
 *     To be presented in: PPSN'08. Dortmund. September 2008.
 */

public class NSGAII extends Algorithm {

  private MOKP_Problem problemMOKP;
  /**
   * Stores the population
   */

  private int evaluations_;

  //thalis
  private static final Comparator dominance_ = new DominanceComparator();

  /**
   * Constructor
   * @param problem Problem to solve
   */
  public NSGAII(Problem problem) {
    super (problem);
    problemMOKP = (MOKP_Problem) problem;
    /*
    try {
      evalsWriter = new FileWriter("LowerLevelParetoVisualNSGAII/evals.txt");
      spreadWriter = new FileWriter("LowerLevelParetoVisualNSGAII/spread.txt");
      hypervolumeWriter = new FileWriter("LowerLevelParetoVisualNSGAII/hypervolume.txt");
      ndsWriter = new FileWriter("LowerLevelParetoVisualNSGAII/nds.txt");
    } catch (IOException e) {
      e.printStackTrace();
    }
     */
  } // NSGAII

  //statistical analysis
  private static int execution = 0;
  private static double evals_mean = 0;
  private static double spread_mean = 0;
  private static double hypervolume_mean = 0;
  private static double nds_mean = 0;
  private static FileWriter evalsWriter;
  private static FileWriter spreadWriter;
  private static FileWriter hypervolumeWriter;
  private static FileWriter ndsWriter;

  /**   
   * Runs the NSGA-II algorithm.
   * @return a <code>SolutionSet</code> that is a set of non dominated solutions
   * as a result of the algorithm execution
   * @throws JMException 
   */
  public SolutionSet execute() throws JMException, ClassNotFoundException {
    int maxEvaluations;
    int populationSize;
    SolutionSet population;

    QualityIndicator indicators; // QualityIndicator object
    //int requiredEvaluations; // Use in the example of use of the

    SolutionSet offspringPopulation;
    SolutionSet union;

    Operator mutationOperator;
    Operator crossoverOperator;
    Operator selectionOperator;

    Comparator  comparator;
    double previousHypervolume;

    Distance distance = new Distance();

    //Read the parameters
    populationSize = ((Integer) getInputParameter("populationSize")).intValue();
    maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
    indicators = (QualityIndicator) getInputParameter("indicators");

    //Initialize the variables
    population = new SolutionSet(populationSize);
    evaluations_ = 0;

    //requiredEvaluations = 0;

    //Read the operators
    comparator = (Comparator) this.getInputParameter("comparator");
    mutationOperator = operators_.get("mutation");
    crossoverOperator = operators_.get("crossover");
    selectionOperator = operators_.get("selection");

    // Create the initial solutionSet
    initPopulation(population, populationSize);

    //used for convergence observation
    int threshold = 0;
    int iteration = 0;
    //used for convergence
    boolean converged = false;
    Ranking r = new Ranking(population);
    SolutionSet pareto = r.getSubfront(0);
    previousHypervolume = indicators.getHypervolume(pareto);

    // Generations 
    while (evaluations_ < maxEvaluations && converged == false) {

      // Create the offSpring solutionSet      
      offspringPopulation = new SolutionSet(populationSize);
      Solution[] parents = new Solution[2];
      boolean doubleOffspring = false;
      int iterations = populationSize;
      if (doubleOffspring)
        iterations = populationSize / 2;
      for (int i = 0; i < iterations; i++) {
        if (evaluations_ < maxEvaluations) {
          //obtain parents
          parents[0] = (Solution) selectionOperator.execute(population);
          parents[1] = (Solution) selectionOperator.execute(population);
          Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);
          mutationOperator.execute(offSpring[0]);
          mutationOperator.execute(offSpring[1]);
          problem_.evaluate(offSpring[0]);
          //problem_.evaluateConstraints(offSpring[0]);
          problem_.evaluate(offSpring[1]);
          //problem_.evaluateConstraints(offSpring[1]);
          evaluations_ += 2;

          if (doubleOffspring) {

            offspringPopulation.add(offSpring[0]);
            offspringPopulation.add(offSpring[1]);

          } else { // Best offspring

            int flagDominate;
            if (problem_.isMaxmized() == false)
              flagDominate = dominance_.compare(offSpring[0], offSpring[1]);
            else flagDominate = dominance_.compare(offSpring[1], offSpring[0]);

            if (flagDominate == -1)
              offspringPopulation.add(offSpring[0]);
            else if (flagDominate == 1)
              offspringPopulation.add(offSpring[1]);
            else {
              double rndSel = PseudoRandom.randDouble();
              if (rndSel < 0.5)
                offspringPopulation.add(offSpring[0]);
              else
                offspringPopulation.add(offSpring[1]);
            }

          } // end of dilemma

        } // if                            
      } // for

      // Create the solutionSet union of solutionSet and offSpring
      union = population.union(offspringPopulation);

      // Ranking the union
      Ranking ranking = new Ranking(union);

      int remain = populationSize;
      int index = 0;
      SolutionSet front = null;
      population.clear();

      // Obtain the next front
      front = ranking.getSubfront(index);

      while ((remain > 0) && (remain >= front.size())) {
        //Assign crowding distance to individuals (for later in tournament selection)
        distance.crowdingDistanceAssignment(front, problem_.getNumberOfObjectives());
        //Add the individuals of this front
        for (int k = 0; k < front.size(); k++) {
          population.add(front.get(k));
        } // for

        //Decrement remain
        remain = remain - front.size();

        //Obtain the next front
        index++;
        if (remain > 0) {
          front = ranking.getSubfront(index);
        }
      } // while

      // Remain is less than front(index).size, insert only the best solutions
      // by sorting the front first
      if (remain > 0) {  // front contains individuals to insert                        
        distance.crowdingDistanceAssignment(front, problem_.getNumberOfObjectives());
        front.sort(new CrowdingComparator());
        for (int k = 0; k < remain; k++) {
          population.add(front.get(k));
        }
      }

      /*
      // This piece of code shows how to use the indicator object into the code
      // of NSGA-II. In particular, it finds the number of evaluations required
      // by the algorithm to obtain a Pareto front with a hypervolume higher
      // than the hypervolume of the true Pareto front.
      if ((indicators != null) &&
          (requiredEvaluations == 0)) {
        double HV = indicators.getHypervolume(population);
        if (HV >= (0.98 * indicators.getTrueParetoFrontHypervolume())) {
          requiredEvaluations = evaluations;
        } // if
      } // if
       */

      if (evaluations_ > threshold){
        threshold += 500;
        Ranking convRanking = new Ranking(population);
        SolutionSet paretoFront = convRanking.getSubfront(0);
        double hypervolume = indicators.getHypervolume(paretoFront);

        double diff = hypervolume - previousHypervolume;
        if (diff <= 0)
          converged = true;
        //else if (execution < 10)
        //  paretoFront.printObjectivesToFile("LowerLevelParetoEvolutionNSGAII/" + execution + "_FUN_" + iteration++);

        previousHypervolume = hypervolume;
      }

    } // while

    execution++;

    // Return as output parameter the required evaluations
    //setOutputParameter("evaluations", requiredEvaluations);


    // Return the first non-dominated front
    Ranking ranking = new Ranking(population);
    SolutionSet paretoFront = ranking.getSubfront(0);
    population = paretoFront;
    population.sort(comparator) ;

    //thalis
    //register Spent Energy of top solutions, to be used when comparing equal solutions
    for (int i = 0; i < population.size(); i++) {
      problemMOKP.calculateSpentEnergy(population.get(i));
    }

    //thalis
    // At last remove identical solutions, based not only on objective value, but also decision vector
    SolutionSet finalSet = new SolutionSet(population.size());
    finalSet.add(population.get(0));

    for (int i = 1; i < population.size(); i++) {
      Solution sol = population.get(i);
      boolean existEqual = false;

      for (int j = 0; j < finalSet.size();j++) {
        if (equalSolution(sol, finalSet.get(j))) {
          existEqual = true;
          break;
        }
      }

      if (existEqual)
        continue;

      finalSet.add(population.get(i));

    } // for

    //print final Pareto Front to file, and calculate/print hypervolume and spread (Delta)
    /*
    Ranking finalRanking = new Ranking(finalSet);
    SolutionSet finalParetoFront = finalRanking.getSubfront(0);

    finalParetoFront.printObjectivesToFile("LowerLevelParetoVisualNSGAII/" + (execution) + "_FUN");
    double spread = indicators.getSpread(finalParetoFront);
    double hypervolume = indicators.getHypervolume(finalParetoFront);
    double nds = finalParetoFront.size();
    try {
      evals_mean += evaluations_;
      evalsWriter.write(evaluations_ + "\n");

      spread_mean += spread;
      spreadWriter.write(spread + "\n");

      hypervolume_mean += hypervolume;
      hypervolumeWriter.write(hypervolume + "\n");

      nds_mean += nds;
      ndsWriter.write(nds + "\n");

      if (execution == 20) {
        evals_mean = evals_mean / 20;
        spread_mean = spread_mean / 20;
        hypervolume_mean = hypervolume_mean / 20;
        nds_mean = nds_mean / 20;
        evalsWriter.write(evals_mean + "\n");
        spreadWriter.write(spread_mean + "\n");
        hypervolumeWriter.write(hypervolume_mean + "\n");
        ndsWriter.write(nds_mean + "\n");
        evalsWriter.close();
        spreadWriter.close();
        hypervolumeWriter.close();
        ndsWriter.close();
      }

    } catch(Exception e){}
    
     */


    return finalSet;
  } // execute

  public boolean equalSolution (Solution sol1, Solution sol2) {
    for (int i = 0; i < sol1.getNumberOfObjectives();i++) {
      if (sol1.getObjective(i) != sol2.getObjective(i))
        return false;
    }
    double[] spentEnergy1 = sol1.getSpentEnergy();
    double[] spentEnergy2 = sol2.getSpentEnergy();
    for (int i=0; i<spentEnergy1.length; i++)
      if (spentEnergy1[i] != spentEnergy2[i])
        return false;
    return true;
    /*
    if (!sol1.getDecisionVariables()[0].toString().equals(
            sol2.getDecisionVariables()[0].toString()
    )) return false;
    return true;
     */
  }

  /**
   *
   */
  public void initPopulation(SolutionSet population, int populationSize) throws JMException, ClassNotFoundException {

    int numberOfUsers = problemMOKP.getNumberOfUsers();
    int numberOfItems = problemMOKP.getNumberOfItems();
    int numOfConstraints = problem_.getNumberOfConstraints();
    int numOfBits = numberOfUsers * numberOfItems * numOfConstraints;

    for (int i = 0; i < populationSize; i++) {
      Solution newSolution = new Solution(problem_);

      /*
      // all devices
      if (i == 1) {
        newSolution.setDecisionVariables(updateSolution(numOfBits, true));
      }
       */

      // zero devices
      if (i == 0) {
        newSolution.setDecisionVariables(updateSolution(numOfBits, false));
      }

      // exactly what the users want
      if (i == populationSize-1) {
        boolean[] pref_vector = problemMOKP.getUserPreferenceVector();
        Variable[] vars = new Variable[problem_.getNumberOfVariables()];
        for (int v = 0; v < vars.length; v++) {
          Binary bin = new Binary(numOfBits);

          for (int j = 0; j < numOfBits; j++) { // for each user
            bin.setIth(j, pref_vector[j]);
          }

          vars[v] = bin;
        }

        newSolution.setDecisionVariables(vars);
      }

      problemMOKP.repair(newSolution);

      problem_.evaluate(newSolution);
      evaluations_++;
      population.add(newSolution) ;
    } // for
  }

  public Variable[] updateSolution(int numOfBits, Boolean val) {
    Variable[] vars = new Variable[problem_.getNumberOfVariables()];
    for (int j = 0; j < vars.length; j++) {
      Binary bin = new Binary(numOfBits);
      for (int k = 0; k < numOfBits; k++) {
        bin.setIth(k, val);
      }
      vars[j] = bin;
    }
    return vars;
  }



} // NSGA-II




