package test.update.publishtoipfs;

import java.util.List;
import java.util.Random;

public class RandomPublishToIpfsStrategy implements PublishToIpfsStrategy {

    private static final Random RANDOM = new Random();

    private final List<Float> probabilities;
    private final List<PublishToIpfsStrategy> publishToIpfsStrategies;

    public RandomPublishToIpfsStrategy(List<Float> probabilities, List<PublishToIpfsStrategy> publishToIpfsStrategies) {
        this.probabilities = probabilities;
        this.publishToIpfsStrategies = publishToIpfsStrategies;
    }

    @Override
    public boolean publishToIpfs() {
        float random = RANDOM.nextFloat();
        for (int i=0; i<this.getProbabilities().size(); i++) {
            if (random < this.getProbabilities().get(i)) {
                return this.getPublishToIpfsStrategies().get(i).publishToIpfs();
            }
        }
        throw new IllegalStateException("No matching probability for " + random);
    }

    public List<Float> getProbabilities() {
        return probabilities;
    }

    public List<PublishToIpfsStrategy> getPublishToIpfsStrategies() {
        return publishToIpfsStrategies;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i<this.getProbabilities().size(); i++) {
            if (i > 0) stringBuilder.append(", ");
            stringBuilder.append(this.getProbabilities().get(i));
            stringBuilder.append(" (").append(this.getPublishToIpfsStrategies().get(i).toString()).append(")");
        }
        return stringBuilder.toString();
    }
}
