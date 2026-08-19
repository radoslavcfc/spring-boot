package example.cashcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/cashcards")
class CashCardController {
    private final CashCardRepository cashCardRepository;

    private CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping("/{requestedId}")
    private ResponseEntity<CashCard> findById(@PathVariable Long requestedId) {
//        if (requestedId.equals(99L)) {
//            CashCard cashCard = new CashCard(99L, 123.45);
//            return ResponseEntity.ok(cashCard);
//        } else {
//            return ResponseEntity.notFound().build();
//        }

        Optional<CashCard> cashCardOptional = cashCardRepository.findById(requestedId);
        if (cashCardOptional.isPresent()) {
            return ResponseEntity.ok(cashCardOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }

// Suggested:
//        Optional<CashCard> cashCardOptional = cashCardRepository.findById(requestedId);
//        return cashCardOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb) {
        CashCard savedCashCard = cashCardRepository.save(newCashCardRequest);
        URI locationOfNewCashCard = ucb
            .path("cashcards/{id}")
            .buildAndExpand(savedCashCard.id())
            .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @GetMapping()
    private ResponseEntity<Iterable<CashCard>> findAll(Pageable pageable) {

        Page<CashCard> page = cashCardRepository.findAll(
            PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
            ));
        return ResponseEntity.ok(page.getContent());
    }
}
