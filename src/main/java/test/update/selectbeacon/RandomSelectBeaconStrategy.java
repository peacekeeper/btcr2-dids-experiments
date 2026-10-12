package test.update.selectbeacon;

import java.util.List;
import java.util.Random;

public class RandomSelectBeaconStrategy implements SelectBeaconStrategy {

    private static final Random RANDOM = new Random();

    private final float probability;
    private final List<SelectBeaconStrategy> selectBeaconStrategies;

    public RandomSelectBeaconStrategy(float probability, List<SelectBeaconStrategy> selectBeaconStrategies) {
        this.probability = probability;
        this.selectBeaconStrategies = selectBeaconStrategies;
    }

    @Override
    public String selectBeacon() {
        if (RANDOM.nextFloat() < this.getProbability()) {
            return this.getSelectBeaconStrategies().get(0).selectBeacon();
        } else {
            return this.getSelectBeaconStrategies().get(1).selectBeacon();
        }
    }

    public float getProbability() {
        return probability;
    }

    public List<SelectBeaconStrategy> getSelectBeaconStrategies() {
        return selectBeaconStrategies;
    }
}
