package test.update.decideupdate;

import java.util.List;
import java.util.Random;

public class RandomDecideUpdateStrategy implements DecideUpdateStrategy {

    private static final Random RANDOM = new Random();

    private final float probability;
    private final List<DecideUpdateStrategy> decideUpdateStrategies;

    public RandomDecideUpdateStrategy(float probability, List<DecideUpdateStrategy> decideUpdateStrategies) {
        this.probability = probability;
        this.decideUpdateStrategies = decideUpdateStrategies;
    }

    @Override
    public boolean decideUpdate() {
        if (RANDOM.nextFloat() < this.getProbability()) {
            return this.getDecideUpdateStrategies().get(0).decideUpdate();
        } else {
            return this.getDecideUpdateStrategies().get(1).decideUpdate();
        }
    }

    public float getProbability() {
        return probability;
    }

    public List<DecideUpdateStrategy> getDecideUpdateStrategies() {
        return decideUpdateStrategies;
    }
}
