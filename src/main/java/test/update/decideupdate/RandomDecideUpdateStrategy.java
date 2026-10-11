package test.update.decideupdate;

import java.util.Random;

public class RandomDecideUpdateStrategy implements DecideUpdateStrategy {

    private static final Random RANDOM = new Random();

    private final float probability;

    public RandomDecideUpdateStrategy(float probability) {
        this.probability = probability;
    }

    @Override
    public boolean decideUpdate() {
        return RANDOM.nextFloat() < this.getProbability();
    }

    public float getProbability() {
        return probability;
    }
}
