package com.neovision.clinico.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de la eliminación masiva de citas vencidas.")
public class EliminarVencidasResponse {
    @Schema(description = "Número de citas vencidas eliminadas.")
    private int eliminadas;
    @Schema(description = "Número de citas vencidas que no se pudieron eliminar (tienen una atención/historia clínica asociada).")
    private int omitidas;

    public EliminarVencidasResponse(int eliminadas, int omitidas) {
        this.eliminadas = eliminadas;
        this.omitidas = omitidas;
    }

    public int getEliminadas() { return eliminadas; }
    public void setEliminadas(int eliminadas) { this.eliminadas = eliminadas; }

    public int getOmitidas() { return omitidas; }
    public void setOmitidas(int omitidas) { this.omitidas = omitidas; }
}
