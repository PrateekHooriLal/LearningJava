package com.manage.fruit.controller;

import com.manage.fruit.model.Fruit;
import com.manage.fruit.service.FruitService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fruits")
public class FruitController {

    private final FruitService fruitService;

    @Autowired
    public FruitController(FruitService fruitService) {
        this.fruitService = fruitService;
    }

    @PostMapping
    public ResponseEntity<Fruit> createFruit(@Valid @RequestBody Fruit fruit) {
        Fruit savedFruit = fruitService.saveFruit(fruit);
        return new ResponseEntity<>(savedFruit, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Fruit>> getAllFruits() {
        List<Fruit> fruits = fruitService.getAllFruits();
        return new ResponseEntity<>(fruits, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Fruit> getFruitById(@PathVariable Long id) {
        return fruitService.getFruitById(id)
                .map(fruit -> new ResponseEntity<>(fruit, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Fruit> getFruitByName(@PathVariable String name) {
        return fruitService.getFruitByName(name)
                .map(fruit -> new ResponseEntity<>(fruit, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<Fruit> updateStock(@PathVariable Long id, @RequestParam Integer quantity) {
        try {
            Fruit updatedFruit = fruitService.updateStock(id, quantity);
            return new ResponseEntity<>(updatedFruit, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFruit(@PathVariable Long id) {
        fruitService.deleteFruit(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
