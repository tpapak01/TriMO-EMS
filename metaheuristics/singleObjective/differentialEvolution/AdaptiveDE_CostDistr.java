//  gGA.java
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

package jmetal.metaheuristics.singleObjective.differentialEvolution;

import jmetal.core.*;
import jmetal.encodings.variable.ArrayReal;
import jmetal.metaheuristics.moead.MOEAD;
import jmetal.operators.crossover.DifferentialEvolutionCrossover;
import jmetal.problems.CostDistr;
import jmetal.util.EuclideanDist;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;

/** 
 * Class implementing a generational genetic algorithm
 */
public class AdaptiveDE_CostDistr extends Algorithm {

  private CostDistr problemCostDistr;
  private SolutionSet population;
  public SolutionSet getPopulation(){
    SolutionSet pop = new SolutionSet(populationSize);
    for (int x=0; x<populationSize; x++){
      pop.add(population.get(x));
    }
    pop.sort(comparator);
    return population;
  }
  private int evaluations;
  private SolutionSet initPopSolution_;

  private final Object lock = new Object();
  public Object getLock(){
    return lock;
  }
  private int id;

  //STATIC
  private static int maxEvaluations ;
  private static int populationSize;

  private static Comparator comparator;

  private static Operator    mutationOperator  ;
  private static Operator    crossoverOperator ;
  private static Operator    selectionOperator ;

  private static double[] Fpool = new double[] {0.4, 0.5, 0.6};
  private static double[] CRpool = new double[] {0.7, 0.8, 0.9};
  private static double[] Kpool = new double[] {0.4, 0.5, 0.6};

 /**
  *
  * Constructor
  * Create a new GGA instance.
  * @param problem Problem to solve.
  */
 public AdaptiveDE_CostDistr(Problem problem, int idd){
   super(problem);
   id = idd;
   problemCostDistr = (CostDistr) problem_;
 }

  public AdaptiveDE_CostDistr(Problem problem){
    super(problem);
  } // GGA


  public void  initAdaptiveDECostDistr(){
    // Read the params
    populationSize = (Integer) this.getInputParameter("populationSize");
    maxEvaluations = (Integer) this.getInputParameter("maxEvaluations");

    comparator = (Comparator) this.getInputParameter("comparator");

    // Read the operators
    selectionOperator = operators_.get("selection");
    crossoverOperator = operators_.get("crossover");
    mutationOperator  = operators_.get("mutation");
  }
  
