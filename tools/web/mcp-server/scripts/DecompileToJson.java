// Ghidra headless post-analysis script.
// Decompiles all functions and writes structured JSON output.
// Usage: analyzeHeadless ... -postScript DecompileToJson.java <outputPath>
// @category Decompiler
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.decompiler.DecompiledFunction;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.listing.FunctionManager;

import java.io.FileWriter;
import java.io.PrintWriter;

public class DecompileToJson extends GhidraScript {

    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        String outputPath = args.length > 0
                ? args[0]
                : currentProgram.getExecutablePath() + "_decompiled.json";

        DecompInterface decompiler = new DecompInterface();
        DecompileOptions options = new DecompileOptions();
        decompiler.setOptions(options);
        decompiler.openProgram(currentProgram);

        FunctionManager fm = currentProgram.getFunctionManager();
        FunctionIterator it = fm.getFunctions(true);
        int total = fm.getFunctionCount();

        PrintWriter w = new PrintWriter(new FileWriter(outputPath));
        w.println("{");
        w.println("  \"binary\": \"" + esc(currentProgram.getExecutablePath()) + "\",");
        w.println("  \"architecture\": \"" + currentProgram.getLanguageID() + "\",");
        w.println("  \"compiler\": \"" + currentProgram.getCompilerSpec().getCompilerSpecID() + "\",");
        w.println("  \"functionCount\": " + total + ",");
        w.println("  \"functions\": [");

        int count = 0;
        while (it.hasNext()) {
            if (monitor.isCancelled()) break;
            Function func = it.next();
            count++;

            DecompileResults res = decompiler.decompileFunction(func, 120, monitor);

            String code = "";
            boolean ok = false;
            if (res != null && res.decompileCompleted()) {
                DecompiledFunction df = res.getDecompiledFunction();
                if (df != null) {
                    code = df.getC();
                    ok = true;
                }
            }

            if (count > 1) w.println(",");
            w.println("    {");
            w.println("      \"name\": \"" + esc(func.getName()) + "\",");
            w.println("      \"address\": \"0x" + func.getEntryPoint() + "\",");
            w.println("      \"signature\": \"" + esc(func.getSignature().getPrototypeString()) + "\",");
            w.println("      \"size\": " + func.getBody().getNumAddresses() + ",");
            w.println("      \"isThunk\": " + func.isThunk() + ",");
            w.println("      \"isExternal\": " + func.isExternal() + ",");
            w.println("      \"decompiled\": " + ok + ",");
            w.println("      \"code\": \"" + esc(code) + "\"");
            w.print("    }");

            if (count % 500 == 0) {
                println("Decompiled " + count + "/" + total + " functions...");
            }
        }

        w.println();
        w.println("  ]");
        w.println("}");
        w.close();
        decompiler.dispose();

        println("Done. Wrote " + count + " functions to " + outputPath);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
