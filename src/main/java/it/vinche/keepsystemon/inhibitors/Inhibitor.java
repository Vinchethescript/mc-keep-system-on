package it.vinche.keepsystemon.inhibitors;

public interface Inhibitor {
    public boolean inhibit(String reason);
    public boolean unhibit(); // haha get it
    public boolean isInhibited();
}
