package test.update.decideupdate;

public class TrueDecideUpdateStrategy implements DecideUpdateStrategy {

    @Override
    public boolean decideUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "true";
    }
}
