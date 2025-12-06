package jmetal.metaheuristics.bilevel;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.metaheuristics.singleObjective.Custom.Custom_CostDistr;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.gGA_CostDistr;
import jmetal.operators.crossover.SBXCrossover;
import jmetal.operators.mutation.UniformMutation;
import jmetal.operators.selection.BinaryTournament;
import jmetal.operators.selection.Selection;
import jmetal.problems.CostDistr;
import jmetal.problems.MOKP_Problem;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class UpperLevelCostDistr_Custom {


    public static Logger logger_;      // Logger object
    public static FileHandler fileHandler_; // FileHandler object

    /**
     * @param args Command line arguments. The first (optional) argument specifies
     *             the problem to solve.
     * @throws JMException
     * @throws IOException
     * @throws SecurityException      Usage: three options
     *                                - jmetal.metaheuristics.moead.MOEAD_main
     *                                - jmetal.metaheuristics.moead.MOEAD_main problemName
     *                                - jmetal.metaheuristics.moead.MOEAD_main problemName ParetoFrontFile
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws JMException, SecurityException, IOException, ClassNotFoundException {

        Problem problem;         // The problem to solve
        Algorithm algorithm;         // The algorithm to use
        Operator crossover;         // Crossover operator
        Operator mutation;         // Mutation operator
        //Operator  selection ;         // Selection operator

        //int bits ; // Length of bit string in the OneMax problem
        HashMap parameters; // Operator parameters

        String problemName = args[0];
        String problemUserPreferences = args[1];
        String lowerLevelAlgorithmName = args[2];
        String costsName = "-";
        if (args.length > 3)
            costsName = args[3];
        String dataPath = "-";
        if (args.length > 4)
            dataPath = args[4];
        String paretoFileName = "-";
        if (args.length > 5)
            paretoFileName = args[5];
        //initialize Lower Level algorithm
        MOKP_Problem lowerLevelProblem = new MOKP_Problem(problemName, problemUserPreferences, dataPath);
        if (lowerLevelAlgorithmName.equals("MOEAD"))
            LowerLevelMOKP_MOEAD.initializeAlgorithm(lowerLevelProblem, dataPath, paretoFileName);
        else
            LowerLevelMOKP_NSGAII.initializeAlgorithm(lowerLevelProblem, dataPath, paretoFileName);

        //thalis
        problem = new CostDistr(problemName, lowerLevelProblem, lowerLevelAlgorithmName, costsName, dataPath);

        algorithm = new Custom_CostDistr(problem); // Generational GA


        /* Algorithm parameters*/
        //algorithm.setInputParameter("populationSize",4); //must be even number
        //algorithm.setInputParameter("maxEvaluations", 2500);
        algorithm.setInputParameter("populationSize", 20); //must be even number
        if (costsName.equals("-"))
            algorithm.setInputParameter("maxEvaluations", 1000000);
        else
            algorithm.setInputParameter("maxEvaluations", 100);


        //thalis
        algorithm.setInputParameter("dataDirectory",
                "/Users/emine/IdeaProjects/JMETALHOME/data/MOEAD_parameters/Weight");

        /* Crossover operator */
        //thalis
        parameters = new HashMap();
        crossover = new SBXCrossover(parameters);

        //thalis
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

        /* Execute the Algorithm */
        long initTime = System.currentTimeMillis();
        SolutionSet population = algorithm.execute();
        long estimatedTime = System.currentTimeMillis() - initTime;
        System.out.println("Total execution time: " + estimatedTime);

        /* Log messages */
        System.out.println("Objectives values have been writen to file FUN");
        population.printObjectivesToFile("FUN");
        System.out.println("Variables values have been writen to file VAR");
        population.printVariablesToFile("VAR", true);

        population.printSpentEnergyToFile("SPENT");
        population.printLowerLevelVarsToFile("LL_VAR");
        population.printLowerLevelObjToFile("LL_FUN", true);


    } //main

}