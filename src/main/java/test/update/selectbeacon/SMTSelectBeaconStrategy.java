package test.update.selectbeacon;

public class SMTSelectBeaconStrategy implements SelectBeaconStrategy {

    @Override
    public String selectBeacon() {
        return "SMTBeacon";
    }

    @Override
    public String toString() {
        return "SMTBeacon";
    }
}
