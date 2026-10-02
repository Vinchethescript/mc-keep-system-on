package it.vinche.keepsystemon.inhibitors;

public interface Inhibitor {
    public void inhibit(String reason) throws Exception;
    public void unhibit() throws Exception; // haha get it
    public boolean isInhibited();
    public void close() throws Exception;
}
