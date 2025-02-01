package com.example.barcodescanning;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static android.util.JsonToken.STRING;
import static com.example.barcodescanning.MainActivity.allowDuplicates;
import static com.example.barcodescanning.MainActivity.disAllowDuplicates;
import static java.sql.Types.BOOLEAN;
import static java.sql.Types.NUMERIC;

public class ScannedDetails extends AppCompatActivity {

    float barcodeNetWeight,totalWeight=0;
    public static ArrayList<Float> netWeights = new ArrayList<Float>();
    public static ArrayList<String> netBarcodeNos = new ArrayList<String>();
    Button nextScanBtn,clearBtn,exportBtn;
    TextView barcodeNetWeightTxtView, totalWeightTxtView;
    CheckBox csvCheckBox;

    String barcodeNo;
    AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanned_details);

        nextScanBtn=findViewById(R.id.newScan);
        clearBtn=findViewById(R.id.clearBtn);
        exportBtn=findViewById(R.id.exportBtn);
        csvCheckBox=findViewById(R.id.csvCheckBox);
        barcodeNetWeightTxtView=findViewById(R.id.barcodeNetWeight);
        totalWeightTxtView=findViewById(R.id.totalWeightTxtView);

        TableLayout tl = findViewById(R.id.table);

        Intent intent = getIntent();
        barcodeNo=intent.getStringExtra("barcodeNetWeight");
