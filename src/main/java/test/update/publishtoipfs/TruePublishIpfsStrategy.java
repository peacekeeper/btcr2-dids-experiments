package test.update.publishtoipfs;

public class TruePublishIpfsStrategy implements PublishToIpfsStrategy {

    @Override
    public boolean publishToIpfs() {
        return true;
    }
}
