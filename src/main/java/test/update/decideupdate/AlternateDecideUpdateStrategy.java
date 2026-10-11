package test.update.decideupdate;

public class AlternateDecideUpdateStrategy implements DecideUpdateStrategy {

    private Boolean alternate = Boolean.FALSE;

    @Override
    public boolean decideUpdate() {

        this.setAlternate(! this.getAlternate());
        return this.alternate;
    }

    public Boolean getAlternate() {
        return alternate;
    }

    public void setAlternate(Boolean alternate) {
        this.alternate = alternate;
    }
}
