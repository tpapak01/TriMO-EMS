package jmetal.metaheuristics.trilevel;

import jmetal.core.*;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.Fast_CostDistr;
import jmetal.operators.crossover.SBXCrossover;
import jmetal.operators.mutation.UniformMutation;
import jmetal.operators.selection.BinaryTournament;
import jmetal.operators.selection.Selection;
import jmetal.problems.CostDistr;
import jmetal.problems.MOKP_Problem;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XReal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class UpperLevelCostDistr_Fast implements Runnable {


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
                                           String dataPath, String paretoFileName) throws SecurityException, IOException {

        Operator crossover;         // Crossover operator
        Operator mutation;         // Mutation operator
        //Operator  selection ;         // Selection operator

        //int bits ; // Length of bit string in the OneMax problem
        HashMap parameters; // Operator parameters

        //initialize Lower Level algorithm
        LowerLevelMOKP_MOEAD.initializeAlgorithm(lowerLevelProblem, dataPath, paretoFileName);

        //thalis
        problem = (CostDistr) upperLevelProblem;

        algorithm = new Fast_CostDistr(problem, problemPath); // Generational GA
        algorithm.setInputParameter("populationSize", 100); //must be even number
        algorithm.setInputParameter("maxEvaluations", 1000000);

        //thalis
        algorithm.setInputParameter("dataDirectory",
                "/Users/emine/IdeaProjects/JMETALHOME/data/MOEAD_parameters/Weight");


        /* Crossover operator */
        parameters = new HashMap();
        crossover = new SBXCrossover(parameters);

        /* Mutation operator */
        Double perturbationIndex = 0.5;
        Double mutationProbability = 1.0 / problem.getNumberOfVariables();
        parameters = new HashMap();
        parameters.put("probability", mutationProbability);
        parameters.put("perturbation", perturbationIndex);
        mutation = new UniformMutation(parameters);

        /* Comparator */
        Comparator comparator;
        if (problem.isMaxmized())
            comparator = new ObjectiveComparator(0, true) ; // Single objective comparator
        else comparator = new ObjectiveComparator(0) ; // Single objective comparator
        algorithm.setInputParameter("comparator", comparator);

        /* Selection Operator */
        parameters.put("comparator", comparator);
        Selection selection = new BinaryTournament(parameters);

        /* Add the operators to the algorithm*/
        algorithm.addOperator("crossover", crossover);
        algorithm.addOperator("mutation", mutation);
        algorithm.addOperator("selection", selection);

    } //main

    public UpperLevelCostDistr_Fast(int id, XReal costOfBuying, Solution solution) throws JMException {
        this.id = id;
        problemCostDistr = new CostDistr(problem);
        problemCostDistr.setCostOfBuying(costOfBuying);
        problemCostDistr.setCostLowerLimit(costOfBuying);

        alg_fast = new Fast_CostDistr(problemCostDistr, algorithm);
        if (solution.isMarked())
            alg_fast.setInputParameter("initPopSolution", null);
        else alg_fast.setInputParameter("initPopSolution", solution.getUL_Transfer_pop());

    }

    private int id;
    private static boolean go;
    public boolean getGo(){
        return go;
    }
    public void setGo(boolean go){
        this.go = go;
    }
    public SolutionSet population;
    public SolutionSet getPopulation(){
        return population;
    }

    @Override
    public void run() {

        System.out.println("Starting now");

        while (true){

            while (!go){
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            population = null;

            /* Execute the Algorithm */
            try {
                population = alg_fast.execute();
            } catch (JMException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            setGo(false);
            break;

        }
    }
}