
package com.jdev.jsprite;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 *
 * @author jdeverna
 */
public class SpriteMaker {

    public static final Pattern extensionPattern = Pattern.compile("(.*)\\.\\w{3,4}");
    public static final DecimalFormat numberFormat = new DecimalFormat("#.##");
    
    private final SpriteRequest request;

    private Map<String,ImageFile> images = new HashMap();
    private StringBuilder css;
    private StringBuilder html;


    public SpriteMaker(SpriteRequest request){
        this.request = request;
    }

    public void processRequest(){

        if(request.getFileList() == null || request.getFileList().length <= 0){
            System.err.println("No images found to sprite!");
            System.exit(2);
        }

       if(request.isCreateHtml()){
            html = new StringBuilder( beginHtml( getFileName( request.getOutputFile() ) ) );
       }

       //if we're inlining, we need to have css since we're not spriting
       if(request.isCreateCss() || request.isInlineImage()){
           css = new StringBuilder();
       }

       if(request.isInlineImage()){
        inline();
       }else if(request.isNormal()){
        normal();
       }else{
        combine();
       }
    }

    /**
     *
     */
    public void inline(){
        File[] files = request.getFileList();
        int oversized = 0;
        for(int i = 0; i < files.length; i++){
            try{
                String className = getUniqueClassName(request, files[i].getName(), images);
                String encoded = base64Encode(files[i]);

                if(encoded.getBytes().length > 1024){
                   oversized++;
                }

                css.append(".")
                   .append(className)
                   .append(" {background-image: url(data:image/png;base64,")
                   .append( encoded )
                   .append("); background-repeat: no-repeat; ")
                   .append( request.getExtraCss() )
                   .append("}\n");

                if(request.isCreateHtml()){
                    html.append(createSampleDiv(className));
                }
            }catch(Exception e){

            }
        }

        if(request.isCreateHtml()){
            html.append( endHtml() );
            writeOutputFile(html.toString(), request.getOutputFile()+".html");
        }

         writeOutputFile(css.toString(),
                        (request.getAppendTo() != null ? request.getAppendTo() : request.getOutputFile() + ".css"),
                        (request.getAppendTo() != null ? true : false)
                       );
        
        if(oversized > 0){
            System.out.println("Images where base64 is more than 1024: " + oversized);
        }
    }

    /**
     *
     */
    public void combine() {
        GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();

        StringBuilder imgNames = new StringBuilder();

        int totalHeight = 0, maxWidth = 0, failed = 0, passed = 0;
        long totalOrigFileSize = 0;

        File[] files = request.getFileList();

        boolean first = true;
        for(int i = 0; i < files.length; i++){
            try{
                String className = getUniqueClassName(request, files[i].getName(), images);
                ImageFile image = new ImageFile(className, files[i]);
                images.put(className, image);

                //append the css and html info for this image
                if (!first) {
                    imgNames.append("," + ((i+1)%5 == 0 ? "\n" : ""));
                }
                imgNames.append(".")
                        .append(className);
                
                if(request.isCreateHtml()){
                    html.append(createSampleDiv(className));
                }

                totalOrigFileSize += image.getFileSize();
                totalHeight += image.getHeight() + request.getSpritePadding();
                maxWidth = image.getWidth() > maxWidth ? image.getWidth() : maxWidth;

                first = false;
            }catch(Exception e){
                System.out.println(" * failed to sprite: " + files[i].getName() + ": " + e.getMessage());
                failed++;
            }
        }

        // grab the full path to the image
        String imgURL = request.getImageURL();

        // if the url is empty and a prefix was specified
        // use the prefix and make sure it ends with a "/"
        // and add the output file name
        if(imgURL == null && request.getImagePrefix() != null){
            imgURL = request.getImagePrefix() + (request.getImagePrefix().endsWith("/") ? "" : "/") + getFileName(request.getOutputFile());
        }

        // if the url is still null, make it just the output filename
        if(imgURL == null){
            imgURL = getFileName(request.getOutputFile());
        }


        imgNames.append(" {background-image: url(")
                .append( imgURL )
                .append("); background-repeat: no-repeat; ")
                .append( request.getExtraCss() )
                .append("}\n");


        BufferedImage output = gc.createCompatibleImage(maxWidth, totalHeight, Transparency.TRANSLUCENT);
        Graphics2D grp = output.createGraphics();

        RenderingHints rh = new RenderingHints(
		RenderingHints.KEY_RENDERING,
		RenderingHints.VALUE_RENDER_QUALITY);

        grp.setRenderingHints(rh);

        try{
            Object[] key = images.keySet().toArray();
            Arrays.sort(key);

            totalHeight = 0;
            for(int j = 0; j < key.length; j++){
                ImageFile imf = (ImageFile)images.get( (String)key[j] );
                String fileName = imf.getName();

                grp.drawImage(imf.getImage(), null, 0, totalHeight);

                if(request.isCreateCss()){
                    css.append( writeCss(fileName, totalHeight, imf.getWidth(), imf.getHeight(), request.isUseImportantFlag()) );
                }else{
                    System.out.println("NOT creating CSS");
                }

                totalHeight += imf.getHeight() + request.getSpritePadding();
                passed++;
            }

            File outfile = new File( request.getOutputFile());
            ImageIO.write(output, request.getOutputType(), outfile);

            double kborig = Double.valueOf( numberFormat.format( totalOrigFileSize / 1000.0) );
            double newsize = Double.valueOf( numberFormat.format( outfile.length() / 1000.0) );
            double savingsamt = Double.valueOf( numberFormat.format(kborig - newsize) );
            double savings = Double.valueOf( numberFormat.format( ((kborig - newsize)/kborig)*100 ) );

            System.out.println("\nSuccessfully sprited " + passed + " images\nFailed to sprite " + failed + " images.");
            System.out.println("Total file size before: " + kborig + "KB");
            System.out.println("Total file size after: " + newsize + "KB");
            System.out.println("Sprite savings: " + savingsamt + "KB, " + savings + "%" );

            if( request.isCreateCss() ){
                writeOutputFile(imgNames.append("\n").append(css).toString(),
                                (request.getAppendTo() != null ? request.getAppendTo() : request.getOutputFile() + ".css"),
                                (request.getAppendTo() != null ? true : false)
                               );
            }

            if(request.isCreateHtml()){
                html.append("<div style=\"clear:both;\"><br/><br/>Successfully sprited ")
                    .append(passed)
                    .append(" images.<br/>Failed to sprite ")
                    .append(failed)
                    .append(" images.<br/><br/>")
                    .append("Total file size before: " + kborig + "KB<br/>")
                    .append("Total file size after: " + newsize + "KB<br/>")
                    .append("Sprite savings: " + savingsamt + "KB, " + savings + "%</div>" )
                    .append( endHtml() );

                writeOutputFile(html.toString(), request.getOutputFile()+".html");
            }

        }catch(Exception e){
            failed++;
        }
    }

