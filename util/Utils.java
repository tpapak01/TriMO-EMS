//  Utils.java
//
//  Author:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jmetal.util;

import jmetal.core.Solution;
import jmetal.core.SolutionSet;

/**
 * Utilities methods to used by MOEA/D
 */
public class Utils {

	public static double distVector(double[] vector1, double[] vector2) {
		int dim = vector1.length;
		double sum = 0;
		for (int n = 0; n < dim; n++) {
			sum += (vector1[n] - vector2[n]) * (vector1[n] - vector2[n]);
		}
		return Math.sqrt(sum);
	} // distVector

	/**
	 * Find the first m minimal elements from x with length n
	 * @param x, an array
	 * @param idx
	 * @param n, the length of x
	 * @param m, the first m minimal elements 
	 */
	public static void minFastSort(double x[], int idx[], int n, int m) {
		for (int i = 0; i < n;i++) {
			idx[i] = i;
		}
		
		for (int i = 0; i < m; i++) { // find m elements 
			for (int j = i + 1; j < n; j++) {
				if (x[i] > x[j]) {
					double temp = x[i];
					x[i]   = x[j];
					x[j]   = temp;
					int id = idx[i];
					idx[i] = idx[j];
					idx[j] = id;
				} // if
			}
		} // for

	} // minFastSort
	
	/**
	 * Find the first m maximum elements from x with length n
	 * @param x, an array
	 * @param idx
	 * @param n, the length of x
	 * @param m, the first m maximum elements 
	 */
	public static void maxFastSort(double x[], int idx[], int n, int m) {
		for (int i = 0; i < n;i++) {
			idx[i] = i;
		}
		
		for (int i = 0; i < m; i++) { // find m elements 
			for (int j = i + 1; j < n; j++) {
				if (x[i] < x[j]) {
					double temp = x[i];
					x[i]   = x[j];
					x[j]   = temp;
					int id = idx[i];
					idx[i] = idx[j];
					idx[j] = id;
				} // if
			}
		} // for

	} // maxFastSort
	
	/**
	 * Quick sort procedure (ascending order)
	 * What it actually sorts is the idx array, where
	 * idx[i] shows the hierarchical position of item i,
	 * e.g. idx[5]=1 means that item 5 has the lowest value per kilo of all items
	 *
	 * @param array
	 * @param idx
	 * @param from
	 * @param to
	 */
	public static void QuickSort(double[] array, int[] idx, int from, int to) {
		if (from >= to) return;
		
		if (from < to) {
			double temp = array[to];
			int tempIdx = idx[to];
			int i = from - 1;
			for (int j = from; j < to; j++) {
				if (array[j] <= temp) {
					i++;
					double tempValue = array[j];
					array[j] = array[i];
					array[i] = tempValue;
					int tempIndex = idx[j];
					idx[j] = idx[i];
					idx[i] = tempIndex;
				}
			}
			array[to] = array[i + 1];
			array[i + 1] = temp;
			idx[to] = idx[i + 1];
			idx[i + 1] = tempIdx;
			
		 
			if (i-1 > from) 
				QuickSort(array, idx, from, i);
			
			if (to > i + 2) 
				QuickSort(array, idx, i + 1, to);
		}
	}

	public static void quickSort(double[] a,int[] idx, int low,int high){
		if (low >= high) return;
		
        int start = low;
        int end = high;
        double key = a[low];
        
        
        while(end>start){
            //�Ӻ���ǰ�Ƚ�
            while(end>start&&a[end]>=key)  //���û�бȹؼ�ֵС�ģ��Ƚ���һ����ֱ���бȹؼ�ֵС�Ľ���λ�ã�Ȼ���ִ�ǰ���Ƚ�
                end--;
            if(a[end]<=key){
            	double temp = a[end];
                a[end] = a[start];
                a[start] = temp;
                
                int tempIndex = idx[end];
				idx[end] = idx[start];
				idx[start] = tempIndex;
            }
            //��ǰ���Ƚ�
            while(end>start&&a[start]<=key)//���û�бȹؼ�ֵ��ģ��Ƚ���һ����ֱ���бȹؼ�ֵ��Ľ���λ��
               start++;
            if(a[start]>=key){
            	double temp = a[start];
                a[start] = a[end];
                a[end] = temp;
                
                int tempIndex = idx[start];
				idx[start] = idx[end];
				idx[end] = tempIndex;				
            }
        //��ʱ��һ��ѭ���ȽϽ���ؼ�ֵ��λ���Ѿ�ȷ���ˡ���ߵ�ֵ���ȹؼ�ֵС���ұߵ�ֵ���ȹؼ�ֵ�󣬵������ߵ�˳���п����ǲ�һ��ģ���������ĵݹ����
        }
        //�ݹ�
        if(start>low) quickSort(a,idx,low,start-1);//������С���һ������λ�õ��ؼ�ֵ����-1
        if(end<high) quickSort(a,idx,end+1,high);//�ұ����С��ӹؼ�ֵ����+1�����һ��
    } //quickSort
    
	
	

