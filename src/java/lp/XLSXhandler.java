/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author kostas kord
 */
public class XLSXhandler {

    public XLSXhandler() {

    }

    public Object[] opener(File uploaded, String name) {

        double[][] data = new double[0][0];
        String[] dmuNames = new String[0];
        String[] variable_names = new String[0];
        Object[] obj = new Object[5];

        try {

            OPCPackage pkg = OPCPackage.open(uploaded);
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            XSSFSheet sheet1 = wb.getSheetAt(0);

            //I find the number of the rows in the file! (0-based)
            int rows = sheet1.getLastRowNum();
            System.out.println("Total Rows of DATA in the file: " + rows);

            //I find the number of columns! (1-based)
            int columns = sheet1.getRow(0).getLastCellNum();
            System.out.println("Total Columns of DATA in the file: " + columns);

            data = new double[rows][columns - 1];
            dmuNames = new String[rows];
            variable_names = new String[columns];

            Row row_trav;
            Cell cell_trav;

            // Retrieve data from file to array
            for (int i = 0; i <= rows; i++) {
                row_trav = sheet1.getRow(i);

                for (int k = 0; k < columns; k++) {
                    cell_trav = row_trav.getCell(k);

                    if (i == 0) {
                        //we are at line 0 of the uploaded file
                        variable_names[k] = cell_trav.getStringCellValue();
                    }

                    if (k == 0 && i < rows) {
                        //we are at column 0 of the uploaded file

                        Row row_name = sheet1.getRow(i + 1);
                        cell_trav = row_name.getCell(0);
                        dmuNames[i] = cell_trav.getStringCellValue();
                    }
                    if (i > 0 && k > 0) {
                        data[i - 1][k - 1] = cell_trav.getNumericCellValue();
                    }
                }
            }

            obj[0] = data;
            obj[1] = rows;
            obj[2] = columns;
            obj[3] = variable_names;
            obj[4] = dmuNames;

        } catch (InvalidFormatException e) {
        } catch (IOException e) {
        }

        return obj;
    }

    public void create_xlsx_file(String absolute, String model, Object[] obj, double[][] results) {

        /*
         obj[0] = data; --
         obj[1] = rows;
         obj[2] = columns;
         obj[3] = variables; --
         obj[4] = dmu Names; --
         */
        double[][] data = (double[][]) obj[0];

        // Rows number in file
        int rows_number = (Integer) obj[1];

        // Columns in file (1-based)
        int col_number = (Integer) obj[2];
        //int res = results.length;
        int results_number = results[0].length;
        int total_col = col_number + results_number;//9

        String[] dmuNames = (String[]) obj[4];

        String[] var = (String[]) obj[3];
        String[] variables = new String[0];

        /*
         Get the selected model/s and create the matrix "variables" 
         for each model respectively.
         */
        if (model.equals("multiplicative") || model.equals("composition")) {

            variables = new String[total_col];
            System.arraycopy(var, 0, variables, 0, var.length);

            variables[col_number] = "e1";
            variables[col_number + 1] = "e2";
            variables[col_number + 2] = "Overall Efficiency";
        }
        if (model.equals("additive")) {

            variables = new String[total_col];
            System.arraycopy(var, 0, variables, 0, var.length);

            variables[col_number] = "Weight 1";
            variables[col_number + 1] = "Weight 2";
            variables[col_number + 2] = "Overall Efficiency";
            variables[col_number + 3] = "Theta 1";
            variables[col_number + 4] = "Theta 2";

        }

        try {
            Workbook wb = new XSSFWorkbook();
            FileOutputStream fileOut = new FileOutputStream(absolute + "\\" + model + ".xlsx");
            wb.write(fileOut);
            fileOut.close();

            InputStream inp = new FileInputStream(absolute + "\\" + model + ".xlsx");
            wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.createSheet("new");
            sheet = wb.getSheetAt(0);

            //int rows_number = results.length;
            //int col_number = results[0].length;
            Row row = sheet.createRow(0);


            row = sheet.getRow(0);
            if (row == null) {
                row = sheet.createRow(0);
            }

            Cell cell = row.getCell(0);
            if (cell == null) {

                //the first ROW: the names and variable names for each column
                for (int l = 0; l < variables.length; l++) {
                    cell = row.createCell(l, cell.CELL_TYPE_STRING);
                    cell.setCellValue(variables[l]);
                }

                for (int i = 0; i < rows_number; i++) {

                    row = sheet.createRow(i + 1);
                        int helper = 0;
                    //the rest file...
                    for (int k = 0; k < total_col + 1; k++) {

                        
                        if (k == 0) {
                            //the first column (dmu names)
                            cell = row.createCell(k, cell.CELL_TYPE_STRING);
                            cell.setCellValue(dmuNames[i]);

                        }
                        if (k > 0 && k < col_number) {
                            //the rest of the columns with data
                            cell = row.createCell(k, cell.CELL_TYPE_NUMERIC);
                            cell.setCellValue(data[i][k - 1]);

                        }

                        if (k >= col_number && helper < results_number) {
                            //the columns with efficiency results
                            cell = row.createCell(k, cell.CELL_TYPE_NUMERIC);
                            cell.setCellValue(results[i][helper]);
                            helper++;
                        }

                    }

                }
            }

            // Write the output to a file
            FileOutputStream fOut = new FileOutputStream(absolute + "\\" + model + ".xlsx");
            wb.write(fOut);
            fOut.close();
            inp.close();

        } catch (FileNotFoundException e) {
            System.out.println("--EXCEPTION: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("--EXCEPTION: " + e.getMessage());

        } catch (InvalidFormatException ex) {
            Logger.getLogger(XLSXhandler.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean fileformat(File uploaded) {

        boolean f = true;
        try {
            OPCPackage pkg = OPCPackage.open(uploaded);
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            XSSFSheet sheet1 = wb.getSheetAt(0);

            //I find the number of the rows in the file! (0-based)
            int rows = sheet1.getLastRowNum() + 1;

            //I find the number of columns! (1-based)
            int columns = sheet1.getRow(0).getLastCellNum();

            /*
             *  I will check only the data part! not the names of the DMUs
             */
            Row row_check;
            Cell cell_check;

            for (int i = 1; i < rows; i++) {
                row_check = sheet1.getRow(i);
                for (int k = 1; k < columns; k++) {
                    cell_check = row_check.getCell(k);

                    /*If there is something else exept a number (0)
                     * or excel function (2)
                     */
                    int current = cell_check.getCellType();
                    if (current == 0 || current == 2) {

                    } else {
                        f = false;
                    }
                }
            }

        } catch (InvalidFormatException e) {
            e.getMessage();
            new Lp_first().cleanDir();
            
        } catch (IOException e) {
        }

        return f;
    }

}
