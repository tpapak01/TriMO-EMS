/* Author: Yi Xiang
 * Many-Objective Knapsack Problems
 */



package jmetal.problems;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.ArrayIntSolutionType;
import jmetal.encodings.solutionType.ArrayRealSolutionType;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.variable.ArrayInt;
import jmetal.encodings.variable.Binary;
import jmetal.encodings.variable.Int;
import jmetal.encodings.variable.MOKP_BinarySolution;
import jmetal.util.JMException;
import jmetal.util.wrapper.XInt;
import jmetal.util.wrapper.XReal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class UpperLevel_Problem extends Problem {

	private static final long serialVersionUID = 1L;
    private String problemPath = "/Users/emine/IdeaProjects/JMETALHOME/Knapsack_data - multi user/"; // The path of the files
    public static String fileName;

    public static int[] producedRE;


  public UpperLevel_Problem(String problemName) {
	  this.setMaxmized_(false); // this problem is not to be maximized
	  this.problemName_ = problemName;
      this.numberOfVariables_ = 5;
      this.numberOfObjectives_ = 1;
      this.lowerLimit_ = new double[] {0.0, 0.0, 0.0, 0.0, 0.0};
      this.upperLimit_ = new double[] {1000.0, 1000.0, 1000.0, 1000.0, 1000.0};
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
		// TODO Auto-generated method stub
        XReal doubleArray = new XReal(solution);

        double sum = 0;
        for (int i=0; i<producedRE.length; i++) {
            //if (doubleArray.getValue(i) > producedRE[i]){
                sum += Math.abs(doubleArray.getValue(i) - producedRE[i]);
            //}
        }

        solution.setObjective(0, sum);

	} // evaluate
}



