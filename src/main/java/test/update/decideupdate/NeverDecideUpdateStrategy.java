package test.update.decideupdate;

public class NeverDecideUpdateStrategy implements DecideUpdateStrategy {

    @Override
    public boolean decideUpdate() {
        return true;
    }
}
