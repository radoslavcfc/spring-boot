package example.cashcard;

import java.net.URI;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The cash card REST API
 *
 * @author Felipe Gutierrez
 * @author Josh Cummings
 */
@RestController
@RequestMapping("/cashcards")
public class CashCardController {
    private final CashCardRepository cashCards;

    public CashCardController(CashCardRepository cashCards) {
        this.cashCards = cashCards;
    }

    @PostAuthorize("returnObject.body.owner == authentication.name")
    @GetMapping("/{requestedId}")
    public ResponseEntity<CashCard> findById(@PathVariable Long requestedId) {
        return this.cashCards.findById(requestedId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CashCard> createCashCard(@RequestBody CashCardRequest newCashCardRequest, UriComponentsBuilder ucb, @CurrentOwner String owner) {
        CashCard cashCard = new CashCard(null, newCashCardRequest.amount(), owner);
        CashCard savedCashCard = cashCards.save(cashCard);
        URI locationOfNewCashCard = ucb
            .path("cashcards/{id}")
            .buildAndExpand(savedCashCard.id())
            .toUri();
        return ResponseEntity.created(locationOfNewCashCard).body(savedCashCard);
    }

//    @GetMapping
//    public ResponseEntity<Iterable<CashCard>> findAll(@CurrentSecurityContext(expression = "authentication")Authentication authentication) {
////        var filtered = new ArrayList<CashCard>();
////        this.cashCards.findAll().forEach(cashCard -> {
////            if (cashCard.owner().equals(authentication.getName())) {
////                filtered.add(cashCard);
////            }
////        });
//
//        var result = this.cashCards.findByOwner(authentication.getName());
//        return ResponseEntity.ok(result);
//    }

//    @GetMapping
//    public ResponseEntity<Iterable<CashCard>> findAll(@CurrentSecurityContext(expression = "authentication.name")String owner) {
//        var result = this.cashCards.findByOwner(owner);
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping
//    public ResponseEntity<Iterable<CashCard>> findAll(@CurrentOwner String owner) {
//        var result = this.cashCards.findByOwner(owner);
//        return ResponseEntity.ok(result);
//    }

    @GetMapping
    public ResponseEntity<Iterable<CashCard>> findAll() {
        return ResponseEntity.ok(cashCards.findAll());
    }
}
