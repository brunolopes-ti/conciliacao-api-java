package br.com.brunolopes.conciliacao.integracao;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

class GeradorArquivosEntradaCobolTests {

    @Test
    void deveGerarArquivosNoFormatoEsperado()
            throws Exception {

        SnapshotExecucaoCobol snapshot =
                new SnapshotExecucaoCobol(
                        List.of(
                                item(
                                        "COB001",
                                        "100.50"
                                ),
                                item(
                                        "COB002",
                                        "200.00"
                                ),
                                item(
                                        "COB003",
                                        "75.25"
                                )
                        ),
                        List.of(
                                item(
                                        "COB001",
                                        "100.50"
                                ),
                                item(
                                        "COB002",
                                        "180.00"
                                ),
                                item(
                                        "COB001",
                                        "100.50"
                                ),
                                item(
                                        "COB999",
                                        "50.00"
                                )
                        )
                );

        try (DiretorioExecucaoCobol execucao =
                     DiretorioExecucaoCobol.criar()) {

            GeradorArquivosEntradaCobol.gerar(
                    execucao,
                    snapshot
            );

            String esperados =
                    Files.readString(
                            execucao.esperados(),
                            UTF_8
                    );

            String recebidos =
                    Files.readString(
                            execucao.recebidos(),
                            UTF_8
                    );

            assertEquals(
                    """
                    COB001;100.50
                    COB002;200.00
                    COB003;75.25
                    """,
                    esperados
            );

            assertEquals(
                    """
                    COB001;100.50
                    COB002;180.00
                    COB001;100.50
                    COB999;50.00
                    """,
                    recebidos
            );
        }
    }

    @Test
    void deveUsarLfSemBomNemCr()
            throws Exception {

        SnapshotExecucaoCobol snapshot =
                new SnapshotExecucaoCobol(
                        List.of(
                                item(
                                        "COB001",
                                        "1.00"
                                )
                        ),
                        List.of(
                                item(
                                        "COB001",
                                        "1.00"
                                )
                        )
                );

        try (DiretorioExecucaoCobol execucao =
                     DiretorioExecucaoCobol.criar()) {

            GeradorArquivosEntradaCobol.gerar(
                    execucao,
                    snapshot
            );

            validarBytes(
                    Files.readAllBytes(
                            execucao.esperados()
                    )
            );

            validarBytes(
                    Files.readAllBytes(
                            execucao.recebidos()
                    )
            );
        }
    }

    @Test
    void devePreservarOrdemEDuplicidades()
            throws Exception {

        SnapshotExecucaoCobol snapshot =
                new SnapshotExecucaoCobol(
                        List.of(
                                item(
                                        "COB-B",
                                        "20.00"
                                ),
                                item(
                                        "COB-A",
                                        "10.00"
                                )
                        ),
                        List.of(
                                item(
                                        "COB-A",
                                        "1.00"
                                ),
                                item(
                                        "COB-B",
                                        "2.00"
                                ),
                                item(
                                        "COB-A",
                                        "3.00"
                                )
                        )
                );

        try (DiretorioExecucaoCobol execucao =
                     DiretorioExecucaoCobol.criar()) {

            GeradorArquivosEntradaCobol.gerar(
                    execucao,
                    snapshot
            );

            List<String> pagamentos =
                    Files.readAllLines(
                            execucao.recebidos(),
                            UTF_8
                    );

            assertEquals(
                    List.of(
                            "COB-A;1.00",
                            "COB-B;2.00",
                            "COB-A;3.00"
                    ),
                    pagamentos
            );
        }
    }

    @Test
    void naoDeveSobrescreverArquivoExistente()
            throws Exception {

        SnapshotExecucaoCobol snapshot =
                new SnapshotExecucaoCobol(
                        List.of(
                                item(
                                        "COB001",
                                        "10.00"
                                )
                        ),
                        List.of(
                                item(
                                        "COB001",
                                        "10.00"
                                )
                        )
                );

        try (DiretorioExecucaoCobol execucao =
                     DiretorioExecucaoCobol.criar()) {

            Files.writeString(
                    execucao.recebidos(),
                    "NAO ALTERAR\n",
                    UTF_8
            );

            assertThrows(
                    IllegalStateException.class,
                    () ->
                            GeradorArquivosEntradaCobol
                                    .gerar(
                                            execucao,
                                            snapshot
                                    )
            );

            assertFalse(
                    Files.exists(
                            execucao.esperados()
                    )
            );

            assertEquals(
                    "NAO ALTERAR\n",
                    Files.readString(
                            execucao.recebidos(),
                            UTF_8
                    )
            );
        }
    }

    private void validarBytes(byte[] conteudo) {
        assertTrue(
                conteudo.length > 0
        );

        assertEquals(
                '\n',
                conteudo[conteudo.length - 1]
        );

        for (byte valor : conteudo) {
            assertFalse(
                    valor == '\r',
                    "Arquivo nao pode conter CR."
            );
        }

        boolean possuiBom =
                conteudo.length >= 3
                        && (conteudo[0] & 0xFF) == 0xEF
                        && (conteudo[1] & 0xFF) == 0xBB
                        && (conteudo[2] & 0xFF) == 0xBF;

        assertFalse(
                possuiBom,
                "Arquivo nao pode conter BOM UTF-8."
        );
    }

    private SnapshotExecucaoCobol.Item item(
            String identificador,
            String valor
    ) {
        return new SnapshotExecucaoCobol.Item(
                identificador,
                new BigDecimal(valor)
        );
    }
}
