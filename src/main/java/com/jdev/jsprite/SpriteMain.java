
package com.jdev.jsprite;

import java.io.File;
import java.io.IOException;

import net.sf.jargs.CmdLineParser;

/**
 * @author jdeverna
 */
public class SpriteMain {

    private static void printUsage() {
        StringBuilder sb = new StringBuilder("\nSprite Builder Usage:\n\n");

        sb.append("-u, --usage     : this list\n")

                .append("\nDirectory Options (Either the -d or -l flag must be passed)\n")
                .append("-l, --files     : comma separated list of files to sprite\n")
                .append("-d, --dir       : directory containing images to sprite\n")
                .append("-g, --regex     : regular expression used to filter file names (used with -d flag only)\n")
                .append("-r, --recurse   : if flag is present, the program will look in subdirectories for other image files\n")
                .append("-n, --hidden    : include hidden subdirectories (used with -r flag only)\n")

                .append("\nSprite Options\n")
                .append("-o, --output    : [sprite.png] the output file\n")
                .append("-f, --format    : [png] the output format (e.g, png, jpeg, etc.)\n")
                .append("-p, --padding   : [0] the number of pixels to skip between images\n")

                .append("\nCss Options\n")
                .append("-c, --css       : if flag is present, CSS will NOT be print\n")
                .append("-e, --prefix    : an optional prefix for the css class name\n")
                .append("-t, --postfix   : an optional postfix for the css class name\n")
                .append("-s, --separator : [\"-\"] the character used to separate prefix/postfix from classname \n")
                .append("-a, --appendTo  : if specified, will append the css styles to this file, instead of creating a new one\n")
                .append("-x, --extra     : extra CSS style(s) to be added (should be a quoted string or valid CSS styles)\n")
                .append("-i, --inline    : use data URI scheme (as defined in RFC 2397) for inline images, instead of normal urls\n")
                .append("-P, --imgPrefix : an optional URL for the CSS background attribute that will prefix the output name (from the -o option)\n")
                .append("-U, --imgUrl    : an optional URL for the CSS background attribute (the -o and -x flags will be ignored if this is specified)\n")
                .append("-I, --important : if flag is present, \"!important\" flag will NOT be used for the background-position property\n")

                .append("\nHTML Options\n")
                .append("-h, --html      : generate a sample html file for the sprite\n");

        System.err.println(sb.toString());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args[0].equals("test")) {
            runTest();
            return;
        }

        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option recurse = parser.addBooleanOption('r', "recurse");
        CmdLineParser.Option directory = parser.addStringOption('d', "dir");
        CmdLineParser.Option fileList = parser.addStringOption('l', "files");
        CmdLineParser.Option regex = parser.addStringOption('g', "regex");
        CmdLineParser.Option hidden = parser.addBooleanOption('n', "hidden");

        CmdLineParser.Option output = parser.addStringOption('o', "output");
        CmdLineParser.Option format = parser.addStringOption('f', "format");
        CmdLineParser.Option padding = parser.addIntegerOption('p', "padding");

        CmdLineParser.Option css = parser.addBooleanOption('c', "css");
        CmdLineParser.Option prefix = parser.addStringOption('e', "prefix");
        CmdLineParser.Option postfix = parser.addStringOption('t', "postfix");
        CmdLineParser.Option append = parser.addStringOption('a', "appendTo");
        CmdLineParser.Option separator = parser.addStringOption('s', "separator");
        CmdLineParser.Option extra = parser.addStringOption('x', "extra");
        CmdLineParser.Option inline = parser.addBooleanOption('i', "inline");
        CmdLineParser.Option imgPrefix = parser.addStringOption('P', "imgPrefix");
        CmdLineParser.Option imgURL = parser.addStringOption('U', "imgUrl");
        CmdLineParser.Option important = parser.addBooleanOption('I', "important");

        CmdLineParser.Option html = parser.addBooleanOption('h', "html");

        CmdLineParser.Option usage = parser.addBooleanOption('u', "usage");


