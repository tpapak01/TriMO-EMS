package jmetal.metaheuristics.trilevel;

import jmetal.core.*;
import jmetal.metaheuristics.singleObjective.differentialEvolution.AdaptiveDE_CostDistr;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.selection.Selection;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.CostDistr;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XReal;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class UpperLevelCostDistr_AdaptiveDE implements Runnable {


    public static Logger logger_;      // Logger object
    public static FileHandler fileHandler_; // FileHandler object
    private static String problemPath = "C:\\Users\\emine\\source\\repos\\SmartHome3\\SmartHome3\\wwwroot\\";
    public static Algorithm algorithm ;
    public static CostDistr problem;         // The problem to solve

    public Algorithm alg_fast;
    public Algorithm getAlg_fast(){
        return alg_fast;
    }
    private CostDistr problemCostDistr;         // The problem to solve
    public CostDistr getProblemCostDistr(){
        return problemCostDistr;
    }

    /**
     * @throws JMException
     * @throws IOException
     * @throws SecurityException      Usage: three options
     *                                - jmetal.metaheuristics.moead.MOEAD_main
     *                                - jmetal.metaheuristics.moead.MOEAD_main problemName
     *                                - jmetal.metaheuristics.moead.MOEAD_main problemName ParetoFrontFile
     * @throws ClassNotFoundException
     */
    public static void initializeAlgorithm(Problem upperLevelProblem, Problem lowerLevelProblem,
                                           String dataPath, String paretoFileName) throws SecurityException, IOException, JMException {

        Operator crossover;         // Crossover operator

        //initialize Lower Level algorithm
        LowerLevelMOKP_MOEAD.initializeAlgorithm(lowerLevelProblem, dataPath, paretoFileName);

        //thalis
        problem = (CostDistr) upperLevelProblem;

        algorithm = new AdaptiveDE_CostDistr(problem); // Generational GA
        algorithm.setInputParameter("populationSize", 100); //must be even number
        algorithm.setInputParameter("maxEvaluations", 1000000);

        //thalis
        algorithm.setInputParameter("dataDirectory",
                "/Users/emine/IdeaProjects/JMETALHOME/data/MOEAD_parameters/Weight");

        /* Crossover operator */
        HashMap parameters = new HashMap() ;
        parameters.put("CR", 0.5);
        parameters.put("F", 0.5);
        parameters.put("K", 0.5);
        parameters.put("DE_VARIANT", "rand/1/bin");
        crossover = CrossoverFactory.getCrossoverOperator("DifferentialEvolutionCrossover", parameters);

        /* Selection Operator */
        parameters = null;
        Selection selection = SelectionFactory.getSelectionOperator("DifferentialEvolutionSelection", parameters);

        /* Comparator */
        Comparator comparator;
        if (problem.isMaxmized())
            comparator = new ObjectiveComparator(0, true) ; // Single objective comparator
        else comparator = new ObjectiveComparator(0) ; // Single objective comparator
        algorithm.setInputParameter("comparator", comparator);


        /* Add the operators to the algorithm*/
        algorithm.addOperator("crossover", crossover);
        algorithm.addOperator("selection", selection);

        ((AdaptiveDE_CostDistr) algorithm).initAdaptiveDECostDistr();

    } //main

    public UpperLevelCostDistr_AdaptiveDE(int id, XReal costOfBuying, Solution solution) throws JMException {
        this.id = id;
        problemCostDistr = new CostDistr(problem, costOfBuying, id);

        alg_fast = new AdaptiveDE_CostDistr(problemCostDistr, id);
        if (solution.isMarked())
            alg_fast.setInputParameter("initPopSolution", null);
        else alg_fast.setInputParameter("initPopSolution", solution.getUL_Transfer_pop());

    }

    private int id;
    public int getId(){
        return id;
    }
    public SolutionSet population;
    public SolutionSet getPopulation(){
        return population;
    }

    @Override
    public void run() {

        System.out.println("Starting now: " + id);

        while (true){

            population = null;

            /* Execute the Algorithm */
            try {
                population = alg_fast.execute();
            } catch (JMException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            break;

        }
    }
}