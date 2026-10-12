package test.update.generateoperation;

import uniregistrar.openapi.model.UpdateRequest;

import java.util.List;
import java.util.Random;

public class RandomGenerateOperationStrategy implements GenerateOperationStrategy {

    private static final Random RANDOM = new Random();

    private final float probability;
    private final List<GenerateOperationStrategy> generateOperationStrategies;

    public RandomGenerateOperationStrategy(float probability, List<GenerateOperationStrategy> generateOperationStrategies) {
        this.probability = probability;
        this.generateOperationStrategies = generateOperationStrategies;
    }

    @Override
    public void generateOperation(UpdateRequest updateRequest) {
        if (RANDOM.nextFloat() < this.getProbability()) {
            this.getGenerateOperationStrategies().get(0).generateOperation(updateRequest);
        } else {
            this.getGenerateOperationStrategies().get(1).generateOperation(updateRequest);
        }
    }

    public float getProbability() {
        return probability;
    }

    public List<GenerateOperationStrategy> getGenerateOperationStrategies() {
        return generateOperationStrategies;
    }
}
