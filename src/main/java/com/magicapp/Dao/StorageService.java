package com.magicapp.Dao;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.File;
import java.io.IOException;

@Repository
public class StorageService {

    public StorageService() {}

    public String store(MultipartFile file) {
        String filePath = System.getProperty("user.dir") + "/images/";
        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }


    // deprecated
    public MultipartFile getImage(String filePath) {
        DiskFileItem fileItem = null;
        fileItem = (DiskFileItem) new DiskFileItemFactory().createItem("image", "image/png", true, filePath);

        return new CommonsMultipartFile(fileItem);
    }
}
