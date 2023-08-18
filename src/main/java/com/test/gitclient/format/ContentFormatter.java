package com.test.gitclient.format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentFormatter {
    public static List<FormatEntry> parserEntry(String entryString) {
        List<FormatEntry> formatEntries = new ArrayList<>();
        String[] lines = entryString.split("\n");
        int startIndex = 0;
        FormatEntry formatEntry = null;
        boolean contentStart = false;

        for (String line : lines) {
            line = line.trim();
            if ((line.startsWith("index") && !contentStart) || "\\ No newline at end of file".equals(line)) {
                continue;
            } else if (line.startsWith("diff --git")) {
                contentStart = false;
                formatEntry = new FormatEntry();
                formatEntries.add(formatEntry);
                formatEntry.setType(DiffEntryType.MODIFY);
            } else if ((line.startsWith("---") || line.startsWith("+++")) && !contentStart) {
                String file = line.split(" ")[1];
                if (isFile(file)) {
                    file = file.substring(file.indexOf("/") + 1);
                    formatEntry.setFilePath(file);
                }
            } else if (line.equals("new file mode 100644") && !contentStart) {
                formatEntry.setType(DiffEntryType.ADD);
            } else if (line.equals("deleted file mode 100644") && !contentStart) {
                formatEntry.setType(DiffEntryType.DELETE);
            } else if (isRange(line)) {
                contentStart = true;
                String[] rangeInfo = line.split(" ");
                String index = rangeInfo[1].replace("-", "");
                startIndex = Integer.parseInt(index.split(",")[0]);
            } else if (line.startsWith("-") && contentStart) {
                formatEntry.addContent(new DiffContent(ContentType.DELETE, startIndex++, line.substring(1)));
            } else if (line.startsWith("+") && contentStart) {
                formatEntry.addContent(new DiffContent(ContentType.ADD, -1, line.substring(1)));
            } else if (contentStart  && contentStart) {
                formatEntry.addContent(new DiffContent(ContentType.ORIGINAL, startIndex++, line));
            }
        }
        return formatEntries;
    }

    private static boolean isFile(String file) {
        Pattern pattern = Pattern.compile("^([a-zA-Z0-9]+/)");
        Matcher matcher = pattern.matcher(file);
        return matcher.find();
    }

    private static boolean isRange(String file){
        Pattern pattern = Pattern.compile("^(@@) -\\d+(,\\d+)? \\+\\d+(,\\d+)? (@@)$");
        Matcher matcher = pattern.matcher(file);
        return matcher.find();
    }
}
