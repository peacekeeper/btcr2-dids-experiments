package test.update.selectbeacon;

public class CASSelectBeaconStrategy implements SelectBeaconStrategy {

    @Override
    public String[] selectBeaconServiceIdAndType() {
        return new String[] { null, "CASBeacon" };
    }

    @Override
    public String toString() {
        return "CASBeacon";
    }
}
