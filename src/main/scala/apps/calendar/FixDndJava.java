package de.unruh.quickfind.apps.calendar;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;

import java.net.URI;
import java.net.URISyntaxException;

public class FixDndJava {
    private static boolean patchInstalled = false;
    private static Object lock = new Object();
    static void installPatch() {
        if (!patchInstalled) synchronized(lock) {
            if (!patchInstalled) {
                System.setProperty("net.bytebuddy.experimental", "true");
                ByteBuddyAgent.install();
                new ByteBuddy()
                    .redefine(URI.class)
                    .visit(Advice.to(FixDndJava.class).on(
                            ElementMatchers.isConstructor().and(ElementMatchers.takesArguments(String.class))))
                    .make()
                    .load(
                            ClassLoader.getSystemClassLoader(),
                            ClassReloadingStrategy.fromInstalledAgent()
                    );
                patchInstalled = true;
            }
        }
    }

    @Advice.OnMethodEnter
    static void beforeConstructor(@Advice.Argument(value=0, readOnly=false) String string) {
        System.out.println("Constructor");
        System.out.println(string);
        if (string.contains("[") || string.contains("]")) {
            string = string.replace("[", "%5B").replace("]", "%5D");
        }
    }
}
