package br.com.brunolopes.conciliacao.integracao;

public class FalhaExecucaoCobolException extends IllegalStateException {
    private final String diagnostico;

    public FalhaExecucaoCobolException(String mensagem) {
        this(mensagem, null, null);
    }
    public FalhaExecucaoCobolException(String mensagem, Throwable causa) {
        this(mensagem, causa, null);
    }
    public FalhaExecucaoCobolException(String mensagem, Throwable causa, String saida) {
        super(mensagem, causa);
        if (saida == null) {
            diagnostico = "";
        } else {
            // Limite em code points evita cortar um par surrogate.
            StringBuilder texto = new StringBuilder();
            saida.codePoints().limit(2000).forEach(c ->
                    texto.appendCodePoint(Character.isISOControl(c) ? ' ' : c));
            diagnostico = texto.toString();
        }
    }
    public String diagnostico() { return diagnostico; }
}
