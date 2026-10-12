package test.update.decideupdate;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomDecideUpdateStrategy implements DecideUpdateStrategy {

    private static final Random RANDOM = new Random();

    private final List<Float> probabilities;
    private final List<DecideUpdateStrategy> decideUpdateStrategies;

    public RandomDecideUpdateStrategy(List<Float> probabilities, List<DecideUpdateStrategy> decideUpdateStrategies) {
        this.probabilities = probabilities;
        this.decideUpdateStrategies = decideUpdateStrategies;
    }

    @Override
    public boolean decideUpdate() {
        float random = RANDOM.nextFloat();
        for (int i=0; i<this.getProbabilities().size(); i++) {
            if (random < this.getProbabilities().get(i)) {
                return this.getDecideUpdateStrategies().get(i).decideUpdate();
            }
        }
        throw new IllegalStateException("No matching probability for " + random);
    }

    public List<Float> getProbabilities() {
        return probabilities;
    }

    public List<DecideUpdateStrategy> getDecideUpdateStrategies() {
        return decideUpdateStrategies;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i<this.getProbabilities().size(); i++) {
            if (i > 0) stringBuilder.append(", ");
            stringBuilder.append(this.getProbabilities().get(i));
            stringBuilder.append(" (").append(this.getDecideUpdateStrategies().get(i).toString()).append(")");
        }
        return stringBuilder.toString();
    }
}
