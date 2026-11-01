package rocks.leug.turmadamonicarank.ballot;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import rocks.leug.turmadamonicarank.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/ballot")
public class BallotController {

    private final BallotService ballotService;
    private final CurrentUser currentUser;

    public BallotController(BallotService ballotService, CurrentUser currentUser) {
        this.ballotService = ballotService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<BallotEnvelope> current(Authentication authentication) {
        var ballot = ballotService.current(currentUser.require(authentication));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new BallotEnvelope(ballot.orElse(null)));
    }

    @PutMapping
    public ResponseEntity<BallotView> replace(
            @Valid @RequestBody BallotRequest request, Authentication authentication) {
        BallotView ballot = ballotService.replace(
                currentUser.require(authentication), request.characterIds());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ballot);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(Authentication authentication) {
        ballotService.withdraw(currentUser.require(authentication));
    }
}
