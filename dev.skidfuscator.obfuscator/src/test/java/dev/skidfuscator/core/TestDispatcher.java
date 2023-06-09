package dev.skidfuscator.core;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class TestDispatcher {
    public static int loaded;
    public static void load() {
        if (loaded != 0) {
            loaded++;
            return;
        }

        loaded++;
    }
}
