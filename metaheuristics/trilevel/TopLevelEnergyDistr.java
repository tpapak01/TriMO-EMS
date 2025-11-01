package jmetal.metaheuristics.trilevel;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.Fast_CostDistr;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.Fast_EnergyDistr;
import jmetal.operators.crossover.SBXCrossover;
import jmetal.operators.mutation.UniformMutation;
import jmetal.operators.selection.BinaryTournament;
import jmetal.operators.selection.Selection;
import jmetal.problems.CostDistr;
import jmetal.problems.EnergyDistr;
import jmetal.problems.MOKP_Problem;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.comparators.ObjectiveComparator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class TopLevelEnergyDistr {


    public static Logger logger_;      // Logger object
    public static FileHandler fileHandler_; // FileHandler object
    private static String problemPath = "C:\\Users\\emine\\source\\repos\\SmartHome3\\SmartHome3\\wwwroot\\";

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

        new PseudoRandom(0.454545);

        Problem problem;         // The problem to solve
        Algorithm algorithm;         // The algorithm to use
        Operator crossover;         // Crossover operator
        Operator mutation;         // Mutation operator
        //Operator  selection ;         // Selection operator

        //int bits ; // Length of bit string in the OneMax problem
        HashMap parameters; // Operator parameters

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

        String generatorsName = "-";
        if (args.length > 7)
            generatorsName = args[7];

        //initialize Lower Level problem
        MOKP_Problem lowerLevelProblem = new MOKP_Problem(problemName, problemUserPreferences, dataPath);

        //initialize Upper Level problem
        CostDistr upperLevelProblem =
                new CostDistr(renewableName, lowerLevelProblem, lowerLevelAlgorithmName, costsName, dataPath);

        //initialize Upper Level algorithm
        UpperLevelCostDistr_Fast.initializeAlgorithm(upperLevelProblem, lowerLevelProblem, dataPath, paretoFileName);

        //thalis
        problem = new EnergyDistr(upperLevelProblem, dataPath, costsName, generatorsName, false);
        //thalis comment
        //int bits = 512 ;
        //problem = new OneMax("Binary", bits);

        //problem = new Sphere("Real", 10) ;
        //problem = new Easom("Real") ;
        //problem = new Griewank("Real", 10) ;

        algorithm = new Fast_EnergyDistr(problem, problemPath); // Generational GA
        //algorithm = new ssGA(problem); // Steady-state GA
        //algorithm = new scGA(problem) ; // Synchronous cGA
        //algorithm = new acGA(problem) ;   // Asynchronous cGA

        /* Algorithm parameters*/
        //algorithm.setInputParameter("populationSize",4); //must be even number
        //algorithm.setInputParameter("maxEvaluations", 2500);
        algorithm.setInputParameter("populationSize", 10); //must be even number
        algorithm.setInputParameter("maxEvaluations", 1000000);


        //thalis
        algorithm.setInputParameter("dataDirectory",
                "/Users/emine/IdeaProjects/JMETALHOME/data/MOEAD_parameters/Weight");

        /*
        // Mutation and Crossover for Real codification
        parameters = new HashMap() ;
        parameters.put("probability", 0.9) ;
        parameters.put("distributionIndex", 20.0) ;
        crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", parameters);

        parameters = new HashMap() ;
        parameters.put("probability", 1.0/problem.getNumberOfVariables()) ;
        parameters.put("distributionIndex", 20.0) ;
        mutation = MutationFactory.getMutationOperator("PolynomialMutation", parameters);
        */

        // Mutation and Crossover for Binary codification
        //thalis comment
        /*
        parameters = new HashMap() ;
        parameters.put("probability", 0.9) ;
        crossover = CrossoverFactory.getCrossoverOperator("SinglePointCrossover", parameters);
         */

        /* Crossover operator */
        //thalis
        parameters = new HashMap();
        //parameters.put("probability", CR_) ;
        //parameters.put("distributionIndex", F_) ;
        crossover = new SBXCrossover(parameters);

        /* Mutation Operator */
        //thalis comment
        /*
        parameters = new HashMap() ;
        parameters.put("probability", 1.0/bits) ;
        mutation = MutationFactory.getMutationOperator("BitFlipMutation", parameters);
         */
        //thalis
        Double perturbationIndex = 0.5;
        Double mutationProbability = 0.2; //1.0 / problem.getNumberOfVariables();
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
        //thalis comment
        /*
        parameters = null ;
        selection = SelectionFactory.getSelectionOperator("BinaryTournament", parameters) ;
         */
        //thalis
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
        population.printObjectivesToFile("TL_FUN");
        System.out.println("Variables values have been writen to file VAR");
        population.printVariablesToFile("TL_VAR");
        population.printVariablesToFile(problemPath + "results\\Winner\\Prices");

        population.printProfitToFile("PROFIT", true);
        population.printProfitToFile("UL_FUN", true);
        population.printUpperLevelVarsToFile("UL_VAR");

        //population.printEnergyDeviationFromProducedToFile("SELF_CONSU");
        population.printSelfConsumptionToFile("SELF_CONSU", true);
        population.printSelfConsumptionToFile(problemPath + "results\\Winner\\SELF_CONSU", false);
        //population.printNonREPaidToFile("NON_RE_PAID");
        population.printLowerLevelVarsToFile("LL_VAR");
        population.printLowerLevelObjToFile("LL_FUN");
        population.printLowerLevelObjToFile(problemPath + "results\\Winner\\LL_FUN");
        //population.printMappingToFile("LL_MAPPING");

        population.printSpentEnergyToFile("SPENT");
        //population.printUserDissatisfactionToFile("USER_DISSAT");
        population.printUserDissatisfactionToFile(problemPath + "results\\Winner\\USER_DISSAT");
        //population.printStdDevUserDissatisfactionToFile("STDDEV_USER_DISSAT");
        //population.printUserEnergyToFile("USER_ENERGY");
        population.printUserEnergyToFile(problemPath + "results\\Winner\\USER_COSTS");
        //population.printStdDevUserEnergyToFile("STDDEV_USER_ENERGY");

        population.printReimbuPenaltyToFile("REIMBU_PEN", false);
        population.printIncomePenaltyToFile("INCOME_PEN", false);

        FileWriter timeWriter = new FileWriter("TIME", true);
        timeWriter.write(estimatedTime + "\n");
        timeWriter.close();

        //SolutionSet specialPareto = population.get(0).getLL_Transfer_pop();
        //specialPareto.printObjectivesToFile("LowerLevelParetoVisual/" + "0_FUN"); //check


        SolutionSet lowerLevelSolutions = population.get(0).getLL_Pareto_pop();
        //lowerLevelSolutions.printParetoToFile("LL_PARETO");
        //System.out.println(population.get(0).getUL_Optimism());
        //Platform
        lowerLevelSolutions.printParetoToFile(problemPath + "results\\Winner\\Pareto");
        lowerLevelSolutions.printSpentsToFile(problemPath + "results\\Spents\\");
        lowerLevelSolutions.printSelfsToFile(problemPath + "results\\Selfs\\");
        //population.printSpentEnergyToFile(platformPath + "Winner\\SPENT");

        System.exit(0);

    } //main

}