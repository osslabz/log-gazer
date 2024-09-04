package net.osslabz.lg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberFinder {


    public static void main(String[] args) {
        String input = "The total cost is 1,234.56 dollars and 78.90 euros, or approximately 2,345 dollars. 1.222.333,00 sdfdsfsdf 22.333";

        // Regular expression to match numbers (integers and floats with optional commas or periods)
        String regex = "(?<!\\S)[+-]?(?:\\d{1,3}(?:[.,]\\d{3})*|\\d+)(?:[.,]\\d+)?(?!\\S)";

        // Compile the regular expression
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        // Find all matches in the input string
        while (matcher.find()) {
            String number = matcher.group();
            int start = matcher.start();
            int end = matcher.end() - 1;

            System.out.println("Found number: " + number + " at position: [" + start + ", " + end + "]");
        }
    }
}