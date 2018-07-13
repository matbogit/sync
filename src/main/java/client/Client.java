package client;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import shared.FileWrapper;
import shared.Result;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mathi on 2018-07-08.
 */
public class Client extends Thread{
    Logger LOG = LogManager.getLogger(getClass());

    public static boolean PRODUCTION = false;


    public static String FILES = "files.matbog";
    public static String TMP = "tmp.matbog";

    public static void main(String args[]){
        String folder = "C:\\Users\\mathi\\Matbog";
        String url;
        if(PRODUCTION){
            url = "http://217.61.236.50:8080/";
        } else {
            url = "http://localhost:8080/";
        }
        new File("C:\\Users\\mathi\\matbog2\\files.matbog").delete();
        new File("C:\\Users\\mathi\\matbog\\files.matbog").delete();
        Client c1 = new Client(folder,"documents",url);
        Client c2 = new Client(folder + 2,"documents2",url);
        try {
            c1.join();
            c2.join();
        }catch(Exception e){}
        System.exit(0);
    }
    private String folder;
    private String url;
    private String folderName;
    public Client(String folder,String folderName,String url){
        this.folder = folder;
        this.folderName = folderName;
        this.url = url;
        start();
    }
    public void run(){
        try {
            long start = System.currentTimeMillis();
            FileWrapper startAfter = null;
            BufferedReader tmp = getReader(getFile(TMP));
            while(readNext(tmp) != null){
                startAfter = get(tmp);
            }
            if(tmp != null) {
                tmp.close();
            }
            BufferedReader files = getReader(getFile(FILES));
            BufferedWriter tmpOut;
            if(startAfter != null){
                skipTo(startAfter.getCompletePath(),files);
                tmpOut = getWriter(getFile(TMP),true);
            }else{
                readNext(files);
                tmpOut = getWriter(getFile(TMP),false);
            }
            iterate(new File(folder), folderName + "/", 0, 1,files,tmpOut,startAfter);
            deleteRemaining(files,tmpOut);
            System.out.println("100.00000% done in " + formatTime(System.currentTimeMillis() - start));
            tmpOut.close();
            if(files != null) {
                files.close();
            }
            getFile(FILES).delete();
            getFile(TMP).renameTo(getFile(FILES));
        }catch(Exception e){
            LOG.error(e.getMessage(),e);
        }
    }
    private void iterate(File folder,String path,double percentage,double step,BufferedReader in,BufferedWriter out,FileWrapper startAfter){
        File files[] = folder.listFiles();
        Arrays.sort(files);
        double size = files.length;
        if(path.equals("/")){
            if(Files.exists(getFile(FILES).toPath())) {
                size--;
            }
            if(Files.exists(getFile(TMP).toPath())) {
                size--;
            }
        }
        double thisStep = step / size;
        for(File file : files){
            if(!file.equals(getFile(FILES)) && !file.equals(getFile(TMP))) {
                System.out.println(String.format("%.5f", percentage * 100) + "% " + path + file.getName());
                try {
                    FileWrapper newFile = new FileWrapper(path,file);
                    if(newFile.isSamePath(startAfter)){
                        readNext(in);
                    }
                    if(startAfter == null || startAfter.getCompletePath().compareTo(newFile.getCompletePath()) < 0) {
                        startAfter = null;
                        FileWrapper oldFile = null;
                        FileWrapper nextFile = get(in);
                        boolean foundFile = false;
                        while (!foundFile){
                            oldFile = nextFile;
                            if(nextFile != null && nextFile.getCompletePath().compareTo(newFile.getCompletePath()) < 0){
                                if(!delete(oldFile)){
                                    out.write(oldFile.toString() + "\n");
                                }
                                nextFile = readNext(in);
                            }else{
                                foundFile = true;
                            }
                        }
                        boolean success = true;
                        if (oldFile == null || !oldFile.isSamePath(newFile)) {
                            success = upload(file,newFile,oldFile);
                            oldFile = null;
                        } else {
                            if(!oldFile.getModified().equals(newFile.getModified())){
                                success = upload(file,newFile,oldFile);
                            }
                            readNext(in); // Skip
                        }
                        if(success) {
                            out.write(newFile.toString() + "\n");
                        }else{
                            if(oldFile != null) {
                                out.write(oldFile.toString() + "\n");
                            }
                        }
                        out.flush();
                    }
                } catch (IOException e) {
                    LOG.error(e.getMessage(),e);
                }
                if (file.isDirectory()) {
                    iterate(file, path + file.getName() + "/", percentage, thisStep, in, out,startAfter);
                }
                percentage += thisStep;
            }
        }
    }
    public void deleteRemaining(BufferedReader in,BufferedWriter out) throws IOException {
        FileWrapper oldFile = get(in);
        while(oldFile != null){
            if(!delete(oldFile)){
                out.write(oldFile.toString() + "\n");
                out.flush();
            }
            oldFile = readNext(in);
        }
    }

