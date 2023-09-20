/**
 * Author: Yi Xiang
 * gzhuxiang_yi@163.com
 */



package jmetal.encodings.variable;

import java.io.FileNotFoundException;
import java.util.Random;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.BinarySolutionType;
import jmetal.problems.MOKP_Problem;
import jmetal.util.PseudoRandom;


public class MOKP_BinarySolution extends BinarySolutionType {

	private MOKP_Problem problemMOKP;

    public MOKP_BinarySolution(Problem problem) {
        super(problem);
		problemMOKP = (MOKP_Problem) problem;
    }    
    
    @Override
    public Variable[] createVariables() {
        Variable[] vars = new Variable[problemMOKP.getNumberOfVariables()];

        for (int i = 0; i < vars.length; i++) {
            vars[i] = new Binary(problemMOKP.getNumberOfItems() * problemMOKP.getNumberOfUsers() * problemMOKP.getNumberOfConstraints());
        }
        
        return vars;        
    }    


     // Update solution according to probability
     // replaces mutation operator
    public void updateProduct(Solution sol,double [] prob) {    	 
		  Variable[] vars = sol.getDecisionVariables();
		  double updateRate = 0.01;
		 
			for (int i = 0; i < vars.length; i++) {
				Binary bin = ((Binary)vars[i]);

				for (int j = 0; j < bin.numberOfBits_; j++){ //

					 if (PseudoRandom.randDouble() < updateRate) {

						 if (PseudoRandom.randDouble() < prob[j]) {
							 bin.setIth(j, true);
						 } else {
							 bin.setIth(j, false);
						 }

					 }
				}

			}
  	} // updateProduct

}




