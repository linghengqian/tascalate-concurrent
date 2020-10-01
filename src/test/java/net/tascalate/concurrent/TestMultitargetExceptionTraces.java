package net.tascalate.concurrent;

import java.io.IOException;
import java.util.Arrays;

public class TestMultitargetExceptionTraces {

    
    public static void main(String[] argv) {
        MultitargetException e = (MultitargetException)err();
        //Throwable e = outer().initCause(err());
        //e.printStackTrace(new PrintWriter(System.err, true));
        e.printStackTrace();
        System.out.println("--------------");
        System.out.println(e.getLocalizedMessage());
        System.out.println("--------------");
        e.printExceptions();
    }
    
    static Exception outer() {
        return new IOException("Invalid user input");
    }
    
    static Exception err() {
        return err_1();
    }
    
    static Exception err_1() {
        MultitargetException e = new MultitargetException(Arrays.asList(
            null, 
            null, 
            b(), null, 
            a(), null, 
            new MultitargetException(Arrays.asList(c(), b()))
        ));
        //e.fillInStackTrace();
        return e;
    }
    
    static Throwable a() {
        Exception e = new IllegalArgumentException("Something wrong", b());
        //e.fillInStackTrace();
        return e;
    }
    
    static Throwable b() {
        return b_1();
    }
    
    static Throwable b_1() {
        return b_2();
    }
    
    static Throwable b_2() {
        Throwable e = new NoSuchMethodError("Data not found");
        //e.fillInStackTrace();
        return e;
    }
    
    static Throwable c() {
        return c_1();
    }
    
    static Throwable c_1() {
        return c_2();
    }
    
    static Exception c_2() {
        Exception e = new IllegalStateException("State is forbidden");
        //e.fillInStackTrace();
        return e;
    }

}
