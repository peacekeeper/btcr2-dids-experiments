package test.update.selectbeacon;

import java.util.List;
import java.util.Random;

public class RandomSelectBeaconStrategy implements SelectBeaconStrategy {

    private static final Random RANDOM = new Random();

    private final List<Float> probabilities;
    private final List<SelectBeaconStrategy> selectBeaconStrategies;

    public RandomSelectBeaconStrategy(List<Float> probabilities, List<SelectBeaconStrategy> selectBeaconStrategies) {
        this.probabilities = probabilities;
        this.selectBeaconStrategies = selectBeaconStrategies;
    }

    @Override
    public String[] selectBeaconServiceIdAndType() {
        float random = RANDOM.nextFloat();
        for (int i=0; i<this.getProbabilities().size(); i++) {
            if (random < this.getProbabilities().get(i)) {
                return this.getSelectBeaconStrategies().get(i).selectBeaconServiceIdAndType();
            }
        }
        throw new IllegalStateException("No matching probability for " + random);
    }

    public List<Float> getProbabilities() {
        return probabilities;
    }

    public List<SelectBeaconStrategy> getSelectBeaconStrategies() {
        return selectBeaconStrategies;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i<this.getProbabilities().size(); i++) {
            if (i > 0) stringBuilder.append(", ");
            stringBuilder.append(this.getProbabilities().get(i));
            stringBuilder.append(" (").append(this.getSelectBeaconStrategies().get(i).toString()).append(")");
        }
        return stringBuilder.toString();
    }
}