	static void QuickSort(int[] array, int[] idx, int from, int to) {
		if (from < to) {
			int temp = array[to];
			int tempIdx = idx[to];
			int i = from - 1;
			for (int j = from; j < to; j++) {
				if (array[j] <= temp) {
					i++;
					int tempValue = array[j];
					array[j] = array[i];
					array[i] = tempValue;
					int tempIndex = idx[j];
					idx[j] = idx[i];
					idx[i] = tempIndex;
				}
			}
			array[to] = array[i + 1];
			array[i + 1] = temp;
			idx[to] = idx[i + 1];
			idx[i + 1] = tempIdx;
			QuickSort(array, idx, from, i);
			QuickSort(array, idx, i + 1, to);
		}
	}
	/**
	 * Quick sort procedure (ascending order)
	 * 
	 * @param array
	 * @param idx
	 * @param from
	 * @param to
	 */
	static void QuickSort(double[] array, double[] idx, int from, int to) {
		if (from < to) {
			double temp = array[to];
			double tempIdx = idx[to];
			int i = from - 1;
			for (int j = from; j < to; j++) {
				if (array[j] <= temp) {
					i++;
					double tempValue = array[j];
					array[j] = array[i];
					array[i] = tempValue;
					double tempIndex = idx[j];
					idx[j] = idx[i];
					idx[i] = tempIndex;
				}
			}
			array[to] = array[i + 1];
			array[i + 1] = temp;
			idx[to] = idx[i + 1];
			idx[i + 1] = tempIdx;
			QuickSort(array, idx, from, i);
			QuickSort(array, idx, i + 1, to);
		}
	}
	public static void randomPermutation(int[] perm, int size) {
		int[] index = new int[size];
		boolean[] flag = new boolean[size];

		for (int n = 0; n < size; n++) {
			index[n] = n;
			flag[n]  = true;
		}

		int num = 0;
		while (num < size) {
			int start = jmetal.util.PseudoRandom.randInt(0, size - 1);
			// int start = int(size*nd_uni(&rnd_uni_init));
			while (true) {
				if (flag[start]) {
					perm[num]   = index[start];
					flag[start] = false;
					num++;
					break;
				}
				if (start == (size - 1)) {
					start = 0;
				} else {
					start++;
				}
			}
		} // while
	} // randomPermutation

	/**
	 * Calculate the dot product of two vectors
	 * 
	 * @param vec1
	 * @param vec2
	 * @return
	 */
	public static double innerproduct(double[] vec1, double[] vec2) {
		double sum = 0;

		for (int i = 0; i < vec1.length; i++)
			sum += vec1[i] * vec2[i];

		return sum;
	}

	/**
	 * Calculate the norm of the vector
	 * 
	 * @param z
	 * @return
	 */
	public static double norm_vector(double[] z, int numberObjectives) {
		double sum = 0;

		for (int i = 0; i < numberObjectives; i++)
			sum += z[i] * z[i];

		return Math.sqrt(sum);
	}

	public static void bubbleSort(double[] arr, int[] idx) {
		int n = arr.length;
		double temp;
		for (int i = 0; i < n; i++) {
			for (int j = 1; j < (n - i); j++) {
				if (arr[j - 1] > arr[j]) {
					//swap elements
					temp = arr[j - 1];
					arr[j - 1] = arr[j];
					arr[j] = temp;
					//
					int tempIndex = idx[j-1];
					idx[j-1] = idx[j];
					idx[j] = tempIndex;
				}
			}
		}
	}

	public static SolutionSet DSS(SolutionSet pop, int popSize){
		return null;
	}

	public static double AchievementScalarizationTcheby(Solution individual, Solution z_, double[] weight,
												 double[] nadirObjectiveValue) {
		double fitness = 0.0;
		double maxFun = -1.0e+30;

		int numOfObjectives = individual.getNumberOfObjectives();
		for (int n = 0; n < numOfObjectives; n++) {
			double diff = Math.abs(
					(individual.getObjective(n) - z_.getObjective(n)) /
					(nadirObjectiveValue[n] - z_.getObjective(n))
			);

			double feval;
			// make sure the multiplication with λ doesn't result in an absolute zero
			if (weight[n] == 0) {
				feval = 0.0001 * diff;
			} else {
				feval = weight[n] * diff;
			}

			//is this the maximum difference found so far?
			if (feval > maxFun) {
				maxFun = feval;
			}
		} // for

		fitness = maxFun;

		return fitness;
	} // fitnessEvaluation

	public static double calculateStandardDeviation(double[] array) {

		// get the sum of array
		double sum = 0.0;
		for (double i : array) {
			sum += i;
		}

		// get the mean of array
		int length = array.length;
		double mean = sum / length;

		// calculate the standard deviation
		double standardDeviation = 0.0;
		for (double num : array) {
			standardDeviation += Math.pow(num - mean, 2);
		}

		return Math.sqrt(standardDeviation / length);
	}



}



