package com.neovision.clinico.service;
//esta es la interfaz que permite llamar a la exportacion de datos 
public interface ExportService {
    byte[] exportarCSV(String estado);
    byte[] exportarExcel(String estado);
    byte[] exportarPDF(String estado);
}
