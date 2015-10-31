/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lp;

/**
 *
 * @author kostas kord
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Servlet implementation
 */
//@WebServlet(name = "Lp_first", urlPatterns = {"/Lp_first"})
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxRequestSize = 1024 * 1024 * 5 * 5)

public class Lp_first extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = Logger.getLogger(Lp_first.class.getCanonicalName());

    public Lp_first() {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        Part filePart = request.getPart("file");

        //Get the name of the file: method at line ~262
        //String fileName = getFileName(filePart);
        String fileName = "datafile.xlsx";

        //CHECKBOX: Values for Model option
        String[] checked = new String[3];
        checked = request.getParameterValues("check");

        //COMBOBOX <select>: Values for input, intermdiate and output variables
        String[] selected = request.getParameterValues("variables");

        //TEXTBOX: Set lower level for all weights
        String[] lower_w = request.getParameterValues("lower_weight");

        double lower_weight;

        try {
            lower_weight = Double.parseDouble(lower_w[0]);
        } catch (NumberFormatException e) {
            lower_weight = 0;
        }

        /*
         Specify the path of the uploaded file
         Initialize public _Path variable
         */
        String relativeWebPath = "/";
        String absolute_path = getServletContext().getRealPath(relativeWebPath);

        double[][] model_results = new double[0][0];

        OutputStream out = null;
        InputStream filecontent = null;

        try {

            /*
             * Clearing the /web directory from old files 
             */
            cleanDir();

            //Create new file: a copy of the uploaded file
            File uploadedFile = new File(absolute_path, fileName);

            out = new FileOutputStream(uploadedFile);
            filecontent = filePart.getInputStream();

            int read = 0;
            final byte[] bytes = new byte[1024];

            while ((read = filecontent.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            LOGGER.log(Level.INFO, "File {0} being uploaded to {1}", new Object[]{fileName, absolute_path});

            RequestDispatcher next_view;

            /*
             * First Check!
             *  fileformat(): Returns true if the data part of the file has got
             * only numbers.
             */
            boolean fileformat_ok = new XLSXhandler().fileformat(uploadedFile);

            if (fileformat_ok) {

                /*
                 * Calling of OpenXlsx class   
                 * Returns an Object array: 
                 * obj[0]=data, obj[1]=rows, obj[2]=columns, obj[3]=variables, obj[4]=dmuNames
                 */
                XLSXhandler xlsx_obj = new XLSXhandler();
                Object[] obj = xlsx_obj.opener(uploadedFile, fileName);

                /*
                 * Calling of model_selected method that
                 * creates the information.xml file
                 */
                XMLhandler xmlnew = new XMLhandler();
                xmlnew.models_selected(checked, absolute_path);

                /*
                 * Second Check!
                 * check_data(): Returns true if User's variables number is the 
                 * same with file's columns number (Method implimentats bellow...)
                 */
                boolean choises_ok = check_data(selected, obj[2]);

                if (choises_ok) {
                    //Select DEA efficiency model
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i].equals("1")) {
                            //Calling of Multiplicative model
                            model_results = new Multiplicative().multi(obj, selected, lower_weight, absolute_path);
                            xmlnew.create_xml(absolute_path, "multiplicative", lower_weight, obj, model_results);
                            xlsx_obj.create_xlsx_file(absolute_path, "multiplicative", obj, model_results);

                        }
                        if (checked[i].equals("2")) {
                            //Calling of Additive model
                            model_results = new Additive().additive(obj, selected, lower_weight, absolute_path);
                            xmlnew.create_xml(absolute_path, "additive", lower_weight, obj, model_results);
                            xlsx_obj.create_xlsx_file(absolute_path, "additive", obj, model_results);

                        }
                        if (checked[i].equals("3")) {
                            //Calling of Composition model
                            model_results = new Composition().composition(obj, selected, lower_weight, absolute_path);
                            xmlnew.create_xml(absolute_path, "composition", lower_weight, obj, model_results);
                            xlsx_obj.create_xlsx_file(absolute_path, "composition", obj, model_results);

                        }

                    }

                    //Results to users screen. 
                    next_view = request.getRequestDispatcher("results.html");
                    next_view.forward(request, response);
                    response.setCharacterEncoding("UTF-8");

                } else {
                    /* 
                     * choises_ok == false. Respond the warning page that describes the problem.
                     */
                    next_view = request.getRequestDispatcher("choises_warning.html");
                    next_view.forward(request, response);
                    response.setCharacterEncoding("UTF-8");

                }
            } else {

                /*
                 * fileformat_ok ==false.  Respond the warning page that describes the problem.
                 */
                next_view = request.getRequestDispatcher("fileformat_warning.html");
                next_view.forward(request, response);
                response.setCharacterEncoding("UTF-8");

            }

        } catch (FileNotFoundException fne) {

        }

        if (out != null) {
            out.close();
        }
        if (filecontent != null) {
            filecontent.close();
        }

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        //Specify the path of the downloaded file
        String relativeWebPath = "/";
        String absoluteFilePath = getServletContext().getRealPath(relativeWebPath);

        // Set the content type based to zip
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=ECresults.zip");

        InputStream inp = null;
        BufferedInputStream binput = null;

        ServletOutputStream out = response.getOutputStream();
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(out));

        File mult = new File(absoluteFilePath + "\\multiplicative.xlsx");
        File add = new File(absoluteFilePath + "\\additive.xlsx");
        File com = new File(absoluteFilePath + "\\composition.xlsx");

        try {
            if (mult.exists()) {
                zos.putNextEntry(new ZipEntry("multiplicative.xlsx"));
                inp = new FileInputStream(absoluteFilePath + "\\multiplicative.xlsx");
                binput = new BufferedInputStream(inp);
                // Write the contents of the file
                int data = 0;
                while ((data = binput.read()) != -1) {
                    zos.write(data);
                }
                inp.close();
                binput.close();

            }
            if (add.exists()) {
                zos.putNextEntry(new ZipEntry("additive.xlsx"));
                inp = new FileInputStream(absoluteFilePath + "\\additive.xlsx");
                binput = new BufferedInputStream(inp);
                // Write the contents of the file
                int data = 0;
                while ((data = binput.read()) != -1) {
                    zos.write(data);
                }
                inp.close();
                binput.close();

            }
            if (com.exists()) {
                zos.putNextEntry(new ZipEntry("composition.xlsx"));
                inp = new FileInputStream(absoluteFilePath + "\\composition.xlsx");
                binput = new BufferedInputStream(inp);
                // Write the contents of the file
                int data = 0;
                while ((data = binput.read()) != -1) {
                    zos.write(data);
                }
                inp.close();
                binput.close();
            }

        } catch (FileNotFoundException fnfe) {
            // If the file does not exists, write an error entry instead of
            // file
            // contents
            zos.write(("ERROR: Could not find file ").getBytes());
            zos.closeEntry();

        }

        /*
         * Clearing the /web directory from old files 
         */
        cleanDir();

        zos.closeEntry();
        zos.close();
    }

    private boolean check_data(String[] selected, Object columns) {

        boolean flag = false;
        int col_sum = (Integer) columns;
        int varX = Integer.parseInt(selected[0]);
        int varZ = Integer.parseInt(selected[1]);
        int varY = Integer.parseInt(selected[2]);

        int sum = varX + varZ + varY + 1;
        if (sum == col_sum) {
            flag = true;
        }
        return flag;
    }

    public void cleanDir() {

         /*
         Specify the path of the uploaded file
         Initialize public _Path variable
         */
        String relativeWebPath = "/";
        String absoluteFilePath = getServletContext().getRealPath(relativeWebPath);
        
        /*  */
        File uploaded = new File(absoluteFilePath + "\\datafile.xlsx");

        /* The .xlsx files  */
        File mult_xlsx = new File(absoluteFilePath + "\\multiplicative.xlsx");
        File add_xlsx = new File(absoluteFilePath + "\\additive.xlsx");
        File com_xlsx = new File(absoluteFilePath + "\\composition.xlsx");

        /* The .xml files  */
        File mult_xml = new File(absoluteFilePath + "\\multiplicative.xml");
        File add_xml = new File(absoluteFilePath + "\\additive.xml");
        File com_xml = new File(absoluteFilePath + "\\composition.xml");

        File info = new File(absoluteFilePath + "\\information.xml");

        if (uploaded.exists()) {
            uploaded.delete();
        }

        if (info.exists()) {
            info.delete();
        }

        if (mult_xml.exists()) {
            mult_xlsx.delete();
            mult_xml.delete();

        }
        if (add_xlsx.exists()) {
            add_xlsx.delete();
            add_xml.delete();

        }
        if (com_xml.exists()) {
            com_xlsx.delete();
            com_xml.delete();
        }

    }
}
