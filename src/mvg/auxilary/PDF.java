package mvg.auxilary;

import mvg.model.*;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.Encoding;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import javax.print.*;
import java.awt.*;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PDF
{
    private static final int LINE_HEIGHT=20;
    private static final int ROW_COUNT = 35;
    private static final int TEXT_VERT_OFFSET=LINE_HEIGHT/4;
    private static final Insets page_margins = new Insets(100,10,100,10);
    private static int quote_page_count=1;
    private static String logo_path = "images/logo.png";
    private static String header_path = "images/header.jpg";

    private static void drawHorzLines(PDPageContentStream contents, int y_start, int page_width, Insets offsets) throws IOException
    {
        contents.setStrokingColor(new Color(171, 170, 166));
        //horizontal top title underline
        contents.moveTo(offsets.left, y_start);
        contents.lineTo(page_width-offsets.right, y_start);
        contents.stroke();
        for(int i=y_start;i>offsets.bottom;i-=LINE_HEIGHT)
        {
            //horizontal underline
            contents.moveTo(offsets.left, i-LINE_HEIGHT);
            contents.lineTo(page_width-offsets.right, i-LINE_HEIGHT);
            contents.stroke();
            //line_pos-=LINE_HEIGHT;
        }
    }

    private static void drawVertLines(PDPageContentStream contents, int[] x_positions, int y_start) throws IOException
    {
        for(int x: x_positions)
        {
            contents.moveTo(x, y_start);
            contents.lineTo(x, page_margins.bottom);
            contents.stroke();
        }
    }

    public static void createBordersOnPage(PDPageContentStream contents, int page_w, int page_top, int page_bottom) throws IOException
    {
        //top border
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, page_top);
        contents.lineTo(page_w-10, page_top);
        contents.stroke();
        //left border
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, page_top);
        contents.lineTo(10, page_bottom-LINE_HEIGHT);
        contents.stroke();
        //right border
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(page_w-10, page_top);
        contents.lineTo(page_w-10, page_bottom-LINE_HEIGHT);
        contents.stroke();
        //bottom border
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, page_bottom-LINE_HEIGHT);
        contents.lineTo(page_w-10, page_bottom-LINE_HEIGHT);
        contents.stroke();
    }

    public static void createLinesAndBordersOnPage(PDPageContentStream contents, int page_w, int page_top, int page_bottom) throws IOException
    {
        boolean isTextMode=false;
        try
        {//try to end the text of stream.
            contents.endText();
            isTextMode=true;
        }catch (IllegalStateException e) {}
        //draw borders
        createBordersOnPage(contents, page_w, page_top, page_bottom);
        //draw horizontal lines
        int line_pos=page_top;
        for(int i=0;i<ROW_COUNT;i++)//35 rows
        {
            //horizontal underline
            contents.setStrokingColor(new Color(171, 170, 166));
            contents.moveTo(10, line_pos-LINE_HEIGHT);
            contents.lineTo(page_w-10, line_pos-LINE_HEIGHT);
            contents.stroke();
            line_pos-=LINE_HEIGHT;
        }
        if(isTextMode)
            contents.beginText();
    }

    public static void addTextToPageStream(PDPageContentStream contents, String text, int font_size, int x, int y) throws IOException
    {
        try
        {
            addTextToPageStream(contents, text, PDType1Font.HELVETICA, font_size, x, y);
        }catch (IllegalArgumentException e)
        {
            IO.log("PDF creator", IO.TAG_ERROR, e.getMessage());
        }
    }

    public static void addTextToPageStream(PDPageContentStream contents, String text, PDFont font,int font_size, int x, int y) throws IOException
    {
        contents.setFont(font, font_size);
        contents.setTextMatrix(new Matrix(1, 0, 0, 1, x, y-TEXT_VERT_OFFSET));

        char[] text_arr = text.toCharArray();
        StringBuilder str_builder = new StringBuilder();
        //PDType0Font.
        Encoding e = org.apache.pdfbox.pdmodel.font.encoding.Encoding.getInstance(COSName.WIN_ANSI_ENCODING);// EncodingManager.INSTANCE.getEncoding(COSName.WIN_ANSI_ENCODING);
        //Encoding e = EncodingManager.INSTANCE.getEncoding(COSName.WIN_ANSI_ENCODING);

        System.out.println("\n\n::::::::::::::::::::Processing Text: [" + text + "]::::::::::::::::::::");
        System.out.println("Encoding Name: " + e.getEncodingName());
        System.out.println("Encoding Name to Code Map: " + e.getNameToCodeMap());
        //String toPDF = String.valueOf(Character.toChars(e.getCode(e.getNameFromCharacter(symbol))));

        for (int i = 0; i < text_arr.length; i++)
        {
            Character c = text_arr[i];
            int code = 0;
            System.out.println(String.format("Character [%s] has codename: [%s] and code [%s]", c, e.getName(c), String.valueOf(e.getNameToCodeMap().get(c))));
            if(e.getName(c).toLowerCase().equals(".notdef"))
                str_builder.append("[?]");
            else str_builder.append(c);
            /*if(Character.isWhitespace(c))
            {
                code = e.getNameToCodeMap().get("space");
            }else{
                String toPDF = String.valueOf(Character.toChars(e.getCodeToNameMap().get(e.getName(symbol))));
                code = e.getNameToCodeMap(e.getName(c));
            }
            str_builder.appendCodePoint(code);*/
        }
        contents.showText(str_builder.toString());
    }

    public static void printPDF(final byte[] byteStream) throws PrintException
    {
        PrinterJob printerJob = PrinterJob.getPrinterJob();

        PrintService printService=null;
        if(printerJob.printDialog())
        {
            printService = printerJob.getPrintService();
        }
        if(printService!=null)
        {
            DocFlavor docType = DocFlavor.INPUT_STREAM.AUTOSENSE;

            DocPrintJob printTrip = printService.createPrintJob();
            Doc documentToBePrinted = new SimpleDoc(new ByteArrayInputStream(byteStream), docType, null);
            printTrip.print(documentToBePrinted, null);
        }else{
            IO.logAndAlert("Print Trip", "Print Trip cancelled.", IO.TAG_INFO);
        }
    }

    public static String createEnquiryPDF(Enquiry enquiry) throws IOException
    {
        //create PDF output directory
        if(new File("out/pdf/").mkdirs())
            IO.log(PDF.class.getName(), "successfully created PDF output directory [out/pdf/]", IO.TAG_INFO);

        // Create a new document with an empty page.
        final PDDocument document = new PDDocument();
        final PDPage page = new PDPage(PDRectangle.A4);

        final float w = page.getBBox().getWidth();
        final float h = page.getBBox().getHeight();

        //Add page to document
        document.addPage(page);

        // Adobe Acrobat uses Helvetica as a default font and
        // stores that under the name '/Helv' in the resources dictionary
        PDFont font = PDType1Font.HELVETICA;
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName("Helv"), font);

        PDPageContentStream contents = new PDPageContentStream(document, page);
        int logo_h = 60;
        PDImageXObject logo = PDImageXObject.createFromFile("images/logo.png", document);
        contents.drawImage(logo, (w/2)-80, 770, 160, logo_h);

        int line_pos = (int)h-logo_h-LINE_HEIGHT;

        /** draw horizontal lines **/
        drawHorzLines(contents, line_pos, (int)w, page_margins);
        /** draw vertical lines **/
        final int[] col_positions = {(int)((w / 2)), (int)((w / 2) + 100), (int)((w / 2) + 200)};
        drawVertLines(contents, col_positions, line_pos-LINE_HEIGHT);
        line_pos = (int)h-logo_h-LINE_HEIGHT;

        /** begin text from the top**/
        contents.beginText();
        contents.setFont(font, 12);
        line_pos-=10;
        //Heading text
        addTextToPageStream(contents, "Enquiry", 16,(int)(w/2)-70, line_pos);
        line_pos-=LINE_HEIGHT*2;//next 2nd line

        addTextToPageStream(contents, "DATE LOGGED: ", 16,10, line_pos);
        addTextToPageStream(contents, (new SimpleDateFormat("yyyy-MM-dd").format(enquiry.getDate_logged()*1000)), 16,(int)w/2+100, line_pos);
        line_pos-=LINE_HEIGHT*2;//next 2nd line

        addTextToPageStream(contents, enquiry.getEnquiry(), PDType1Font.TIMES_ITALIC, 16 ,10, line_pos);
        line_pos-=LINE_HEIGHT*2;//next 2nd line

        //Create column headings
        addTextToPageStream(contents,"Trip Type", 14,10, line_pos);
        addTextToPageStream(contents,"From", 14, col_positions[0]+10, line_pos);
        addTextToPageStream(contents,"To", 14,col_positions[1]+10, line_pos);
        addTextToPageStream(contents,"Date", 14,col_positions[2]+10, line_pos);

        contents.endText();
        line_pos-=LINE_HEIGHT;//next line

        //int pos = line_pos;
        contents.beginText();
        addTextToPageStream(contents, String.valueOf(enquiry.getTrip_type()), 14, 10, line_pos);
        addTextToPageStream(contents, enquiry.getPickup_location(), 12, col_positions[0]+5, line_pos);
        addTextToPageStream(contents, enquiry.getDestination(), 12, col_positions[1]+5, line_pos);
        if(enquiry.getDate_scheduled()>0)
            addTextToPageStream(contents, (new SimpleDateFormat("yyyy-MM-dd").format(enquiry.getDate_scheduled()*1000)), 12, col_positions[2]+5, line_pos);
        else addTextToPageStream(contents, "N/A", 12, col_positions[2]+5, line_pos);

        line_pos-=LINE_HEIGHT*2;//next 2nd line
        String status = "N/A";
        /*switch (leave.getStatus())
        {
            case Leave.STATUS_PENDING:
                status="PENDING";
                break;
            case Leave.STATUS_APPROVED:
                status="GRANTED";
                break;
            case Leave.STATUS_ARCHIVED:
                status="ARCHIVED";
                break;
        }
        addTextToPageStream(contents, "STATUS: ", 14,10, line_pos);
        addTextToPageStream(contents, status, 14,100, line_pos);*/
        line_pos-=LINE_HEIGHT*2;//next 2nd line

        if(enquiry.getOther()!=null)
            addTextToPageStream(contents, enquiry.getOther(), 16, 15, line_pos);

        line_pos-=LINE_HEIGHT*3;//next 3rd line
        addTextToPageStream(contents, "Applicant's Signature", 16,10, line_pos);
        addTextToPageStream(contents, "Manager Signature", 16, 200, line_pos);
        contents.endText();

        //draw first signature line
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos+LINE_HEIGHT+5);
        contents.lineTo(120, line_pos+LINE_HEIGHT+5);
        contents.stroke();
        //draw second signature line
        contents.moveTo(200, line_pos+LINE_HEIGHT+5);
        contents.lineTo(320, line_pos+LINE_HEIGHT+5);
        contents.stroke();

        String path = "out/pdf/enquiry_" + enquiry.get_id() + ".pdf";
        int i=1;
        while(new File(path).exists())
        {
            path = "out/pdf/enquiry_" + enquiry.get_id() + "." + i + ".pdf";
            i++;
        }

        contents.close();
        document.save(path);
        document.close();

        return path;
    }

    public static String createQuotePdf(Quote quote) throws IOException
    {
        if(quote==null)
        {
            IO.logAndAlert("PDF Viewer", "Quote object passed is null.", IO.TAG_ERROR);
            return null;
        }
        if(quote.getEnquiry()==null)
        {
            IO.logAndAlert("PDF Viewer", "Quote's Enquiry object passed is null.", IO.TAG_ERROR);
            return null;
        }
        //Prepare PDF data from database.
        //Load Quote Client
        Client client = quote.getClient();
        if(client==null)
        {
            IO.logAndAlert("PDF Viewer Error", "Quote has no client assigned to it.", IO.TAG_ERROR);
            return null;
        }
        User contact = quote.getContact_person();
        if(contact==null)
        {
            IO.logAndAlert("PDF Viewer Error", "Quote has no client contact person assigned to it.", IO.TAG_ERROR);
            return null;
        }

        // Create a new document with an empty page.
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        // Adobe Acrobat uses Helvetica as a default font and
        // stores that under the name '/Helv' in the resources dictionary
        PDFont font = PDType1Font.HELVETICA;
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName("Helv"), font);

        PDPageContentStream contents = new PDPageContentStream(document, page);
        int logo_h = 60;
        PDImageXObject logo = PDImageXObject.createFromFile("images/logo.png", document);
        contents.drawImage(logo, 10, 770, 160, logo_h);

        float w = page.getBBox().getWidth();
        float h = page.getBBox().getHeight();
        int line_pos = (int)h-logo_h-20;
        int digit_font_size=9;

        /**Draw lines**/
        int center_vert_line_start = line_pos;
        int bottom_line = (int)h-logo_h-(ROW_COUNT+1)*LINE_HEIGHT;
        createLinesAndBordersOnPage(contents, (int)w, line_pos, bottom_line);

        /** begin text from the top**/
        contents.beginText();
        contents.setFont(font, 12);
        line_pos-=LINE_HEIGHT/2;
        //left text
        addTextToPageStream(contents,"Client Information", PDType1Font.COURIER_BOLD_OBLIQUE, 15,(int)((w/2)/4), line_pos);
        //right text
        addTextToPageStream(contents,"Quotation No.: " + quote.quoteProperty().getValue(), PDType1Font.COURIER_BOLD_OBLIQUE, 11, (int)(w/2)+5, line_pos);
        line_pos-=LINE_HEIGHT;//next line

        //left text
        addTextToPageStream(contents,"Company: " + client.getClient_name(), 12, 20, line_pos);
        //right text
        addTextToPageStream(contents,"Date Generated:  " + (new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis()))), 12,(int)(w/2)+5, line_pos);
        line_pos-=LINE_HEIGHT;
        addTextToPageStream(contents,"Date Logged:  " + (new SimpleDateFormat("yyyy-MM-dd").format(new Date(quote.getDate_logged()))), 12,(int)(w/2)+5, line_pos);
        line_pos-=LINE_HEIGHT;//next line

        //left text
        addTextToPageStream(contents,"Company Tel: " + client.getTel(), 12,20, line_pos);
        //right text
        addTextToPageStream(contents,"Sale Consultant(s): ", PDType1Font.COURIER_BOLD_OBLIQUE, 16,(int)((w/2)+((w/2)/4)), line_pos);

        //horizontal solid line after company details
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos-LINE_HEIGHT/2);
        contents.lineTo(w-10, line_pos-LINE_HEIGHT/2);
        contents.stroke();
        contents.beginText();

        line_pos-=LINE_HEIGHT;//next line

        int temp_pos = line_pos;
        //left text
        addTextToPageStream(contents,"Contact Person:  " + contact.toString(), PDType1Font.HELVETICA_BOLD, 12,20, line_pos);
        line_pos-=LINE_HEIGHT;//next line
        addTextToPageStream(contents,"Tel    :  " + contact.getTel(), PDType1Font.HELVETICA_BOLD, 12,120, line_pos);
        line_pos-=LINE_HEIGHT;//next line
        addTextToPageStream(contents,"Cell   :  " + contact.getCell(), PDType1Font.HELVETICA_BOLD, 12,120, line_pos);
        line_pos-=LINE_HEIGHT;//next line
        addTextToPageStream(contents,"eMail :  " + contact.getEmail(), PDType1Font.HELVETICA_BOLD, 12,120, line_pos);

        //horizontal solid line
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos-LINE_HEIGHT/2);
        contents.lineTo(w-10, line_pos-LINE_HEIGHT/2);
        contents.stroke();
        contents.beginText();

        line_pos-=LINE_HEIGHT;//next line (for external consultants)
        //temp_pos-=LINE_HEIGHT;//next line (for internal consultants)
        //Render sale representatives
        int int_rep_count=0;
        User creator = quote.getCreatorUser();
        //if the page can't hold 4 more lines add a new page
        if(line_pos-(4*LINE_HEIGHT)<h-logo_h-(ROW_COUNT*LINE_HEIGHT) || temp_pos-(4*LINE_HEIGHT)<h-logo_h-(ROW_COUNT*LINE_HEIGHT))
        {
            addTextToPageStream(contents, "Page "+quote_page_count, PDType1Font.HELVETICA_OBLIQUE, 14,(int)(w/2)-20, 50);
            //add new page
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            //TODO: setup page, i.e. draw lines and stuff
            contents.close();
            contents = new PDPageContentStream(document, page);
            temp_pos = (int)h-logo_h;
            line_pos = (int)h-logo_h;

            createLinesAndBordersOnPage(contents, (int)w, line_pos, line_pos+LINE_HEIGHT/2);

            contents.beginText();
            quote_page_count++;
        }

        if(!creator.isActiveVal())//external user
        {
            addTextToPageStream(contents,"Contact Person:   " + creator.toString(), 12,20, line_pos);
            line_pos-=LINE_HEIGHT;//next line
            addTextToPageStream(contents,"Tel    :  " + creator.getTel(), 12,120, line_pos);
            line_pos-=LINE_HEIGHT;//next line
            addTextToPageStream(contents,"Cell   :  " + creator.getCell(), 12,120, line_pos);
            line_pos-=LINE_HEIGHT;//next line
            addTextToPageStream(contents,"eMail :  " + creator.getEmail(), 12,120, line_pos);
            line_pos-=LINE_HEIGHT;//next line
        }else {//internal representatives
            if(int_rep_count==0)//make first internal rep bold
            {
                addTextToPageStream(contents, "Sale Consultant:  " + creator.toString(), PDType1Font.HELVETICA_BOLD, 12, (int) (w / 2) + 5, temp_pos);
                temp_pos -= LINE_HEIGHT;//next line
                addTextToPageStream(contents, "Tel    :  " + creator.getTel(), PDType1Font.HELVETICA_BOLD, 12, (int) (w / 2) + 105, temp_pos);
                temp_pos -= LINE_HEIGHT;//next line
                addTextToPageStream(contents, "Cell   :  " + creator.getCell(), PDType1Font.HELVETICA_BOLD, 12, (int) (w / 2) + 105, temp_pos);
                temp_pos -= LINE_HEIGHT;//next line
                addTextToPageStream(contents, "eMail :  " + creator.getEmail(), PDType1Font.HELVETICA_BOLD, 12, (int) (w / 2) + 105, temp_pos);
                temp_pos -= LINE_HEIGHT;//next line
            }else
            {
                addTextToPageStream(contents, "Sale Consultant:  " + creator.toString(), 12, (int) (w / 2) + 5, temp_pos);
                temp_pos -= LINE_HEIGHT;//next line
                addTextToPageStream(contents, "Tel    :  " + creator.getTel(), 12, (int) (w / 2) + 105, temp_pos);
                temp_pos -= LINE_HEIGHT;//next line
                addTextToPageStream(contents, "Cell   :  " + creator.getCell(), 12, (int) (w / 2) + 105, temp_pos);
                temp_pos -= LINE_HEIGHT;//next line
                addTextToPageStream(contents, "eMail :  " + creator.getEmail(), 12, (int) (w / 2) + 105, temp_pos);
                temp_pos -= LINE_HEIGHT;//next line
            }
            int_rep_count++;
        }
        //set the cursor to the line after the sale/client rep info
        line_pos = line_pos<temp_pos?line_pos:temp_pos;
        addTextToPageStream(contents,"Request: " + quote.getRequest(),PDType1Font.HELVETICA, 13,20, line_pos);
        line_pos-=LINE_HEIGHT;//next line

        contents.endText();

        //horizontal solid line after reps
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos+LINE_HEIGHT+LINE_HEIGHT/2);
        contents.lineTo(w-10, line_pos+LINE_HEIGHT+LINE_HEIGHT/2);
        contents.stroke();
        //horizontal solid line after request
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos+LINE_HEIGHT/2);
        contents.lineTo(w-10, line_pos+LINE_HEIGHT/2);
        contents.stroke();
        //solid horizontal line after site location, before quote_items
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, (line_pos-LINE_HEIGHT+(int)Math.ceil(LINE_HEIGHT/2)));
        contents.lineTo(w-10, (line_pos-LINE_HEIGHT+(int)Math.ceil(LINE_HEIGHT/2)));
        contents.stroke();

        int col_divider_start = line_pos-LINE_HEIGHT;

        //vertical line going through center of page
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo((w/2), center_vert_line_start);
        contents.lineTo((w/2),(col_divider_start+LINE_HEIGHT*2+(int)Math.ceil(LINE_HEIGHT/2)));
        contents.stroke();
        //
        contents.moveTo((w/2), (col_divider_start+(int)Math.ceil(LINE_HEIGHT/2)));
        contents.lineTo((w/2),(col_divider_start-LINE_HEIGHT+(int)Math.ceil(LINE_HEIGHT/2)));
        contents.stroke();

        contents.beginText();
        addTextToPageStream(contents,"Pickup Address: "+quote.getEnquiry().getPickup_location(),PDType1Font.HELVETICA, 13,20, line_pos);
        line_pos-=LINE_HEIGHT;//next line

        contents.beginText();
        addTextToPageStream(contents,"Destination Address: "+quote.getEnquiry().getDestination(),PDType1Font.HELVETICA, 13,20, line_pos);
        line_pos-=LINE_HEIGHT;//next line

        contents.endText();

        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, (line_pos-LINE_HEIGHT+(int)Math.ceil(LINE_HEIGHT/2)));
        contents.lineTo(w-10, (line_pos-LINE_HEIGHT+(int)Math.ceil(LINE_HEIGHT/2)));
        contents.stroke();
        contents.beginText();

        //Column headings
        int col_pos = 10;
        addTextToPageStream(contents,"Item No.", PDType1Font.COURIER_BOLD,14,15, line_pos);
        col_pos += 80;
        addTextToPageStream(contents,"Equipment description", PDType1Font.COURIER_BOLD,14,col_pos+20, line_pos);
        col_pos = (int)(w/2);
        String[] cols = {"Unit", "Qty", "Rate", "Labour", "Total"};
        for(int i=0;i<5;i++)//7 cols in total
            addTextToPageStream(contents,cols[i], PDType1Font.COURIER_BOLD, 12,col_pos+(55*i)+10, line_pos);
        line_pos-=LINE_HEIGHT;//next line

        //Actual quote information
        col_pos = 10;
        double sub_total = 0;
        if(quote.getResources()!=null)
        {
            for(QuoteItem item: quote.getResources())
            {
                //quote content column dividers
                contents.endText();
                //#1
                contents.moveTo(80, (col_divider_start+(int)Math.ceil(LINE_HEIGHT/2)));
                contents.lineTo(80, line_pos-LINE_HEIGHT/2);
                contents.stroke();
                //vertical line going through center of page
                contents.setStrokingColor(Color.BLACK);
                contents.moveTo((w/2), (col_divider_start-LINE_HEIGHT+(int)Math.ceil(LINE_HEIGHT/2)));
                contents.lineTo((w/2),line_pos-LINE_HEIGHT/2);
                contents.stroke();
                //#3+
                for(int i=1;i<5;i++)//7 cols in total
                {
                    contents.moveTo((w/2)+55*i, (col_divider_start+(int)Math.ceil(LINE_HEIGHT/2)));
                    contents.lineTo((w/2)+55*i,line_pos-LINE_HEIGHT/2);
                    contents.stroke();
                }
                contents.beginText();
                //end draw columns

                //if the page can't hold another 4 lines[current item, blank, sub-total, vat] add a new page
                if(line_pos-LINE_HEIGHT<h-logo_h-(ROW_COUNT*LINE_HEIGHT))
                {
                    addTextToPageStream(contents, "Page "+quote_page_count, PDType1Font.COURIER_OBLIQUE, 14,(int)(w/2)-20, 30);
                    //add new page
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    //TODO: setup page, i.e. draw lines and stuff
                    contents.close();
                    contents = new PDPageContentStream(document, page);
                    contents.beginText();
                    line_pos = (int)h-logo_h;
                    col_divider_start = line_pos+LINE_HEIGHT;
                    createLinesAndBordersOnPage(contents, (int)w, line_pos+LINE_HEIGHT/2, bottom_line);
                    quote_page_count++;
                }

                col_pos =0;//first column
                //Item col
                addTextToPageStream(contents, item.getItem_number(), 12,col_pos+30, line_pos);
                col_pos += 80;//next column
                //Description col
                addTextToPageStream(contents, item.getResource().getResource_name(), 12,col_pos+5, line_pos);
                col_pos = (int)w/2;//next column - starts at middle of page
                //Unit col
                addTextToPageStream(contents,item.getUnit(), 12,col_pos+5, line_pos);
                col_pos+=55;//next column
                //Quantity col
                addTextToPageStream(contents,item.getQuantity(), digit_font_size,col_pos+5, line_pos);
                col_pos+=55;//next column
                //Rate col
                addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance().format(item.getRate())), digit_font_size,col_pos+5, line_pos);
                col_pos+=55;//next column
                //Labour col
                //addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance().format(item.getLabourCost())), digit_font_size,col_pos+5, line_pos);
                col_pos+=55;//next column
                //Total col
                sub_total+=item.getTotal();
                addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance().format(item.getTotal())), digit_font_size,col_pos+5, line_pos);

                line_pos -= LINE_HEIGHT;//next line
            }
            IO.log(PDF.class.getName(), IO.TAG_INFO, "successfully created quote PDF.");
        } else IO.log(PDF.class.getName(), IO.TAG_INFO, "quote has no resources.");
        col_pos = 0;
        //line_pos -= LINE_HEIGHT;//skip another line
        /*if the page can't hold another 2 lines add a new page
        if(line_pos-LINE_HEIGHT*2<h-logo_h-(ROW_COUNT*LINE_HEIGHT) || temp_pos-LINE_HEIGHT*2<h-logo_h-(ROW_COUNT*LINE_HEIGHT))
        {
            //add new page
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            //TODO: setup page, i.e. draw lines and stuff
            contents.close();
            contents = new PDPageContentStream(document, page);
            contents.beginText();
            line_pos = (int)h-logo_h;
            col_divider_start = line_pos+LINE_HEIGHT;
        }*/
        //solid horizontal line
        int col_divider_end= line_pos;

        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos+LINE_HEIGHT/2);
        contents.lineTo(w-10, line_pos+LINE_HEIGHT/2);
        contents.stroke();

        contents.beginText();
        addTextToPageStream(contents, "Sub-Total Excl. VAT: ", PDType1Font.COURIER_BOLD_OBLIQUE, 14,col_pos+30, line_pos);
        addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance().format(sub_total)), PDType1Font.COURIER_BOLD_OBLIQUE, 14,(int)(5+(w/2)), line_pos);
        line_pos -= LINE_HEIGHT;//next line

        //solid horizontal line
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos+LINE_HEIGHT/2);
        contents.lineTo(w-10, line_pos+LINE_HEIGHT/2);
        contents.stroke();

        double vat = sub_total*(quote.getVat()/100);
        contents.beginText();
        addTextToPageStream(contents, "VAT [" + quote.getVat() + "%]: ", PDType1Font.COURIER_BOLD_OBLIQUE, 14,col_pos+30, line_pos);
        addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance().format(vat)), PDType1Font.COURIER_BOLD_OBLIQUE, 14,(int)(5+(w/2)), line_pos);
        line_pos -= LINE_HEIGHT;//next line

        //solid horizontal line
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos+LINE_HEIGHT/2);
        contents.lineTo(w-10, line_pos+LINE_HEIGHT/2);
        contents.stroke();

        contents.beginText();
        addTextToPageStream(contents, "Total Incl. VAT: ", PDType1Font.COURIER_BOLD_OBLIQUE, 14,col_pos+30, line_pos);
        addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance().format(sub_total + vat)), PDType1Font.COURIER_BOLD_OBLIQUE, 14,(int)(5+(w/2)), line_pos);
        contents.endText();
        line_pos -= LINE_HEIGHT;//next line

        //solid horizontal line
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos+LINE_HEIGHT/2);
        contents.lineTo(w-10, line_pos+LINE_HEIGHT/2);
        contents.stroke();

        //int col_divider_end = line_pos;
        line_pos -= LINE_HEIGHT*3;//next 3rd line
        /*solid horizontal lines after quote_items
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, col_divider_end+LINE_HEIGHT+LINE_HEIGHT/2);
        contents.lineTo(w-10, col_divider_end+LINE_HEIGHT+LINE_HEIGHT/2);
        contents.stroke();
        contents.moveTo(10, col_divider_end+LINE_HEIGHT/2);
        contents.lineTo(w-10, col_divider_end+LINE_HEIGHT/2);
        contents.stroke();
        contents.moveTo(10, col_divider_end-LINE_HEIGHT+LINE_HEIGHT/2);
        contents.lineTo(w-10, col_divider_end-LINE_HEIGHT+LINE_HEIGHT/2);
        contents.stroke();*/

        //quote content column dividers
        //#1
        contents.moveTo(80, (col_divider_start-LINE_HEIGHT+(int)Math.ceil(LINE_HEIGHT/2)));
        contents.lineTo(80, col_divider_end+LINE_HEIGHT+LINE_HEIGHT/2);
        contents.stroke();
        //vertical line going through center of page again
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo((w/2), (col_divider_start-LINE_HEIGHT+(int)Math.ceil(LINE_HEIGHT/2)));
        contents.lineTo((w/2), col_divider_end+LINE_HEIGHT+LINE_HEIGHT/2);
        //contents.lineTo((w/2),col_divider_end-LINE_HEIGHT/2);
        contents.stroke();
        //#3+
        for(int i=1;i<5;i++)//7 cols in total
        {
            contents.moveTo((w/2)+55*i, (col_divider_start-LINE_HEIGHT+(int)Math.ceil(LINE_HEIGHT/2)));
            contents.lineTo((w/2)+55*i,col_divider_end+LINE_HEIGHT+LINE_HEIGHT/2);
            contents.stroke();
        }

        contents.beginText();

        if(quote.getOther()!=null)
            addTextToPageStream(contents, "P.S. "+quote.getOther(), PDType1Font.TIMES_ITALIC, 14,col_pos+5, line_pos);

        line_pos -= LINE_HEIGHT;//next line
        //if the page can't hold another 9 lines add a new page
        if(line_pos-(LINE_HEIGHT*4)<h-logo_h-(ROW_COUNT*LINE_HEIGHT))
        {
            addTextToPageStream(contents, "Page "+quote_page_count, PDType1Font.COURIER_OBLIQUE, 14,(int)(w/2)-20, 30);
            //add new page
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contents.close();
            contents = new PDPageContentStream(document, page);
            contents.beginText();
            line_pos = (int)h-logo_h;
            createLinesAndBordersOnPage(contents, (int)w, line_pos+LINE_HEIGHT/2, bottom_line);
            quote_page_count++;
        }
        addTextToPageStream(contents, "TERMS AND CONDITIONS OF SALE", PDType1Font.HELVETICA_BOLD, 14,(int)(w/2)-130, line_pos);
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo((int)(w/2)-140, line_pos-LINE_HEIGHT/2);
        contents.lineTo((w/2)+120, line_pos-LINE_HEIGHT/2);
        contents.stroke();
        contents.beginText();

        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*Validity: Quote valid for 24 Hours.", PDType1Font.HELVETICA, 12,col_pos+30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*Payment Terms: COD / 30 Days on approved accounts. ", PDType1Font.HELVETICA, 12,col_pos+30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*Delivery: 1 - 6 Weeks, subject to stock availability.", PDType1Font.HELVETICA, 12,col_pos+30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*All pricing quoted, is subject to Rate of Exchange USD=R.", PDType1Font.HELVETICA_BOLD, 12,col_pos+30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*All goods / equipment remain the property of " + Globals.COMPANY.getValue()+ " until paid for completely. ", PDType1Font.HELVETICA, 12,col_pos+30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*" + Globals.COMPANY.getValue() + " reserves the right to retake posession of all equipment not paid for completely", PDType1Font.HELVETICA, 12,col_pos+30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "  Within the payment term set out above.", PDType1Font.HELVETICA, 12,col_pos+30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*E & O E", PDType1Font.HELVETICA, 12,col_pos+30, line_pos);

        addTextToPageStream(contents, "Page "+quote_page_count, PDType1Font.COURIER_OBLIQUE, 14,(int)(w/2)-20, 30);
        contents.endText();
        contents.close();

        //create PDF output directory
        if(new File("out/pdf/").mkdirs())
            IO.log(PDF.class.getName(), "successfully created PDF output directory [out/pdf/]", IO.TAG_INFO);

        String path = "out/pdf/quote_" + quote.get_id() + ".pdf";
        int i=1;
        while(new File(path).exists())
        {
            path = "out/pdf/quote_" + quote.get_id() + "." + i + ".pdf";
            i++;
        }

        //Files.delete(Paths.get("bin/pdf/quote_"+quote.get_id()+".pdf"));//delete previous versions

        if(contents!=null)
            contents.close();
        document.save(path);
        document.close();

        //PDFViewer pdfViewer = PDFViewer.getInstance();
        //pdfViewer.setVisible(true);
        //pdfViewer.doOpen(path);//"bin/pdf/quote_" + quote.get_id() + ".pdf"
        return path;
    }

    public static String createInvoicePdf(Invoice invoice) throws IOException//, HashMap<String, Quote> quote_revisions
    {
        if(invoice==null)
        {
            IO.logAndAlert("PDF Viewer", "Invoice object passed is null.", IO.TAG_ERROR);
            return null;
        }
        if(invoice.getTrip()==null)
        {
            IO.logAndAlert("PDF Viewer", "Invoice->Trip object passed is null.", IO.TAG_ERROR);
            return null;
        }
        if(invoice.getTrip().getQuote()==null)
        {
            IO.logAndAlert("PDF Viewer", "Invoice->Quote object passed is null.", IO.TAG_ERROR);
            return null;
        }
        if(invoice.getTrip().getQuote().getEnquiry()==null)
        {
            IO.logAndAlert("PDF Viewer", "Invoice->Quote's Enquiry object passed is null.", IO.TAG_ERROR);
            return null;
        }
        Trip Trip = invoice.getTrip();
        // Create a new document with an empty page.
        PDDocument document = new PDDocument();

        // Adobe Acrobat uses Helvetica as a default font and
        // stores that under the name '/Helv' in the resources dictionary
        PDFont font = PDType1Font.HELVETICA;
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName("Helv"), font);

        Quote quote = invoice.getTrip().getQuote();
        //Quote[] quotes = invoice.getTrip().getQuote().getSortedSiblings("revision");
        //Prepare PDF data from database.
        //Load Invoice Client
        Client client = quote.getClient();
        if (client == null)
        {
            IO.logAndAlert("PDF Viewer Error", "Quote[" + quote
                    .get_id() + "] has no client assigned to it.", IO.TAG_ERROR);
            return null;
        }
        User contact = quote.getContact_person();
        if (contact == null)
        {
            IO.logAndAlert("PDF Viewer Error", "Quote has no client contact person assigned to it.", IO.TAG_ERROR);
            return null;
        }

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDPageContentStream contents = new PDPageContentStream(document, page);
        int logo_h = 60;
        //PDImageXObject logo = PDImageXObject.createFromFile(logo_path, document);
        //contents.drawImage(logo, 10, 770, 160, logo_h);

        float w = page.getBBox().getWidth();
        float h = page.getBBox().getHeight();
        int line_pos = (int) h - 20;//(int)h-logo_h-20;
        int digit_font_size = 9;

        /**Draw lines**/
        int bottom_line = (int) h - logo_h - (ROW_COUNT + 1) * LINE_HEIGHT;
        createLinesAndBordersOnPage(contents, (int) w, line_pos, bottom_line);

        /** begin text from the top**/
        line_pos -= LINE_HEIGHT / 2;

        //left text
        contents.beginText();
        int temp_pos = line_pos;
        addTextToPageStream(contents, "Invoice ID: " + invoice
                .get_id(), PDType1Font.COURIER_BOLD_OBLIQUE, 15, 20, line_pos);
        line_pos -= LINE_HEIGHT;
        int center_vert_line_start = line_pos;
        addTextToPageStream(contents, "Date Generated:  " + (new SimpleDateFormat("yyyy-MM-dd")
                .format(new Date(System.currentTimeMillis()))), 12, 20, line_pos);
        line_pos -= LINE_HEIGHT;
        addTextToPageStream(contents, "Date Logged:  " + (new SimpleDateFormat("yyyy-MM-dd")
                .format(new Date(Trip.getDate_logged() * 1000))), 12, 20, line_pos);
        line_pos -= LINE_HEIGHT;
        addTextToPageStream(contents, "Creator:  " + invoice.getCreator(), 12, 20, line_pos);
        line_pos -= LINE_HEIGHT;
        addTextToPageStream(contents, "Quote ID: " + quote.get_id(), 14, 20, line_pos);
        //line_pos-=LINE_HEIGHT;
        //addTextToPageStream(contents,"Quote Date Generated: " + (new SimpleDateFormat("yyyy-MM-dd").format(new Date(quote.getDate_generated()*1000))), 12,(int)(w/2)+ 5, line_pos);

        line_pos = temp_pos;

        //right content
        contents.endText();
        PDImageXObject logo = PDImageXObject.createFromFile(logo_path, document);
        contents.drawImage(logo, (int) (w / 2) + 20, line_pos - logo_h - 10, 150, logo_h);

        line_pos -= LINE_HEIGHT * 5;
        temp_pos = line_pos;

        //horizontal solid line after company logo
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos + LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos + LINE_HEIGHT / 2);
        contents.stroke();

        //horizontal solid line after consultants heading
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos - LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos - LINE_HEIGHT / 2);
        contents.stroke();
        contents.beginText();

        //left text
        addTextToPageStream(contents, "Client Information", PDType1Font.COURIER_BOLD_OBLIQUE, 15, 20, line_pos);
        line_pos -= LINE_HEIGHT;
        addTextToPageStream(contents, "Company: " + client.getClient_name(), 12, 20, line_pos);
        line_pos -= LINE_HEIGHT;
        addTextToPageStream(contents, "Company Tel: " + client.getTel(), 12, 20, line_pos);
        //addTextToPageStream(contents,"Contact Person[s]: ", 12,20, line_pos-LINE_HEIGHT);

        line_pos = temp_pos;

        //right content
        addTextToPageStream(contents, "Trip Number #" + Trip.get_id(),
                PDType1Font.COURIER_BOLD_OBLIQUE, 15, (int) (w / 2) + 5, line_pos);
        line_pos -= LINE_HEIGHT;
        addTextToPageStream(contents, "Date Logged:  " + (new SimpleDateFormat("yyyy-MM-dd")
                .format(new Date(Trip.getDate_logged() * 1000))), 12, (int) (w / 2) + 5, line_pos);
        line_pos -= LINE_HEIGHT;
        addTextToPageStream(contents, "Date Scheduled:  " + (new SimpleDateFormat("yyyy-MM-dd")
                .format(new Date(Trip.getDate_scheduled() * 1000))), 12, (int) (w / 2) + 5, line_pos);
        line_pos -= LINE_HEIGHT;
        addTextToPageStream(contents, "Driver Assignment Date:  " + (new SimpleDateFormat("yyyy-MM-dd")
                .format(new Date(Trip.getDate_assigned() * 1000))), 12, (int) (w / 2) + 5, line_pos);
        line_pos -= LINE_HEIGHT;
        addTextToPageStream(contents, "Creator:  " + invoice.getCreator(), 12, (int) (w / 2) + 5, line_pos);
        //contents.endText();
        //PDImageXObject logo = PDImageXObject.createFromFile(logo_path, document);
        //contents.drawImage(logo, (int)(w/2)+ 20, line_pos-logo_h, 150, logo_h);

        line_pos -= LINE_HEIGHT;

        //horizontal solid line after Trip details
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos - LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos - LINE_HEIGHT / 2);
        contents.stroke();
        contents.beginText();

        //horizontal solid line after company details
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos - LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos - LINE_HEIGHT / 2);
        contents.stroke();
        contents.beginText();

        line_pos -= LINE_HEIGHT;//next line

        temp_pos = line_pos;
        //left text
        addTextToPageStream(contents, "Contact Person:  " + contact
                .getName(), PDType1Font.HELVETICA_BOLD, 12, 20, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "Tel    :  " + contact
                .getTel(), PDType1Font.HELVETICA_BOLD, 12, 120, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "Cell   :  " + contact
                .getCell(), PDType1Font.HELVETICA_BOLD, 12, 120, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "eMail :  " + contact
                .getEmail(), PDType1Font.HELVETICA_BOLD, 12, 120, line_pos);

        //horizontal solid line
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos - LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos - LINE_HEIGHT / 2);
        contents.stroke();
        contents.beginText();

        line_pos -= LINE_HEIGHT;//next line (for external consultants)
        //temp_pos-=LINE_HEIGHT;//next line (for internal consultants)
        //Render sale representatives
        int int_rep_count = 0;
        if(invoice.getTrip().getQuote().getCreatorUser()!=null)
        {
            //if the page can't hold 4 more lines add a new page
            if (line_pos - (4 * LINE_HEIGHT) < h - logo_h - (ROW_COUNT * LINE_HEIGHT) || temp_pos - (4 * LINE_HEIGHT) < h - logo_h - (ROW_COUNT * LINE_HEIGHT))
            {
                addTextToPageStream(contents, "Page " + quote_page_count, PDType1Font.HELVETICA_OBLIQUE, 14, (int) (w / 2) - 20, 50);
                //add new page
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                //TODO: setup page, i.e. draw lines and stuff
                contents.close();
                contents = new PDPageContentStream(document, page);
                temp_pos = (int) h - logo_h;
                line_pos = (int) h - logo_h;

                createLinesAndBordersOnPage(contents, (int) w, line_pos, line_pos + LINE_HEIGHT / 2);

                contents.beginText();
                quote_page_count++;
            }


            addTextToPageStream(contents, "Created By:  " + invoice.getTrip().getQuote().getCreatorUser().getFirstname()
                    +" "+invoice.getTrip().getQuote().getCreatorUser().getLastname(), PDType1Font.HELVETICA_BOLD, 12, (int) (w / 2) + 5, temp_pos);
            temp_pos -= LINE_HEIGHT;//next line
            addTextToPageStream(contents, "Tel    :  " + invoice.getTrip().getQuote().getCreatorUser()
                    .getTel(), PDType1Font.HELVETICA_BOLD, 12, (int) (w / 2) + 105, temp_pos);
            temp_pos -= LINE_HEIGHT;//next line
            addTextToPageStream(contents, "Cell   :  " + invoice.getTrip().getQuote().getCreatorUser()
                    .getCell(), PDType1Font.HELVETICA_BOLD, 12, (int) (w / 2) + 105, temp_pos);
            temp_pos -= LINE_HEIGHT;//next line
            addTextToPageStream(contents, "eMail :  " + invoice.getTrip().getQuote().getCreatorUser()
                    .getEmail(), PDType1Font.HELVETICA_BOLD, 12, (int) (w / 2) + 105, temp_pos);
            temp_pos -= LINE_HEIGHT;//next line
        }
        //set the cursor to the line after the sale/client rep info
        line_pos = line_pos < temp_pos ? line_pos : temp_pos;
        addTextToPageStream(contents, "Request: " + quote.getRequest(), PDType1Font.HELVETICA, 13, 20, line_pos);
        line_pos -= LINE_HEIGHT;//next line

        contents.endText();

        //erase middle line by request field
        /*contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos+LINE_HEIGHT/2);
        contents.lineTo(w-10, line_pos+LINE_HEIGHT/2);
        contents.stroke();*/

        //horizontal solid line after reps
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos + LINE_HEIGHT + LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos + LINE_HEIGHT + LINE_HEIGHT / 2);
        contents.stroke();
        //horizontal solid line after request
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos + LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos + LINE_HEIGHT / 2);
        contents.stroke();
        //solid horizontal line after site location, before quote_items
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, (line_pos - LINE_HEIGHT + (int) Math.ceil(LINE_HEIGHT / 2)));
        contents.lineTo(w - 10, (line_pos - LINE_HEIGHT + (int) Math.ceil(LINE_HEIGHT / 2)));
        contents.stroke();

        int col_divider_start = line_pos - LINE_HEIGHT;

        //vertical line going through center of page
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo((w / 2), center_vert_line_start + LINE_HEIGHT / 2);
        contents.lineTo((w / 2), (col_divider_start + LINE_HEIGHT * 2 + (int) Math.ceil(LINE_HEIGHT / 2)));
        contents.stroke();
        //
        contents.moveTo((w / 2), (col_divider_start + (int) Math.ceil(LINE_HEIGHT / 2)));
        contents.lineTo((w / 2), (col_divider_start - LINE_HEIGHT + (int) Math.ceil(LINE_HEIGHT / 2)));
        contents.stroke();

        contents.beginText();
        addTextToPageStream(contents, "Pickup Address: " + quote.getEnquiry().getPickup_location(), PDType1Font.HELVETICA, 13, 20, line_pos);
        line_pos -= LINE_HEIGHT;//next line

        addTextToPageStream(contents, "Destination Address: " + quote.getEnquiry().getDestination(), PDType1Font.HELVETICA, 13, 20, line_pos);
        line_pos -= LINE_HEIGHT;//next line

        contents.endText();

        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, (line_pos - LINE_HEIGHT + (int) Math.ceil(LINE_HEIGHT / 2)));
        contents.lineTo(w - 10, (line_pos - LINE_HEIGHT + (int) Math.ceil(LINE_HEIGHT / 2)));
        contents.stroke();
        contents.beginText();

        //Column headings
        int col_pos = 10;
        addTextToPageStream(contents, "Item No.", PDType1Font.COURIER_BOLD, 14, 15, line_pos);
        col_pos += 80;
        addTextToPageStream(contents, "Equipment description", PDType1Font.COURIER_BOLD, 14, col_pos + 20, line_pos);
        col_pos = (int) (w / 2);
        String[] cols = {"Unit", "Qty", "Rate", "Labour", "Total"};
        for (int i = 0; i < 5; i++)//7 cols in total
            addTextToPageStream(contents, cols[i], PDType1Font.COURIER_BOLD, 12, col_pos + (55 * i) + 10, line_pos);
        line_pos -= LINE_HEIGHT;//next line

        //Actual quote information
        col_pos = 10;
        double sub_total = 0;
        if (quote.getResources() != null)
        {
            for (QuoteItem item : quote.getResources())
            {
                contents.endText();
                //quote content column dividers
                //#1
                contents.moveTo(80, (col_divider_start + (int) Math.ceil(LINE_HEIGHT / 2)));
                contents.lineTo(80, line_pos - LINE_HEIGHT / 2);
                contents.stroke();
                //vertical line going through center of page
                contents.setStrokingColor(Color.BLACK);
                contents.moveTo((w / 2), (col_divider_start - LINE_HEIGHT + (int) Math.ceil(LINE_HEIGHT / 2)));
                contents.lineTo((w / 2), line_pos - LINE_HEIGHT / 2);
                contents.stroke();
                //#3+
                for (int i = 1; i < 5; i++)//7 cols in total
                {
                    contents.moveTo((w / 2) + 55 * i, (col_divider_start + (int) Math.ceil(LINE_HEIGHT / 2)));
                    contents.lineTo((w / 2) + 55 * i, line_pos - LINE_HEIGHT / 2);
                    contents.stroke();
                }
                contents.beginText();

                //if the page can't hold another 4 lines[current item, blank, sub-total, vat] add a new page
                if (line_pos - LINE_HEIGHT < h - logo_h - (ROW_COUNT * LINE_HEIGHT))
                {
                    addTextToPageStream(contents, "Page " + quote_page_count, PDType1Font.COURIER_OBLIQUE, 14, (int) (w / 2) - 20, 30);
                    //add new page
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    //TODO: setup page, i.e. draw lines and stuff
                    contents.close();
                    contents = new PDPageContentStream(document, page);
                    contents.beginText();
                    line_pos = (int) h - logo_h;
                    col_divider_start = line_pos + LINE_HEIGHT;
                    createLinesAndBordersOnPage(contents, (int) w, line_pos + LINE_HEIGHT / 2, bottom_line);
                    quote_page_count++;
                }

                col_pos = 0;//first column
                //Item col
                addTextToPageStream(contents, item.getItem_number(), 12, col_pos + 30, line_pos);
                col_pos += 80;//next column
                //Description col
                addTextToPageStream(contents, item.getResource().getResource_name(), 12, col_pos + 5, line_pos);
                col_pos = (int) w / 2;//next column - starts at middle of page
                //Unit col
                addTextToPageStream(contents, item.getUnit(), 12, col_pos + 5, line_pos);
                col_pos += 55;//next column
                //Quantity col
                addTextToPageStream(contents, item.getQuantity(), digit_font_size, col_pos + 5, line_pos);
                col_pos += 55;//next column
                //Rate col
                addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance()
                        .format(item.getRate())), digit_font_size, col_pos + 5, line_pos);
                col_pos += 55;//next column
                //Labour col
                //addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance().format(item.getLabourCost())), digit_font_size,col_pos+5, line_pos);
                col_pos += 55;//next column
                //Total col
                sub_total += item.getTotal();
                addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance()
                        .format(item.getTotal())), digit_font_size, col_pos + 5, line_pos);

                line_pos -= LINE_HEIGHT;//next line
                }
                IO.log(PDF.class.getName(), IO.TAG_INFO, "successfully created quote PDF.");
            } else IO.log(PDF.class.getName(), IO.TAG_INFO, "quote has no resources.");
        col_pos = 0;
        //line_pos -= LINE_HEIGHT;//skip another line
        /*if the page can't hold another 2 lines add a new page
        if(line_pos-LINE_HEIGHT*2<h-logo_h-(ROW_COUNT*LINE_HEIGHT) || temp_pos-LINE_HEIGHT*2<h-logo_h-(ROW_COUNT*LINE_HEIGHT))
        {
            //add new page
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            //TODO: setup page, i.e. draw lines and stuff
            contents.close();
            contents = new PDPageContentStream(document, page);
            contents.beginText();
            line_pos = (int)h-logo_h;
            col_divider_start = line_pos+LINE_HEIGHT;
        }*/
        //solid horizontal line
        int col_divider_end = line_pos;

        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos + LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos + LINE_HEIGHT / 2);
        contents.stroke();

        contents.beginText();
        addTextToPageStream(contents, "Sub-Total Excl. VAT: ", PDType1Font.COURIER_BOLD_OBLIQUE, 14, col_pos + 30, line_pos);
        addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance()
                .format(sub_total)), PDType1Font.COURIER_BOLD_OBLIQUE, 14, (int) (5 + (w / 2)), line_pos);
        line_pos -= LINE_HEIGHT;//next line

        //solid horizontal line
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos + LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos + LINE_HEIGHT / 2);
        contents.stroke();

        double vat = sub_total * (quote.getVat() / 100);
        contents.beginText();
        addTextToPageStream(contents, "VAT[" + quote
                .getVat() + "%]: ", PDType1Font.COURIER_BOLD_OBLIQUE, 14, col_pos + 30, line_pos);
        addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance()
                .format(vat)), PDType1Font.COURIER_BOLD_OBLIQUE, 14, (int) (5 + (w / 2)), line_pos);
        line_pos -= LINE_HEIGHT;//next line

        //solid horizontal line
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos + LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos + LINE_HEIGHT / 2);
        contents.stroke();

        contents.beginText();
        addTextToPageStream(contents, "Total Incl. VAT: ", PDType1Font.COURIER_BOLD_OBLIQUE, 14, col_pos + 30, line_pos);
        addTextToPageStream(contents, String.valueOf(DecimalFormat.getCurrencyInstance()
                .format(sub_total + vat)), PDType1Font.COURIER_BOLD_OBLIQUE, 14, (int) (5 + (w / 2)), line_pos);
        contents.endText();
        line_pos -= LINE_HEIGHT;//next line

        //solid horizontal line
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos + LINE_HEIGHT / 2);
        contents.lineTo(w - 10, line_pos + LINE_HEIGHT / 2);
        contents.stroke();

        //int col_divider_end = line_pos;
        line_pos -= LINE_HEIGHT * 3;//next 3rd line
        /*solid horizontal lines after quote_items
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, col_divider_end+LINE_HEIGHT+LINE_HEIGHT/2);
        contents.lineTo(w-10, col_divider_end+LINE_HEIGHT+LINE_HEIGHT/2);
        contents.stroke();
        contents.moveTo(10, col_divider_end+LINE_HEIGHT/2);
        contents.lineTo(w-10, col_divider_end+LINE_HEIGHT/2);
        contents.stroke();
        contents.moveTo(10, col_divider_end-LINE_HEIGHT+LINE_HEIGHT/2);
        contents.lineTo(w-10, col_divider_end-LINE_HEIGHT+LINE_HEIGHT/2);
        contents.stroke();*/

        //quote content column dividers
        //#1
        contents.moveTo(80, (col_divider_start - LINE_HEIGHT + (int) Math.ceil(LINE_HEIGHT / 2)));
        contents.lineTo(80, col_divider_end + LINE_HEIGHT + LINE_HEIGHT / 2);
        contents.stroke();
        //vertical line going through center of page again
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo((w / 2), (col_divider_start - LINE_HEIGHT + (int) Math.ceil(LINE_HEIGHT / 2)));
        contents.lineTo((w / 2), col_divider_end + LINE_HEIGHT + LINE_HEIGHT / 2);
        //contents.lineTo((w/2),col_divider_end-LINE_HEIGHT/2);
        contents.stroke();
        //#3+
        for (int i = 1; i < 5; i++)//7 cols in total
        {
            contents.moveTo((w / 2) + 55 * i, (col_divider_start - LINE_HEIGHT + (int) Math.ceil(LINE_HEIGHT / 2)));
            contents.lineTo((w / 2) + 55 * i, col_divider_end + LINE_HEIGHT + LINE_HEIGHT / 2);
            contents.stroke();
        }

        contents.beginText();

        if (quote.getOther() != null)
            addTextToPageStream(contents, "P.S. " + quote
                    .getOther(), PDType1Font.TIMES_ITALIC, 14, col_pos + 5, line_pos);

        line_pos -= LINE_HEIGHT;//next line
        //if the page can't hold another 9 lines add a new page
        if (line_pos - (LINE_HEIGHT * 4) < h - logo_h - (ROW_COUNT * LINE_HEIGHT))
        {
            addTextToPageStream(contents, "Page " + quote_page_count, PDType1Font.COURIER_OBLIQUE, 14, (int) (w / 2) - 20, 30);
            //add new page
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contents.close();
            contents = new PDPageContentStream(document, page);
            contents.beginText();
            line_pos = (int) h - logo_h;
            createLinesAndBordersOnPage(contents, (int) w, line_pos + LINE_HEIGHT / 2, bottom_line);
            quote_page_count++;
        }
        addTextToPageStream(contents, "TERMS AND CONDITIONS OF SALE", PDType1Font.HELVETICA_BOLD, 14, (int) (w / 2) - 130, line_pos);
        contents.endText();
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo((int) (w / 2) - 140, line_pos - LINE_HEIGHT / 2);
        contents.lineTo((w / 2) + 120, line_pos - LINE_HEIGHT / 2);
        contents.stroke();
        contents.beginText();

        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*Validity: Quote valid for 24 Hours.", PDType1Font.HELVETICA, 12, col_pos + 30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*Payment Terms: COD / 30 Days on approved accounts. ", PDType1Font.HELVETICA, 12, col_pos + 30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*Delivery: 1 - 6 Weeks, subject to stock availability.", PDType1Font.HELVETICA, 12, col_pos + 30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*All pricing quoted, is subject to Rate of Exchange USD=R.", PDType1Font.HELVETICA_BOLD, 12, col_pos + 30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*All goods / equipment remain the property of " + Globals.COMPANY
                .getValue() + " until paid for completely. ", PDType1Font.HELVETICA, 12, col_pos + 30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*" + Globals.COMPANY
                .getValue() + " reserves the right to retake posession of all equipment not paid for completely", PDType1Font.HELVETICA, 12, col_pos + 30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "  Within the payment term set out above.", PDType1Font.HELVETICA, 12, col_pos + 30, line_pos);
        line_pos -= LINE_HEIGHT;//next line
        addTextToPageStream(contents, "*E & O E", PDType1Font.HELVETICA, 12, col_pos + 30, line_pos);

        addTextToPageStream(contents, "Page " + quote_page_count, PDType1Font.COURIER_OBLIQUE, 14, (int) (w / 2) - 20, 30);
        contents.endText();
        contents.close();

        //create PDF output directory
        if(new File("out/pdf/").mkdirs())
            IO.log(PDF.class.getName(), "successfully created PDF output directory [out/pdf/]", IO.TAG_INFO);

        String path = "out/pdf/invoice_" + Trip.get_id() + ".pdf";
        int i=1;
        while(new File(path).exists())
        {
            path = "out/pdf/invoice_" + Trip.get_id() + "." + i + ".pdf";
            i++;
        }

        //if(contents!=null)
        //    contents.close();

        document.save(path);
        document.close();

        return path;
    }
}