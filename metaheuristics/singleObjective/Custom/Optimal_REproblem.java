package jmetal.metaheuristics.singleObjective.Custom;
import jmetal.core.*;
import jmetal.encodings.variable.Binary;
import jmetal.problems.REproblem;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

public class Optimal_REproblem extends Algorithm {

    public Optimal_REproblem(Problem problem){
        super(problem) ;
    } // GGA

    static int[] winner;

    public SolutionSet execute() throws JMException, ClassNotFoundException {

        //PART 1: Calculate max possible sum per time slot
        ArrayList<int[]> itemsPerTimeslot = new ArrayList<>(problem_.getNumberOfConstraints());
        ArrayList<Integer[]> indexPerTimeslot = new ArrayList<>(problem_.getNumberOfConstraints());
        int maxPossibleSum[] = new int[problem_.getNumberOfConstraints()];
        for (int i=0; i<problem_.getNumberOfConstraints(); i++){
            itemsPerTimeslot.add(((REproblem) problem_).produceListForTimeslot(i));
            indexPerTimeslot.add(((REproblem) problem_).getIndexListPerTimeslot(i));
            /////
            int[] list = itemsPerTimeslot.get(i);
            int sum = 0;
            for (int k = 0; k < list.length; k++) {
                sum += list[k];
            }
            /////
            maxPossibleSum[i] = ((REproblem) problem_).calculateMaxPossibleSum(itemsPerTimeslot.get(i), sum, i);
            System.out.println("Max possible for " + i + " = "+ (double)maxPossibleSum[i]/10);
        }

        //PART 2: Perform exhaustive search to find that combination per time slot
        Solution newSolution = new Solution(problem_);
        int numOfBits = ((REproblem) problem_).getNumberOfUsers() * ((REproblem) problem_).getNumberOfItems() * problem_.getNumberOfConstraints();
        Variable[] vars = new Variable[problem_.getNumberOfVariables()];
        Binary bin = new Binary(numOfBits);
        for (int k = 0; k < numOfBits; k++) {
            bin.setIth(k, false);
        }
        for (int i=0; i<problem_.getNumberOfConstraints(); i++){
            //update winner variable
            int[] list = itemsPerTimeslot.get(i);
            int smallest = list[0];
            int sum = 0;
            for (int k = 0; k < list.length; k++) {
                sum += list[k];
                //Compare elements of array with min
                if(list[k] <smallest)
                    smallest = list[k];
            }
            int r = list.length;
            if (sum > maxPossibleSum[i]){
                r = maxPossibleSum[i] / smallest;
                int start = 1;
                do {
                    boolean res = printCombination(list, list.length, start, maxPossibleSum[i]);
                    if (res)
                        break;
                    start++;
                } while (start <= r);
            } else {
                do {
                    boolean res = printCombination(list, list.length, r, maxPossibleSum[i]);
                    if (res)
                        break;
                    r--;
                } while (r > 0);
            }

            //now that winner is full, use it to fill up MOKP solution with 1's in the right places\
            Integer[] indexList = indexPerTimeslot.get(i);
            for (int j=0; j<winner.length; j++){
                int index = winner[j];
                int position = indexList[index];
                bin.setIth(position, true);
            }
        }
        vars[0] = bin;
        newSolution.setDecisionVariables(vars);
        problem_.evaluate(newSolution);

        // Return a population with the best individual
        SolutionSet resultPopulation = new SolutionSet(1) ;
        resultPopulation.add(newSolution) ;


        return resultPopulation ;
    } // execute

    /* arr[]  ---> Input Array
data[] ---> Temporary array to store current combination
start & end ---> Starting and Ending indexes in arr[]
index  ---> Current index in data[]
r ---> Size of a combination to be printed */
    static boolean combinationUtil(int arr[], int data[], int dataIndex[], int start,
                                int end, int index, int r, int target)
    {
        // Current combination is ready to be printed, print it
        if (index == r)
        {
            int sum = 0;
            for (int j=0; j<r; j++) {
                sum += data[j];
                //System.out.print(data[j] + " ");
            }
            //System.out.println(sum);
            //System.out.println("");
            if (sum == target){
                winner = new int[r];
                for (int j=0; j<r; j++) {
                    winner[j] = dataIndex[j];
                    //System.out.print(data[j] + " ");
                }
                return true;
            }
            return false;
        }

        // replace index with all possible elements. The condition
        // "end-i+1 >= r-index" makes sure that including one element
        // at index will make a combination with remaining elements
        // at remaining positions
        for (int i=start; i<=end && end-i+1 >= r-index; i++)
        {
            data[index] = arr[i];
            dataIndex[index] = i;
            boolean res = combinationUtil(arr, data, dataIndex, i+1, end, index+1, r, target);
            if (res)
                return res;
        }
        return false;
    }

    // The main function that prints all combinations of size r
    // in arr[] of size n. This function mainly uses combinationUtil()
    static boolean printCombination(int arr[], int n, int r, int target)
    {
        // A temporary array to store all combination one by one
        int data[]=new int[r];
        int dataIndex[]=new int[r];

        // Print all combination using temporary array 'data[]'
        boolean res = combinationUtil(arr, data, dataIndex,0, n-1, 0, r, target);
        return res;
    }

    /*Driver function to check for above function*/
    /*
    public static void main (String[] args) {
        int arr[] = {1, 2, 3, 4, 5};
        int r = 3;
        int n = arr.length;
        printCombination(arr, n, r);
    }
     */


}
