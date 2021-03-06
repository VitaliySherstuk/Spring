package chart;

import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.regex.Pattern;

public class CreateRadar {

    private XSLFSlide templiteSlide;
    private XSLFShape chartShape;
    private Rectangle2D templeteAnchor;

    //diagram parameters from templite
    private List<CTValAx> ctValAxes;
    private List<CTCatAx> ctCatAxes;
    private CTTextBody legendPropertiesText;
    private CTShapeProperties legendPropertiesShape;
    private CTShapeProperties ctShapePropertiesPloatArea;
    private CTRadarSer ctRadarSerTempliteRefOne;
    private CTRadarSer ctRadarSerTempliteRefTwo;
    private CTRadarSer ctRadarSerTempliteOne;
    private CTRadarSer ctRadarSerTempliteTwo;
    private  CTLayout ctLayout;

    public CreateRadar(XSLFSlide templiteSlide, XSLFShape chartShape){
        this.templiteSlide = templiteSlide;
        this.chartShape = chartShape;
        parseChartTemplite();
    }

    // If you take input data from excel file for diagrams(not use json), you have to use this method
    public List<DataChart> getDataRadar(Workbook workbook) {

        List<DataChart> dataRadar = new ArrayList<>();

        Sheet sheet = workbook.getSheetAt(0);
        Row rowKey = sheet.getRow(0);
        for(int i=1; i<sheet.getLastRowNum()+1; i++)
        {
            Map<String, String> allData = new HashMap<>();
            Map<String, String> chartData = new HashMap<>();
            String round = "";
            Row rowValue = sheet.getRow(i);
            for(int y=0; y<rowKey.getLastCellNum(); y++)
            {

                if(!rowKey.getCell(y).getStringCellValue().equals("system_id") & !rowKey.getCell(y).getStringCellValue().equals("_presentation_id"))
                {

                    String value = "";
                    switch(rowValue.getCell(y).getCellTypeEnum())
                    {
                        case NUMERIC:
                            if(Math.abs(rowValue.getCell(y).getNumericCellValue())>=10)
                            {
                                double formatValue = Math.round(rowValue.getCell(y).getNumericCellValue());
                                value = String.valueOf(formatValue);
                            }
                            else
                            {
                                double formatValue = Math.round(rowValue.getCell(y).getNumericCellValue()*100);
                                value = String.valueOf(formatValue/100);
                            }
                            break;
                        case  STRING:
                            value = rowValue.getCell(y).getStringCellValue();
                            break;
                    }
                    String keyWord = "";
                    if(rowKey.getCell(y).getStringCellValue().substring(1).replace("_", " ").equals("agreed upon timeline"))
                    {
                        keyWord = "agreed-upon timeline";
                    }
                    else
                    {
                        keyWord = rowKey.getCell(y).getStringCellValue().substring(1).replace("_", " ");
                    }

                    allData.put(keyWord, value);
                }
                Set<String> keys = allData.keySet();
                round = allData.get("csat round");

                for(String key : keys)
                {
                    if(!key.equals("csat round"))
                    {
                        chartData.put(key, allData.get(key));
                    }
                }
            }
            dataRadar.add(new DataChart(round, chartData));
        }

        return dataRadar;
    }

    private void parseChartTemplite() {
        List<XSLFChart> charts = new ArrayList<>();
        XSLFSheet sheet = chartShape.getSheet();
        XSLFChart chartGraf=null;
        for (POIXMLDocumentPart docPart : chartShape.getSheet().getRelations()) {

            if (docPart instanceof XSLFChart) {

               chartGraf = (XSLFChart) docPart;
                charts.add(chartGraf);
            }
        }

        //get old diagrams properties
        ctValAxes = charts.get(0).getCTChart().getPlotArea().getValAxList();
        ctCatAxes = charts.get(0).getCTChart().getPlotArea().getCatAxList();
        templeteAnchor = chartShape.getAnchor();
        ctLayout = charts.get(0).getCTChart().getPlotArea().getLayout();
        legendPropertiesText = charts.get(0).getCTChart().getLegend().getTxPr();
        legendPropertiesShape = charts.get(0).getCTChart().getLegend().getSpPr();
        ctShapePropertiesPloatArea = charts.get(0).getCTChart().getPlotArea().getSpPr();
        ctRadarSerTempliteOne = charts.get(0).getCTChart().getPlotArea().getRadarChartArray(0).getSerArray(0);
        ctRadarSerTempliteTwo = charts.get(0).getCTChart().getPlotArea().getRadarChartArray(0).getSerArray(1);

        //save template of background ser
        //ctRadarSerTempliteRefOne = charts.get(0).getCTChart().getPlotArea().getRadarChartArray(0).getSerArray(0);
        //ctRadarSerTempliteRefTwo = charts.get(0).getCTChart().getPlotArea().getRadarChartArray(0).getSerArray(1);

        //delete the old diagrams
        templiteSlide.getXmlObject().getCSld().getSpTree().getGraphicFrameList().clear();

    }

