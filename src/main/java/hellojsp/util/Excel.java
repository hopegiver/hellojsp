package hellojsp.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Writer;
import java.util.Arrays;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Excel {

    private String sheet = "Sheet1";
    private boolean debug = false;
    private Writer out = null;

    public Excel() {}

    public void setDebug(Writer out) {
        this.debug = true;
        this.out = out;
    }

    private void setError(String msg) {
        try {
            if(debug && null != out) out.write("<hr>" + msg + "<hr>\n");
        } catch(Exception ignored) {}
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public DataSet read(String path, String[] columns) {
        DataSet rs = new DataSet();
        FileInputStream fis = null;
        Workbook wb = null;
        try {
            fis = new FileInputStream(path);
            wb = WorkbookFactory.create(fis);
            Sheet sh = wb.getSheet(sheet);
            int total = sh.getLastRowNum();
            for(int i=0; i<=total; i++) {
                rs.addRow();
                for(int j=0; j<columns.length; j++) {
                    rs.put(columns[j], sh.getRow(i).getCell(j).getStringCellValue());
                }
            }
            rs.first();
        } catch (Exception e) {
            Hello.errorLog("{Excel.read} columns:" + Arrays.toString(columns), e);
            setError(e.getMessage());
        } finally {
            if(fis != null) try { fis.close(); } catch (Exception ignored) {}
            if(wb != null) try { wb.close(); } catch (Exception ignored) {}
        }
        return rs;
    }

    public void write(String path, String[] columns, DataSet rs) {
        write(path, rs, columns);
    }
    public void write(String path, DataSet rs, String[] columns) {
        XSSFWorkbook wb = null;
        FileOutputStream fos = null;
        try {
            wb = new XSSFWorkbook();
            Sheet sh = wb.createSheet(sheet);
            Row row1 = sh.createRow(0);
            for (int j = 0; j < columns.length; j++) {
                String[] arr = columns[j].split("=>");
                String title;
                if(arr.length == 2) {
                    columns[j] = arr[0].trim();
                    title = arr[1].trim();
                } else {
                    title = arr[0];
                }
                Cell cell1 = row1.createCell(j);
                cell1.setCellValue(title);
            }
            for (int i = 1; rs.next(); i++) {
                Row row = sh.createRow(i);
                for (int j = 0; j < columns.length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(rs.s(columns[j]));
                }
            }
            fos = new FileOutputStream(path);
            wb.write(fos);
        } catch (Exception e) {
            Hello.errorLog("{Excel.write} columns:" + Arrays.toString(columns), e);
            setError(e.getMessage());
        } finally {
            if(fos != null) try { fos.close(); } catch (Exception ignored) {}
            if(wb != null) try { wb.close(); } catch (Exception ignored) {}
        }
    }
}
