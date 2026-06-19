package me.ichun.mods.morph.api;
public class MorphApi {
    private static IApi apiImpl;
    public static IApi getApi() { return apiImpl; }
    public static void setApiImpl(IApi impl) { apiImpl = impl; }
}
