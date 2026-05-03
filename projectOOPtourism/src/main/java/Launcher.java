import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class Launcher {
    private static final int INSTANCE_COUNT = 3;

    public static void main(String[] args) throws IOException, InterruptedException {
        List<Process> instances = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Process instance : instances) {
                instance.destroy();
            }
        }));

        for (int i = 1; i <= INSTANCE_COUNT; i++) {
            List<String> command = buildLaunchCommand(args);
            Process process = new ProcessBuilder(command)
                    .directory(new File(System.getProperty("user.dir")))
                    .inheritIO()
                    .start();

            instances.add(process);
            System.out.println("Started hotel reservation instance " + i + " of " + INSTANCE_COUNT);
        }

        for (Process instance : instances) {
            instance.waitFor();
        }
    }

    private static List<String> buildLaunchCommand(String[] appArgs) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.addAll(childVmArguments());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("Main");

        for (String arg : appArgs) {
            command.add(arg);
        }

        return command;
    }

    private static List<String> childVmArguments() {
        List<String> vmArgs = new ArrayList<>();

        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!arg.startsWith("-agentlib:")
                    && !arg.startsWith("-javaagent:")
                    && !arg.startsWith("-agentpath:")) {
                vmArgs.add(arg);
            }
        }

        return vmArgs;
    }

    private static String javaExecutable() {
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }
}
