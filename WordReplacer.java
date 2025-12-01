import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Main class for WordReplacer program which performs a global search and replace
 * in a text file. The program reads the input text file and text file
 * containing word/replacement pairs, stores them in a map, and performs replacement.
 * This program functions for three data structures (BSTrees, RBTrees, and Hash) which implement
 * the MyMap interface.
 * @author Zoya Khan (zk2278)
 * @version 1.0.0 December 16, 2024
 */
public class WordReplacer {
    /**
     * Main method that handles overarching 4 components of the programs: parsing command line
     * arguments, creating map, parsing input files, and displaying output.
     *
     * @param args command line arguments: input text file, word replacement file,
     *             data structure type (bst, rbt, hash)
     */
    public static void main(String[] args) {
        //throughout class logic, because processing large files, used BufferedReaders and try-catch blocks
        //(https://www.geeksforgeeks.org/try-catch-block-in-programming/)
        try {
            verifyArguments(args); //calls helper method (ensures correct #args, file exists/can open, valid DS)
            //extract the verified arguments
            String inputTextFile = args[0];
            String wordReplacementsFile = args[1];
            String dataStructure = args[2];

            //instantiate DS by creating reference to interface rather than class itself (from spec)
            MyMap<String, String> map;

            //use switch-case for efficiency (instead of checking individually (like if-else))
            //(https://www.geeksforgeeks.org/switch-statement-in-java/)
            //switch-case implementation (https://www.w3schools.com/java/java_switch.asp)
            switch (dataStructure) {
                case "bst":
                    map = new BSTreeMap<>();
                    break;
                case "rbt":
                    map = new RBTreeMap<>();
                    break;
                case "hash":
                    map = new MyHashMap<>();
                    break;
                default:
                    System.err.println("Error: Invalid data structure '" + dataStructure + "' received.");
                    System.exit(1);
                    return;
            }

            rules(wordReplacementsFile, map); //create map w stored replacement rules
            StringBuilder replacedText = processInput(inputTextFile, map); //process input file + apply rules from map
            System.out.printf("%s\n", replacedText); //output specified in spec
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Verifies user specified 3 arguments (input file, word replacement file, data structure),
     * validates the 2 files, and checks the data structure.
     * Exits program with error message if arguments invalid.
     *
     * @param args the command line arguments that need to be verified and processed
     * @throws IOException
     * @throws FileNotFoundException if arguments invalid
     */
    //handling IOException in method header: (https://www.geeksforgeeks.org/exception-handling-with-method-overriding-in-java/?ref=lbp)
    private static void verifyArguments(String[] args) throws IOException {
        if (args.length != 3) { //exit if not exactly 3 arguments
            System.err.println("Usage: java WordReplacer <input text file> <word replacements file> <bst|rbt|hash>");
            System.exit(1);
        }

        String inputTextFile = args[0];
        String wordReplacementsFile = args[1];
        String dataStructure = args[2];

        //check input text file
        //handling FileNotFoundException errors (https://www.geeksforgeeks.org/java-io-filenotfoundexception-in-java/)
        //for FileReader (https://www.geeksforgeeks.org/java-io-filereader-class/)
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputTextFile));
        } catch (FileNotFoundException e) {
            System.err.println("Error: Cannot open file '" + inputTextFile + "' for input.");
            System.exit(1);
        }
        //check word replacements file
        try {
            BufferedReader reader = new BufferedReader(new FileReader(wordReplacementsFile));
        } catch (FileNotFoundException e) {
            System.err.println("Error: Cannot open file '" + wordReplacementsFile + "' for input.");
            System.exit(1);
        }
        //verify validity of data structure (string with diff capitalization is invalid)
        if (!dataStructure.equals("bst") && !dataStructure.equals("rbt") && !dataStructure.equals("hash")) {
            System.err.println("Error: Invalid data structure '" + dataStructure + "' received.");
            System.exit(1);
        }
    }

    /**
     * Reads file with replacement rules and adds them to map.
     * Because of potential presence of transitive dependencies and cycles, and
     * because mapping is directed, rules are modeled and parsed through union-find graph
     * algorithm (efficient for large datasets)
     *
     * @param wordReplacementsFile the name of file with replacement rules
     * @param map                  user-specified data structure (rbt, bst, hash)
     * @throws IllegalArgumentException if cycle is detected in rules
     * @throws IOException              if error occurs while reading file
     */
    private static void rules(String wordReplacementsFile, MyMap<String, String> map) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(wordReplacementsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] rule = line.split(" -> "); //split rule into key value pairs along arrow
                if (rule.length != 2) {
                    continue; //(https://www.w3schools.com/java/java_break.asp)
                }
                String key = rule[0];
                String value = rule[1];

                //cycle detection BEFORE adding to map (to avoid conflicts)
                if (findCycle(key, value, map)) {
                    System.err.println("Cycle detected when trying to add replacement rule: " + key + " -> " + value);
                    System.exit(1);
                }

                //functions just like union in union-find algorithm, links value as parent/replacement of key
                map.put(key, value); //store role in the map
            }
        } catch (IOException e) {
            System.err.println("Error: An I/O error occurred reading '" + wordReplacementsFile + "'.");
            System.exit(1);
        }
    }

    /**
     * Reads input text file, implements replacement rules, and returns adjusted text.
     *
     * @param inputTextFile the name of input text file
     * @param map           map containing the replacement rules
     * @return StringBuilder containing replaced/adjusted text
     * @throws IOException if error occurs while reading file
     */
    private static StringBuilder processInput(String inputTextFile, MyMap<String, String> map) throws IOException {
        StringBuilder replacedText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputTextFile))) {
            String line; //parse input file line by line as a String
            while ((line = reader.readLine()) != null) {
                StringBuilder processedLine = new StringBuilder(); //accumulates FULL processed version of current line
                StringBuilder tempWord = new StringBuilder(); //temp holds char of word as encountered

                for (char c : line.toCharArray()) { //convert line to char array, then iterate through each char
                    if (Character.isLetter(c)) {
                        tempWord.append(c); //collect alphabetic letters
                    } else { //non-alphabetic characters encountered
                        if (tempWord.length() > 0) { //if word, append replacement or original word
                            processedLine.append(replaceWord(tempWord.toString(), map));
                            tempWord.setLength(0); //before you start looking at next word, reset
                        }
                        processedLine.append(c); //adds this non-alphabetic character to processedLine
                    }
                }
                if (tempWord.length() > 0) { //words at end of line
                    processedLine.append(replaceWord(tempWord.toString(), map));
                }
                replacedText.append(processedLine).append('\n');
            }
        } catch (IOException e) {
            System.err.println("Error: An I/O error occurred reading '" + inputTextFile + "'.");
            System.exit(1);
        }
        return replacedText;
    }

    /**
     * Traverses map to detect any cycles in replacement rules. Boolean returns true if
     * cycle detected, else returns false.
     * Implemented from code logic (find method) in Kruskal MST2 / lecture provided in CourseWorks.
     *
     * @param key original word in replacement rule (left)
     * @param value what replaces the key (OG word) (right)
     * @param map  map with the replacement rules
     * @return true if cycle is found, else false
     */
    private static boolean findCycle(String key, String value, MyMap<String, String> map) {
        String current = value;
        while (current != null) { //follows parents/replacement from value to value's root
            if (current.equals(key)) { //if encounter key (original world), cycle exists
                return true;
            }
            current = map.get(current);
        }
        return false;
    }

    /**
     * Reads and processes one line at a time, replacing words using the map.
     * Preserves non-alphabetic characters and resolves all transitive replacements.
     * Resolves any transitive dependencies by traversing the replacement rule (from map)
     * and finding the FINAL replacement (root of dependency);
     * Implemented from code logic (find method) in Kruskal MST2 / lecture provided in CourseWorks.
     *
     * @param word word to be replaced
     * @param map  map with the replacement rules
     * @return either final replacement word or null
     */
    private static String replaceWord(String word, MyMap<String, String> map) {
        String replacement = word;
        //traverses up, following replacement/dependency chain, until root (replacement with no parent)
        while (map.get(replacement) != null) {
            replacement = map.get(replacement);
        }

        //compress path by making all nodes point to root
        String current = word;
        //root found, iterate back down chain, updating nodes to point to root
        while (map.get(current) != null && !map.get(current).equals(replacement)) {
            String parent = map.get(current); //current parent
            map.put(current, replacement); //point to root
            current = parent; //go up through the chain
        }
        return replacement;
    }
}