package com.pdsa.games.knightstour;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knights-tour")
@CrossOrigin
public class KnightsTourController {

    private final KnightsTourService service;

    public KnightsTourController(KnightsTourService service) {
        this.service = service;
    }

    @PostMapping("/run-warnsdorff")
    public ResponseEntity<KnightsTourModel> runWarnsdorff(@RequestBody KnightsTourModel request) {
        return ResponseEntity.ok(service.runWarnsdorff(request));
    }

}