    private String formatTime(long time){
        long ms = time % 1000;
        time = (time - ms) / 1000;
        long s = time % 60;
        time = (time - s) / 60;
        long m = time % 60;
        long h = (time - m) / 60;
        return String.format("%02d:%02d:%02d.%03d",h,m,s,ms);
    }
    private boolean upload(File file,FileWrapper newFile,FileWrapper oldFile) throws IOException {
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        addParam(entity,"oldFile",oldFile);
        addParam(entity,"newFile",newFile);

        String type;
        if(file.isDirectory()){
            type = "Folder";
        }else{
            type = "File";
            entity.addPart("content",new FileBody(file));
        }
        boolean success = post(entity,"upload" + type);
        if(success){
            System.out.println("UPLOAD: " + newFile);
        }else{
            System.out.println("FAILED UPLOAD: " + newFile);
        }
        return success;
    }
    private boolean delete(FileWrapper oldFile) throws IOException {
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        addParam(entity,"oldFile",oldFile);
        boolean success = post(entity,"delete");
        if(success){
            System.out.println("DELETE: " + oldFile);
        }else{
            System.out.println("FAILED DELETE: " + oldFile);
        }
        return success;
    }
    private boolean post(MultipartEntity entity,String url) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(this.url + url);
        httpPost.setEntity(entity);

        boolean success = false;
        CloseableHttpResponse response = client.execute(httpPost);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
            String result = br.readLine();
            if (result != null && result.equals(Result.SUCCESS)) {
                success = true;
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
        client.close();
        return success;
    }
    private void addParam(MultipartEntity entity,String key,FileWrapper file) throws UnsupportedEncodingException {
        if(file != null) {
            entity.addPart(key, new StringBody(file.toString()));
        }else{
            entity.addPart(key, new StringBody(""));
        }
    }
    private File getFile(String name){
        return new File(folder + "/" + name);
    }

    private BufferedReader getReader(File file) throws FileNotFoundException, UnsupportedEncodingException {
        if(file.exists()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            nextLine.put(br,null);
            return br;
        }else{
            return null;
        }
    }
    private BufferedWriter getWriter(File file, boolean append) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append),"UTF-8"));
    }
    Map<BufferedReader,FileWrapper> nextLine = new HashMap<>();
    private FileWrapper skipTo(String path,BufferedReader br){
        if(path == null){
            return null;
        }
        while(readNext(br) != null && get(br).getCompletePath().compareTo(path) < 0){}
        return get(br);
    }
    private FileWrapper readNext(BufferedReader br){
        if(br == null){
            return null;
        }else{
            try {
                nextLine.put(br, FileWrapper.parse(br.readLine()));
            } catch (IOException e) {
                nextLine.put(br, null);
            }
        }
        return get(br);
    }
    private FileWrapper get(BufferedReader br){
        return nextLine.get(br);
    }
}
