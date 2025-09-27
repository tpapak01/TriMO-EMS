package jmetal.metaheuristics.bilevel;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.metaheuristics.singleObjective.differentialEvolution.AdaptiveDE_CostDistr;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.selection.Selection;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.CostDistr;
import jmetal.problems.MOKP_Problem;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class UpperLevelCostDistr_AdaptiveDE {


    public static Logger logger_;      // Logger object
    public static FileHandler fileHandler_; // FileHandler object
    private static String problemPath = "C:\\Users\\emine\\IdeaProjects\\MOEAD test project\\";

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

        String problemName = args[0];
        String problemUserPreferences = args[1];
        String renewableName = args[2];
        String lowerLevelAlgorithmName = args[3];
        String costsName = "-";
        if (args.length > 4)
            costsName = args[4];
        String dataPath = "-";
        if (args.length > 5) {
            dataPath = args[5];
            if (!dataPath.equals("-")) { problemPath = dataPath; dataPath += "data\\"; }
        }
        String paretoFileName = "-";
        if (args.length > 6)
            paretoFileName = args[6];
        //initialize Lower Level algorithm
        MOKP_Problem lowerLevelProblem = new MOKP_Problem(problemName, problemUserPreferences, dataPath);
        if (lowerLevelAlgorithmName.equals("MOEAD"))
            LowerLevelMOKP_MOEAD.initializeAlgorithm(lowerLevelProblem, dataPath, paretoFileName);
        else
            LowerLevelMOKP_NSGAII.initializeAlgorithm(lowerLevelProblem, dataPath, paretoFileName);

        //thalis
        problem = new CostDistr(renewableName, lowerLevelProblem, lowerLevelAlgorithmName, costsName, dataPath);

        algorithm = new AdaptiveDE_CostDistr(problem, problemPath); // Generational GA

        /* Algorithm parameters*/
        algorithm.setInputParameter("populationSize", 100); //must be even number
        if (costsName.equals("-"))
            algorithm.setInputParameter("maxEvaluations", 1000000);
        else
            algorithm.setInputParameter("maxEvaluations", 100);


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
        Selection selection = SelectionFactory.getSelectionOperator("DifferentialEvolutionSelection", parameters) ;

        /* Comparator */
        Comparator comparator;
        if (problem.isMaxmized())
            comparator = new ObjectiveComparator(0, true) ; // Single objective comparator
        else comparator = new ObjectiveComparator(0) ; // Single objective comparator
        algorithm.setInputParameter("comparator", comparator);

        /* Add the operators to the algorithm*/
        algorithm.addOperator("crossover", crossover);
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
        population.printMyVariablesToFile("VAR", true);
        population.printMyVariablesToFile(problemPath + "results\\Winner\\Prices", false);

        population.printSelfConsumptionToFile("SELF_CONSU", true);
        population.printSelfConsumptionToFile(problemPath + "results\\Winner\\SELF_CONSU", false);
        population.printNonREPaidToFile("NON_RE_PAID");
        population.printLowerLevelVarsToFile("LL_VAR");
        population.printLowerLevelObjToFile("LL_FUN", true);
        population.printLowerLevelObjToFile(problemPath + "results\\Winner\\LL_FUN", false);
        population.printMappingToFile("LL_MAPPING");

        population.printSpentEnergyToFile("SPENT");
        population.printUserDissatisfactionToFile("USER_DISSAT", true);
        population.printUserDissatisfactionToFile(problemPath + "results\\Winner\\USER_DISSAT", false);
        population.printStdDevUserDissatisfactionToFile("STDDEV_USER_DISSAT");
        population.printUserEnergyToFile("USER_ENERGY");
        population.printUserEnergyToFile(problemPath + "results\\Winner\\USER_COSTS");
        population.printStdDevUserEnergyToFile("STDDEV_USER_ENERGY");

        FileWriter timeWriter = new FileWriter("TIME", true);
        timeWriter.write(estimatedTime + "\n");
        timeWriter.close();

        //SolutionSet specialPareto = population.get(0).getLL_Transfer_pop();
        //specialPareto.printObjectivesToFile("LowerLevelParetoVisual/" + "0_FUN"); //check


        SolutionSet lowerLevelSolutions = population.get(0).getLL_Pareto_pop();
        lowerLevelSolutions.printParetoToFile("LL_PARETO", true);
        System.out.println(population.get(0).getUL_Optimism());
        //Platform
        lowerLevelSolutions.printParetoToFile(problemPath + "results\\Winner\\Pareto", false);
        lowerLevelSolutions.printSpentsToFile(problemPath + "results\\Spents\\");
        lowerLevelSolutions.printSelfsToFile(problemPath + "results\\Selfs\\");
        lowerLevelSolutions.printUserDissatsToFile(problemPath + "results\\UserDissats\\");
        lowerLevelSolutions.printUserCostsToFile(problemPath + "results\\UserCosts\\");
        //population.printSpentEnergyToFile(platformPath + "Winner\\SPENT");



    } //main

}