        try {
            parser.parse(args);

            if ((Boolean) parser.getOptionValue(usage) != null) {
                printUsage();
                System.exit(2);
            }

            SpriteRequest request = new SpriteRequest();
            request.setRecurse((Boolean) parser.getOptionValue(recurse));
            request.setHidden((Boolean) parser.getOptionValue(hidden));

            String dir = (String) parser.getOptionValue(directory);
            String fls = (String) parser.getOptionValue(fileList);
            String rgx = (String) parser.getOptionValue(regex);

            if (dir != null) {
                if (rgx != null) {
                    request.setFilesByRegex(dir, rgx);
                } else {
                    request.setFilesByDirectory(dir);
                }

            } else if (fls != null) {
                request.setFilesByList(fls);
            } else {
                throw new Exception("Either a directory or file list must be specified");
            }

            request.setOutputType((String) parser.getOptionValue(format, "PNG"));
            request.setOutputFile((String) parser.getOptionValue(output, "sprite.png"));
            request.setSpritePadding((Integer) parser.getOptionValue(padding, 0));
            request.setCreateCss((Boolean) parser.getOptionValue(css));
            request.setCreateHtml((Boolean) parser.getOptionValue(html));

            request.setPrefix((String) parser.getOptionValue(prefix));
            request.setPostfix((String) parser.getOptionValue(postfix));
            request.setAppendTo((String) parser.getOptionValue(append));
            request.setSeparator((String) parser.getOptionValue(separator, "-"));
            request.setExtraCss((String) parser.getOptionValue(extra, ""));
            request.useInlineImage((Boolean) parser.getOptionValue(inline, false));
            request.setImagePrefix((String) parser.getOptionValue(imgPrefix, null));
            request.setImageURL((String) parser.getOptionValue(imgURL, null));
            request.setUseImportantFlag((Boolean) parser.getOptionValue(important));

            SpriteMaker maker = new SpriteMaker(request);
            maker.processRequest();
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }


    public static void runTest() {
        String[] folders = new String[]{"1", "5", "10", "20", "50", "80", "100", "200", "400", "600", "800", "1000"};

        for (int i = 0; i < folders.length; i++) {
            String folder = folders[i];
            System.out.println("\nFOLDER: " + folder);

            SpriteRequest request = new SpriteRequest();
            request.setRecurse(false);
            request.setHidden(false);
            request.setFilesByDirectory(folder);

            request.setOutputType("PNG");
            request.setOutputFile("output" + File.separator + "sprite_" + folder + ".png");
            request.setSpritePadding(0);
            request.setCreateCss(null);
            request.setCreateHtml(true);

            request.setPrefix("icon");
            request.setPostfix(null);
            request.setAppendTo(null);
            request.setSeparator("_");
            request.setExtraCss("");
            request.useInlineImage(false);
            request.setNormal(false);

            System.out.println("\nSPRITE: " + folder);
            long start = System.currentTimeMillis();
            SpriteMaker makerSprite = new SpriteMaker(request);
            makerSprite.processRequest();
            System.out.println("\nProcess time: " + (System.currentTimeMillis() - start) + "ms");

            System.out.println("CSS size: " + new File(request.getOutputFile() + ".css").length());
            System.out.println("HTML size: " + new File(request.getOutputFile() + ".html").length());
            try {
                System.out.println("Sprite Size: "
                        + new ImageFile("test", new File(request.getOutputFile())).getFileSize());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            request.useInlineImage(true);
            request.setOutputFile("output" + File.separator + "inline_" + folder);
            System.out.println("\nINLINE: " + folder);
            start = System.currentTimeMillis();
            SpriteMaker makerInline = new SpriteMaker(request);
            makerInline.processRequest();
            System.out.println("\nProcess time: " + (System.currentTimeMillis() - start) + "ms");
            System.out.println("CSS size: " + new File(request.getOutputFile() + ".css").length());
            System.out.println("HTML size: " + new File(request.getOutputFile() + ".html").length());


            request.useInlineImage(false);
            request.setNormal(true);
            request.setOutputFile("output" + File.separator + "normal_" + folder);
            System.out.println("\nNORMAL: " + folder);
            start = System.currentTimeMillis();
            SpriteMaker makerNormal = new SpriteMaker(request);
            makerNormal.processRequest();
            System.out.println("\nProcess time: " + (System.currentTimeMillis() - start) + "ms");
            System.out.println("CSS size: " + new File(request.getOutputFile() + ".css").length());
            System.out.println("HTML size: " + new File(request.getOutputFile() + ".html").length());

            System.out.println("\n---------------------------------------------------\n");
        }
    }
}


