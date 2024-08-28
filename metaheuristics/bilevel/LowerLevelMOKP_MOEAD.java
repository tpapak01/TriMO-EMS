package jmetal.metaheuristics.bilevel;

import jmetal.core.*;
import jmetal.metaheuristics.moead.MOEAD;
import jmetal.operators.crossover.*;
import jmetal.operators.mutation.BitFlipMutation;
import jmetal.operators.mutation.SwapMutation;
import jmetal.problems.MOKP_Problem;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.comparators.LambdaComparator;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XReal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class LowerLevelMOKP_MOEAD {

    public static Logger logger_ ;      // Logger object
    public static FileHandler fileHandler_ ; // FileHandler object
    public static MOKP_Problem problemMOKP   ;         // The problem to solve
    public static Algorithm algorithm ;         // The algorithm to use
    public static int popSize;

    // statistical analysis
    private static int execution = 0;
    private static double time_mean = 0;
    private static FileWriter timeWriter;

    public static MOKP_Problem initializeAlgorithm(String problemName, String problemUserPreferences) throws SecurityException, IOException {


        Operator crossover ;         // Crossover operator
        Operator  mutation  ;         // Mutation operator

        QualityIndicator indicators ; // Object to get quality indicators

        HashMap parameters ; // Operator parameters

        // Logger object and file to store log messages
        logger_      = Configuration.logger_ ;
        fileHandler_ = new FileHandler("MOEAD.log");
        logger_.addHandler(fileHandler_) ;

        indicators = null ;

        //thalis
        problemMOKP = new MOKP_Problem(problemName, problemUserPreferences);
        //thalis comment, default option
        //problem = new Kursawe("Real", 3);
        //problem = new Kursawe("BinaryReal", 3);
        //problem = new Water("Real");
        //problem = new ZDT1("ArrayReal", 100);
        //problem = new ConstrEx("Real");
        //problem = new DTLZ1("Real");
        //problem = new OKA2("Real") ;

        indicators = new QualityIndicator(problemMOKP, "OPTIMAL_PARETO") ;
        
        algorithm = new MOEAD(problemMOKP);
        //algorithm = new MOEAD_DRA(problem);

        // Algorithm parameters
        //thalis
        int populationSize;
        if (problemMOKP.getNumberOfObjectives() == 2) {
            populationSize              = 100   ;
        } else if (problemMOKP.getNumberOfObjectives() == 3) {
            populationSize              = 105   ;
        } else if (problemMOKP.getNumberOfObjectives() == 4) {
            populationSize              = 120   ;
        } else if (problemMOKP.getNumberOfObjectives() == 6) {
            populationSize              = 126   ;
        } else if (problemMOKP.getNumberOfObjectives() == 8) {
            populationSize              = 120   ;
        } else if (problemMOKP.getNumberOfObjectives() == 10) {
            populationSize              = 220   ;
        } else {
            populationSize              = 100   ;
        }
        popSize = 300;
        algorithm.setInputParameter("populationSize", popSize);
        algorithm.setInputParameter("maxEvaluations",1000000);
        //thalis comment
        //algorithm.setInputParameter("populationSize",300);
        //algorithm.setInputParameter("maxEvaluations",150000);

        // Directory with the files containing the weight vectors used in
        // Q. Zhang,  W. Liu,  and H Li, The Performance of a New Version of MOEA/D
        // on CEC09 Unconstrained MOP Test Instances Working Report CES-491, School
        // of CS & EE, University of Essex, 02/2009.
        // http://dces.essex.ac.uk/staff/qzhang/MOEAcompetition/CEC09final/code/ZhangMOEADcode/moead0305.rar
        algorithm.setInputParameter("dataDirectory",
                "/Users/emine/IdeaProjects/JMETALHOME/data/MOEAD_parameters/Weight");

        //algorithm.setInputParameter("finalSize", 300) ; // used by MOEAD_DRA

        //thalis
        algorithm.setInputParameter("T", 100) ; // number of neighbours per individual
        algorithm.setInputParameter("delta", 1.0) ; // 1 = parents always from neighbourhood = MOEAD
        algorithm.setInputParameter("nr", 10) ; // maximal number of solutions that can be updated in "updateProblem"
        //theta_ = 5.0; // used in PBI
        //algorithm.setInputParameter("theta", theta_) ;
        //thalis comment
        //algorithm.setInputParameter("T", 20) ;
        //algorithm.setInputParameter("delta", 0.9) ;
        //algorithm.setInputParameter("nr", 2) ;

        // Crossover operator
        //thalis
        parameters = new HashMap();
        double crossoverProbability = 1.0;
        parameters.put("probability", crossoverProbability);
        //crossover = new PartiallyMappedTwoPointCrossover(parameters);
        crossover = new PartiallyMappedHUXCrossover(parameters);
        algorithm.setInputParameter("repairAfterCrossoverMutation",0);
        //crossover = new TwoPointCrossoverCustom(parameters);
        //crossover = new SinglePointCrossover(parameters);
        //crossover = new HUXCrossover(parameters);

        // Mutation operator
        //thalis - authors have replaced this mutation operator with "updateProduct", but not us
        parameters = new HashMap();
        double mutationProbability = 0.01;
        parameters.put("probability", mutationProbability);
        parameters.put("repair", 1);
        parameters.put("problem", problemMOKP);
        mutation = new BitFlipMutation(parameters);
        //mutation = new SwapMutation(parameters);
        //thalis comment
        //parameters = new HashMap() ;
        //parameters.put("probability", 1.0/problem.getNumberOfVariables()) ;
        //parameters.put("distributionIndex", 20.0) ;
        //mutation = MutationFactory.getMutationOperator("PolynomialMutation", parameters);

        algorithm.addOperator("crossover",crossover);
        algorithm.addOperator("mutation",mutation);

        //algorithm.setInputParameter("functionType","TCHE1"); // scalar function type
        algorithm.setInputParameter("rpType","Ideal"); // reference point z_ type, Ideal (best) or Nadir (worst)
        // Ideal by default
        algorithm.setInputParameter("normalize",false);

        /* Comparator */
        Comparator comparator;
        if (problemMOKP.isMaxmized())
            comparator = new ObjectiveComparator(0, false) ; // Single objective comparator
        else comparator = new ObjectiveComparator(0, true) ; // Single objective comparator
        algorithm.setInputParameter("comparator", comparator);
        Comparator lambdaComparator;
        if (problemMOKP.isMaxmized())
            lambdaComparator = new LambdaComparator(0, false) ; // Single objective comparator
        else lambdaComparator = new LambdaComparator(0, true) ; // Single objective comparator
        algorithm.setInputParameter("lambdaComparator", lambdaComparator);

        // Add the indicator object to the algorithm
        algorithm.setInputParameter("indicators", indicators) ;

        //timeWriter = new FileWriter("LowerLevelParetoVisual/time.txt");

        return problemMOKP;
    }


    public static SolutionSet evaluate(XReal y, Solution solution) throws JMException, SecurityException, ClassNotFoundException {

        problemMOKP.setCostOfUsage(y);
        int execType = solution.getExecType();
        if (solution.isMarked()) {
            algorithm.setInputParameter("initPopSolution", null);
        } else {
            switch (execType) {
                case 0:
                    algorithm.setInputParameter("T", 100) ;
                    algorithm.setInputParameter("initPopSolution", null);
                    break;
                case 1:
                    algorithm.setInputParameter("T", 100) ;
                    algorithm.setInputParameter("initPopSolution", solution.getLL_ND_pop());
                    break;
                case 2:
                    algorithm.setInputParameter("T", 50) ;
                    algorithm.setInputParameter("initPopSolution", solution.getLL_Special_pop());
                    break;
                case 3:
                    algorithm.setInputParameter("T", 50) ;
                    algorithm.setInputParameter("initPopSolution", solution.getLL_Reverse_pop());
                    break;
                case 4:
                    algorithm.setInputParameter("initPopSolution", solution.getLL_Random_pop());
                    break;
                default:
                    break;
            }
        }

        // Execute the Algorithm
        long initTime = System.currentTimeMillis();
        SolutionSet population = algorithm.execute();
        long estimatedTime = System.currentTimeMillis() - initTime;
        //if (solution.isMarked() == false) {
        //    System.out.print("Type:" + execType + ", time:" + (estimatedTime) + ",");
        //}

        /*
        try {
            execution++;
            time_mean += estimatedTime;
            timeWriter.write(estimatedTime + "\n");
            if (execution == 20) {
                time_mean = time_mean / 20;
                timeWriter.write(time_mean + "\n");
                timeWriter.close();
            }
        } catch(Exception e){}

         */

        return population;
    }

}
