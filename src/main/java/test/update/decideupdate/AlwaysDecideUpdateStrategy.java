package test.update.decideupdate;

public class AlwaysDecideUpdateStrategy implements DecideUpdateStrategy {

    @Override
    public boolean decideUpdate() {
        return true;
    }
}
