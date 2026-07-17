// Ghidra headless post-analysis script: decompiles one or more WASM functions
// (by wasm function index) to C pseudocode and writes them as a JSON array.
// Usage: analyzeHeadless ... -postScript DecompileWasmFuncsToJson.java <indicesFile> <outputPath>
//
// indicesFile is a plain text file of function indices separated by whitespace
// and/or commas. Decompiling a batch in one invocation amortizes the expensive
// whole-module auto-analysis pass over every requested function, instead of
// re-importing and re-analyzing the module once per function.
//
// Requires the nneonneo ghidra-wasm-plugin (provides wasm.WasmLoader and
// wasm.analysis.WasmAnalysis). The plugin addresses defined functions at
// CODE_BASE + code-entry offset and exposes WasmLoader.getFunctionAddress to map
// a wasm function index to a Ghidra Address. API targets plugin v2.x; adjust if
// a future plugin release changes these signatures.
// @category Decompiler
import ghidra.app.cmd.disassemble.DisassembleCommand;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.decompiler.DecompiledFunction;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSet;
import ghidra.program.model.listing.Function;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import wasm.WasmLoader;
import wasm.analysis.WasmAnalysis;
import wasm.analysis.WasmFunctionAnalysis;

public class DecompileWasmFuncsToJson extends GhidraScript {

    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        String indicesFile = args[0];
        String outputPath = args[1];

        String raw = new String(Files.readAllBytes(Paths.get(indicesFile)));
        List<Integer> indices = new ArrayList<>();
        for (String tok : raw.split("[\\s,]+")) {
            if (tok.isEmpty()) continue;
            try { indices.add(Integer.parseInt(tok.trim())); } catch (NumberFormatException e) {}
        }

        DecompInterface decompiler = new DecompInterface();
        decompiler.setOptions(new DecompileOptions());
        decompiler.openProgram(currentProgram);

        PrintWriter w = new PrintWriter(new FileWriter(outputPath));
        w.println("[");
        boolean first = true;
        for (int funcIndex : indices) {
            String name = "", signature = "", code = "", addrStr = "", error = "";
            boolean ok = false;
            try {
                Address addr = WasmLoader.getFunctionAddress(
                        currentProgram.getAddressFactory(),
                        WasmAnalysis.getState(currentProgram).getModule(),
                        funcIndex);
                addrStr = addr == null ? "" : addr.toString();
                Function func = addr == null ? null
                        : currentProgram.getFunctionManager().getFunctionAt(addr);
                if (addr == null) {
                    error = "no address mapped for wasm function index " + funcIndex;
                } else if (func == null) {
                    error = "no function defined at " + addr + " (index out of range or an import)";
                } else {
                    name = func.getName();
                    signature = func.getSignature().getPrototypeString();
                    if (currentProgram.getListing().getInstructionAt(addr) == null) {
                        WasmFunctionAnalysis fa = WasmAnalysis.getState(currentProgram).getFunctionAnalysis(addr);
                        if (fa != null) fa.applyContext(currentProgram, funcIndex);
                        AddressSet body = new AddressSet(addr, func.getBody().getMaxAddress());
                        new DisassembleCommand(addr, body, true).applyTo(currentProgram, monitor);
                    }
                    DecompileResults res = decompiler.decompileFunction(func, 120, monitor);
                    if (res != null && res.decompileCompleted()) {
                        DecompiledFunction df = res.getDecompiledFunction();
                        if (df != null) { code = df.getC(); ok = true; }
                        else error = "decompiler produced no C output";
                    } else {
                        String msg = res == null ? null : res.getErrorMessage();
                        error = msg != null && !msg.isEmpty() ? msg.trim() : "decompilation did not complete";
                    }
                }
            } catch (Exception e) {
                error = e.toString();
            }

            if (!first) w.println(",");
            first = false;
            w.print("  {");
            w.print("\"funcIndex\": " + funcIndex + ", ");
            w.print("\"name\": \"" + esc(name) + "\", ");
            w.print("\"address\": \"" + esc(addrStr) + "\", ");
            w.print("\"signature\": \"" + esc(signature) + "\", ");
            w.print("\"decompiled\": " + ok + ", ");
            w.print("\"error\": \"" + esc(error) + "\", ");
            w.print("\"code\": \"" + esc(code) + "\"");
            w.print("}");
        }
        w.println();
        w.println("]");
        w.close();
        decompiler.dispose();

        println("Wrote " + indices.size() + " decompiled function(s) to " + outputPath);
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