    public MyXSLFChart createChart() throws Exception {

        OPCPackage oPCPackage = templiteSlide.getSlideShow().getPackage();
        int chartCount = oPCPackage.getPartsByName(Pattern.compile("/ppt/charts/chart.*")).size() + 1;
        PackagePartName partName = PackagingURIHelper.createPartName("/ppt/charts/chart" + chartCount + ".xml");
        PackagePart part = oPCPackage.createPart(partName, "application/vnd.openxmlformats-officedocument.drawingml.chart+xml");

        MyXSLFChart myXSLFChart = new MyXSLFChart(part);
        return myXSLFChart;
    }

    public MyXSLFChartShape createChartShape(MyXSLFChart myXSLFChart) throws XmlException {

        MyXSLFChartShape myXSLFChartShape = new MyXSLFChartShape(templiteSlide, myXSLFChart, templeteAnchor);
        return myXSLFChartShape;
    }

    public void drawRadar(MyXSLFChartShape myXSLFChartShape, List<DataChart> dataRadar)
    {
        //create excel in the pptx file for edit data
        String[] mas = {"Adaptability", "Agreed-upon Timeline", "Appropriate Team", "Communication", "Comprehensive Capabilities", "Innovation", "Process", "Technical Excellence", "Value"};

        myXSLFChartShape.getMyXSLFChart().getWorkbook().getXSSFWorkbook();
        XSSFWorkbook workbook = myXSLFChartShape.getMyXSLFChart().getWorkbook().getXSSFWorkbook();
        XSSFSheet sheet = workbook.getSheetAt(0);
        sheet.createRow(0);
        sheet.getRow(0).createCell(0).setCellValue("CSAT round");
       for(int i=0; i<mas.length; i++)
       {
           sheet.getRow(0).createCell(i+1).setCellValue(mas[i]);
       }

        for(int i=0; i<dataRadar.size(); i++)
        {
            sheet.createRow(i+1);
            sheet.getRow(i+1).createCell(0).setCellValue(dataRadar.get(i).getPeriod());

            for (int c = 1; c < mas.length+1; c++) {

                sheet.getRow(i+1).createCell(c).setCellValue(Double.valueOf(dataRadar.get(i).getData().get((mas[c-1]).toLowerCase())));
            }

        }

        //create Radar chart
        CTChartSpace ctChartSpace = myXSLFChartShape.getMyXSLFChart().getChartSpace();
        CTChart ctChart = ctChartSpace.addNewChart();
        ctChart.addNewPlotVisOnly().setVal(true);
        ctChart.addNewDispBlanksAs().setVal(STDispBlanksAs.GAP);
        ctChart.addNewShowDLblsOverMax().setVal(false);
        ctChart.addNewAutoTitleDeleted();

        CTPlotArea ctPlotArea = ctChart.addNewPlotArea();
        ctPlotArea.setLayout(ctLayout);
        ctPlotArea.setSpPr(ctShapePropertiesPloatArea);

        CTRadarChart ctRadarChart = ctPlotArea.addNewRadarChart();
        ctRadarChart.addNewVaryColors().setVal(false);
        ctRadarChart.addNewRadarStyle().setVal(STRadarStyle.MARKER);

        //generate massive English letters to use for creating excel formulas(e.g. =Sheet!$A2:$J2)
        Character[] letters = new Character[26];
        int n=0;
        for(char q ='A'; q<='Z'; q++)
        {
            letters[n]=q;
            n++;
        }

        //copy RadarSer from diagram template in order to use its styles
        CTRadarSer[] arrayRadarSer = null;

        if(dataRadar.size()>2)
        {
            //code that we use for background(create for case when we have axis from 2 until 5)
            /*arrayRadarSer = new CTRadarSer[dataRadar.size() + 2];
            arrayRadarSer[0] = ctRadarSerTempliteRefOne;
            arrayRadarSer[1] = ctRadarSerTempliteRefTwo;
            arrayRadarSer[2] = ctRadarSerTempliteOne;
            arrayRadarSer[3] = ctRadarSerTempliteTwo;
            for(int i=0; i<dataRadar.size()-2; i++)
            {
               arrayRadarSer[3+(i+1)] = ctRadarSerTempliteTwo;
            }*/

            arrayRadarSer = new CTRadarSer[dataRadar.size()];
            arrayRadarSer[0] = ctRadarSerTempliteOne;
            arrayRadarSer[1] = ctRadarSerTempliteTwo;
            for(int i=0; i<dataRadar.size()-2; i++)
            {
                arrayRadarSer[1+(i+1)] = ctRadarSerTempliteTwo;
            }

        }
        else if(dataRadar.size()==1)
        {
            arrayRadarSer = new CTRadarSer[3];
            arrayRadarSer[0] = ctRadarSerTempliteRefOne;
            arrayRadarSer[1] = ctRadarSerTempliteRefTwo;
            arrayRadarSer[2] = ctRadarSerTempliteOne;
        }
        else
        {
            arrayRadarSer = new CTRadarSer[2];
            //arrayRadarSer[0] = ctRadarSerTempliteRefOne;
            //arrayRadarSer[1] = ctRadarSerTempliteRefTwo;
            arrayRadarSer[0] = ctRadarSerTempliteOne;
            arrayRadarSer[1] = ctRadarSerTempliteTwo;
        }


        //create RadarSer !REF(background)
        /*ctRadarChart.setSerArray(arrayRadarSer);
        for(int i=0; i<2; i++)
        {
            CTRadarSer ctRadarSerOne = ctRadarChart.getSerArray()[i];
            ctRadarSerOne.getIdx().setVal(i);
            ctRadarSerOne.getOrder().setVal(i);
            CTStrRef ctStrRef_cat = ctRadarSerOne.getCat().getStrRef();
            ctStrRef_cat.setF("Sheet1!$B$1:$" + letters[dataRadar.get(0).getData().size()] + "$1");
            CTStrData ctStrData1_cat = ctStrRef_cat.getStrCache();
            ctStrData1_cat.getPtCount().setVal(dataRadar.get(0).getData().size());
            CTStrVal[] ctStrVal = ctStrData1_cat.getPtArray();
            for(int y=0; y<dataRadar.get(0).getData().size(); y++)
            {
                ctStrVal[y].setIdx(y);
                ctStrVal[y].setV(mas[y]);
            }
        }*/


        //create RadarSer with value
        ctRadarChart.setSerArray(arrayRadarSer);
        for(int i=0; i<dataRadar.size(); i++)
        {       //if you deal with radar and background
            //CTRadarSer ctRadarSerTwo = ctRadarChart.getSerArray()[2+i];
            //ctRadarSerTwo.getIdx().setVal(2+i);
            //ctRadarSerTwo.getOrder().setVal(2+i);
            //ctRadarSerTwo.getTx().getStrRef().setF("Sheet1!$A$" + String.valueOf(i+2));
            //ctRadarSerTwo.getTx().getStrRef().getStrCache().getPtCount().setVal(1);
            //ctRadarSerTwo.getTx().getStrRef().getStrCache().getPtList().get(0).setV(dataRadar.get(i).getPeriod());

            CTRadarSer ctRadarSerTwo = ctRadarChart.getSerArray()[i];
            ctRadarSerTwo.getIdx().setVal(i);
            ctRadarSerTwo.getOrder().setVal(i);
            ctRadarSerTwo.getTx().getStrRef().setF("Sheet1!$A$" + String.valueOf(i+2));
            ctRadarSerTwo.getTx().getStrRef().getStrCache().getPtCount().setVal(1);

            ctRadarSerTwo.getTx().getStrRef().getStrCache().getPtList().get(0).setV(dataRadar.get(i).getPeriod());

            switch(i)
            {
                case 0:
                    ctRadarSerTwo.getSpPr().getLn().getSolidFill().getSchemeClr().setVal(STSchemeColorVal.ACCENT_1);
                    break;
                case 1:
                    ctRadarSerTwo.getSpPr().getLn().getSolidFill().getSchemeClr().setVal(STSchemeColorVal.ACCENT_2);
                    break;
                case 2:
                    ctRadarSerTwo.getSpPr().getLn().getSolidFill().getSchemeClr().setVal(STSchemeColorVal.ACCENT_3);
                    break;
                case 3:
                    ctRadarSerTwo.getSpPr().getLn().getSolidFill().getSchemeClr().setVal(STSchemeColorVal.ACCENT_6);
                    break;
            }

            ctRadarSerTwo.getCat().getStrRef().setF("Sheet1!$B$1:$" + letters[dataRadar.get(0).getData().size()] + "$1");

            //To delete <cat> and <val> data from RadarSer. To create new StrRef and NumRef and fill new data from input json or excel
            CTStrRef ctStrRef_cat_Two = ctRadarSerTwo.getCat().getStrRef();
            ctStrRef_cat_Two.unsetStrCache();
            CTStrData ctStrData1_cat_Two = ctStrRef_cat_Two.addNewStrCache();
            ctStrData1_cat_Two.addNewPtCount().setVal(dataRadar.get(0).getData().size());

            CTNumRef ctNumRefSerTwo = ctRadarSerTwo.getVal().getNumRef();
            ctNumRefSerTwo.setF("Sheet1!$B$" + String.valueOf(i+2) + ":$" + letters[dataRadar.get(0).getData().size()] + "$" + String.valueOf(i+2));
            ctNumRefSerTwo.unsetNumCache();
            CTNumData ctNumData_val_Two = ctNumRefSerTwo.addNewNumCache();
            ctNumData_val_Two.setFormatCode("General");
            ctNumData_val_Two.addNewPtCount().setVal(dataRadar.get(0).getData().size());

            for(int y=0; y<dataRadar.get(0).getData().size(); y++)
            {
                CTStrVal ctStrValTwo = ctStrData1_cat_Two.addNewPt();
                ctStrValTwo.setIdx(y);
                ctStrValTwo.setV(mas[y]);
                CTNumVal ctNumValTwo= ctNumData_val_Two.addNewPt();
                ctNumValTwo.setIdx(y);
                ctNumValTwo.setV(dataRadar.get(i).getData().get((mas[y]).toLowerCase()));
            }

        }
        ctRadarChart.addNewAxId().setVal(490351680);
        ctRadarChart.addNewAxId().setVal(490346688);

        //val axis
        CTValAx[] arrayCTValAx = new CTValAx[ctValAxes.size()];
        ctValAxes.toArray(arrayCTValAx);
        ctPlotArea.setValAxArray(arrayCTValAx);

        int[] valueScaling = estimateData(dataRadar);
        ctPlotArea.getValAxArray(0).getScaling().getMax().setVal(valueScaling[1]);
        ctPlotArea.getValAxArray(0).getScaling().getMin().setVal(valueScaling[0]);

        //create cat axis
        CTCatAx[] arrayCTCatAx = new CTCatAx[ctCatAxes.size()];
        ctCatAxes.toArray(arrayCTCatAx);
        ctPlotArea.setCatAxArray(arrayCTCatAx);

        //legend
        CTLegend cTLegend = ctChart.addNewLegend();
        cTLegend.addNewLegendPos().setVal(STLegendPos.T);

        CTLegendEntry ctLegendEntryOne = cTLegend.addNewLegendEntry();
        ctLegendEntryOne.addNewIdx().setVal(0);
        ctLegendEntryOne.addNewDelete().setVal(false);

        CTLegendEntry ctLegendEntryTwo = cTLegend.addNewLegendEntry();
        ctLegendEntryTwo.addNewIdx().setVal(1);
        ctLegendEntryTwo.addNewDelete().setVal(false);

        cTLegend.addNewOverlay().setVal(false);
        cTLegend.setTxPr(legendPropertiesText);
        cTLegend.setSpPr(legendPropertiesShape);

    }

    private int[] estimateData(List<DataChart> dataRadar)
    {
        int[] arrayMinMAx = new int[2];

        int min = 2;
        int max = 5;
        for(DataChart dataChart : dataRadar)
        {
            Map<String, String> diagramValues = dataChart.getData();
            Iterator<Map.Entry<String, String>> iterator = diagramValues.entrySet().iterator();
            while(iterator.hasNext())
            {
                Map.Entry<String, String> entry = iterator.next();
                String value = entry.getValue();
                if(Math.round(Double.valueOf(value))>max)
                {
                    max = (int) Math.round(Double.valueOf(value));
                }
                else if(Math.round(Double.valueOf(value))<min)
                {
                    min = (int) Math.round(Double.valueOf(value));

                }
            }

        }
        arrayMinMAx[0] = min;
        arrayMinMAx[1] = max;
        return arrayMinMAx;
    }
}
