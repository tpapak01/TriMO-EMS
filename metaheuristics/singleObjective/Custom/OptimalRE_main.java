package jmetal.metaheuristics.singleObjective.Custom;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.metaheuristics.bilevel.LowerLevelMOKP_MOEAD;
import jmetal.metaheuristics.bilevel.LowerLevelMOKP_NSGAII;
import jmetal.metaheuristics.singleObjective.geneticAlgorithm.gGA_CostDistr;
import jmetal.operators.crossover.SBXCrossover;
import jmetal.operators.mutation.UniformMutation;
import jmetal.operators.selection.BinaryTournament;
import jmetal.operators.selection.Selection;
import jmetal.problems.CostDistr;
import jmetal.problems.MOKP_Problem;
import jmetal.problems.REproblem;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class OptimalRE_main {


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

        String problemName = args[0];
        String problemUserPreferences = args[1];
        String costsName = args[2];
        //initialize Lower Level algorithm

        //thalis
        problem = new REproblem(problemName, problemUserPreferences, costsName);

        algorithm = new Optimal_REproblem(problem); // Generational GA

        /* Comparator */
        Comparator comparator;
        if (problem.isMaxmized())
            comparator = new ObjectiveComparator(0, true) ; // Single objective comparator
        else comparator = new ObjectiveComparator(0) ; // Single objective comparator
        algorithm.setInputParameter("comparator", comparator);

        /* Execute the Algorithm */
        long initTime = System.currentTimeMillis();
        SolutionSet population = algorithm.execute();
        long estimatedTime = System.currentTimeMillis() - initTime;
        System.out.println("Total execution time: " + estimatedTime);

        /* Log messages */
        System.out.println("Objectives values have been writen to file FUN");
        population.printObjectivesToFile("FUN");
        System.out.println("Variables values have been writen to file VAR");
        population.printVariablesToFile("VAR");

        population.printSelfConsumptionToFile("SELF_CONSU", true);
        population.printNonREPaidToFile("NON_RE_PAID");
        population.printLowerLevelVarsToFile("LL_VAR");
        population.printLowerLevelObjToFile("LL_FUN", true);

        population.printSpentEnergyToFile("SPENT");
        population.printUserDissatisfactionToFile("USER_DISSAT", true);
        population.printStdDevUserDissatisfactionToFile("STDDEV_USER_DISSAT");
        population.printUserEnergyToFile("USER_ENERGY");
        population.printStdDevUserEnergyToFile("STDDEV_USER_ENERGY");

        FileWriter timeWriter = new FileWriter("TIME", true);
        timeWriter.write(estimatedTime + "\n");
        timeWriter.close();


    } //main

}