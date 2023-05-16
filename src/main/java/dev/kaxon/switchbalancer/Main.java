package dev.kaxon.switchbalancer;

import dev.kaxon.switchbalancer.util.UpdateUtil;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static final String VERSION = "1.0.0";
    private static final String CONFIG_FILE = "config.yml";
    private static int sides;

    public static void main(String[] args) {
        setWindowTitle("DingKey SwitchBalancer by Kaxon");
        System.out.println("DingKey SwitchBalancer by Kaxon - Github.com/kaxlabs/DingKeySwitchBalancer\n");

        UpdateUtil.getLatestVersionAsync()
                .thenAccept(latestVersion -> {
                    if (UpdateUtil.isGreater(latestVersion, VERSION)) {
                        AnsiConsole.systemInstall(); // Install Jansi to enable ANSI escape codes on Windows
                        Ansi ansi = Ansi.ansi();
                        ansi.fg(Ansi.Color.GREEN); // Set foreground color to yellow
                        ansi.a("\nAn update is available: v").a(latestVersion).a(" (current version: v").a(VERSION).a(")\n").a("Download it at: ").a(UpdateUtil.GITHUB_RELEASES_URL);
                        ansi.reset(); // Reset the color to default
                        System.out.print(ansi + "\nInput: ");
                        AnsiConsole.systemUninstall(); // Uninstall Jansi when you're done
                    }
                })
                .exceptionally(ex -> {
                    System.err.print("\nError fetching latest version: " + ex.getMessage() + "\nInput: ");
                    return null;
                });

        Scanner scanner = new Scanner(System.in);

        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            changeConfig(scanner);
        } else {
            loadConfig();
            if (askToChangeConfig(scanner)) {
                changeConfig(scanner);
            }
        }

        main:while (true) {
            System.out.print("Total amount of switches to break in (type 'exit' to quit): ");
            String targetSumInput;
            int targetSum;
            while (true) {
                targetSumInput = scanner.nextLine().trim();

                if (targetSumInput.equalsIgnoreCase("exit")) {
                    break main;
                }

                try {
                    targetSum = Integer.parseInt(targetSumInput);
                    break; // Exit the loop if the input is valid
                } catch (NumberFormatException ignored) {
                    System.out.print("\nInvalid input. Please enter a valid number or 'exit' to quit: ");
                }
            }

            System.out.print("Number of additional switches to check from " + targetSum + " (leave blank for 0): ");
            int additionalChecks = readIntOrDefault(scanner, 0);

            System.out.print("Amount of possible combinations (leave blank for 20): ");
            int maxPairs = readIntOrDefault(scanner, 20);

            for (int i = 0; i <= additionalChecks; i++) {
                int currentTarget = targetSum + i;
                List<int[]> result = findPairs(currentTarget, maxPairs);
                if (!result.isEmpty()) {
                    result = simplifyPairs(result);
                    System.out.println("Pairs for " + currentTarget + ":");
                    for (int[] pair : result) {
                        System.out.println(pair[0] + " sides of " + pair[1] + " switches");
                    }
                    System.out.println();
                }
            }
        }
    }

    private static List<int[]> simplifyPairs(List<int[]> pairs) {
        Map<Integer, Integer> groupedPairs = new HashMap<>();
        for (int[] pair : pairs) {
            groupedPairs.put(pair[1], groupedPairs.getOrDefault(pair[1], 0) + pair[0]);
        }
        return groupedPairs.entrySet().stream()
                .map(entry -> new int[]{entry.getValue(), entry.getKey()})
                .collect(Collectors.toList());
    }

    private static boolean askToChangeConfig(Scanner scanner) {
        System.out.print("Do you want to change the config (carriage sides=" + sides + ")? (y/N [N=default]): ");
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y");
    }

    private static void changeConfig(Scanner scanner) {
        while (true) {
            System.out.print("How many carriage sides does your set up have? (8 per 4-way machine): ");
            sides = readInt(scanner);
            if (sides >= 4 && sides % 4 == 0) {
                break;
            } else {
                System.out.println("Invalid input. Sides must be at least 4 and divisible by 4.");
            }
        }
        saveConfig();
    }

    private static int readInt(Scanner scanner) {
        while (true) {
            try {
                int value = scanner.nextInt();
                scanner.nextLine(); // Consume newline character
                return value;
            } catch (Exception e) {
                System.out.print("Invalid input. Please enter a valid number: ");
                scanner.nextLine(); // Consume invalid input
            }
        }
    }

    private static int readIntOrDefault(Scanner scanner, int defaultValue) {
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Using default value: " + defaultValue);
            return defaultValue;
        }
    }

    private static void loadConfig() {
        Yaml yaml = new Yaml();
        try (FileInputStream inputStream = new FileInputStream(CONFIG_FILE)) {
            Map<String, Integer> config = yaml.load(inputStream);
            sides = config.get("sides");
        } catch (IOException e) {
            System.out.println("Error loading config file: " + e.getMessage());
        }
    }

    private static void saveConfig() {
        Yaml yaml = new Yaml();
        Map<String, Integer> config = new HashMap<>();
        config.put("sides", sides);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            yaml.dump(config, writer);
        } catch (IOException e) {
            System.out.println("Error saving config file: " + e.getMessage());
        }
    }

    private static List<int[]> findPairs(int targetSum, int maxPairs) {
        List<List<int[]>> validPairs = new ArrayList<>();
        for (int numPairs = 1; numPairs <= maxPairs; numPairs++) {
            List<int[]> pairs = new ArrayList<>();
            Map<String, Boolean> memo = new HashMap<>();
            findPairsRecursively(validPairs, pairs, numPairs, targetSum, 0, memo);
        }

        return validPairs.stream()
                .min(Comparator.comparingInt(pairList -> pairList.stream().mapToInt(pair -> pair[0]).sum()))
                .orElse(new ArrayList<>());
    }

    private static void findPairsRecursively(List<List<int[]>> validPairs, List<int[]> pairs, int numPairs, int targetSum, int currentSum, Map<String, Boolean> memo) {
        if (pairs.size() == numPairs) {
            if (currentSum == targetSum) {
                validPairs.add(new ArrayList<>(pairs));
            }
            return;
        }

        String memoKey = pairs.size() + "," + currentSum;
        if (memo.containsKey(memoKey)) {
            return;
        }

        for (int first = 4; first <= 18; first++) {
            if (first % 4 != 0 && first != 6 && first != 7) {
                continue;
            }

            if (first == 6 || first == 7) {
                boolean hasPair = false;
                for (int[] pair : pairs) {
                    if (pair[0] == first) {
                        hasPair = true;
                        break;
                    }
                }
                if (!hasPair) {
                    continue;
                }
            }

            for (int second = 4; second <= 9; second++) {
                int[] newPair = {first, second};
                pairs.add(newPair);
                findPairsRecursively(validPairs, pairs, numPairs, targetSum, currentSum + first * second, memo);
                pairs.remove(pairs.size() - 1);
            }
        }

        memo.put(memoKey, false);
    }

    private static void setWindowTitle(String title) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec("cmd.exe /c title " + title);
            } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                System.out.print("\033]0;" + title + "\007");
            } else {
                System.out.println("Unsupported platform for setting window title.");
            }
        } catch (IOException e) {
            System.out.println("Error setting window title: " + e.getMessage());
        }
    }
}
