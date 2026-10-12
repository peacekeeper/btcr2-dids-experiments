package test.update.decideupdate;

public class FalseDecideUpdateStrategy implements DecideUpdateStrategy {

    @Override
    public boolean decideUpdate() {
        return false;
    }
}
