package test.update.publishtoipfs;

public class FalsePublishIpfsStrategy implements PublishToIpfsStrategy {

    @Override
    public boolean publishToIpfs() {
        return false;
    }
}