 /**
  * Execute the GGA algorithm
 * @throws JMException 
  */
  public SolutionSet execute() throws JMException, ClassNotFoundException {
   
    // Initialize the variables
    population          = new SolutionSet(populationSize) ;
    
    evaluations  = 0;

    initPopSolution_ = ((SolutionSet) this.getInputParameter("initPopSolution"));

    initPopulation();
    //double[] producedRE = problemCostDistr.getProducedRE();
    //initPopulationCostDistr(producedRE);

    // Sort population
    population.sort(comparator) ;

    synchronized (lock) {
      lock.notify();
    }

    //Convergence
    int generations_left_for_convergence = 5;
    int converged = generations_left_for_convergence;
    double best_solution = population.get(0).getObjective(0);
    //System.out.println("BEST SOL: " + best_solution);

    int stop = 0;
    int generation = 0;

    int flag = PseudoRandom.randInt(1,3);

    while (evaluations < maxEvaluations && converged != 0) {

      HashMap parameters = new HashMap();
      parameters.put("CR", CRpool[flag-1]);
      parameters.put("F", Fpool[flag-1]);
      parameters.put("K", Kpool[flag-1]);
      //parameters.put("DE_VARIANT", "rand/1/bin");
      ((DifferentialEvolutionCrossover) crossoverOperator).updateParameters(parameters);

      Solution winner = population.get(0);
      //System.out.println("q: " + winner.getUL_Optimism());
      /*
      SolutionSet winnerPareto = winner.getLL_Pareto_pop();
      winnerPareto.printParetoToFile(problemPath + "results\\Paretos\\PARETO_" + (generation));
      SolutionSet onlyWinner = new SolutionSet(1);
      onlyWinner.add(winner);
      onlyWinner.printSelfConsumptionToFile(problemPath + "results\\SelfsInProgress\\SELF_" + (generation));
       */
      generation++;

      SolutionSet offspringPopulation = new SolutionSet(populationSize) ;


      double diff = Math.abs(winner.getObjective(0) - population.get(populationSize-1).getObjective(0));
      if (diff <= 10 ) {
        ((MOEAD) problemCostDistr.getLL_wrapper().alg_moead).setConv(0.001);
      }
      if (diff <= 5 ) {
        ((MOEAD) problemCostDistr.getLL_wrapper().alg_moead).setConv(0.0001);
      }

      // Reproductive cycle: keep adding 2 offspring to the offspring population until it reaches the max size
      for (int i = 0 ; i < populationSize; i++) {
        Solution current = population.get(i);

        // Selection: 3 random parents
        // Two parameters are required: the population and the index of the current individual
        Solution[] tempParents = (Solution [])selectionOperator.execute(new Object[]{population, i});
        Solution[] parents = new Solution[4];
        parents[0] = tempParents[0]; parents[1] = tempParents[1]; parents[2] = tempParents[2];
        parents[3] = winner;

        // Crossover.
        // Two parameters are required: the current individual and the array of parents
        Solution offspring = (Solution)crossoverOperator.execute(new Object[]{current, parents}) ;

        // Mutation
        mutationOperator.execute(offspring);

        //attach to offspring the LL ND of the closest UL solution
        double min_dist = Double.MAX_VALUE;
        int min_index = -1;

        Double[] offspringVar = (((ArrayReal) offspring.getDecisionVariables()[0]).array_);
        for (int j = 0; j < populationSize; j++) {
          Double[] popSolutionVar = (((ArrayReal) population.get(j).getDecisionVariables()[0]).array_);
          double dist = EuclideanDist.distance(offspringVar, popSolutionVar);
          if (dist < min_dist){
            min_dist = dist;
            min_index = j;
          }
        }
        offspring.setLL_Transfer_pop(population.get(min_index).getLL_Transfer_pop());

        problemCostDistr.repair(offspring);

        // Evaluation of the new individuals
        problem_.evaluate(offspring);
        evaluations++;

        offspringPopulation.add(offspring);

        if (stop == 1){
          break;
        }

      } // for

      // The offspring population is added to the new current population
      SolutionSet popCombined = population.union(offspringPopulation);
      offspringPopulation.clear();

      popCombined.sort(comparator);
      for (int i = popCombined.size()-1; i >= populationSize; i--) {
        popCombined.remove(i);
      }
      SolutionSet old = population;
      population = popCombined;
      old.clear();

      if (stop == 1){
        break;
      }

      winner = population.get(0);
      //check for convergence
      if (winner.getObjective(0) > best_solution - 1){
        converged--;
        flag = PseudoRandom.randInt(1,3);
      } else {
        best_solution = winner.getObjective(0);
        //System.out.println("BEST SOL at " + evaluations + ": " + best_solution);
        converged = generations_left_for_convergence;
      }

    } // while
    
    // Return the population
    SolutionSet resultPopulation = new SolutionSet(populationSize) ;
    //for (int i = 0; i < (populationSize * 3 / 4); i++) {
    for (int i = 0; i < populationSize; i++) {
      resultPopulation.add(population.get(i)) ;
    }
    
    //System.out.println("Evaluations: " + evaluations ) ;
    FileWriter evalsWriter = null;
    try {
      evalsWriter = new FileWriter("EVALS", true);
      evalsWriter.write(evaluations + "\n");
      evalsWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return resultPopulation ;
  } // execute

  /**
   *
   */
  public void initPopulation() throws JMException, ClassNotFoundException {
    if (CostDistr.getInputCosts() == null) {
      if (initPopSolution_ == null) {
        for (int i = 0; i < populationSize; i++) {
          Solution newSolution = new Solution(problem_, true);

          problem_.evaluate(newSolution);
          evaluations++;
          population.add(newSolution);
        } // for
      } else {
        for (int i = 0; i < initPopSolution_.size(); i++) {
          Solution toAdd = initPopSolution_.get(i);
          Solution newSolution = new Solution(toAdd);
          problemCostDistr.repair(newSolution);
          problem_.evaluate(newSolution);
          evaluations++;
          population.add(newSolution);
        }
        for (int i = initPopSolution_.size(); i < populationSize; i++) {
          Solution newSolution = new Solution(problem_, true);

          problem_.evaluate(newSolution);
          evaluations++;
          population.add(newSolution);
        }
      }
    } else {
      for (int i = 0; i < populationSize; i++) {
        Solution newSolution = new Solution(problem_, true);
        newSolution.setDecisionVariables(updateSolution(CostDistr.getInputCosts()));
        problem_.evaluate(newSolution);
        evaluations++;
        population.add(newSolution);
      } // for
    }
  } // initPopulation

  public Variable[] updateSolution(double[] costsToSend) throws JMException {
    Variable [] variables = new Variable[1];
    ArrayReal arrayReal = new ArrayReal(problem_.getNumberOfVariables(), problem_);
    for (int j=0; j<problem_.getNumberOfVariables(); j++)
      arrayReal.setValue(j, costsToSend[j]);
    variables[0] = arrayReal;
    return variables;
  }


} // gGA