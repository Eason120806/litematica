package fi.dy.masa.litematica.util.invoker;

//#if MC>=260200
public interface IWorldUpdateSuppressor
{
    boolean litematica_getShouldPreventBlockUpdates();

    void litematica_setShouldPreventBlockUpdates(boolean preventUpdates);
}
//#endif
