package com.manage.fruit.service;

import com.manage.fruit.model.Fruit;
import com.manage.fruit.repository.FruitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FruitService {

    private final FruitRepository fruitRepository;

    @Autowired
    public FruitService(FruitRepository fruitRepository) {
        this.fruitRepository = fruitRepository;
    }

    public Fruit saveFruit(Fruit fruit) {
        return fruitRepository.save(fruit);
    }

    public List<Fruit> getAllFruits() {
        return fruitRepository.findAll();
    }

    public Optional<Fruit> getFruitById(Long id) {
        return fruitRepository.findById(id);
    }

    public Optional<Fruit> getFruitByName(String name) {
        return fruitRepository.findByName(name);
    }

    public void deleteFruit(Long id) {
        fruitRepository.deleteById(id);
    }

    public Fruit updateStock(Long id, Integer quantity) throws Exception {
        Fruit fruit = fruitRepository.findById(id)
                .orElseThrow(() -> new Exception("Fruit not found with id: " + id));
        fruit.setStockQuantity(quantity);
        return fruitRepository.save(fruit);
    }
}
