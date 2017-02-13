package com.ngw.seed.Controller;

import com.ngw.seed.CommunicationObjects.Thought;
import com.ngw.seed.Service.ThoughtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/thoughts")
public class ImageController {

    @Autowired
    private ThoughtService thoughtService;

    @PostMapping(value = "/rateThought")
    @ResponseStatus(value = HttpStatus.OK)
    public void rateThought(@RequestParam("idBefore") String idBefore,
                            @RequestParam("likedBefore") Boolean likedBefore,
                            @RequestParam("idNow") String idNow,
                            @RequestParam("likedNow") Boolean likedNow) {
        thoughtService.rateThought(idBefore, likedBefore, idNow, likedNow);
    }

    @PostMapping(value = "/createThought")
    @ResponseStatus(value = HttpStatus.OK)
    public void createThought(@RequestParam("text") String text,
                              @RequestParam("idBefore") String idBefore,
                              @RequestParam("likedBefore") Boolean likedBefore) {
        thoughtService.createThought(text, idBefore, likedBefore);
    }

    @GetMapping(value = "/getNextAfterLiked/{id}")
    public @ResponseBody Thought getNextThoughtAfterLiked(@PathVariable String id) {
        return thoughtService.getNextThought(id, true);
    }

    @GetMapping(value = "/getNextAfterDisliked/{id}")
    public @ResponseBody Thought getNextThoughtAfterDisliked(@PathVariable String id) {
        return thoughtService.getNextThought(id, false);
    }

    @GetMapping(value = "/getThoughtFromStart")
    public @ResponseBody Thought getThoughtFromStart() {
        return thoughtService.getThoughtFromStart();
    }
}
