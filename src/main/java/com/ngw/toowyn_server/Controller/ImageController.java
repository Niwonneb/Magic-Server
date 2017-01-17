package com.ngw.toowyn_server.Controller;

import com.esotericsoftware.minlog.Log;
import com.ngw.toowyn_server.Entity.Thought;
import com.ngw.toowyn_server.Service.ThoughtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/thoughts")
public class ImageController {

    @Autowired
    private ThoughtService thoughtService;

    @GetMapping(value = "/exampleNodes")
    public ResponseEntity createExampleNodes() {
        thoughtService.createExampleNodes();
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/recommendations/{id}", method = RequestMethod.GET)
    public ResponseEntity<Object> deleteImageById(@PathVariable String id) {
        return new ResponseEntity<>(String.valueOf(thoughtService.getRecommendations(id)), HttpStatus.OK);
    }

    @GetMapping(value = "/random")
    public @ResponseBody Thought getRandom() {
        return thoughtService.getRandomThought();
    }

    @PostMapping(value = "/{id}")
    public ResponseEntity newThought(@PathVariable String id,
                                     @RequestBody String text) {
        Log.info("controller", "new thought: " + text);
        thoughtService.createThought(text);

        return new ResponseEntity(HttpStatus.OK);
    }
}
