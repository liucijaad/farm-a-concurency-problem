import java.lang.StackTraceElement;

public class Logger {
    private static final Boolean VERBOSE = false;
    private static final String FORMAT  = "[%s][%4d][%10s|%2d][%10s][%-12s] -> %s%n";
    private static final String FORMAT2 = "[%s][%4d][%10s] -> %s%n";

    public static synchronized void log(String msg){
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
        if (VERBOSE) {
            System.out.printf(FORMAT,
                            'L',
                            Simulation.getInstance().getTickCount(),
                            Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            ste.getClassName(),
                            ste.getMethodName(),
                            msg);
        } else {
            System.out.printf(FORMAT2,
                            'L',
                            Simulation.getInstance().getTickCount(),
                            Thread.currentThread().getName(),
                            msg);

        }
    }

    public static synchronized void status(String msg) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
        if (VERBOSE) {
            System.out.printf(FORMAT,
                            'S',
                            Simulation.getInstance().getTickCount(),
                            Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            ste.getClassName(),
                            ste.getMethodName(),
                            msg);
        } else {
            System.out.printf(FORMAT2,
                            'S',
                            Simulation.getInstance().getTickCount(),
                            Thread.currentThread().getName(),
                            msg);

        }
    }

    public static synchronized void error(String msg) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
        if (VERBOSE) {
            System.out.printf(FORMAT,
                            'E',
                            Simulation.getInstance().getTickCount(),
                            Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            ste.getClassName(),
                            ste.getMethodName(),
                            msg);
        } else {
            System.out.printf(FORMAT2,
                            'E',
                            Simulation.getInstance().getTickCount(),
                            Thread.currentThread().getName(),
                            msg);

        }
    }

    public static synchronized void debug(String msg) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
        if (VERBOSE) {
            System.out.printf(FORMAT,
                            'D',
                            Simulation.getInstance().getTickCount(),
                            Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            ste.getClassName(),
                            ste.getMethodName(),
                            msg);
        } else {
            System.out.printf(FORMAT2,
                            'D',
                            Simulation.getInstance().getTickCount(),
                            Thread.currentThread().getName(),
                            msg);

        }
    }
}
