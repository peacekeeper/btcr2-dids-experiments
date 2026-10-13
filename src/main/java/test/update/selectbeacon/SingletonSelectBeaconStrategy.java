package test.update.selectbeacon;

public class SingletonSelectBeaconStrategy implements SelectBeaconStrategy {

    @Override
    public String[] selectBeaconServiceIdAndType() {
        return new String[] { "#initialP2PKH", "SingletonBeacon" };
    }

    @Override
    public String toString() {
        return "SingletonBeacon";
    }
}
