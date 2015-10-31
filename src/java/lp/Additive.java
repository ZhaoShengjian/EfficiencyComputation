/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lp;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

/**
 *
 * @author kostas kord
 */
public class Additive {

    public Additive() {

    }

    public double[][] additive(Object obj[], String[] selected, double lower_weight, String path) {

        //Retrieve data
        double[][] data = (double[][]) obj[0];
        int rows = (Integer) obj[1];
        int cols = (Integer) obj[2] - 1;
        System.out.println("Rows: " + rows + " -Columns: " + cols);

        //Variables given from user 
        int varX = Integer.parseInt(selected[0]);
        int varZ = Integer.parseInt(selected[1]);

        //line Array, for each line of data
        double[] line = new double[cols];
        double[] objective = new double[cols + 1];
        double[] equal = new double[cols + 1];

        double[] constrA = new double[cols + 1];
        double[] constrB = new double[cols + 1];

        double[] positive = new double[cols + 1];

        double[] theta1 = new double[rows];
        double[] theta2 = new double[rows];

        double[] objective_value = new double[rows];

        double[] w1 = new double[rows];
        double[] w2 = new double[rows];

        double[][] results_array = new double[rows][5];

        try {
            // CREATE THE PROBLEM
            // Create a problem with "cols" variables and 0 constraints
            LpSolve solver = LpSolve.makeLp(0, cols);
            solver.setMaxim();

            //Add Constraints
            //Initialize Unitary matrix OK!
            for (int i = 1; i < cols + 1; i++) {
                positive[i] = 1;
                solver.addConstraint(positive, LpSolve.GE, lower_weight);
                positive[i] = 0;
            }

            //Initialize Standard Constraints OK!
            for (int m = 0; m < rows; m++) {
                for (int n = 0; n < cols; n++) {
                    line[n] = data[m][n];
                }

                // Same constraints like Composition model
                for (int k = 1; k < cols + 1; k++) {
                    if (k <= varX) {
                        constrA[k] = -line[k - 1];
                    }
                    if (k > varX && k <= varX + varZ) {
                        constrA[k] = line[k - 1];
                        constrB[k] = -line[k - 1];
                    }
                    if (k > varX + varZ) {
                        constrB[k] = line[k - 1];
                    }
                }
                solver.addConstraint(constrA, LpSolve.LE, 0);
                solver.addConstraint(constrB, LpSolve.LE, 0);
            }

            //...for each DMU:
            // Set Objective 
            for (int i = 0; i < rows; i++) {

                //CREATE
                for (int k = 0; k < cols; k++) {
                    line[k] = data[i][k];
                }

                for (int k = 1; k < cols + 1; k++) {
                    //fill the constraint with -Xs
                    if (k <= varX) {
                        equal[k] = line[k - 1];
                    }
                    // Xs + Zs = equal One. Constraint is ready
                    if (k > varX && k <= varX + varZ) {
                        equal[k] = line[k - 1];

                        //Add Zs to objective
                        objective[k] = line[k - 1];
                    }
                    //fill the objective with Y
                    if (k > varX + varZ) {
                        objective[k] = line[k - 1];
                    }
                }
                // Objective function(s)
                solver.setObjFn(objective);
                solver.addConstraint(equal, LpSolve.EQ, 1);

                // solve the problem
                solver.solve();

                double[] var = solver.getPtrVariables();
                objective_value[i] = solver.getObjective();

                double numerator = 0;
                double denominator = 0;

                //make the sum of the numerator and denominator for the weights 
                for (int t = 0; t < varX + varZ; t++) {
                    if (t < varX) {
                        numerator = numerator + var[t] * line[t];
                    } else {
                        denominator = denominator + var[t] * line[t];
                    }
                }

                w1[i] = numerator / (numerator + denominator);
                w2[i] = denominator / (numerator + denominator);

                theta1[i] = theta(objective_value[i], data, cols, rows, i, varX, varZ);
                theta2[i] = (objective_value[i] - w1[i] * theta1[i]) / w2[i];

                //solver.writeLp("model" + i + ".lp");
                //Delete the redundant constraint
                solver.delConstraint(cols + 2 * rows + 1);

            }

            // delete the problem and free memory
            solver.deleteLp();

            for (int v = 0; v < rows; v++) {
                results_array[v][0] = w1[v];
                results_array[v][1] = w2[v];
                results_array[v][2] = objective_value[v];
                results_array[v][3] = theta1[v];
                results_array[v][4] = theta2[v];

            }

            //Object for writing results in .xml file
//            OpenXlsx results = new OpenXlsx();
//            results.create_xlsx_file(path, "additive", results_array);
            
        } catch (LpSolveException e) {
            //e.printStackTrace();
        }

        return results_array;

    }

    public double theta(double dmu_obj, double[][] data, int cols, int rows, int current, int userX, int userZ) {

        double[] positive = new double[cols + 1];
        double[] line = new double[cols + 1];

        double[] constrA = new double[cols + 1];
        double[] constrB = new double[cols + 1];
        double[] constrC = new double[cols + 1];

        double[] objective = new double[cols + 1];
        double[] equal = new double[cols + 1];

        int varX = userX;
        int varZ = userZ;

        double overall_theta_one = 0;

        try {
            LpSolve solver = LpSolve.makeLp(0, cols);
            solver.setMaxim();
            //Add Constraints
            //Initialize Unitary matrix OK!
            for (int i = 1; i < cols + 1; i++) {
                positive[i] = 1;
                solver.addConstraint(positive, LpSolve.GE, 0);
                positive[i] = 0;
            }

            //Initialize Standard Constraints OK!
            for (int m = 0; m < rows; m++) {
                for (int n = 0; n < cols; n++) {
                    line[n] = data[m][n];
                }

                // constraint A: -X +Z <= 0 
                // constraint B: -Z +Y <= 0 
                for (int k = 1; k < cols + 1; k++) {
                    if (k <= varX) {
                        constrA[k] = -line[k - 1];
                    }
                    if (k > varX && k <= varX + varZ) {
                        constrA[k] = line[k - 1];
                        constrB[k] = -line[k - 1];
                    }
                    if (k > varX + varZ) {
                        constrB[k] = line[k - 1];
                    }
                }
                solver.addConstraint(constrA, LpSolve.LE, 0);
                solver.addConstraint(constrB, LpSolve.LE, 0);
            }

            // Set Objective 
            for (int k = 0; k < cols; k++) {
                line[k] = data[current][k];
            }

            double mult = 1 - dmu_obj;

            for (int k = 1; k < cols + 1; k++) {

                // Xs = 1
                if (k <= varX) {
                    equal[k] = line[k - 1];
                }
                // mult* Zs + Ys = dmu_obj
                if (k > varX && k <= varX + varZ) {
                    //Add Zs to objective
                    objective[k] = line[k - 1];
                    constrC[k] = mult * line[k - 1];
                }
                //fill the objective with Y
                if (k > varX + varZ) {
                    constrC[k] = line[k - 1];
                }
            }

            mult = 0;

            // Objective function(s)
            solver.setObjFn(objective);
            solver.addConstraint(constrC, LpSolve.EQ, dmu_obj);
            solver.addConstraint(equal, LpSolve.EQ, 1);

            // solve the problem
            solver.solve();
            //solver.writeLp("model" + p + ".lp");
            overall_theta_one = solver.getObjective();

            // delete the problem and free memory
            solver.deleteLp();

        } catch (LpSolveException e) {
            //e.printStackTrace();
        }

        return overall_theta_one;
    }
}
