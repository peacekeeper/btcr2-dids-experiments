package test.update.selectbeacon;

public class CASSelectBeaconStrategy implements SelectBeaconStrategy {

    @Override
    public String selectBeacon() {
        return "CASBeacon";
    }

    @Override
    public String toString() {
        return "CASBeacon";
    }
}
