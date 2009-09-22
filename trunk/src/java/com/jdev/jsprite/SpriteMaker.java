/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jdev.jsprite;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 *
 * @author U0105442
 */
public class SpriteMaker {

    public static final Pattern extensionPattern = Pattern.compile("(.*)\\.\\w{3,4}");
    public static final DecimalFormat numberFormat = new DecimalFormat("#.##");

    public static void processRequest(SpriteRequest request){
       SpriteMaker.combine(request);
    }

    public static void combine(SpriteRequest request) {
        GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration gc = gs.getDefaultConfiguration();

        StringBuilder css = new StringBuilder();
        StringBuilder imgNames = new StringBuilder();
        StringBuilder html = new StringBuilder( beginHtml( getFileName( request.getOutputFile() ) ) );

        Map<String,ImageFile> images = new HashMap();

        int totalHeight = 0, maxWidth = 0, failed = 0, passed = 0;
        long totalOrigFileSize = 0;

        File[] files = request.getFileList();

        if(files.length <= 0){
            System.err.println("No images found to sprite!");
            System.exit(2);
        }

        for(int i = 0; i < files.length; i++){
            try{
                String className = SpriteMaker.getUniqueClassName(request, files[i].getName(), images);
                ImageFile image = new ImageFile(className, files[i]);
                images.put(className, image);

                //append the css and html info for this image
                imgNames.append(".")
                        .append(className)
                        .append( (i < files.length-1 ? "," + ((i+1)%5 == 0 ? "\n" : "") : "") );
                html.append(createSampleDiv(className));

                totalOrigFileSize += image.getFileSize();
                totalHeight += image.getHeight() + request.getSpritePadding();
                maxWidth = image.getWidth() > maxWidth ? image.getWidth() : maxWidth;

            }catch(Exception e){
                System.out.println("\n*failed to sprite: " + files[i].getName());
                failed++;
            }
        }


        imgNames.append(" {background-image: url(")
                .append( getFileName(request.getOutputFile()) )
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

                css.append( writeCss(fileName, totalHeight, imf.getWidth(), imf.getHeight()) );

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


            html.append("<div style=\"clear:both;\"><br/><br/>Successfully sprited ")
                .append(passed)
                .append(" images.<br/>Failed to sprite ")
                .append(failed)
                .append(" images.<br/><br/>")
                .append("Total file size before: " + kborig + "KB<br/>")
                .append("Total file size after: " + newsize + "KB<br/>")
                .append("Sprite savings: " + savingsamt + "KB, " + savings + "%</div>" )
                .append( endHtml() );

            if( request.isCreateCss() ){
                writeOutputFile(imgNames.append("\n").append(css).toString(), 
                                (request.getAppendTo() != null ? request.getAppendTo() : request.getOutputFile() + ".css"),
                                (request.getAppendTo() != null ? true : false)
                               );
            }

            if( request.isCreateHtml() ){
                writeOutputFile(html.toString(), request.getOutputFile()+".html");
            }

        }catch(Exception e){
            failed++;
        }
    }

    private static String getUniqueClassName(SpriteRequest request, String fileName, Map allImages){
        String newname = extensionPattern.matcher(fileName).replaceAll("$1").replaceAll(" ", request.getSeparator());

        newname = (request.getPrefix() != null ? request.getPrefix() + request.getSeparator() + newname : newname);
        newname = (request.getPostfix() != null ? newname + request.getSeparator() + request.getPostfix() : newname);

        int cnt = 1; String orig = newname;
        while( allImages.containsKey(newname) ){
            newname = orig + request.getSeparator() + (++cnt);
        }

        return newname;
    }

    private static String getFileName(String fullPath){
        String name = fullPath;

        if(fullPath.contains( File.pathSeparator)){
            name = fullPath.substring( fullPath.lastIndexOf( File.pathSeparator ) + 1 );
        }

        if(name.contains( File.separator )){
            name = fullPath.substring( fullPath.lastIndexOf( File.separator ) + 1 );
        }

        return name;
    }

    private static String writeCss(String fileName, int totalHeight, int width, int height){
        StringBuffer buff = new StringBuffer();

        buff.append(".").append( fileName ).append(" {");
        buff.append("background-position: 0px -").append(totalHeight).append("px; ");
        buff.append("height:").append(height).append("px; ");
        buff.append("width:").append(width).append("px; ");
        buff.append("}\n");

        return buff.toString();
    }

    private static String createSampleDiv(String name){
        return "<span class=\""+name+"\" alt=\""+name+"\" title=\""+name+"\"></span>\n";
    }

    private static String beginHtml(String target){
        return "<html>\n<head>\n<link rel=\"stylesheet\" type=\"text/css\" href=\""+target+".css\"/>\n" +
                "<style>span {display:block; float: left; margin: 5px;}</style>\n" +
                "</head>\n<body><a href=\""+target+"\">view sprite</a><br/><br/>\n";
    }

    private static String endHtml(){
        return "</body>\n</html>";
    }

    private static void writeOutputFile(String output, String fileName){
        writeOutputFile(output, fileName, false);
    }

    private static void writeOutputFile(String output, String fileName, boolean isAppend){
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

}