package shared;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created by mathi on 2018-07-11.
 */
public class FileWrapper {
    private String name = null;
    private String path = null;
    private String modified = null;

    public FileWrapper(){}
    public FileWrapper(String path,File file){
        this.name = file.getName();
        this.path = path;
        this.modified = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(file.lastModified());
    }
    public String getName(){
        return name;
    }
    public String getPath(){
        return path;
    }
    public String getModified(){
        return modified;
    }
    public long getLastModified() throws ParseException {
        return new SimpleDateFormat("yyyyMMddHHmmssSSS").parse(modified).getTime();
    }
    public String getCompletePath(){
        if(path != null && name != null){
            return path + name;
        }
        return null;
    }
    public boolean isSamePath(FileWrapper fileWrapper){
        return
            fileWrapper != null &&
            name != null &&
            name.equals(fileWrapper.getName()) &&
            path != null &&
            path.equals(fileWrapper.getPath()
        );
    }
    public static FileWrapper parse(String encoded){
        FileWrapper fileWrapper = null;
        if(encoded != null){
            String arr[] = encoded.split("\t");
            if(arr.length == 3){
                fileWrapper = new FileWrapper();
                fileWrapper.path = arr[0];
                fileWrapper.name = arr[1];
                fileWrapper.modified = arr[2];
                return fileWrapper;
            }
        }
        return fileWrapper;
    }
    public String toString(){
        return path + "\t" + name + "\t" + modified;
    }
}
