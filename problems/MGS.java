/* Author: Yi Xiang
 * Many-Objective Knapsack Problems
 */



package jmetal.problems;

import com.sun.xml.internal.bind.v2.TODO;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.ArrayIntSolutionType;
import jmetal.encodings.solutionType.ArrayRealSolutionType;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.variable.ArrayInt;
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.Int;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.metaheuristics.bilevel.LowerLevelMOKP;
import jmetal.util.JMException;
import jmetal.util.wrapper.XInt;
import jmetal.util.wrapper.XReal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class MGS extends Problem {

	private static final long serialVersionUID = 1L;
    private String problemPath = ""; // The path of the files
    public static String fileName;

    public static int[] producedRE;


  public MGS(String problemName) {
	  this.setMaxmized_(false); // this problem is not to be maximized
	  this.problemName_ = problemName;
      this.numberOfVariables_ = 5;
      this.numberOfObjectives_ = 1;
      this.lowerLimit_ = new double[] {0.0, 0.0, 0.0, 0.0, 0.0};
      this.upperLimit_ = new double[] {3.0, 3.0, 3.0, 3.0, 3.0};
      producedRE = new int[5];

      fileName = problemPath + problemName + ".txt";
      System.out.println(fileName);

      //fills up numberOfItems, p, w, sackCapacity
      //simply read the input textfile
      this.loadProblem(fileName);
      this.solutionType_ = new ArrayRealSolutionType(this);

  }  // 

  public void loadProblem(String problemFileName) {

      /*
      try {
          BufferedReader in = new BufferedReader(new FileReader(problemFileName));
          String line;

          // Read number of items
          line = in.readLine();

          in.close();
      } catch (IOException e){
          System.out.println("Error reading MOKP problemFile: " + e.getMessage());
      }

       */

      for (int i=0; i<producedRE.length; i++)
          producedRE[i] = 100;

  }
  
	@Override
	public void evaluate(Solution solution) throws JMException {

        SolutionSet lowerLevelSols = null;
        try {
            lowerLevelSols = LowerLevelMOKP.evaluate(new XReal(solution));
        } catch (Exception e){
            System.out.println("Exception at LowerLevelMOKP.evaluate: " + e.getMessage());
        }

        //TODO pick best solution, not simply the first one
        double result = upperLevel_evaluate(lowerLevelSols.get(0));

        //TODO calculate total energy based on solution picked
        //only take weight in account, not cost. where 1, add the weight to the total for that bucket

        solution.setObjective(0, result);

	} // evaluate

    public double upperLevel_evaluate(Solution solution) throws JMException {

        XReal doubleArray = new XReal(solution);

        double sum = 0;
        for (int i=0; i<producedRE.length; i++) {
            //if (doubleArray.getValue(i) > producedRE[i]){
            sum += Math.abs(doubleArray.getValue(i) - producedRE[i]);
            //}
        }

        return sum;
    }

}



