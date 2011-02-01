
package com.jdev.jsprite;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 *
 * @author jdeverna
 */
public class SpriteRequest {

    private static final Pattern validFilePattern = Pattern.compile("^.*\\.(png|gif|jpg|jpeg|bmp)$", Pattern.CASE_INSENSITIVE);
    
    private static final FilenameFilter validFileFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            if(validFilePattern.matcher(name).matches()){
                return true;
            }
            return false;
        }
    };

    private DirectoryFilter directoryFilter = new DirectoryFilter();

    private boolean recurse;
    private boolean hidden;
    private File[] fileList;

    private String outputFile;
    private String outputType;

    private boolean createCss;
    private boolean createHtml;

    private long totalOrigFileSize = 0;
    private Integer spritePadding = 0;

    private String appendTo;

    private String prefix;
    private String postfix;
    private String separator;
    private String extraCss;
    private boolean inlineImage;
    private String imagePrefix;
    private String imageURL;
    private boolean useImportantFlag;

    private boolean normal;

    public boolean isNormal() {
        return normal;
    }

    public void setNormal(boolean normal) {
        this.normal = normal;
    }

    public SpriteRequest(){}

    public File[] getFileList(){
        return this.fileList;
    }

    public void setFileList(File[] fileList) {
        this.fileList = fileList;
    }

    public boolean isRecurse() {
        return recurse;
    }

    public void setRecurse(Boolean recurse) {
        this.recurse = (recurse == null ? Boolean.FALSE : Boolean.TRUE);
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = (hidden == null ? Boolean.TRUE : Boolean.FALSE);
        directoryFilter.setRecurseHidden(this.hidden);
    }

    public String getAppendTo() {
        return appendTo;
    }

    public void setAppendTo(String appendTo) {
        this.appendTo = appendTo;
    }

    public String getPostfix() {
        return postfix;
    }

    public void setPostfix(String postfix) {
        this.postfix = postfix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getExtraCss() {
        return extraCss;
    }

    public void setExtraCss(String extraCss) {
        this.extraCss = extraCss;
    }

    public boolean isInlineImage(){
        return this.inlineImage;
    }

    public void useInlineImage(boolean useInlineImage){
        this.inlineImage = useInlineImage;
    }

    public Integer getSpritePadding() {
        return spritePadding;
    }

    public void setSpritePadding(Integer spritePadding) {
        this.spritePadding = spritePadding;
    }

    public boolean isCreateCss() {
        return createCss;
    }

    public void setCreateCss(Boolean createCss) {
        this.createCss = (createCss == null ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isCreateHtml() {
        return createHtml;
    }

    public void setCreateHtml(Boolean createHtml) {
        this.createHtml = (createHtml == null ? Boolean.FALSE : Boolean.TRUE);
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public long getTotalOrigFileSize() {
        return totalOrigFileSize;
    }

    public void setTotalOrigFileSize(long totalOrigFileSize) {
        this.totalOrigFileSize = totalOrigFileSize;
    }

    public String getImageURL(){
        return this.imageURL;
    }

    public void setImageURL(String imageURL){
        this.imageURL = imageURL;
    }

    public String getImagePrefix(){
        return this.imagePrefix;
    }

    public boolean isUseImportantFlag() {
        return this.useImportantFlag;
    }

    public void setUseImportantFlag(Boolean useImportantFlag){
        this.useImportantFlag = (useImportantFlag == null ? Boolean.TRUE : Boolean.FALSE);
    }

    public void setImagePrefix(String imagePrefix){
        this.imagePrefix = imagePrefix;
    }

    public void setFilesByList(String files){
        String[] list = files.split(",");

        File[] flist = new File[list.length];

        for(int i = 0; i < list.length; i++){
            flist[i] = new File( list[i].trim() );
        }

        this.fileList = flist;
    }

    public void setFilesByDirectory(String directory){
        File dir = new File(directory);
        this.fileList = dir.listFiles(validFileFilter);

        //if we want to recurse, check subfolders for more documents
        if(this.recurse){
            this.recurseDirectories(dir.listFiles(directoryFilter));
        }
    }

    public void setFilesByRegex(String directory, String regex){
        File dir = new File(directory);

        FilenameFilter filter = new CustomFilenameFilter(regex);

        this.fileList = dir.listFiles(filter);

        //if we want to recurse, check subfolders for more documents
        if(this.recurse){
            this.recurseDirectories(dir.listFiles(directoryFilter), filter);
        }
    }

    private void recurseDirectories(File[] dirs, FilenameFilter filter){

        for(int i = 0; i < dirs.length; i++){
            this.appendToFileList( dirs[i].listFiles(filter) );

            if(this.recurse){
                this.recurseDirectories(dirs[i].listFiles(directoryFilter), filter);
            }
        }
    }

    private void recurseDirectories(File[] dirs){
        this.recurseDirectories(dirs, validFileFilter);
    }

    private void appendToFileList(File[] newFiles){
        File[] newList = new File[ this.fileList.length + newFiles.length ];
        int i = 0;
        for(i = 0; i < this.fileList.length; i++){
            newList[i] = this.fileList[i];
        }

        for(int j = 0; j < newFiles.length; j++){
            newList[i+j] = newList[j];
        }

        this.fileList = newList;
    }



    private class CustomFilenameFilter implements FilenameFilter {

        private Pattern customPattern;

        public CustomFilenameFilter(Pattern pattern){
            this.customPattern = pattern;
        }

        public CustomFilenameFilter(String regex){
            this.customPattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
        }

        public boolean accept(File dir, String name) {
            
            if(validFilePattern.matcher(name).matches() && this.customPattern.matcher(name).matches()){
                return true;
            }

            return false;
        }
    }

    private class DirectoryFilter implements FileFilter {

        private boolean recurseHidden = false;

        public boolean accept(File pathname) {
            return pathname.isDirectory() && (!recurseHidden || (recurseHidden && !pathname.isHidden()) );
        }

        public boolean isRecurseHidden() {
            return recurseHidden;
        }

        public void setRecurseHidden(boolean recurseHidden) {
            this.recurseHidden = recurseHidden;
        }
    }
}