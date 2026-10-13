package test.update.selectbeacon;

public class SMTSelectBeaconStrategy implements SelectBeaconStrategy {

    @Override
    public String[] selectBeaconServiceIdAndType() {
        return new String[] { null, "SMTBeacon" };
    }

    @Override
    public String toString() {
        return "SMTBeacon";
    }
}
