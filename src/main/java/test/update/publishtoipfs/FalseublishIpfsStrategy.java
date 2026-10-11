package test.update.publishtoipfs;

public class FalseublishIpfsStrategy implements PublishToIpfsStrategy {

    @Override
    public boolean publishToIpfs() {
        return false;
    }
}
