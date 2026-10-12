package test.update.publishtoipfs;

import java.util.List;
import java.util.Random;

public class RandomPublishToIpfsStrategy implements PublishToIpfsStrategy {

    private static final Random RANDOM = new Random();

    private final float probability;
    private final List<PublishToIpfsStrategy> publishToIpfsStrategies;

    public RandomPublishToIpfsStrategy(float probability, List<PublishToIpfsStrategy> publishToIpfsStrategies) {
        this.probability = probability;
        this.publishToIpfsStrategies = publishToIpfsStrategies;
    }

    @Override
    public boolean publishToIpfs() {
        if (RANDOM.nextFloat() < this.getProbability()) {
            return this.getPublishToIpfsStrategies().get(0).publishToIpfs();
        } else {
            return this.getPublishToIpfsStrategies().get(1).publishToIpfs();
        }
    }

    public float getProbability() {
        return probability;
    }

    public List<PublishToIpfsStrategy> getPublishToIpfsStrategies() {
        return publishToIpfsStrategies;
    }
}