    public void normal(){
        File[] files = request.getFileList();

        for(int i = 0; i < files.length; i++){
            try{
                String className = getUniqueClassName(request, files[i].getName(), images);

                css.append(".")
                   .append(className)
                   .append(" {background-image: url(../images/")
                   .append( files[i].getName() )
                   .append("); background-repeat: no-repeat; ")
                   .append( request.getExtraCss() )
                   .append("}\n");

                if(request.isCreateHtml()){
                    html.append(createSampleDiv(className));
                }
            }catch(Exception e){

            }
        }

        if(request.isCreateHtml()){
            html.append( endHtml() );
            writeOutputFile(html.toString(), request.getOutputFile()+".html");
        }

         writeOutputFile(css.toString(),
                        (request.getAppendTo() != null ? request.getAppendTo() : request.getOutputFile() + ".css"),
                        (request.getAppendTo() != null ? true : false)
                       );
    }

    private String getUniqueClassName(SpriteRequest request, String fileName, Map allImages){
        String newname = extensionPattern.matcher(fileName).replaceAll("$1").replaceAll(" ", request.getSeparator());

        newname = (request.getPrefix() != null ? request.getPrefix() + request.getSeparator() + newname : newname);
        newname = (request.getPostfix() != null ? newname + request.getSeparator() + request.getPostfix() : newname);

        int cnt = 1; String orig = newname;
        while( allImages.containsKey(newname) ){
            newname = orig + request.getSeparator() + (++cnt);
        }

        return newname;
    }

    private String getFileName(String fullPath){
        String name = fullPath;

        if(fullPath.contains( File.pathSeparator)){
            name = fullPath.substring( fullPath.lastIndexOf( File.pathSeparator ) + 1 );
        }

        if(name.contains( File.separator )){
            name = fullPath.substring( fullPath.lastIndexOf( File.separator ) + 1 );
        }

        return name;
    }

    private String writeCss(String fileName, int totalHeight, int width, int height, boolean useImportantFlag){
        StringBuffer buff = new StringBuffer();

        buff.append(".").append( fileName ).append(" {")
            .append("background-position: 0px -").append(totalHeight).append("px");

        if(useImportantFlag){
            buff.append(" !important");
        }

        buff.append("; ")
            .append("height:").append(height).append("px; ")
            .append("width:").append(width).append("px; ")
            .append("}\n");

        return buff.toString();
    }

    private String createSampleDiv(String name){
        return "<span class=\""+name+"\" alt=\""+name+"\" title=\""+name+"\"></span>\n";
    }

    private String beginHtml(String target){
        return "<html>\n<head>\n<link rel=\"stylesheet\" type=\"text/css\" href=\""+target+".css\"/>\n" +
                "<style>span {display:block; float: left; margin: 5px;" +
                (request.isNormal() || request.isInlineImage() ? "width: 16px;height:16px;" : "") +
                "}</style>\n" +
                "</head>\n<body><a href=\""+target+"\">view sprite</a><br/><br/>\n";
    }

    private String endHtml(){
        return "</body>\n</html>";
    }

    private void writeOutputFile(String output, String fileName){
        writeOutputFile(output, fileName, false);
    }

    private void writeOutputFile(String output, String fileName, boolean isAppend){
        try{
            FileWriter fw = new FileWriter( fileName, isAppend );
            if(isAppend){
                fw.write("\n\n");
            }

            fw.write(output);
            fw.flush();
            fw.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    private String base64Encode(File file){

        try{
            
            byte[] b = getBytesFromFile(file);
            
            return Base64.encode(b);
            
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }


    private static byte[] getBytesFromFile(File file) throws IOException {

        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        /*
         * You cannot create an array using a long type. It needs to be an int
         * type. Before converting to an int type, check to ensure that file is
         * not loarger than Integer.MAX_VALUE;
         */
        if (length > Integer.MAX_VALUE) {
            System.out.println("File is too large to process");
            return null;
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while ( (offset < bytes.length)
                &&
                ( (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) ) {

            offset += numRead;

        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        is.close();
        return bytes;

    }

}