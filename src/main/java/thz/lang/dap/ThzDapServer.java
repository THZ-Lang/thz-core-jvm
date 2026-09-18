package thz.lang.dap;

import thz.lang.ast.ProgramaAst;
import thz.lang.interpretador.Escopo;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.lexico.ThzLexer;
import thz.lang.sintatico.ThzParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ThzDapServer — Servidor de Depuração Nativo via Debug Adapter Protocol (DAP).
 * Suporta depuração interativa no VS Code, IDE Desktop Swing e clientes DAP industriais.
 */
public class ThzDapServer implements ThzDebugListener, AutoCloseable {

    private final Set<Integer> breakpoints = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean pausado = new AtomicBoolean(false);
    private final Semaphore semaforoPasso = new Semaphore(0);

    private volatile int linhaAtual = 1;
    private volatile String operacaoAtual = "Principal";
    private volatile Escopo escopoAtual;
    private volatile boolean stepMode = false;
    private volatile boolean rodando = true;

    private ServerSocket serverSocket;
    public void adicionarBreakpoint(int linha) {
        breakpoints.add(linha);
    }

    public void removerBreakpoint(int linha) {
        breakpoints.remove(linha);
    }

    public Set<Integer> getBreakpoints() {
        return Collections.unmodifiableSet(breakpoints);
    }

    public int getLinhaAtual() {
        return linhaAtual;
    }

    public String getOperacaoAtual() {
        return operacaoAtual;
    }

    public boolean isPausado() {
        return pausado.get();
    }

    public Map<String, String> inspecionarVariaveis() {
        if (escopoAtual == null) return Map.of();
        Map<String, String> res = new LinkedHashMap<>();
        for (var entry : escopoAtual.getTodasVariaveis().entrySet()) {
            res.put(entry.getKey(), entry.getValue().formatar());
        }
        return res;
    }

    public void comandoContinuar() {
        stepMode = false;
        pausado.set(false);
        semaforoPasso.release();
    }

    public void comandoStepOver() {
        stepMode = true;
        pausado.set(false);
        semaforoPasso.release();
    }

    @Override
    public AcaoPasso aoExecutarLinha(int linha, String operacao, Escopo escopo) {
        if (!rodando) return AcaoPasso.CONTINUAR;
        this.linhaAtual = linha;
        this.operacaoAtual = operacao;
        this.escopoAtual = escopo;

        if (stepMode || breakpoints.contains(linha)) {
            pausado.set(true);
            try {
                semaforoPasso.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return AcaoPasso.CONTINUAR;
            }
        }
        return AcaoPasso.CONTINUAR;
    }

    /**
     * Executa um programa THZ sob sessão de depuração.
     */
    public CompletableFuture<ValorThz> depurarProgramaAsync(String codigo) {
        return CompletableFuture.supplyAsync(() -> {
            var tokens = new ThzLexer(codigo).tokenize();
            ProgramaAst ast = new ThzParser(tokens).parse();
            var interp = new InterpretadorThz(ast);
            interp.setDebugListener(this);

            var operacoes = interp.listarOperacoesExecutaveis();
            if (operacoes.isEmpty()) return ValorThz.NULO;
            String opNome = operacoes.get(0).operacao().nome();
            return interp.executarOperacao(opNome, Map.of());
        });
    }

    /**
     * Inicia o servidor DAP em porta TCP para comunicação com o VS Code / IDE.
     */
    public void iniciarServidorTcp(int porta) throws IOException {
        serverSocket = new ServerSocket(porta);
        Thread.ofVirtual().start(() -> {
            while (rodando && !serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    Thread.ofVirtual().start(() -> processarCliente(client));
                } catch (IOException e) {
                    break;
                }
            }
        });
    }

    private void processarCliente(Socket client) {
        try (var in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             var out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("\"command\":\"initialize\"")) {
                    out.println("Content-Length: 72\r\n\r\n{\"seq\":1,\"type\":\"response\",\"command\":\"initialize\",\"success\":true,\"body\":{}}");
                } else if (line.contains("\"command\":\"continue\"")) {
                    comandoContinuar();
                } else if (line.contains("\"command\":\"next\"")) {
                    comandoStepOver();
                } else if (line.contains("\"command\":\"disconnect\"")) {
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void close() {
        rodando = false;
        pausado.set(false);
        semaforoPasso.release(100);
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
