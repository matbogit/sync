package web;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import shared.FileWrapper;
import shared.Result;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

@RestController
public class WebController {
    @Autowired
    Environment environment;

    private String user = "mathias";

    @RequestMapping("/")
    public String index() {
        return "index<br><ul><li><a href='/test?asd='>test</a></li><li>" + environment.getProperty("files.root") + "</li></ul>";
    }

    @RequestMapping("/test")
    public String hello(
            String asd
    ) {
        return asd;
    }
    @RequestMapping(value = "/uploadFile",method = RequestMethod.POST)
    public String uploadFile(
        @RequestParam("oldFile") String oldFile,
        @RequestParam("newFile") String newFile,
        @RequestParam("content") MultipartFile content
    ) throws IOException, ParseException {
        FileWrapper oldFileObj = FileWrapper.parse(oldFile);
        FileWrapper newFileObj = FileWrapper.parse(newFile);
        File file = new File(environment.getProperty("files.root") + "/" + user + "/" + newFileObj.getCompletePath());
        content.transferTo(file);
        file.setLastModified(newFileObj.getLastModified());
        return Result.SUCCESS;
    }
    @RequestMapping(value = "/uploadFolder",method = RequestMethod.POST)
    public String uploadFolder(
            @RequestParam("oldFile") String oldFile,
            @RequestParam("newFile") String newFile
    ) throws ParseException {
        FileWrapper oldFileObj = FileWrapper.parse(oldFile);
        FileWrapper newFileObj = FileWrapper.parse(newFile);
        File file = new File(environment.getProperty("files.root") + "/" + user + "/" + newFileObj.getCompletePath());
        file.mkdirs();
        file.setLastModified(newFileObj.getLastModified());
        return Result.SUCCESS;
    }
    @RequestMapping(value = "/delete",method = RequestMethod.POST)
    public String delete(
            @RequestParam("oldFile") String oldFile
    ) throws ParseException {
        FileWrapper oldFileObj = FileWrapper.parse(oldFile);
        File file = new File(environment.getProperty("files.root") + "/" + user + "/" + oldFileObj.getCompletePath());
        if(file.exists()) {
            if (file.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(file);
                    return Result.SUCCESS;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (file.delete()) {
                return Result.SUCCESS;
            }
            return Result.FAILED;
        }
        return Result.SUCCESS;
    }
}