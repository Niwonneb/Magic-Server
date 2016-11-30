package com.magicapp.Controller;

import com.magicapp.Service.ImageService;
import com.magicapp.Dao.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private StorageService storageService;

    @RequestMapping(value = "/exampleNodes", method = RequestMethod.GET)
    public String deleteImageById() {
        imageService.createExampleNodes();
        return "added example nodes";
    }

    @RequestMapping(value = "/recommendations/{nr}", method = RequestMethod.GET)
    public String deleteImageById(@PathVariable int nr) {
        return imageService.getRecommendations(nr);
    }

    //@RequestParam("description") String description,


    @PostMapping("/images/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file) {

        String path = storageService.store(file);
        imageService.createImage("", path);

        return "Ok";
    }
}
