package test.update.generateoperation;

import uniregistrar.openapi.model.UpdateRequest;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomGenerateOperationStrategy implements GenerateOperationStrategy {

    private static final Random RANDOM = new Random();

    private final List<Float> probabilities;
    private final List<GenerateOperationStrategy> generateOperationStrategies;

    public RandomGenerateOperationStrategy(List<Float> probabilities, List<GenerateOperationStrategy> generateOperationStrategies) {
        this.probabilities = probabilities;
        this.generateOperationStrategies = generateOperationStrategies;
    }

    @Override
    public void generateOperation(UpdateRequest updateRequest) {
        float random = RANDOM.nextFloat();
        for (int i=0; i<this.getProbabilities().size(); i++) {
            if (random < this.getProbabilities().get(i)) {
                this.getGenerateOperationStrategies().get(i).generateOperation(updateRequest);
                return;
            }
        }
        throw new IllegalStateException("No matching probability for " + random);
    }

    public List<Float> getProbabilities() {
        return probabilities;
    }

    public List<GenerateOperationStrategy> getGenerateOperationStrategies() {
        return generateOperationStrategies;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i<this.getProbabilities().size(); i++) {
            if (i > 0) stringBuilder.append(", ");
            stringBuilder.append(this.getProbabilities().get(i));
            stringBuilder.append(" (").append(this.getGenerateOperationStrategies().get(i).toString()).append(")");
        }
        return stringBuilder.toString();
    }
}