//        barcodeNo="AS5.27-1521";
//        barcodeNo="AS5.26-1420";
        int len=barcodeNo.length();
        try{
            if(!Character.isLetter(barcodeNo.charAt(0))){
                if(barcodeNo.charAt(len-2)=='.'){    //last 2nd character
                    barcodeNetWeight = Float.valueOf(barcodeNo.substring(len-5,len));
                }else if(barcodeNo.contains(".")) {
                    barcodeNetWeight = Float.valueOf(barcodeNo.substring(len-4,len));
                    barcodeNetWeight/=10;
                }else{
                    barcodeNetWeight = Float.valueOf(barcodeNo.substring(len-4,len));
                    barcodeNetWeight/=10;
                }
            }else{
                if(barcodeNo.charAt(len-2)=='.'){
                    barcodeNetWeight = Float.valueOf(barcodeNo.substring(len-5,len));
                }else if(barcodeNo.contains(".")) {
                    barcodeNetWeight = Float.valueOf(barcodeNo.substring(len-4,len));
                    barcodeNetWeight/=10;
                }else{
                    barcodeNetWeight=0;
                }
            }
        }catch(Exception e){
            Toast.makeText(ScannedDetails.this, "This is incorrect barcode", Toast.LENGTH_SHORT).show();
        }

        System.out.println("barcodeNetWeight "+barcodeNetWeight);
        barcodeNetWeightTxtView.setText(String.valueOf(barcodeNetWeight));

        tl.removeAllViews();
        totalWeight=0;
        netWeights.add(barcodeNetWeight);
        netBarcodeNos.add(barcodeNo);
        addHeaders();
        addData();
        totalWeightTxtView.setText(String.valueOf(totalWeight));

        nextScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(ScannedDetails.this, MainActivity.class);
                startActivity(intent1);
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllValues(tl);
            }
        });
        builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);

        if (editText.getParent() != null) {
            ((ViewGroup) editText.getParent()).removeView(editText);
        }
        exportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create the AlertDialog
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ScannedDetails.this);
                dialogBuilder.setTitle("Enter File Name")
                        .setMessage("Please enter some file name:")
                        .setView(editText)  // Set the EditText as the view of the dialog
                        .setPositiveButton("Save", (dialog, which) -> {
                            // Get the text entered by the user
                            String userInput = editText.getText().toString();
                            if (!userInput.isEmpty()) {
                                // Handle saving the input (e.g., saving it in SharedPreferences, database, etc.)
                                Toast.makeText(ScannedDetails.this, "Saved: " + userInput, Toast.LENGTH_SHORT).show();
                                File filePath,csvFile=null;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    filePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), userInput+".xls");
                                    if(csvCheckBox.isChecked()){
                                        csvFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), userInput+".csv");
                                    }
                                }else{
                                    filePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + userInput + ".xls");
                                    if(csvCheckBox.isChecked()){
                                        csvFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + userInput + ".csv");
                                    }
                                }

                                try {

                                    if (!filePath.exists()) {
                                        HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
                                        filePath.createNewFile();

                                        FileInputStream fileInputStream = new FileInputStream(filePath);
                                        hssfWorkbook = upsertValueInExcelSheet(hssfWorkbook);
                                        fileInputStream.close();

                                        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
                                        hssfWorkbook.write(fileOutputStream);
                                        editText.setText("");
                                        Toast.makeText(ScannedDetails.this, "File Created", Toast.LENGTH_SHORT).show();

                                        if (fileOutputStream != null) {
                                            fileOutputStream.flush();
                                            fileOutputStream.close();
                                        }
                                        clearAllValues(tl);
                                    }

                                    else{

                                        FileInputStream fileInputStream = new FileInputStream(filePath);
                                        HSSFWorkbook hssfWorkbook = new HSSFWorkbook(fileInputStream);
                                        hssfWorkbook = upsertValueInExcelSheet(hssfWorkbook);

                                        fileInputStream.close();

                                        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
                                        hssfWorkbook.write(fileOutputStream);
                                        editText.setText("");
                                        Toast.makeText(ScannedDetails.this, "File Updated", Toast.LENGTH_SHORT).show();
                                        fileOutputStream.close();
                                        clearAllValues(tl);
                                    }
                                    if(csvCheckBox.isChecked()) {
                                        convertXlsToCsv(filePath, csvFile);
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();

                                }
                            } else {
                                Toast.makeText(ScannedDetails.this, "Input cannot be empty", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()) // Dismiss the dialog on cancel
                        .setOnDismissListener(dialog -> {
                            // Handle the cleanup after the dialog is dismissed
                            if (editText.getParent() != null) {
                                ((ViewGroup) editText.getParent()).removeView(editText); // Remove the EditText from its parent
                            }
                        })
                        .create();

                // Show the dialog
                dialogBuilder.show();

            }
        });
    }

    public static void convertXlsToCsv(File xlsFile, File csvFile) {
        try (InputStream fis = new FileInputStream(xlsFile)) {
            // Create workbook depending on whether it's .xls or .xlsx
            Workbook workbook = new HSSFWorkbook(fis);  // for .xls file


            // Get the first sheet from the workbook
            Sheet sheet = workbook.getSheetAt(0);

            // Write to CSV
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
                Iterator<Row> rowIterator = sheet.iterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Iterator<Cell> cellIterator = row.iterator();
                    StringBuilder rowData = new StringBuilder();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        // Get the cell value as a string
                        String cellValue = getCellValue(cell);
                        // Append the value with a comma separator
                        rowData.append(cellValue).append(",");
                    }
                    // Remove trailing comma and write row data to CSV file
                    if (rowData.length() > 0) {
                        rowData.setLength(rowData.length() - 1);
                    }
                    writer.write(rowData.toString());
                    writer.newLine();
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case 1:
                return cell.getStringCellValue();
            case 0:
                return String.valueOf(cell.getNumericCellValue());
            case 4:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private void clearAllValues(TableLayout tl) {
        allowDuplicates.setChecked(false);
        disAllowDuplicates.setChecked(false);
        barcodeNetWeightTxtView.setText("");
        netWeights.clear();
        netBarcodeNos.clear();
        tl.removeAllViews();
        totalWeight=0;
        totalWeightTxtView.setText(String.valueOf(totalWeight));
    }

    @NonNull
    private HSSFWorkbook upsertValueInExcelSheet(HSSFWorkbook hssfWorkbook) throws IOException {
        HSSFSheet hssfSheet;
        if (hssfWorkbook.getNumberOfSheets() > 0) {
            hssfSheet = hssfWorkbook.getSheetAt(0);
        } else {
            hssfSheet = hssfWorkbook.createSheet("Sheet 1");
        }
        HSSFRow hssfRow = hssfSheet.createRow(0);
        hssfRow.createCell(0).setCellValue("Sr No");
        hssfRow.createCell(1).setCellValue("Barcode");
        hssfRow.createCell(2).setCellValue("Net Weight");

        int lastRowNum = 1;
        for(int i=0;i<netWeights.size();i++){
            hssfRow = hssfSheet.createRow(lastRowNum+i);

            // Create a cell in that row, and set its value from the ArrayList
            HSSFCell cell0 = hssfRow.createCell(0);
            cell0.setCellValue(lastRowNum+i);
            HSSFCell cell1 = hssfRow.createCell(1);
            cell1.setCellValue(String.valueOf(netBarcodeNos.get(i)));
            HSSFCell cell2 = hssfRow.createCell(2);
            cell2.setCellValue(String.valueOf(netWeights.get(i)));
        }
        return hssfWorkbook;
    }

    @NonNull
    private TableRow.LayoutParams getLayoutParams() {
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        params.setMargins(2, 0, 0, 2);
        return params;
    }

    @NonNull
    private TableRow.LayoutParams getTblLayoutParams() {
        return new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT);
    }

    private TextView getTextView(int id, String title, int color, int typeface, int bgColor) {
        TextView tv = new TextView(this);
        tv.setId(id);
        tv.setText(title.toUpperCase());
        tv.setTextColor(color);
        tv.setPadding(10, 10, 10, 10);
        tv.setTypeface(Typeface.DEFAULT, typeface);
        tv.setBackgroundColor(bgColor);
        tv.setLayoutParams(getLayoutParams());
        tv.setVisibility(View.VISIBLE);
        return tv;
    }

    public void addHeaders() {
        TableLayout tl = findViewById(R.id.table);
        TableRow tr = new TableRow(this);
        tr.setLayoutParams(getLayoutParams());
        tr.addView(getTextView(0, "Sr No", Color.WHITE, Typeface.BOLD, Color.BLUE));
        tr.addView(getTextView(0, "Barcode", Color.WHITE, Typeface.BOLD, Color.BLUE));
        tr.addView(getTextView(0, "Net Weight", Color.WHITE, Typeface.BOLD, Color.BLUE));
        tl.addView(tr, getTblLayoutParams());
    }

    public void addData() {
        try {
            TableLayout tl = findViewById(R.id.table);
            int i = 0;
            for (i = 0; i < netWeights.size(); i++) {
                TableRow tr = new TableRow(this);
                tr.setLayoutParams(getLayoutParams());
                tr.addView(getTextView(i, String.valueOf(i+1), Color.WHITE, Typeface.NORMAL, ContextCompat.getColor(this, R.color.colorAccent)));
                tr.addView(getTextView(i, netBarcodeNos.get(i), Color.WHITE, Typeface.NORMAL, ContextCompat.getColor(this, R.color.colorAccent)));
                tr.addView(getTextView(i, String.valueOf(netWeights.get(i)), Color.WHITE, Typeface.NORMAL, ContextCompat.getColor(this, R.color.colorAccent)));
                totalWeight = Math.round((totalWeight + netWeights.get(i)) * 10.0f) / 10.0f;
                tl.addView(tr, getTblLayoutParams());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}